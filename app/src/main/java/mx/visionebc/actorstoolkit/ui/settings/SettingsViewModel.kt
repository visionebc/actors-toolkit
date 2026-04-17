package mx.visionebc.actorstoolkit.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.visionebc.actorstoolkit.data.preferences.SettingsDataStore
import mx.visionebc.actorstoolkit.data.preferences.ThemeMode

class SettingsViewModel(private val settingsDataStore: SettingsDataStore) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsDataStore.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val playOwnRecordings: StateFlow<Boolean> = settingsDataStore.playOwnRecordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }

    fun setPlayOwnRecordings(value: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setPlayOwnRecordings(value)
        }
    }

    class Factory(private val settingsDataStore: SettingsDataStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsDataStore) as T
        }
    }
}
