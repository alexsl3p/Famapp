package com.kinly.famapp.features.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinly.famapp.data.models.Family
import com.kinly.famapp.data.models.FamilyMember
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyUiState(
    val family: Family? = null,
    val members: List<FamilyMember> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val familyRepository: FamilyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyUiState())
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

    fun load(familyId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val family = familyRepository.getFamily(familyId)
            val members = familyRepository.getMembers(familyId)
            _uiState.value = FamilyUiState(family = family, members = members, isLoading = false)
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
