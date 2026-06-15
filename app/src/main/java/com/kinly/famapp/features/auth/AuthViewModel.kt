package com.kinly.famapp.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinly.famapp.data.models.Profile
import com.kinly.famapp.features.storage.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class NeedsFamily(val profile: Profile) : AuthState()
    data class Authenticated(val profile: Profile) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    private val _signInError = MutableStateFlow<String?>(null)
    val signInError: StateFlow<String?> = _signInError.asStateFlow()

    private val _isSavingProfile = MutableStateFlow(false)
    val isSavingProfile: StateFlow<Boolean> = _isSavingProfile.asStateFlow()

    init {
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            authRepository.sessionFlow.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> resolveProfile()
                    is SessionStatus.NotAuthenticated -> _authState.value = AuthState.Unauthenticated
                    is SessionStatus.Initializing -> _authState.value = AuthState.Loading
                    is SessionStatus.RefreshFailure -> _authState.value = AuthState.Unauthenticated
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String, rawNonce: String) {
        viewModelScope.launch {
            _isSigningIn.value = true
            _signInError.value = null
            val result = authRepository.signInWithGoogle(idToken, rawNonce)
            if (result.isFailure) {
                _signInError.value = result.exceptionOrNull()?.message ?: "Sign-in failed"
                _isSigningIn.value = false
            }
            // On success, sessionFlow emits Authenticated which calls resolveProfile()
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun refreshProfile() {
        viewModelScope.launch { resolveProfile() }
    }

    /** Текущий профиль, если уже авторизованы (для экрана профиля). */
    fun currentProfile(): Profile? = when (val s = _authState.value) {
        is AuthState.Authenticated -> s.profile
        is AuthState.NeedsFamily -> s.profile
        else -> null
    }

    fun updateName(name: String, onDone: () -> Unit = {}) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _isSavingProfile.value = true
            runCatching { authRepository.updateName(name.trim()) }
            resolveProfile()
            _isSavingProfile.value = false
            onDone()
        }
    }

    fun updateAvatar(bytes: ByteArray, onDone: () -> Unit = {}) {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _isSavingProfile.value = true
            runCatching {
                val url = storageRepository.uploadAvatar(userId, bytes)
                authRepository.updateAvatarUrl(url)
            }
            resolveProfile()
            _isSavingProfile.value = false
            onDone()
        }
    }

    fun clearSignInError() {
        _signInError.value = null
    }

    fun setSignInError(message: String) {
        _signInError.value = message
        _isSigningIn.value = false
    }

    fun signInAsGuest() {
        _authState.value = AuthState.Authenticated(
            Profile(
                id = "guest-test-user",
                fullName = "Тест",
                activeFamilyId = "00000000-0000-0000-0000-000000000001"
            )
        )
    }

    private suspend fun resolveProfile() {
        _isSigningIn.value = false
        val profile = authRepository.getProfile()
        _authState.value = when {
            profile == null -> AuthState.Unauthenticated
            profile.activeFamilyId == null -> AuthState.NeedsFamily(profile)
            else -> AuthState.Authenticated(profile)
        }
    }
}
