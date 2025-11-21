package com.abyssinia.dev.ascend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abyssinia.dev.ascend.preferences.ThemePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(private val prefs: ThemePreferences) : ViewModel() {

    // Convert DataStore Flow to StateFlow
    val isDarkThemeFlow = prefs.isDarkThemeFlow

    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            prefs.setDarkTheme(isDark)
        }
    }

    /*fun setFollowSystem(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setFollowSystem(enabled)
        }
    }*/
}
