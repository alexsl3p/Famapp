package com.kinly.famapp.features.tasks

import com.kinly.famapp.data.models.Task
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

@Serializable
data class CompleteTaskResult(
    val success: Boolean = false,
    @kotlinx.serialization.SerialName("rolled_forward") val rolledForward: Boolean = false,
    @kotlinx.serialization.SerialName("new_due_date") val newDueDate: String? = null
)

@Singleton
class TasksRepository @Inject constructor(private val supabase: SupabaseClient) {

    suspend fun getTasks(familyId: String): List<Task> = runCatching {
        supabase.postgrest["tasks"]
            .select {
                filter { eq("family_id", familyId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<Task>()
    }.getOrElse { emptyList() }

    suspend fun createTask(
        familyId: String,
        title: String,
        description: String? = null,
        assignedTo: String? = null,
        dueDate: String? = null,
        repeatType: String = "none"
    ) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        supabase.postgrest.rpc(
            "create_task",
            buildJsonObject {
                put("p_family_id", familyId)
                put("p_title", title)
                if (description != null) put("p_description", description)
                if (assignedTo != null) put("p_assigned_to", assignedTo)
                if (dueDate != null) put("p_due_date", dueDate)
                put("p_repeat_type", repeatType)
                put("p_created_by", userId)
            }
        )
    }

    suspend fun completeTask(taskId: String): CompleteTaskResult = runCatching {
        supabase.postgrest.rpc(
            "complete_task",
            buildJsonObject { put("p_task_id", taskId) }
        ).decodeSingleOrNull<CompleteTaskResult>() ?: CompleteTaskResult(success = true)
    }.getOrElse { CompleteTaskResult(success = false) }

    suspend fun uncompleteTask(taskId: String) {
        runCatching {
            supabase.postgrest.rpc(
                "uncomplete_task",
                buildJsonObject { put("p_task_id", taskId) }
            )
        }
    }

    suspend fun deleteTask(taskId: String) {
        runCatching {
            supabase.postgrest["tasks"].delete {
                filter { eq("id", taskId) }
            }
        }
    }
}
