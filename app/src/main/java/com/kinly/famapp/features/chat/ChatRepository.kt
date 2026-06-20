package com.kinly.famapp.features.chat

import com.kinly.famapp.data.models.Message
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

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

    suspend fun send(
        familyId: String,
        recipientId: String,
        body: String? = null,
        imageUrl: String? = null,
        audioUrl: String? = null
    ) {
        val me = supabase.auth.currentUserOrNull()?.id ?: return
        supabase.postgrest["messages"].insert(
            buildJsonObject {
                put("family_id", familyId)
                put("sender_id", me)
                put("recipient_id", recipientId)
                if (body != null) put("body", body)
                if (imageUrl != null) put("image_url", imageUrl)
                if (audioUrl != null) put("audio_url", audioUrl)
            }
        )
    }
}
