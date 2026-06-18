package com.kinly.famapp.features.inventory

import com.kinly.famapp.data.models.InventoryItem
import com.kinly.famapp.data.models.InventoryLocation
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

private val rpcJson = Json { ignoreUnknownKeys = true }

@Singleton
class InventoryRepository @Inject constructor(private val supabase: SupabaseClient) {

    suspend fun getItems(familyId: String): List<InventoryItem> = runCatching {
        supabase.postgrest["inventory_items"]
            .select {
                filter { eq("family_id", familyId) }
                order("updated_at", Order.DESCENDING)
            }
            .decodeList<InventoryItem>()
    }.getOrElse { emptyList() }

    suspend fun getLocations(familyId: String): List<InventoryLocation> = runCatching {
        supabase.postgrest["inventory_locations"]
            .select { filter { eq("family_id", familyId) } }
            .decodeList<InventoryLocation>()
    }.getOrElse { emptyList() }

    suspend fun upsertItem(familyId: String, productId: String, locationId: String? = null, quantityAdd: Double = 1.0): InventoryItem {
        val result = supabase.postgrest.rpc(
            "upsert_inventory_item",
            buildJsonObject {
                put("p_family_id", familyId)
                put("p_product_id", productId)
                if (locationId != null) put("p_location_id", locationId) else put("p_location_id", JsonNull)
                put("p_quantity_add", quantityAdd)
            }
        )
        return rpcJson.decodeFromString<InventoryItem>(result.data)
    }

    suspend fun updateQuantity(itemId: String, quantity: Double) {
        // Через RPC: при количестве 0 товар возвращается в список покупок.
        supabase.postgrest.rpc(
            "set_inventory_quantity",
            buildJsonObject {
                put("p_item_id", itemId)
                put("p_quantity", quantity)
            }
        )
    }

    suspend fun deleteItem(itemId: String) {
        runCatching {
            supabase.postgrest["inventory_items"].delete { filter { eq("id", itemId) } }
        }
    }

    suspend fun createLocation(familyId: String, name: String, icon: String? = null): InventoryLocation =
        supabase.postgrest["inventory_locations"].insert(
            buildJsonObject {
                put("family_id", familyId)
                put("name", name)
                if (icon != null) put("icon", icon)
            }
        ) { select() }.decodeSingle<InventoryLocation>()

    suspend fun getRecentEvents(familyId: String, limit: Int = 20): List<com.kinly.famapp.data.models.InventoryEvent> =
        runCatching {
            supabase.postgrest["inventory_events"]
                .select {
                    filter { eq("family_id", familyId) }
                    order("event_at", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<com.kinly.famapp.data.models.InventoryEvent>()
        }.getOrElse { emptyList() }
}
