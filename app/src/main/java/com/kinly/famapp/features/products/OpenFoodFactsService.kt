package com.kinly.famapp.features.products

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
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
        if (normalized.isBlank()) return@runCatching null

        val body = client
            .get("https://world.openfoodfacts.org/api/v3/product/$normalized.json") {
                header("User-Agent", "KinlyFamilyOS/1.0 (Android)")
            }
            .bodyAsText()

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
            imageUrl = (product.imageFrontUrl ?: product.imageUrl)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        )
    }.getOrNull()
}
