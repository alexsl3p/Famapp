package com.kinly.famapp.features.chat

import com.kinly.famapp.data.models.Message
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/** Ключ группового (семейного) чата на стороне БД. */
const val GROUP_CHAT_KEY = "group"

@Serializable
private data class UnreadRow(val chat_key: String, val unread: Int)

@Singleton
class ChatRepository @Inject constructor(private val supabase: SupabaseClient) {

    /** Сообщения диалога между мной и собеседником, по возрастанию времени. */
    suspend fun getDialog(meId: String, otherId: String): List<Message> = runCatching {
        supabase.postgrest["messages"]
            .select {
                filter {
                    or {
                        and { eq("sender_id", meId); eq("recipient_id", otherId) }
                        and { eq("sender_id", otherId); eq("recipient_id", meId) }
                    }
                }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<Message>()
    }.getOrElse { emptyList() }

    /** Сообщения семейного (группового) чата — recipient_id пустой. */
    suspend fun getGroup(familyId: String): List<Message> = runCatching {
        supabase.postgrest["messages"]
            .select {
                filter { eq("family_id", familyId) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<Message>()
            .filter { it.recipientId == null }
    }.getOrElse { emptyList() }

    /** Отправка. recipientId = null → сообщение в семейный чат. */
    suspend fun send(
        familyId: String,
        recipientId: String?,
        body: String? = null,
        imageUrl: String? = null,
        audioUrl: String? = null
    ) {
        val me = supabase.auth.currentUserOrNull()?.id ?: return
        supabase.postgrest["messages"].insert(
            buildJsonObject {
                put("family_id", familyId)
                put("sender_id", me)
                if (recipientId != null) put("recipient_id", recipientId)
                if (body != null) put("body", body)
                if (imageUrl != null) put("image_url", imageUrl)
                if (audioUrl != null) put("audio_url", audioUrl)
            }
        )
    }

    /** Отметить чат прочитанным (chatKey = id собеседника или "group"). */
    suspend fun markRead(familyId: String, chatKey: String) {
        runCatching {
            supabase.postgrest.rpc(
                "mark_chat_read",
                buildJsonObject { put("p_family_id", familyId); put("p_chat_key", chatKey) }
            )
        }
    }

    /** Непрочитанные по чатам: ключ → количество (ключ = id отправителя или "group"). */
    suspend fun unreadCounts(familyId: String): Map<String, Int> = runCatching {
        supabase.postgrest.rpc(
            "chat_unread",
            buildJsonObject { put("p_family_id", familyId) }
        ).decodeList<UnreadRow>().associate { it.chat_key to it.unread }
    }.getOrElse { emptyMap() }
}
