package com.kinly.famapp.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinly.famapp.data.models.Message
import com.kinly.famapp.features.family.FamilyRepository
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

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val members: Map<String, String> = emptyMap(), // userId -> имя (для группового чата)
    val isLoading: Boolean = false,
    val isSending: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val storageRepository: StorageRepository,
    private val familyRepository: FamilyRepository,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var familyId: String = ""
    private var meId: String = ""
    private var otherId: String? = null // null → семейный (групповой) чат
    private var started = false

    private val isGroup: Boolean get() = otherId == null
    private val chatKey: String get() = otherId ?: GROUP_CHAT_KEY

    /** otherId = null → открыть семейный (групповой) чат. */
    fun start(familyId: String, meId: String, otherId: String?) {
        if (started && this.otherId == otherId) return
        started = true
        this.familyId = familyId
        this.meId = meId
        this.otherId = otherId
        if (isGroup) loadMembers()
        refresh()
        subscribe()
    }

    private fun loadMembers() {
        viewModelScope.launch {
            val map = familyRepository.getMembers(familyId).associate { it.userId to it.displayName }
            _uiState.value = _uiState.value.copy(members = map)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val msgs = if (isGroup) chatRepository.getGroup(familyId)
                       else chatRepository.getDialog(meId, otherId!!)
            _uiState.value = _uiState.value.copy(messages = msgs)
            // Чат открыт — считаем прочитанным.
            chatRepository.markRead(familyId, chatKey)
        }
    }

    private var channel: io.github.jan.supabase.realtime.RealtimeChannel? = null

    private fun subscribe() {
        viewModelScope.launch {
            val ch = supabase.realtime.channel("chat-$meId-$chatKey")
            channel = ch
            ch.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "messages"
            }.onEach { refresh() }.launchIn(this)
            ch.subscribe()
        }
    }

    override fun onCleared() {
        // Не оставляем подписку висеть после закрытия чата.
        val ch = channel ?: return
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching { supabase.realtime.removeChannel(ch) }
        }
    }

    fun sendText(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        viewModelScope.launch {
            runCatching { chatRepository.send(familyId, otherId, body = t) }
            refresh()
        }
    }

    fun sendImage(bytes: ByteArray) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            runCatching {
                val url = storageRepository.uploadChatImage(familyId, bytes)
                chatRepository.send(familyId, otherId, imageUrl = url)
            }
            _uiState.value = _uiState.value.copy(isSending = false)
            refresh()
        }
    }

    fun sendAudio(bytes: ByteArray) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            runCatching {
                val url = storageRepository.uploadChatAudio(familyId, bytes)
                chatRepository.send(familyId, otherId, audioUrl = url)
            }
            _uiState.value = _uiState.value.copy(isSending = false)
            refresh()
        }
    }

    fun isMine(m: Message): Boolean = m.senderId == meId
    fun senderName(m: Message): String = _uiState.value.members[m.senderId] ?: "Участник"
}
