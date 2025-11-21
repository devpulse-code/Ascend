package com.abyssinia.dev.ascend.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
// Create a DataStore for reminder settings
val Context.reminderDataStore by preferencesDataStore("reminder_settings")

class ReminderPreferences(private val context: Context) {

    companion object {
        // Habit reminder keys
        private val HABIT_REMINDER_ENABLED = booleanPreferencesKey("habit_reminder_enabled")
        private val HABIT_REMINDER_HOUR = intPreferencesKey("habit_reminder_hour")
        private val HABIT_REMINDER_MINUTE = intPreferencesKey("habit_reminder_minute")

        // Task reminder keys
        private val TASK_REMINDER_ENABLED = booleanPreferencesKey("task_reminder_enabled")
        private val TASK_REMINDER_HOUR = intPreferencesKey("task_reminder_hour")
        private val TASK_REMINDER_MINUTE = intPreferencesKey("task_reminder_minute")
    }

    /** Flow for habit reminder enabled state */
    val isHabitReminderEnabled: Flow<Boolean> = context.reminderDataStore.data.map { prefs ->
        prefs[HABIT_REMINDER_ENABLED] ?: false
    }

    /** Flow for task reminder enabled state */
    val isTaskReminderEnabled: Flow<Boolean> = context.reminderDataStore.data.map { prefs ->
        prefs[TASK_REMINDER_ENABLED] ?: false
    }

    /** Flow for habit reminder time */
    val habitReminderHour: Flow<Int> = context.reminderDataStore.data.map { prefs ->
        prefs[HABIT_REMINDER_HOUR] ?: 8 // default 8 AM
    }
    val habitReminderMinute: Flow<Int> = context.reminderDataStore.data.map { prefs ->
        prefs[HABIT_REMINDER_MINUTE] ?: 0
    }

    /** Flow for task reminder time */
    val taskReminderHour: Flow<Int> = context.reminderDataStore.data.map { prefs ->
        prefs[TASK_REMINDER_HOUR] ?: 9 // default 9 AM
    }
    val taskReminderMinute: Flow<Int> = context.reminderDataStore.data.map { prefs ->
        prefs[TASK_REMINDER_MINUTE] ?: 0
    }

    /** Get habit reminder time as Pair(hour, minute) */
    suspend fun getHabitReminderTime(): Pair<Int, Int> {
        val prefs = context.reminderDataStore.data.first()  // <-- suspend function
        val hour = prefs[HABIT_REMINDER_HOUR] ?: 8
        val minute = prefs[HABIT_REMINDER_MINUTE] ?: 0
        return Pair(hour, minute)
    }

    /** Get task reminder time as Pair(hour, minute) */
    suspend fun getTaskReminderTime(): Pair<Int, Int> {
        val prefs = context.reminderDataStore.data.first() // no .map needed
        val hour = prefs[TASK_REMINDER_HOUR] ?: 9
        val minute = prefs[TASK_REMINDER_MINUTE] ?: 0
        return hour to minute
    }

    /** Save habit reminder enabled state */
    suspend fun setHabitReminderEnabled(enabled: Boolean) {
        context.reminderDataStore.edit { prefs ->
            prefs[HABIT_REMINDER_ENABLED] = enabled
        }
    }

    /** Save habit reminder time */
    suspend fun setHabitReminderTime(hour: Int, minute: Int) {
        context.reminderDataStore.edit { prefs ->
            prefs[HABIT_REMINDER_HOUR] = hour
            prefs[HABIT_REMINDER_MINUTE] = minute
        }
    }

    /** Save task reminder enabled state */
    suspend fun setTaskReminderEnabled(enabled: Boolean) {
        context.reminderDataStore.edit { prefs ->
            prefs[TASK_REMINDER_ENABLED] = enabled
        }
    }

    /** Save task reminder time */
    suspend fun setTaskReminderTime(hour: Int, minute: Int) {
        context.reminderDataStore.edit { prefs ->
            prefs[TASK_REMINDER_HOUR] = hour
            prefs[TASK_REMINDER_MINUTE] = minute
        }
    }

    /** Clear all reminder preferences */
    suspend fun clearAll() {
        context.reminderDataStore.edit { it.clear() }
    }
}
