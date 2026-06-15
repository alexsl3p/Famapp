package com.kinly.famapp.features.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinly.famapp.data.models.InventoryItem
import com.kinly.famapp.data.models.InventoryLocation
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

data class InventoryUiState(
    val items: List<InventoryItem> = emptyList(),
    val locations: List<InventoryLocation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private var currentFamilyId: String? = null

    fun load(familyId: String) {
        currentFamilyId = familyId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val items = inventoryRepository.getItems(familyId)
            val locations = inventoryRepository.getLocations(familyId)
            _uiState.value = InventoryUiState(items = items, locations = locations, isLoading = false)
            subscribeRealtime(familyId)
        }
    }

    /** Принудительная перезагрузка остатков (вызывается при открытии вкладки). */
    fun reload() {
        val familyId = currentFamilyId ?: return
        viewModelScope.launch { refreshItems() }
    }

    private fun subscribeRealtime(familyId: String) {
        viewModelScope.launch {
            val channel = supabase.realtime.channel("inventory-$familyId")
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "inventory_items"
            }.onEach { refreshItems() }.launchIn(this)
            channel.subscribe()
        }
    }

    private suspend fun refreshItems() {
        val familyId = currentFamilyId ?: return
        val items = inventoryRepository.getItems(familyId)
        _uiState.value = _uiState.value.copy(items = items)
    }

    fun updateQuantity(itemId: String, quantity: Double) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map {
                if (it.id == itemId) it.copy(quantity = quantity) else it
            }
        )
        viewModelScope.launch {
            runCatching { inventoryRepository.updateQuantity(itemId, quantity) }
                .onFailure { refreshItems() }
        }
    }

    fun createLocation(name: String, icon: String? = null) {
        val familyId = currentFamilyId ?: return
        viewModelScope.launch {
            runCatching {
                val location = inventoryRepository.createLocation(familyId, name, icon)
                _uiState.value = _uiState.value.copy(
                    locations = _uiState.value.locations + location
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
