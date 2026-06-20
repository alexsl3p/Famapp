package com.kinly.famapp.features.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinly.famapp.data.models.Family
import com.kinly.famapp.data.models.FamilyMember
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

data class FamilyUiState(
    val family: Family? = null,
    val members: List<FamilyMember> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inviteMessage: String? = null,
    val isInviting: Boolean = false
)

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyUiState())
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

    private var currentFamilyId: String? = null
    private var subscribed = false

    fun load(familyId: String) {
        currentFamilyId = familyId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val family = familyRepository.getFamily(familyId)
            val members = familyRepository.getMembers(familyId)
            _uiState.value = _uiState.value.copy(family = family, members = members, isLoading = false)
            subscribeRealtime(familyId)
        }
    }

    /** Перечитать участников (например, при открытии вкладки «Семья»). */
    fun reload() {
        val familyId = currentFamilyId ?: return
        viewModelScope.launch {
            val members = familyRepository.getMembers(familyId)
            _uiState.value = _uiState.value.copy(members = members)
        }
    }

    private fun subscribeRealtime(familyId: String) {
        if (subscribed) return
        subscribed = true
        viewModelScope.launch {
            val channel = supabase.realtime.channel("family-members-$familyId")
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "family_members"
            }.onEach { reload() }.launchIn(this)
            channel.subscribe()
        }
    }

    fun createFamily(name: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val family = familyRepository.createFamily(name)
                _uiState.value = FamilyUiState(family = family, isLoading = false)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun joinFamily(code: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                familyRepository.joinFamily(code)
                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    /** Пригласить пользователя по email — ему придёт уведомление с подтверждением. */
    fun inviteByEmail(email: String) {
        val familyId = _uiState.value.family?.id ?: return
        if (email.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isInviting = true, inviteMessage = null)
            val result = familyRepository.inviteByEmail(familyId, email.trim())
            val msg = when (result) {
                "ok" -> "Приглашение отправлено — ждём подтверждения"
                "user_not_found" -> "Нет пользователя с таким email (нужно, чтобы он зарегистрировался)"
                "already_member" -> "Этот человек уже в семье"
                else -> "Не удалось отправить приглашение"
            }
            _uiState.value = _uiState.value.copy(isInviting = false, inviteMessage = msg)
        }
    }

    fun clearInviteMessage() {
        _uiState.value = _uiState.value.copy(inviteMessage = null)
    }

    fun regenerateCode() {
        val familyId = _uiState.value.family?.id ?: return
        viewModelScope.launch {
            val newCode = familyRepository.regenerateInviteCode(familyId)
            _uiState.value = _uiState.value.copy(
                family = _uiState.value.family?.copy(inviteCode = newCode)
            )
        }
    }
}
