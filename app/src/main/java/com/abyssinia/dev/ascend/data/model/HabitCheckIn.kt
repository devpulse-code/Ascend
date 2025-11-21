package com.abyssinia.dev.ascend.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habit_checkins")
data class HabitCheckIn(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val habitId: Int,
    val checkInDate: Long // store date at midnight (truncate time)
)
