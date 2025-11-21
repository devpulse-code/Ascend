// ReminderHelper.kt
package com.abyssinia.dev.ascend.util

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import java.util.Calendar

object ReminderHelper {

    private const val HABIT_WORK_TAG = "habit_reminder_work"
    private const val TASK_WORK_TAG = "task_reminder_work"
    private const val PREFS_NAME = "reminder_prefs"

    // Save reminder time in SharedPreferences
    private fun saveReminderTime(context: Context, type: String, hour: Int, minute: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("${type}_hour", hour)
            putInt("${type}_minute", minute)
            apply()
        }
    }

    // Retrieve reminder time from SharedPreferences
    private fun getReminderTime(context: Context, type: String): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hour = prefs.getInt("${type}_hour", 9)
        val minute = prefs.getInt("${type}_minute", 0)
        return Pair(hour, minute)
    }

    // Schedule a WorkManager task to fire at a precise time
    private fun scheduleReminder(
        context: Context,
        hour: Int,
        minute: Int,
        message: String,
        workTag: String
    ) {
        // Cancel any existing work with same tag
        WorkManager.getInstance(context).cancelAllWorkByTag(workTag)

        val now = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_MONTH, 1)
        }

        val delay = targetTime.timeInMillis - now.timeInMillis

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(workTag)
            .setInputData(workDataOf("message" to message))
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    // Schedule habit reminder
    fun scheduleHabitReminder(context: Context, hour: Int, minute: Int) {
        saveReminderTime(context, "habit", hour, minute)
        scheduleReminder(context, hour, minute, "Don't forget your habits today!", HABIT_WORK_TAG)
    }

    fun cancelHabitReminder(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(HABIT_WORK_TAG)
    }

    // Schedule task reminder
    fun scheduleTaskReminder(context: Context, hour: Int, minute: Int) {
        saveReminderTime(context, "task", hour, minute)
        scheduleReminder(context, hour, minute, "Don't forget your tasks today!", TASK_WORK_TAG)
    }

    fun cancelTaskReminder(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TASK_WORK_TAG)
    }

    // Reschedule all reminders (e.g., after reboot)
    fun rescheduleReminders(context: Context) {
        val (habitHour, habitMinute) = getReminderTime(context, "habit")
        val (taskHour, taskMinute) = getReminderTime(context, "task")
        scheduleHabitReminder(context, habitHour, habitMinute)
        scheduleTaskReminder(context, taskHour, taskMinute)
    }
}
