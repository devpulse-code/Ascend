package com.abyssinia.dev.ascend.data.dao

import androidx.room.*
import com.abyssinia.dev.ascend.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    // ---------------- Tags ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag): Long

    @Query("SELECT * FROM tags ORDER BY id ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE id = :tagId LIMIT 1")
    suspend fun getTagById(tagId: Long): Tag?

    @Delete
    suspend fun deleteTag(tag: Tag)

    // ---------------- Habit ↔ Tag ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitTagCrossRef(crossRef: HabitTagCrossRef)

    @Delete
    suspend fun deleteHabitTagCrossRef(crossRef: HabitTagCrossRef)

    @Transaction
    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN habit_tag_cross_ref ht ON t.id = ht.tagId
        WHERE ht.habitId = :habitId
        ORDER BY t.id ASC
    """)
    fun getTagsForHabit(habitId: Long): Flow<List<Tag>>

    @Transaction
    @Query("""
        SELECT h.* FROM habits h
        INNER JOIN habit_tag_cross_ref ht ON h.id = ht.habitId
        WHERE ht.tagId = :tagId
        ORDER BY h.id DESC
    """)
    fun getHabitsForTag(tagId: Long): Flow<List<Habit>>

    // ---------------- Task ↔ Tag ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskTagCrossRef(crossRef: TaskTagCrossRef)

    @Delete
    suspend fun deleteTaskTagCrossRef(crossRef: TaskTagCrossRef)

    @Transaction
    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN task_tag_cross_ref tt ON t.id = tt.tagId
        WHERE tt.taskId = :taskId
        ORDER BY t.id ASC
    """)
    fun getTagsForTask(taskId: Long): Flow<List<Tag>>

    @Transaction
    @Query("""
        SELECT ta.* FROM tasks ta
        INNER JOIN task_tag_cross_ref tt ON ta.id = tt.taskId
        WHERE tt.tagId = :tagId
        ORDER BY ta.dueDate ASC
    """)
    fun getTasksForTag(tagId: Long): Flow<List<Task>>
}
