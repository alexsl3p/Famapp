package com.kinly.famapp.features.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinly.famapp.data.models.Task
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

enum class TaskFilter { ALL, MINE, COMPLETED }

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val filter: TaskFilter = TaskFilter.ALL,
    val snackbarMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val tasksRepository: TasksRepository,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private var currentFamilyId: String? = null
    private var currentUserId: String? = null

    fun load(familyId: String, userId: String) {
        currentFamilyId = familyId
        currentUserId = userId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            refresh()
            subscribeRealtime(familyId)
        }
    }

    private suspend fun refresh() {
        val familyId = currentFamilyId ?: return
        val tasks = tasksRepository.getTasks(familyId)
        _uiState.value = _uiState.value.copy(tasks = tasks, isLoading = false)
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
        repeatType: String = "none"
    ) {
        val familyId = currentFamilyId ?: return
        viewModelScope.launch {
            try {
                tasksRepository.createTask(familyId, title, assignedTo = assignedTo, dueDate = dueDate, repeatType = repeatType)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun getFilteredTasks(): List<Task> {
        val tasks = _uiState.value.tasks
        val userId = currentUserId
        return when (_uiState.value.filter) {
            TaskFilter.ALL -> tasks.filter { !it.isCompleted }
            TaskFilter.MINE -> tasks.filter { !it.isCompleted && it.assignedTo == userId }
            TaskFilter.COMPLETED -> tasks.filter { it.isCompleted }
        }
    }
}
