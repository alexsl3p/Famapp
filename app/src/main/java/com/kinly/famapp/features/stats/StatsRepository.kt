package com.kinly.famapp.features.stats

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class FrequentProduct(
    @SerialName("product_id") val productId: String,
    @SerialName("family_id") val familyId: String,
    @SerialName("product_name") val productName: String,
    val brand: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("purchase_count") val purchaseCount: Long,
    @SerialName("last_purchased_at") val lastPurchasedAt: String? = null
)

@Serializable
data class InventoryStatusRow(
    val id: String,
    @SerialName("family_id") val familyId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("product_name") val productName: String,
    val brand: String? = null,
    val quantity: Double,
    @SerialName("min_quantity") val minQuantity: Double? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    val status: String
)

@Serializable
data class PurchaseCadence(
    @SerialName("product_id") val productId: String,
    @SerialName("family_id") val familyId: String,
    @SerialName("product_name") val productName: String,
    @SerialName("total_purchases") val totalPurchases: Long,
    @SerialName("avg_days_between_purchases") val avgDaysBetween: Double? = null,
    @SerialName("last_purchased_at") val lastPurchasedAt: String? = null
)

@Singleton
class StatsRepository @Inject constructor(private val supabase: SupabaseClient) {

    suspend fun getFrequentProducts(familyId: String, limit: Int = 5): List<FrequentProduct> =
        runCatching {
            supabase.postgrest["v_frequent_products_90d"]
                .select {
                    filter { eq("family_id", familyId) }
                    order("purchase_count", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<FrequentProduct>()
        }.getOrElse { emptyList() }

    suspend fun getLowStockItems(familyId: String): List<InventoryStatusRow> =
        runCatching {
            supabase.postgrest["v_inventory_status"]
                .select {
                    filter {
                        eq("family_id", familyId)
                        neq("status", "ok")
                    }
                    order("status", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }
                .decodeList<InventoryStatusRow>()
        }.getOrElse { emptyList() }

    suspend fun getPurchaseCadence(familyId: String): List<PurchaseCadence> =
        runCatching {
            supabase.postgrest["v_purchase_cadence"]
                .select { filter { eq("family_id", familyId) } }
                .decodeList<PurchaseCadence>()
        }.getOrElse { emptyList() }

    /** Обнуляет статистику покупок (удаляет историю событий инвентаря семьи). */
    suspend fun resetStats(familyId: String): Result<Unit> = runCatching {
        supabase.postgrest.rpc(
            "reset_family_stats",
            buildJsonObject { put("p_family_id", familyId) }
        )
        Unit
    }
}
