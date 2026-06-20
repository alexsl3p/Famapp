package com.kinly.famapp.features.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinly.famapp.data.models.Task
import com.kinly.famapp.data.models.TaskComment
import com.kinly.famapp.features.storage.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TaskFilter { ALL, CURRENT, DAILY, LONGTERM, COMPLETED }

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val attachmentTaskIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val filter: TaskFilter = TaskFilter.ALL,
    val snackbarMessage: String? = null,
    val error: String? = null
)

data class CommentsUiState(
    val taskId: String? = null,
    val comments: List<TaskComment> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val tasksRepository: TasksRepository,
    private val storageRepository: StorageRepository,
    private val supabase: SupabaseClient,
    private val widgetUpdater: com.kinly.famapp.widget.WidgetUpdater
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private val _commentsState = MutableStateFlow(CommentsUiState())
    val commentsState: StateFlow<CommentsUiState> = _commentsState.asStateFlow()

    private var currentFamilyId: String? = null
    private var currentUserId: String? = null

    fun load(familyId: String, userId: String) {
        currentFamilyId = familyId
        currentUserId = userId
        widgetUpdater.setFamily(familyId)
        widgetUpdater.setUser(userId)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            refresh()
            subscribeRealtime(familyId)
        }
    }

    private suspend fun refresh() {
        val familyId = currentFamilyId ?: return
        val tasks = tasksRepository.getTasks(familyId)
        val attachments = tasksRepository.getTaskIdsWithAttachments(familyId)
        _uiState.value = _uiState.value.copy(tasks = tasks, attachmentTaskIds = attachments, isLoading = false)
        widgetUpdater.updateTasks(tasks.filter { !it.isCompleted }.map { it.title })
    }

    private fun subscribeRealtime(familyId: String) {
        viewModelScope.launch {
            val channel = supabase.realtime.channel("tasks-$familyId")
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "tasks"
            }.onEach { refresh() }.launchIn(this)
            channel.subscribe()
        }
    }

    fun setFilter(filter: TaskFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            try {
                val result = tasksRepository.completeTask(taskId)
                if (result.rolledForward) {
                    val msg = if (result.newDueDate != null)
                        "Повторяющаяся задача перенесена на ${result.newDueDate}"
                    else "Задача перенесена"
                    _uiState.value = _uiState.value.copy(snackbarMessage = msg)
                }
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun uncompleteTask(taskId: String) {
        viewModelScope.launch {
            tasksRepository.uncompleteTask(taskId)
            refresh()
        }
    }

    fun createTask(
        title: String,
        assignedTo: String? = null,
        dueDate: String? = null,
        repeatType: String = "none",
        taskType: String = "current",
        description: String? = null,
        isPriority: Boolean = false,
        photoBytes: ByteArray? = null
    ) {
        val familyId = currentFamilyId ?: return
        viewModelScope.launch {
            try {
                val taskId = tasksRepository.createTask(
                    familyId, title,
                    description = description,
                    assignedTo = assignedTo,
                    dueDate = dueDate,
                    repeatType = repeatType,
                    taskType = taskType,
                    isPriority = isPriority
                )
                // Если при создании прикрепили фото — кладём его первым комментарием.
                if (taskId != null && photoBytes != null) {
                    runCatching {
                        val url = storageRepository.uploadTaskPhoto(familyId, photoBytes)
                        tasksRepository.addComment(taskId, familyId, body = null, imageUrl = url)
                    }
                }
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateTask(
        taskId: String,
        title: String,
        assignedTo: String?,
        dueDate: String?,
        repeatType: String,
        taskType: String,
        description: String?,
        isPriority: Boolean
    ) {
        viewModelScope.launch {
            try {
                tasksRepository.updateTask(taskId, title, description, assignedTo, dueDate, repeatType, taskType, isPriority)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /** Удаление задачи. */
    fun deleteTask(taskId: String) {
        val newTasks = _uiState.value.tasks.filterNot { it.id == taskId }
        _uiState.value = _uiState.value.copy(tasks = newTasks)
        viewModelScope.launch {
            try {
                tasksRepository.deleteTask(taskId)
                refresh()
            } catch (e: Exception) {
                refresh()
            }
        }
    }

    fun togglePriority(taskId: String, isPriority: Boolean) {
        viewModelScope.launch {
            tasksRepository.setPriority(taskId, isPriority)
            refresh()
        }
    }

    fun openComments(taskId: String) {
        _commentsState.value = CommentsUiState(taskId = taskId, isLoading = true)
        viewModelScope.launch {
            val comments = tasksRepository.getComments(taskId)
            _commentsState.value = _commentsState.value.copy(comments = comments, isLoading = false)
        }
    }

    fun closeComments() {
        _commentsState.value = CommentsUiState()
    }

    fun addComment(body: String?, photoBytes: ByteArray?) {
        val familyId = currentFamilyId ?: return
        val taskId = _commentsState.value.taskId ?: return
        if (body.isNullOrBlank() && photoBytes == null) return
        _commentsState.value = _commentsState.value.copy(isSending = true)
        viewModelScope.launch {
            try {
                val imageUrl = photoBytes?.let { storageRepository.uploadTaskPhoto(familyId, it) }
                tasksRepository.addComment(taskId, familyId, body?.takeIf { it.isNotBlank() }, imageUrl)
                val comments = tasksRepository.getComments(taskId)
                _commentsState.value = _commentsState.value.copy(comments = comments, isSending = false)
                if (imageUrl != null) refresh() // обновить индикатор вложения на карточке
            } catch (e: Exception) {
                _commentsState.value = _commentsState.value.copy(isSending = false)
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun getFilteredTasks(): List<Task> {
        val tasks = _uiState.value.tasks
        return when (_uiState.value.filter) {
            TaskFilter.ALL -> tasks.filter { !it.isCompleted }
            TaskFilter.CURRENT -> tasks.filter { !it.isCompleted && it.taskType == "current" }
            TaskFilter.DAILY -> tasks.filter { !it.isCompleted && it.taskType == "daily" }
            TaskFilter.LONGTERM -> tasks.filter { !it.isCompleted && it.taskType == "longterm" }
            TaskFilter.COMPLETED -> tasks.filter { it.isCompleted }
        }
    }
}
