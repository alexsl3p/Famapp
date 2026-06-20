package com.kinly.famapp.features.tasks

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Черновик новой задачи — переживает сворачивание и выгрузку процесса. */
data class TaskDraft(
    val open: Boolean = false,
    val title: String = "",
    val description: String = "",
    val dueDate: String? = null,
    val assignedTo: String? = null,
    val taskType: String = "current",
    val isPriority: Boolean = false
)

private val K_OPEN = booleanPreferencesKey("task_draft_open")
private val K_TITLE = stringPreferencesKey("task_draft_title")
private val K_DESC = stringPreferencesKey("task_draft_desc")
private val K_DUE = stringPreferencesKey("task_draft_due")
private val K_ASSIGNEE = stringPreferencesKey("task_draft_assignee")
private val K_TYPE = stringPreferencesKey("task_draft_type")
private val K_PRIORITY = booleanPreferencesKey("task_draft_priority")

@HiltViewModel
class TaskDraftViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    val draft: StateFlow<TaskDraft> = dataStore.data
        .map { p ->
            TaskDraft(
                open = p[K_OPEN] ?: false,
                title = p[K_TITLE] ?: "",
                description = p[K_DESC] ?: "",
                dueDate = p[K_DUE]?.ifBlank { null },
                assignedTo = p[K_ASSIGNEE]?.ifBlank { null },
                taskType = p[K_TYPE] ?: "current",
                isPriority = p[K_PRIORITY] ?: false
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, TaskDraft())

    fun open() = edit { it[K_OPEN] = true }
    fun setTitle(v: String) = edit { it[K_TITLE] = v }
    fun setDescription(v: String) = edit { it[K_DESC] = v }
    fun setDueDate(v: String?) = edit { it[K_DUE] = v ?: "" }
    fun setAssignee(v: String?) = edit { it[K_ASSIGNEE] = v ?: "" }
    fun setTaskType(v: String) = edit { it[K_TYPE] = v }
    fun setPriority(v: Boolean) = edit { it[K_PRIORITY] = v }

    /** Закрыть и очистить черновик. */
    fun close() = edit {
        it[K_OPEN] = false
        it.remove(K_TITLE); it.remove(K_DESC); it.remove(K_DUE)
        it.remove(K_ASSIGNEE); it.remove(K_TYPE); it.remove(K_PRIORITY)
    }

    private fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        viewModelScope.launch { dataStore.edit(block) }
    }
}
