package com.kinly.famapp.features.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsUiState(
    val frequentProducts: List<FrequentProduct> = emptyList(),
    val lowStockItems: List<InventoryStatusRow> = emptyList(),
    val cadence: List<PurchaseCadence> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private var currentFamilyId: String? = null

    fun load(familyId: String) {
        currentFamilyId = familyId
        refresh()
    }

    fun refresh() {
        val familyId = currentFamilyId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val frequentDeferred = async { statsRepository.getFrequentProducts(familyId) }
            val lowStockDeferred = async { statsRepository.getLowStockItems(familyId) }
            val cadenceDeferred = async { statsRepository.getPurchaseCadence(familyId) }
            _uiState.value = StatsUiState(
                frequentProducts = frequentDeferred.await(),
                lowStockItems = lowStockDeferred.await(),
                cadence = cadenceDeferred.await(),
                isLoading = false
            )
        }
    }
}
