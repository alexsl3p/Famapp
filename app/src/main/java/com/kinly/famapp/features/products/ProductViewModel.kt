package com.kinly.famapp.features.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinly.famapp.data.models.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductUiState(
    val products: List<Product> = emptyList(),
    val searchResults: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    private var currentFamilyId: String? = null

    init {
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query -> searchProducts(query) }
            .launchIn(viewModelScope)
    }

    fun load(familyId: String) {
        currentFamilyId = familyId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val products = productRepository.getProducts(familyId)
            _uiState.value = _uiState.value.copy(products = products, isLoading = false)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private suspend fun searchProducts(query: String) {
        val familyId = currentFamilyId ?: return
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }
        val results = productRepository.findByName(familyId, query)
        _uiState.value = _uiState.value.copy(searchResults = results)
    }

    fun createProduct(
        name: String,
        brand: String? = null,
        barcode: String? = null,
        productType: String = "other",
        onSuccess: (Product) -> Unit = {}
    ) {
        val familyId = currentFamilyId ?: return
        viewModelScope.launch {
            runCatching {
                val product = productRepository.createProduct(
                    familyId = familyId,
                    name = name,
                    brand = brand,
                    barcode = barcode,
                    productType = productType,
                    source = "manual"
                )
                val updated = _uiState.value.products + product
                _uiState.value = _uiState.value.copy(products = updated)
                onSuccess(product)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
