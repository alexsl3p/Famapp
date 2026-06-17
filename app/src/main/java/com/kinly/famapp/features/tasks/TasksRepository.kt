package com.kinly.famapp.features.tasks

import com.kinly.famapp.data.models.Task
import com.kinly.famapp.data.models.TaskComment
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

private val rpcJson = Json { ignoreUnknownKeys = true }

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

    /** Создаёт задачу и возвращает её id (для прикрепления фото-комментария). */
    suspend fun createTask(
        familyId: String,
        title: String,
        description: String? = null,
        assignedTo: String? = null,
        dueDate: String? = null,
        repeatType: String = "none",
        isPriority: Boolean = false
    ): String? {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return null
        val result = supabase.postgrest.rpc(
            "create_task",
            buildJsonObject {
                put("p_family_id", familyId)
                put("p_title", title)
                if (description != null) put("p_description", description)
                if (assignedTo != null) put("p_assigned_to", assignedTo)
                if (dueDate != null) put("p_due_date", dueDate)
                put("p_repeat_type", repeatType)
                put("p_created_by", userId)
                put("p_is_priority", isPriority)
            }
        )
        // RPC возвращает uuid в виде JSON-строки, напр. "\"<uuid>\""
        return result.data.trim().trim('"').takeIf { it.isNotBlank() && it != "null" }
    }

    /** Множество id задач, у которых есть прикреплённое фото/файл (комментарий с image_url). */
    suspend fun getTaskIdsWithAttachments(familyId: String): Set<String> = runCatching {
        supabase.postgrest["task_comments"]
            .select { filter { eq("family_id", familyId) } }
            .decodeList<TaskComment>()
            .filter { it.imageUrl != null }
            .map { it.taskId }
            .toSet()
    }.getOrElse { emptySet() }

    /** Полное редактирование задачи (название, описание, исполнитель, срок, повтор, приоритет). */
    suspend fun updateTask(
        taskId: String,
        title: String,
        description: String?,
        assignedTo: String?,
        dueDate: String?,
        repeatType: String,
        isPriority: Boolean
    ) {
        runCatching {
            supabase.postgrest["tasks"].update(
                buildJsonObject {
                    put("title", title)
                    put("description", description)
                    put("assigned_to", assignedTo)
                    put("due_date", dueDate)
                    put("repeat_type", repeatType)
                    put("is_priority", isPriority)
                }
            ) {
                filter { eq("id", taskId) }
            }
        }
    }

    suspend fun setPriority(taskId: String, isPriority: Boolean) {        runCatching {
            supabase.postgrest["tasks"].update(
                buildJsonObject { put("is_priority", isPriority) }
            ) {
                filter { eq("id", taskId) }
            }
        }
    }

    suspend fun getComments(taskId: String): List<TaskComment> = runCatching {        supabase.postgrest["task_comments"]
            .select {
                filter { eq("task_id", taskId) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<TaskComment>()
    }.getOrElse { emptyList() }

    suspend fun addComment(
        taskId: String,
        familyId: String,
        body: String?,
        imageUrl: String?
    ) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        supabase.postgrest["task_comments"].insert(
            buildJsonObject {
                put("task_id", taskId)
                put("family_id", familyId)
                put("author_id", userId)
                if (body != null) put("body", body)
                if (imageUrl != null) put("image_url", imageUrl)
            }
        )
    }

    suspend fun completeTask(taskId: String): CompleteTaskResult = runCatching {
        val result = supabase.postgrest.rpc(
            "complete_task",
            buildJsonObject { put("p_task_id", taskId) }
        )
        if (result.data.isBlank() || result.data == "null") CompleteTaskResult(success = true)
        else rpcJson.decodeFromString<CompleteTaskResult>(result.data)
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
