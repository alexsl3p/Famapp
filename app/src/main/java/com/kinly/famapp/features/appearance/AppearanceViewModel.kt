package com.kinly.famapp.features.appearance

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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

private val FONT_THEME_KEY = stringPreferencesKey("font_theme_id")

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    val fontThemeId: StateFlow<String> = dataStore.data
        .map { it[FONT_THEME_KEY] ?: "cosmo" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "cosmo")

    fun setFontTheme(id: String) {
        viewModelScope.launch {
            dataStore.edit { it[FONT_THEME_KEY] = id }
        }
    }
}
