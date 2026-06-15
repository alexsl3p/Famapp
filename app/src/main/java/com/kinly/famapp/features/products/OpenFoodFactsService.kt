package com.kinly.famapp.features.products

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Данные о продукте, полученные из Open Food Facts. */
data class ProductInfo(
    val name: String,
    val brand: String? = null,
    val packageSize: String? = null,
    val imageUrl: String? = null
)

@Serializable
private data class OffResponse(
    val status: String? = null,
    val product: OffProduct? = null
)

@Serializable
private data class OffProduct(
    @SerialName("product_name") val productName: String? = null,
    val brands: String? = null,
    val quantity: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("image_front_url") val imageFrontUrl: String? = null
)

/**
 * Lookup товара в Open Food Facts по штрихкоду.
 * Возвращает null, если продукт не найден или нет осмысленного названия.
 */
@Singleton
class OpenFoodFactsService @Inject constructor() {

    private val client = HttpClient(Android)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun lookup(barcode: String): ProductInfo? = runCatching {
        val normalized = normalizeBarcode(barcode)
        // Retail product barcodes (EAN/UPC/ITF) are numeric. Reject anything
        // else (QR codes, Code-128 text, etc.) so a crafted scan value cannot
        // inject path/query metacharacters into the request URL.
        if (!BARCODE_PATTERN.matches(normalized)) return@runCatching null

        val body = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
            client
                .get("https://world.openfoodfacts.org/api/v3/product/$normalized.json") {
                    header("User-Agent", "KinlyFamilyOS/1.0 (Android)")
                }
                .bodyAsText()
        } ?: return@runCatching null

        val product = json.decodeFromString<OffResponse>(body).product
            ?: return@runCatching null

        val name = product.productName?.trim()?.takeIf { it.isNotEmpty() }
            ?: return@runCatching null

        ProductInfo(
            name = name,
            brand = product.brands
                ?.split(",")
                ?.firstOrNull()
                ?.trim()
                ?.takeIf { it.isNotEmpty() },
            packageSize = product.quantity?.trim()?.takeIf { it.isNotEmpty() },
            // Only trust https image URLs from Open Food Facts' image host —
            // never auto-load/persist an arbitrary URL from the response.
            imageUrl = (product.imageFrontUrl ?: product.imageUrl)
                ?.trim()
                ?.takeIf { it.startsWith("https://") && it.contains("openfoodfacts.org") }
        )
    }.getOrNull()

    private companion object {
        val BARCODE_PATTERN = Regex("\\d{6,14}")
        const val REQUEST_TIMEOUT_MS = 8_000L
    }
}
