package com.kinly.famapp.features.notifications

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

data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private var userId: String? = null
    private var subscribed = false

    fun load(uid: String) {
        userId = uid
        refresh()
        subscribeRealtime(uid)
    }

    private fun refresh() {
        val uid = userId ?: return
        viewModelScope.launch {
            val list = repository.getNotifications(uid)
            _uiState.value = _uiState.value.copy(
                notifications = list,
                unreadCount = list.count { !it.isRead },
                isLoading = false
            )
        }
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
        _uiState.value = _uiState.value.copy(
            notifications = _uiState.value.notifications.map { it.copy(isRead = true) },
            unreadCount = 0
        )
        viewModelScope.launch { repository.markAllRead(uid) }
    }

    fun clearAll() {
        val uid = userId ?: return
        _uiState.value = _uiState.value.copy(notifications = emptyList(), unreadCount = 0)
        viewModelScope.launch { repository.clearAll(uid) }
    }
}
