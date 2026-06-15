package com.kinly.famapp.features.products

import com.kinly.famapp.data.models.Product
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

private val rpcJson = Json { ignoreUnknownKeys = true }

@Singleton
class ProductRepository @Inject constructor(private val supabase: SupabaseClient) {

    suspend fun getProducts(familyId: String): List<Product> = runCatching {
        supabase.postgrest["products"]
            .select { filter { eq("family_id", familyId) } }
            .decodeList<Product>()
    }.getOrElse { emptyList() }

    suspend fun findByName(familyId: String, query: String): List<Product> = runCatching {
        supabase.postgrest["products"]
            .select {
                filter {
                    eq("family_id", familyId)
                    ilike("name", "%$query%")
                }
            }
            .decodeList<Product>()
    }.getOrElse { emptyList() }

    suspend fun findByBarcode(familyId: String, barcode: String): Product? = runCatching {
        val normalized = normalizeBarcode(barcode)
        supabase.postgrest["products"]
            .select {
                filter {
                    eq("family_id", familyId)
                    eq("barcode_normalized", normalized)
                }
                limit(1)
            }
            .decodeList<Product>()
            .firstOrNull()
    }.getOrNull()

    suspend fun createProduct(
        familyId: String,
        name: String,
        brand: String? = null,
        barcode: String? = null,
        packageSize: String? = null,
        defaultUnit: String? = null,
        productType: String = "other",
        imageUrl: String? = null,
        source: String? = null
    ): Product {
        val result = supabase.postgrest.rpc(
            "create_product",
            buildJsonObject {
                put("p_family_id", familyId)
                put("p_name", name)
                if (brand != null) put("p_brand", brand)
                if (barcode != null) put("p_barcode", barcode)
                if (packageSize != null) put("p_package_size", packageSize)
                if (defaultUnit != null) put("p_default_unit", defaultUnit)
                put("p_product_type", productType)
                if (imageUrl != null) put("p_image_url", imageUrl)
                if (source != null) put("p_source", source)
            }
        )
        return rpcJson.decodeFromString<Product>(result.data)
    }
}

fun normalizeBarcode(raw: String): String =
    raw.trim().uppercase().replace("-", "").replace(" ", "")
