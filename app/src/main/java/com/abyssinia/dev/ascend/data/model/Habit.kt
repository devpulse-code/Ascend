package com.abyssinia.dev.ascend.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,
    val name: String,
    val description: String? = null,
    var streakCount: Int = 0,
    var frequency: String = "daily",
    var lastCheckInDate: Long? = null, // changed from String to Long
    var startDate: Long? = null
)
