package com.abyssinia.dev.ascend.repository

import android.content.Context
import com.abyssinia.dev.ascend.data.dao.TaskDao
import com.abyssinia.dev.ascend.data.dao.TagDao
import com.abyssinia.dev.ascend.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TaskRepository(
    private val taskDao: TaskDao,
    private val tagDao: TagDao
) {

    // ---------------- Tasks ----------------
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun getTaskById(id: Long): Task? = taskDao.getTaskById(id)

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    // ---------------- Tags ----------------
    suspend fun insertTag(tag: Tag): Long = tagDao.insertTag(tag)

    fun getAllTags(): Flow<List<Tag>> = tagDao.getAllTags()

    suspend fun assignTagToTask(taskId: Long, tagId: Long) {
        // Safe insert, avoids duplicate issues
        tagDao.insertTaskTagCrossRef(TaskTagCrossRef(taskId, tagId))
    }

    suspend fun removeTagFromTask(taskId: Long, tagId: Long) {
        tagDao.deleteTaskTagCrossRef(TaskTagCrossRef(taskId, tagId))
    }

    suspend fun getTagsByTask(taskId: Long): List<Tag> {
        // Safe fetching with Flow.first()
        return tagDao.getTagsForTask(taskId).first()
    }

    // ---------------- Singleton ----------------
    companion object {
        @Volatile
        private var INSTANCE: TaskRepository? = null

        fun getInstance(context: Context): TaskRepository {
            return INSTANCE ?: synchronized(this) {
                val database = com.abyssinia.dev.ascend.data.database.AppDatabase.getDatabase(context)
                val instance = TaskRepository(database.taskDao(), database.tagDao())
                INSTANCE = instance
                instance
            }
        }
    }
}
