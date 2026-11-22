package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.LuminaThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.luminaDataStore: DataStore<Preferences> by preferencesDataStore(name = "lumina_prefs")

data class ThemeUiPreferences(
    val preset: LuminaThemePreset = LuminaThemePreset.OBSIDIAN_NEON,
    val darkModeEnabled: Boolean = true,
    val gridColumns: Int = 3,
    val ambientMotionEnabled: Boolean = true,
    val slideshowSeconds: Int = 4
)

class ThemePreferencesStore(private val context: Context) {
    private object Keys {
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val AMBIENT_MOTION = booleanPreferencesKey("ambient_motion")
        val SLIDESHOW_SECONDS = intPreferencesKey("slideshow_seconds")
    }

    val preferencesFlow: Flow<ThemeUiPreferences> = context.luminaDataStore.data.map { prefs ->
        ThemeUiPreferences(
            preset = LuminaThemePreset.fromId(prefs[Keys.THEME_PRESET]),
            darkModeEnabled = prefs[Keys.DARK_MODE] ?: true,
            gridColumns = (prefs[Keys.GRID_COLUMNS] ?: 3).coerceIn(2, 4),
            ambientMotionEnabled = prefs[Keys.AMBIENT_MOTION] ?: true,
            slideshowSeconds = (prefs[Keys.SLIDESHOW_SECONDS] ?: 4).coerceIn(2, 10)
        )
    }

    suspend fun setThemePreset(preset: LuminaThemePreset) {
        context.luminaDataStore.edit { it[Keys.THEME_PRESET] = preset.id }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.luminaDataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setGridColumns(columns: Int) {
        context.luminaDataStore.edit { it[Keys.GRID_COLUMNS] = columns.coerceIn(2, 4) }
    }

    suspend fun setAmbientMotion(enabled: Boolean) {
        context.luminaDataStore.edit { it[Keys.AMBIENT_MOTION] = enabled }
    }

    suspend fun setSlideshowSeconds(seconds: Int) {
        context.luminaDataStore.edit { it[Keys.SLIDESHOW_SECONDS] = seconds.coerceIn(2, 10) }
    }
}
