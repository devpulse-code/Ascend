package com.abyssinia.dev.ascend.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.abyssinia.dev.ascend.data.database.AppDatabase
import com.abyssinia.dev.ascend.data.model.Task
import com.abyssinia.dev.ascend.data.model.Tag
import com.abyssinia.dev.ascend.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository by lazy {
        val db = AppDatabase.getDatabase(application)
        TaskRepository(db.taskDao(), db.tagDao())
    }

    val allTasks: LiveData<List<Task>> = repository.getAllTasks().asLiveData()
    val allTags: LiveData<List<Tag>> = repository.getAllTags().asLiveData()

    private val _selectedTags = MutableLiveData<MutableSet<Tag>>(mutableSetOf())
    val selectedTags: LiveData<Set<Tag>> = _selectedTags.map { it.toSet() }

    fun selectTag(tag: Tag) {
        val current = _selectedTags.value ?: mutableSetOf()
        current.add(tag)
        _selectedTags.value = current
    }

    fun deselectTag(tag: Tag) {
        val current = _selectedTags.value ?: mutableSetOf()
        current.remove(tag)
        _selectedTags.value = current
    }

    suspend fun insertTaskSuspend(task: Task): Long {
        val id = repository.insertTask(task)
        _selectedTags.value?.forEach { tag -> repository.assignTagToTask(id, tag.id) }
        return id
    }

    suspend fun updateTaskSuspend(task: Task) {
        repository.updateTask(task)
        _selectedTags.value?.forEach { tag -> repository.assignTagToTask(task.id, tag.id) }
    }

    suspend fun deleteTaskSuspend(task: Task) {
        repository.deleteTask(task)
    }

    suspend fun getTaskById(id: Long): Task? = repository.getTaskById(id)
    suspend fun getTagsByTask(taskId: Long): List<Tag> = repository.getTagsByTask(taskId)

    suspend fun getTagByName(name: String): Tag? {
        return repository.getAllTags().first().firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    suspend fun insertTagSuspend(tag: Tag): Long = repository.insertTag(tag)
    suspend fun assignTagToTask(taskId: Long, tagId: Long) = repository.assignTagToTask(taskId, tagId)
    suspend fun removeTagFromTask(taskId: Long, tagId: Long) = repository.removeTagFromTask(taskId, tagId)
    suspend fun getAllTasksSuspend(): List<Task> = withContext(Dispatchers.IO) {
        repository.getAllTasks().first()
    }


    fun insert(task: Task) = viewModelScope.launch(Dispatchers.IO) { repository.insertTask(task) }
    fun update(task: Task) = viewModelScope.launch(Dispatchers.IO) { repository.updateTask(task) }
    fun delete(task: Task) = viewModelScope.launch(Dispatchers.IO) { repository.deleteTask(task) }
}