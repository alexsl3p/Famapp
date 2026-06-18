package com.kinly.famapp.features.navstate

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

private val LAST_TAB_KEY = stringPreferencesKey("last_tab_route")

/** Запоминает последнюю открытую вкладку, чтобы восстановить её после выгрузки процесса. */
@HiltViewModel
class TabStateViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    /** null — пока не загружено; затем сохранённый маршрут или "home". */
    val lastTab: StateFlow<String?> = dataStore.data
        .map { it[LAST_TAB_KEY] ?: "home" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun saveTab(route: String) {
        viewModelScope.launch {
            dataStore.edit { it[LAST_TAB_KEY] = route }
        }
    }
}
