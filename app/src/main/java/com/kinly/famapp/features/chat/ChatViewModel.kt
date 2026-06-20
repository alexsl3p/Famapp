package com.kinly.famapp.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinly.famapp.data.models.Message
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
    val isLoading: Boolean = false,
    val isSending: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val storageRepository: StorageRepository,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var familyId: String = ""
    private var meId: String = ""
    private var otherId: String = ""
    private var started = false

    fun start(familyId: String, meId: String, otherId: String) {
        if (started && this.otherId == otherId) return
        started = true
        this.familyId = familyId
        this.meId = meId
        this.otherId = otherId
        refresh()
        subscribe()
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(messages = chatRepository.getDialog(meId, otherId))
        }
    }

    private fun subscribe() {
        viewModelScope.launch {
            val channel = supabase.realtime.channel("chat-$meId-$otherId")
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "messages"
            }.onEach { refresh() }.launchIn(this)
            channel.subscribe()
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
}
