package com.kinly.famapp.features.shopping

import com.kinly.famapp.data.models.ShoppingItem
import com.kinly.famapp.data.models.ShoppingList
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingRepository @Inject constructor(private val supabase: SupabaseClient) {

    suspend fun getLists(familyId: String): List<ShoppingList> = runCatching {
        supabase.postgrest["shopping_lists"]
            .select { filter { eq("family_id", familyId) } }
            .decodeList<ShoppingList>()
    }.getOrElse { emptyList() }

    suspend fun getItems(listId: String): List<ShoppingItem> = runCatching {
        supabase.postgrest["shopping_items"]
            .select {
                filter { eq("list_id", listId) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<ShoppingItem>()
    }.getOrElse { emptyList() }

    /** Все товары семьи (по всем спискам сразу). */
    suspend fun getAllItems(familyId: String): List<ShoppingItem> = runCatching {
        supabase.postgrest["shopping_items"]
            .select {
                filter { eq("family_id", familyId) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<ShoppingItem>()
    }.getOrElse { emptyList() }

    suspend fun addItem(
        familyId: String,
        listId: String,
        title: String,
        quantity: String? = null,
        unit: String? = null,
        productId: String? = null
    ) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        supabase.postgrest["shopping_items"].insert(
            buildJsonObject {
                put("family_id", familyId)
                put("list_id", listId)
                put("title", title)
                if (quantity != null) put("quantity", quantity)
                if (unit != null) put("unit", unit)
                put("created_by", userId)
                if (productId != null) put("product_id", productId)
            }
        )
    }

    suspend fun updateQuantity(itemId: String, quantity: String) {
        supabase.postgrest["shopping_items"].update(
            buildJsonObject { put("quantity", quantity) }
        ) { filter { eq("id", itemId) } }
    }

    suspend fun checkItem(itemId: String, checked: Boolean) {        supabase.postgrest.rpc(
            "mark_shopping_item_checked",
            buildJsonObject {
                put("p_item_id", itemId)
                put("p_checked", checked)
            }
        )
    }

    suspend fun clearChecked(listId: String) {
        runCatching {
            supabase.postgrest["shopping_items"].delete {
                filter {
                    eq("list_id", listId)
                    eq("is_checked", true)
                }
            }
        }
    }

    suspend fun deleteItem(itemId: String) {
        runCatching {
            supabase.postgrest["shopping_items"].delete {
                filter { eq("id", itemId) }
            }
        }
    }

    suspend fun createList(familyId: String, title: String): ShoppingList {
        val userId = supabase.auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
        return supabase.postgrest["shopping_lists"].insert(
            buildJsonObject {
                put("family_id", familyId)
                put("title", title)
                put("created_by", userId)
            }
        ) { select() }.decodeSingle<ShoppingList>()
    }
}
