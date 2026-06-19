package com.kinly.famapp.features.shopping

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinly.famapp.data.models.ShoppingItem
import com.kinly.famapp.data.models.ShoppingList
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

private val CACHED_ITEMS_KEY = stringPreferencesKey("shopping_items_cache")
private val cacheJson = Json { ignoreUnknownKeys = true }

data class ShoppingUiState(
    val lists: List<ShoppingList> = emptyList(),
    val currentList: ShoppingList? = null,
    val items: List<ShoppingItem> = emptyList(),
    val itemsByList: Map<String, List<ShoppingItem>> = emptyMap(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val supabase: SupabaseClient,
    private val dataStore: DataStore<Preferences>,
    private val widgetUpdater: com.kinly.famapp.widget.WidgetUpdater
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingUiState())
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    private var currentFamilyId: String? = null

    fun load(familyId: String) {
        currentFamilyId = familyId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val lists = shoppingRepository.getLists(familyId)
                val all = shoppingRepository.getAllItems(familyId)
                saveToCache(all)
                _uiState.value = ShoppingUiState(
                    lists = lists,
                    currentList = lists.firstOrNull(),
                    items = all,
                    itemsByList = all.groupBy { it.listId },
                    isLoading = false
                )
                pushWidget(all)
                subscribeRealtime(familyId)
            } catch (e: Exception) {
                val cached = loadFromCache()
                _uiState.value = _uiState.value.copy(
                    items = cached,
                    itemsByList = cached.groupBy { it.listId },
                    isLoading = false,
                    isOffline = cached.isNotEmpty()
                )
            }
        }
    }

    private fun subscribeRealtime(familyId: String) {
        viewModelScope.launch {
            val channel = supabase.realtime.channel("shopping-$familyId")
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "shopping_items"
            }.onEach { refreshItems() }.launchIn(this)
            channel.subscribe()
        }
    }

    private suspend fun refreshItems() {
        val familyId = currentFamilyId ?: return
        val all = shoppingRepository.getAllItems(familyId)
        saveToCache(all)
        _uiState.value = _uiState.value.copy(
            items = all,
            itemsByList = all.groupBy { it.listId },
            isOffline = false
        )
        pushWidget(all)
    }

    /** Кладёт активные (некупленные) товары в снимок виджета. */
    private fun pushWidget(items: List<ShoppingItem>) {
        widgetUpdater.updateShopping(items.filter { !it.isChecked }.map { it.title })
    }

    private suspend fun saveToCache(items: List<ShoppingItem>) {
        runCatching {
            dataStore.edit { it[CACHED_ITEMS_KEY] = cacheJson.encodeToString(items) }
        }
    }

    private suspend fun loadFromCache(): List<ShoppingItem> = runCatching {
        val json = dataStore.data.first()[CACHED_ITEMS_KEY] ?: return@runCatching emptyList()
        cacheJson.decodeFromString<List<ShoppingItem>>(json)
    }.getOrElse { emptyList() }

    fun checkItem(itemId: String, checked: Boolean) {
        val newItems = _uiState.value.items.map {
            if (it.id == itemId) it.copy(isChecked = checked) else it
        }
        _uiState.value = _uiState.value.copy(
            items = newItems,
            itemsByList = newItems.groupBy { it.listId }
        )
        viewModelScope.launch {
            try {
                shoppingRepository.checkItem(itemId, checked)
                refreshItems()
            } catch (e: Exception) {
                refreshItems()
            }
        }
    }

    fun addItem(listId: String, title: String, quantity: String? = null, unit: String? = null, productId: String? = null) {
        val familyId = currentFamilyId ?: return
        viewModelScope.launch {
            try {
                shoppingRepository.addItem(familyId, listId, title, quantity, unit, productId)
                refreshItems()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /** Удаление товара из списка. */
    fun deleteItem(itemId: String) {
        val newItems = _uiState.value.items.filterNot { it.id == itemId }
        _uiState.value = _uiState.value.copy(items = newItems, itemsByList = newItems.groupBy { it.listId })
        viewModelScope.launch {
            runCatching { shoppingRepository.deleteItem(itemId) }.onFailure { refreshItems() }
        }
    }

    /** Выбор активного списка для дропдауна (на экране показывается один список). */
    fun selectList(listId: String) {
        val list = _uiState.value.lists.firstOrNull { it.id == listId } ?: return
        _uiState.value = _uiState.value.copy(currentList = list)
    }

    /** Изменение количества товара (минимум 1). */
    fun updateItemQuantity(itemId: String, quantity: Int) {
        val q = quantity.coerceAtLeast(1).toString()
        val newItems = _uiState.value.items.map {
            if (it.id == itemId) it.copy(quantity = q) else it
        }
        _uiState.value = _uiState.value.copy(items = newItems, itemsByList = newItems.groupBy { it.listId })
        viewModelScope.launch {
            runCatching { shoppingRepository.updateQuantity(itemId, q) }.onFailure { refreshItems() }
        }
    }

    fun clearChecked(listId: String) {        viewModelScope.launch {
            shoppingRepository.clearChecked(listId)
            refreshItems()
        }
    }

    fun createList(title: String) {
        val familyId = currentFamilyId ?: return
        viewModelScope.launch {
            try {
                val list = shoppingRepository.createList(familyId, title)
                _uiState.value = _uiState.value.copy(
                    lists = _uiState.value.lists + list,
                    currentList = _uiState.value.currentList ?: list
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /** Удаление списка покупок вместе с его товарами. */
    fun deleteList(listId: String) {
        val remaining = _uiState.value.lists.filterNot { it.id == listId }
        val newCurrent = if (_uiState.value.currentList?.id == listId) remaining.firstOrNull()
                         else _uiState.value.currentList
        val newItems = _uiState.value.items.filterNot { it.listId == listId }
        _uiState.value = _uiState.value.copy(
            lists = remaining,
            currentList = newCurrent,
            items = newItems,
            itemsByList = newItems.groupBy { it.listId }
        )
        viewModelScope.launch {
            shoppingRepository.deleteList(listId)
            refreshItems()
        }
    }
}
