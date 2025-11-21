// TaskTagCrossRef.kt
package com.abyssinia.dev.ascend.data.model

import androidx.room.Entity

@Entity(
    tableName = "task_tag_cross_ref",
    primaryKeys = ["taskId", "tagId"]
)
data class TaskTagCrossRef(
    val taskId: Long,
    val tagId: Long
)
