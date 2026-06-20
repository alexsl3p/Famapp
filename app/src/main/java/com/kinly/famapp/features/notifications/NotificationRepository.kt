package com.kinly.famapp.features.notifications

import com.kinly.famapp.data.models.Notification
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(private val supabase: SupabaseClient) {

    suspend fun getNotifications(userId: String): List<Notification> = runCatching {
        supabase.postgrest["notifications"]
            .select {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
                limit(100L)
            }
            .decodeList<Notification>()
    }.getOrElse { emptyList() }

    suspend fun markAllRead(userId: String) {
        runCatching {
            supabase.postgrest["notifications"].update(
                buildJsonObject { put("is_read", true) }
            ) {
                filter {
                    eq("user_id", userId)
                    eq("is_read", false)
                }
            }
        }
    }

    suspend fun clearAll(userId: String) {
        runCatching {
            supabase.postgrest["notifications"].delete {
                filter { eq("user_id", userId) }
            }
        }
    }

    suspend fun acceptFamilyInvite(familyId: String): Boolean = runCatching {
        supabase.postgrest.rpc(
            "accept_family_invite",
            buildJsonObject { put("p_family_id", familyId) }
        )
        true
    }.getOrElse { false }

    suspend fun declineFamilyInvite(familyId: String) {
        runCatching {
            supabase.postgrest.rpc(
                "decline_family_invite",
                buildJsonObject { put("p_family_id", familyId) }
            )
        }
    }
}
