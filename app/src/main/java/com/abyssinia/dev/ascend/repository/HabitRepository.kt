package com.abyssinia.dev.ascend.repository

import com.abyssinia.dev.ascend.data.dao.HabitDao
import com.abyssinia.dev.ascend.data.dao.HabitCheckInDao
import com.abyssinia.dev.ascend.data.dao.TagDao
import com.abyssinia.dev.ascend.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.*

class HabitRepository(
    private val habitDao: HabitDao,
    private val habitCheckInDao: HabitCheckInDao,
    private val tagDao: TagDao
) {

    // ---------------- Habits ----------------
    fun getAllHabits(): Flow<List<Habit>> = habitDao.getAllHabits()
    fun getHabitByIdFlow(id: Long): Flow<Habit?> = habitDao.getHabitByIdFlow(id)
    fun getHabitByIdLive(id: Long) = habitDao.getHabitByIdLive(id)
    suspend fun getHabitById(id: Long): Habit? = habitDao.getHabitById(id)
    suspend fun insertHabit(habit: Habit): Long = habitDao.insertHabit(habit)
    suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)
    suspend fun deleteHabit(habit: Habit) = habitDao.deleteHabit(habit)

    // ---------------- Check-in business logic ----------------
    suspend fun checkInHabit(habit: Habit): Pair<Boolean, Int> {
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayTimestamp = todayCal.timeInMillis

        // Already checked in today
        if (habit.lastCheckInDate == todayTimestamp) return Pair(false, habit.streakCount)

        val streak = when (habit.frequency.lowercase()) {
            "daily" -> {
                habit.lastCheckInDate?.let { lastTs ->
                    val diff = ((todayTimestamp - lastTs) / (1000 * 60 * 60 * 24)).toInt()
                    when (diff) {
                        1 -> habit.streakCount + 1
                        0 -> habit.streakCount
                        else -> 1
                    }
                } ?: 1
            }

            "weekly" -> {
                habit.lastCheckInDate?.let { lastTs ->
                    val lastCal = Calendar.getInstance().apply { timeInMillis = lastTs }
                    val thisWeek = todayCal.get(Calendar.WEEK_OF_YEAR)
                    val lastWeek = lastCal.get(Calendar.WEEK_OF_YEAR)
                    val thisYear = todayCal.get(Calendar.YEAR)
                    val lastYear = lastCal.get(Calendar.YEAR)

                    if (thisYear == lastYear && thisWeek - lastWeek == 1) habit.streakCount + 1
                    else if (thisYear - lastYear == 1 && lastWeek == 52 && thisWeek == 1) habit.streakCount + 1
                    else 1
                } ?: 1
            }

            "monthly" -> {
                habit.lastCheckInDate?.let { lastTs ->
                    val lastCal = Calendar.getInstance().apply { timeInMillis = lastTs }
                    val thisMonth = todayCal.get(Calendar.MONTH)
                    val lastMonth = lastCal.get(Calendar.MONTH)
                    val thisYear = todayCal.get(Calendar.YEAR)
                    val lastYear = lastCal.get(Calendar.YEAR)

                    if (thisYear == lastYear && thisMonth - lastMonth == 1) habit.streakCount + 1
                    else if (thisYear - lastYear == 1 && lastMonth == 11 && thisMonth == 0) habit.streakCount + 1
                    else 1
                } ?: 1
            }

            else -> 1
        }

        habit.streakCount = streak
        habit.lastCheckInDate = todayTimestamp
        habitDao.updateCheckIn(habit.id, todayTimestamp, streak)
        habitCheckInDao.insertCheckIn(HabitCheckIn(habitId = habit.id.toInt(), checkInDate = todayTimestamp))

        return Pair(true, streak)
    }

    // ---------------- Undo today check-in ----------------
    suspend fun undoTodayCheckIn(habit: Habit): Boolean {
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayTimestamp = todayCal.timeInMillis

        if (habit.lastCheckInDate != todayTimestamp) return false

        habitCheckInDao.deleteCheckIn(habit.id, todayTimestamp)

        val checkIns = habitCheckInDao.getCheckInsForHabit(habit.id).first()
            .map { it.checkInDate }
            .filter { it < todayTimestamp }
            .sorted()

        val newStreak = if (checkIns.isEmpty()) {
            0
        } else {
            val lastCheckInMillis = checkIns.last()
            val lastCal = Calendar.getInstance().apply { timeInMillis = lastCheckInMillis }

            when (habit.frequency.lowercase()) {
                "daily" -> {
                    val diffDays = ((todayTimestamp - lastCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                    if (diffDays == 1) habit.streakCount - 1 else 0
                }
                "weekly" -> {
                    val thisWeek = todayCal.get(Calendar.WEEK_OF_YEAR)
                    val lastWeek = lastCal.get(Calendar.WEEK_OF_YEAR)
                    val thisYear = todayCal.get(Calendar.YEAR)
                    val lastYear = lastCal.get(Calendar.YEAR)

                    if ((thisYear == lastYear && thisWeek - lastWeek == 1) ||
                        (thisYear - lastYear == 1 && lastWeek == 52 && thisWeek == 1)) habit.streakCount - 1
                    else 0
                }
                "monthly" -> {
                    val thisMonth = todayCal.get(Calendar.MONTH)
                    val lastMonth = lastCal.get(Calendar.MONTH)
                    val thisYear = todayCal.get(Calendar.YEAR)
                    val lastYear = lastCal.get(Calendar.YEAR)

                    if ((thisYear == lastYear && thisMonth - lastMonth == 1) ||
                        (thisYear - lastYear == 1 && lastMonth == 11 && thisMonth == 0)) habit.streakCount - 1
                    else 0
                }
                else -> 0
            }
        }

        habit.streakCount = if (newStreak < 0) 0 else newStreak
        habit.lastCheckInDate = checkIns.lastOrNull()

        habitDao.updateCheckIn(habit.id, habit.lastCheckInDate ?: 0L, habit.streakCount)

        return true
    }

    fun getCheckInsForHabit(habitId: Long): Flow<List<HabitCheckIn>> =
        habitCheckInDao.getCheckInsForHabit(habitId)

    // ---------------- Tags ----------------
    suspend fun insertTag(tag: Tag): Long = tagDao.insertTag(tag)
    fun getAllTags(): Flow<List<Tag>> = tagDao.getAllTags()
    suspend fun assignTagToHabit(habitId: Long, tagId: Long) =
        tagDao.insertHabitTagCrossRef(HabitTagCrossRef(habitId, tagId))
    suspend fun removeTagFromHabit(habitId: Long, tagId: Long) =
        tagDao.deleteHabitTagCrossRef(HabitTagCrossRef(habitId, tagId))
    fun getHabitsByTag(tagId: Long): Flow<List<Habit>> = tagDao.getHabitsForTag(tagId)
    fun getTagsByHabit(habitId: Long): Flow<List<Tag>> = tagDao.getTagsForHabit(habitId)
}
