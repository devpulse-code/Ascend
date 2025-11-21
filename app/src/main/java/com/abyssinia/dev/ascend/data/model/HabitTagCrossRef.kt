// HabitTagCrossRef.kt
package com.abyssinia.dev.ascend.data.model

import androidx.room.Entity

@Entity(
    tableName = "habit_tag_cross_ref",
    primaryKeys = ["habitId", "tagId"]
)
data class HabitTagCrossRef(
    val habitId: Long,
    val tagId: Long
)
