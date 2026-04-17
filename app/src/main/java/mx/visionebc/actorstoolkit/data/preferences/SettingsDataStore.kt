package mx.visionebc.actorstoolkit.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK, BLUE, DEEP_BLUE, PINK_VIOLET, GREEN, YELLOW, IOS, MODERN }

class SettingsDataStore(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val playOwnRecordingsKey = booleanPreferencesKey("play_own_recordings")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        try { ThemeMode.valueOf(prefs[themeKey] ?: "SYSTEM") } catch (_: Exception) { ThemeMode.SYSTEM }
    }

    val playOwnRecordings: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[playOwnRecordingsKey] ?: false
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[themeKey] = mode.name
        }
    }

    suspend fun setPlayOwnRecordings(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[playOwnRecordingsKey] = value
        }
    }
}
