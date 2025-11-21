package com.abyssinia.dev.ascend.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.abyssinia.dev.ascend.data.model.Habit
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    // ---------------- Read ----------------
    @Query("SELECT * FROM habits ORDER BY id DESC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    fun getHabitByIdLive(habitId: Long): LiveData<Habit?>

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    fun getHabitByIdFlow(habitId: Long): Flow<Habit?>

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    suspend fun getHabitById(habitId: Long): Habit?

    // ---------------- Write ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    // ---------------- Special ----------------
    @Query("""
        UPDATE habits 
        SET lastCheckInDate = :timestamp, streakCount = :streak 
        WHERE id = :habitId
    """)
    suspend fun updateCheckIn(habitId: Long, timestamp: Long, streak: Int)
}
