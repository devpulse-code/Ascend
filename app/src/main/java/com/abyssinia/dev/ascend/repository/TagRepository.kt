package com.abyssinia.dev.ascend.repository

import com.abyssinia.dev.ascend.data.dao.TagDao
import com.abyssinia.dev.ascend.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TagRepository(private val tagDao: TagDao) {

    // ---------------- Tags ----------------
    fun getAllTags(): Flow<List<Tag>> = tagDao.getAllTags()

    suspend fun insertTag(tag: Tag): Long = tagDao.insertTag(tag)

    suspend fun getTagById(tagId: Long): Tag? = tagDao.getTagById(tagId)

    suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag)

    // ---------------- Habit ↔ Tag ----------------
    suspend fun assignTagToHabit(habitId: Long, tagId: Long) {
        // Avoid duplicate insertion (REPLACE strategy in DAO handles it, but safe check optional)
        tagDao.insertHabitTagCrossRef(HabitTagCrossRef(habitId, tagId))
    }

    suspend fun removeTagFromHabit(habitId: Long, tagId: Long) {
        tagDao.deleteHabitTagCrossRef(HabitTagCrossRef(habitId, tagId))
    }

    fun getTagsForHabit(habitId: Long): Flow<List<Tag>> = tagDao.getTagsForHabit(habitId)

    fun getHabitsForTag(tagId: Long): Flow<List<Habit>> = tagDao.getHabitsForTag(tagId)

    // ---------------- Task ↔ Tag ----------------
    suspend fun assignTagToTask(taskId: Long, tagId: Long) {
        tagDao.insertTaskTagCrossRef(TaskTagCrossRef(taskId, tagId))
    }

    suspend fun removeTagFromTask(taskId: Long, tagId: Long) {
        tagDao.deleteTaskTagCrossRef(TaskTagCrossRef(taskId, tagId))
    }

    fun getTagsForTask(taskId: Long): Flow<List<Tag>> = tagDao.getTagsForTask(taskId)

    // ---------------- Safe fetch for tasks by tag ----------------
    suspend fun getTasksForTag(tagId: Long): List<Task> {
        // Always use Flow.first() carefully to prevent empty lists
        return tagDao.getTasksForTag(tagId).first()
    }
}
