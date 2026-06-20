package com.kinly.famapp.features.notifications

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinly.famapp.data.models.Notification
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

/** Настройки: какие типы уведомлений показывать. */
data class NotificationSettings(
    val shopping: Boolean = true,
    val assigned: Boolean = true,
    val created: Boolean = true,
    val completed: Boolean = true
) {
    fun isEnabled(type: String): Boolean = when (type) {
        "shopping_added" -> shopping
        "task_assigned" -> assigned
        "task_created" -> created
        "task_completed" -> completed
        else -> true
    }
}

data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val settings: NotificationSettings = NotificationSettings()
)

private val KEY_SHOPPING = booleanPreferencesKey("notif_shopping")
private val KEY_ASSIGNED = booleanPreferencesKey("notif_assigned")
private val KEY_CREATED = booleanPreferencesKey("notif_created")
private val KEY_COMPLETED = booleanPreferencesKey("notif_completed")

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val supabase: SupabaseClient,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private var userId: String? = null
    private var subscribed = false

    private var rawList: List<Notification> = emptyList()
    private var settings = NotificationSettings()

    fun load(uid: String) {
        userId = uid
        observeSettings()
        refresh()
        subscribeRealtime(uid)
    }

    private fun observeSettings() {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                settings = NotificationSettings(
                    shopping = prefs[KEY_SHOPPING] ?: true,
                    assigned = prefs[KEY_ASSIGNED] ?: true,
                    created = prefs[KEY_CREATED] ?: true,
                    completed = prefs[KEY_COMPLETED] ?: true
                )
                emit()
            }
        }
    }

    private fun refresh() {
        val uid = userId ?: return
        viewModelScope.launch {
            rawList = repository.getNotifications(uid)
            emit()
        }
    }

    /** Пересобирает видимый список с учётом настроек. */
    private fun emit() {
        val filtered = rawList.filter { settings.isEnabled(it.type) }
        _uiState.value = _uiState.value.copy(
            notifications = filtered,
            unreadCount = filtered.count { !it.isRead },
            isLoading = false,
            settings = settings
        )
    }

    private fun subscribeRealtime(uid: String) {
        if (subscribed) return
        subscribed = true
        viewModelScope.launch {
            val channel = supabase.realtime.channel("notifications-$uid")
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "notifications"
            }.onEach { refresh() }.launchIn(this)
            channel.subscribe()
        }
    }

    /** Помечает все уведомления прочитанными (вызывается при открытии экрана). */
    fun markAllRead() {
        val uid = userId ?: return
        rawList = rawList.map { it.copy(isRead = true) }
        emit()
        viewModelScope.launch { repository.markAllRead(uid) }
    }

    /** Принять приглашение в семью; onAccepted вызывается для обновления профиля. */
    fun acceptInvite(familyId: String, onAccepted: () -> Unit) {
        viewModelScope.launch {
            val ok = repository.acceptFamilyInvite(familyId)
            refresh()
            if (ok) onAccepted()
        }
    }

    fun declineInvite(familyId: String) {
        viewModelScope.launch {
            repository.declineFamilyInvite(familyId)
            refresh()
        }
    }

    fun clearAll() {
        val uid = userId ?: return
        rawList = emptyList()
        emit()
        viewModelScope.launch { repository.clearAll(uid) }
    }

    fun setShopping(enabled: Boolean) = saveSetting(KEY_SHOPPING, enabled)
    fun setAssigned(enabled: Boolean) = saveSetting(KEY_ASSIGNED, enabled)
    fun setCreated(enabled: Boolean) = saveSetting(KEY_CREATED, enabled)
    fun setCompleted(enabled: Boolean) = saveSetting(KEY_COMPLETED, enabled)

    private fun saveSetting(key: Preferences.Key<Boolean>, enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[key] = enabled } }
    }
}
