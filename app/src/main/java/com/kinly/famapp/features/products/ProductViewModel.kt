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

/** Результат поиска по отсканированному штрихкоду. */
sealed interface BarcodeLookup {
    /** В процессе запроса (локальный каталог + Open Food Facts). */
    data object Loading : BarcodeLookup

    /** Товар уже есть в семейном каталоге. */
    data class ExistingProduct(val product: Product) : BarcodeLookup

    /** Товар найден в Open Food Facts — предлагаем добавить в каталог. */
    data class Suggestion(
        val barcode: String,
        val name: String,
        val brand: String?,
        val packageSize: String?,
        val imageUrl: String?
    ) : BarcodeLookup

    /** Ничего не нашли — предлагаем создать вручную с этим штрихкодом. */
    data class NotFound(val barcode: String) : BarcodeLookup
}

@OptIn(FlowPreview::class)
@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val openFoodFacts: OpenFoodFactsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    private val _scanResult = MutableStateFlow<BarcodeLookup?>(null)
    val scanResult: StateFlow<BarcodeLookup?> = _scanResult.asStateFlow()

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

    /** Перезагрузка каталога (например, после автосоздания товаров покупками). */
    fun reload() {
        currentFamilyId?.let { load(it) }
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

    /** Удаление продукта из каталога (инвентарь по нему каскадно удаляется, в списках product_id обнуляется). */
    fun deleteProduct(productId: String) {
        _uiState.value = _uiState.value.copy(products = _uiState.value.products.filterNot { it.id == productId })
        viewModelScope.launch {
            runCatching { productRepository.deleteProduct(productId) }
                .onFailure { reload() }
        }
    }

    /**
     * Обрабатывает отсканированный штрихкод: сначала ищет в семейном каталоге,
     * затем в Open Food Facts. Результат публикуется в [scanResult].
     */
    fun onBarcodeScanned(barcode: String) {
        val familyId = currentFamilyId ?: return
        _scanResult.value = BarcodeLookup.Loading
        viewModelScope.launch {
            val existing = productRepository.findByBarcode(familyId, barcode)
            if (existing != null) {
                _scanResult.value = BarcodeLookup.ExistingProduct(existing)
                return@launch
            }
            val info = openFoodFacts.lookup(barcode)
            _scanResult.value = if (info != null) {
                BarcodeLookup.Suggestion(
                    barcode = barcode,
                    name = info.name,
                    brand = info.brand,
                    packageSize = info.packageSize,
                    imageUrl = info.imageUrl
                )
            } else {
                BarcodeLookup.NotFound(barcode)
            }
        }
    }

    fun clearScanResult() {
        _scanResult.value = null
    }

    /** Создаёт продукт из данных скана (Open Food Facts или ручной ввод). */
    fun createScannedProduct(
        name: String,
        barcode: String,
        brand: String? = null,
        packageSize: String? = null,
        imageUrl: String? = null,
        productType: String = "other",
        source: String = "openfoodfacts",
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
                    packageSize = packageSize,
                    productType = productType,
                    imageUrl = imageUrl,
                    source = source
                )
                _uiState.value = _uiState.value.copy(products = _uiState.value.products + product)
                onSuccess(product)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
