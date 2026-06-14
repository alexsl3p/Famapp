package com.kinly.famapp.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinly.famapp.data.models.Profile
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
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    private val _signInError = MutableStateFlow<String?>(null)
    val signInError: StateFlow<String?> = _signInError.asStateFlow()

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

    fun clearSignInError() {
        _signInError.value = null
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
