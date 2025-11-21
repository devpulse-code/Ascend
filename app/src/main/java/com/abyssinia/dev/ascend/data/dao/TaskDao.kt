package com.abyssinia.dev.ascend.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.abyssinia.dev.ascend.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // ---------------- Read ----------------
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Long): Task?

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    fun getTaskByIdLive(taskId: Long): LiveData<Task?>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    fun getTaskByIdFlow(taskId: Long): Flow<Task?>

    // ---------------- Write ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)
}
