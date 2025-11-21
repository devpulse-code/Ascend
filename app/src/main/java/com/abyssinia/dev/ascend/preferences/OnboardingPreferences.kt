package com.abyssinia.dev.ascend.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property to get datastore instance
val Context.onboardingDataStore by preferencesDataStore("onboarding_prefs")

class OnboardingPreferences(private val context: Context) {

    companion object {
        private val KEY_HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
    }

    // Flow to read onboarding status
    val hasSeenOnboarding: Flow<Boolean> =
        context.onboardingDataStore.data.map { prefs ->
            prefs[KEY_HAS_SEEN_ONBOARDING] ?: false
        }

    // Function to save flag
    suspend fun setHasSeenOnboarding(seen: Boolean) {
        context.onboardingDataStore.edit { prefs ->
            prefs[KEY_HAS_SEEN_ONBOARDING] = seen
        }
    }
}
