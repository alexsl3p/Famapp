package com.kinly.famapp.features.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinly.famapp.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UpdateState {
    object Idle : UpdateState()
    data class Available(val info: AppUpdateInfo) : UpdateState()
    data class Downloading(val progress: Float) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val repository: UpdateRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var checked = false

    /** Последняя версия берётся из таблицы app_release (её обновляет разработчик через БД). */
    private val manifestUrl: String =
        "${BuildConfig.SUPABASE_URL}/rest/v1/app_release?select=version_code,version_name,apk_url,notes&order=version_code.desc&limit=1"

    fun checkForUpdate() {
        if (checked) return
        checked = true
        viewModelScope.launch {
            val info = repository.fetchManifest(manifestUrl) ?: return@launch
            if (info.versionCode > BuildConfig.VERSION_CODE) {
                _state.value = UpdateState.Available(info)
            }
        }
    }

    fun startUpdate(context: Context) {
        val info = (_state.value as? UpdateState.Available)?.info ?: return
        viewModelScope.launch {
            _state.value = UpdateState.Downloading(0f)
            val file = repository.downloadApk(context, info.apkUrl) { p ->
                _state.value = UpdateState.Downloading(if (p < 0f) 0f else p)
            }
            if (file != null) {
                UpdateRepository.installApk(context, file)
                _state.value = UpdateState.Idle
            } else {
                _state.value = UpdateState.Error("Не удалось скачать обновление")
            }
        }
    }

    fun dismiss() {
        _state.value = UpdateState.Idle
    }
}
