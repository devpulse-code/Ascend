package com.abyssinia.dev.ascend.data.dao

import androidx.room.*
import com.abyssinia.dev.ascend.data.model.HabitCheckIn
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitCheckInDao {

    // ---------------- Insert ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIn(checkIn: HabitCheckIn)

    // ---------------- Query ----------------
    @Query("""
        SELECT * FROM habit_checkins
        WHERE habitId = :habitId
        ORDER BY checkInDate ASC
    """)
    fun getCheckInsForHabit(habitId: Long): Flow<List<HabitCheckIn>>

    @Query("""
        SELECT COUNT(*) FROM habit_checkins
        WHERE habitId = :habitId AND checkInDate = :timestamp
    """)
    suspend fun isCheckedInOnDate(habitId: Long, timestamp: Long): Int

    // ---------------- Delete ----------------
    @Query("""
        DELETE FROM habit_checkins
        WHERE habitId = :habitId AND checkInDate = :timestamp
    """)
    suspend fun deleteCheckIn(habitId: Long, timestamp: Long)
}
