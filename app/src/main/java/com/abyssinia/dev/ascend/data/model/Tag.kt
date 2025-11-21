package com.abyssinia.dev.ascend.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    val name: String
)
