package com.kinly.famapp.features.shopping

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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShoppingUiState(
    val lists: List<ShoppingList> = emptyList(),
    val currentList: ShoppingList? = null,
    val items: List<ShoppingItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingUiState())
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    private var currentFamilyId: String? = null

    fun load(familyId: String) {
        currentFamilyId = familyId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val lists = shoppingRepository.getLists(familyId)
            val firstList = lists.firstOrNull()
            val items = if (firstList != null) shoppingRepository.getItems(firstList.id) else emptyList()
            _uiState.value = ShoppingUiState(lists = lists, currentList = firstList, items = items, isLoading = false)
            if (firstList != null) subscribeRealtime(familyId)
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
        val listId = _uiState.value.currentList?.id ?: return
        val items = shoppingRepository.getItems(listId)
        _uiState.value = _uiState.value.copy(items = items)
    }

    fun checkItem(itemId: String, checked: Boolean) {
        // Optimistic update
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map {
                if (it.id == itemId) it.copy(isChecked = checked) else it
            }
        )
        viewModelScope.launch {
            try {
                shoppingRepository.checkItem(itemId, checked)
            } catch (e: Exception) {
                refreshItems()
            }
        }
    }

    fun addItem(title: String, quantity: String? = null, unit: String? = null) {
        val familyId = currentFamilyId ?: return
        val listId = _uiState.value.currentList?.id ?: return
        viewModelScope.launch {
            try {
                shoppingRepository.addItem(familyId, listId, title, quantity, unit)
                refreshItems()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun clearChecked() {
        val listId = _uiState.value.currentList?.id ?: return
        viewModelScope.launch {
            shoppingRepository.clearChecked(listId)
            refreshItems()
        }
    }
}
