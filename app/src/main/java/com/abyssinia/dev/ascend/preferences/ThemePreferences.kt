package com.abyssinia.dev.ascend.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore for theme preferences
private val Context.themeDataStore by preferencesDataStore("theme_preferences")

class ThemePreferences(private val context: Context) {

    companion object {
        private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
    }

    /** Flow for dark theme state */
    val isDarkThemeFlow: Flow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[DARK_THEME_KEY] ?: false
    }

    /** Save dark theme setting */
    suspend fun setDarkTheme(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[DARK_THEME_KEY] = enabled
        }
    }

    /** Optional: clear theme preference */
    suspend fun clearAll() {
        context.themeDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
