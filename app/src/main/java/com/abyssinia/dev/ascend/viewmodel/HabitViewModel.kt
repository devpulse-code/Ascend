package com.abyssinia.dev.ascend.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.abyssinia.dev.ascend.data.database.AppDatabase
import com.abyssinia.dev.ascend.data.model.Habit
import com.abyssinia.dev.ascend.data.model.HabitCheckIn
import com.abyssinia.dev.ascend.data.model.Tag
import com.abyssinia.dev.ascend.repository.HabitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository by lazy {
        val db = AppDatabase.getDatabase(application)
        HabitRepository(db.habitDao(), db.habitCheckInDao(), db.tagDao())
    }

    val allHabits: LiveData<List<Habit>> = repository.getAllHabits().asLiveData()
    val allTags: LiveData<List<Tag>> = repository.getAllTags().asLiveData()

    fun insert(habit: Habit) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertHabit(habit)
    }

    fun update(habit: Habit) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateHabit(habit)
    }

    fun delete(habit: Habit) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteHabit(habit)
    }

    suspend fun insertSuspend(habit: Habit): Long = withContext(Dispatchers.IO) {
        repository.insertHabit(habit)
    }

    suspend fun getHabitById(id: Long): Habit? = withContext(Dispatchers.IO) {
        repository.getHabitById(id)
    }

    fun checkInHabit(habit: Habit, onResult: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            val result = repository.checkInHabit(habit)
            onResult(result.first, result.second)
        }
    }

    fun undoTodayCheckIn(habit: Habit, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.undoTodayCheckIn(habit)
            onResult(success)
        }
    }

    fun insertTag(tag: Tag) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertTag(tag)
    }

    suspend fun insertTagSuspend(tag: Tag): Long = withContext(Dispatchers.IO) {
        repository.insertTag(tag)
    }

    suspend fun getTagByName(name: String): Tag? = withContext(Dispatchers.IO) {
        val tags = repository.getAllTags().first()
        tags.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    fun assignTagToHabit(habitId: Long, tagId: Long) = viewModelScope.launch(Dispatchers.IO) {
        repository.assignTagToHabit(habitId, tagId)
    }

    suspend fun getAllHabitsSuspend(): List<Habit> = withContext(Dispatchers.IO) {
        repository.getAllHabits().first()
    }

    fun removeTagFromHabit(habitId: Long, tagId: Long) = viewModelScope.launch(Dispatchers.IO) {
        repository.removeTagFromHabit(habitId, tagId)
    }

    suspend fun getTagsByHabit(habitId: Long): List<Tag> = withContext(Dispatchers.IO) {
        repository.getTagsByHabit(habitId).first() // collect the first emitted value from the Flow
    }
    suspend fun getHabitsByTag(tagId: Long): List<Habit> = withContext(Dispatchers.IO) {
        repository.getHabitsByTag(tagId).first() // collect the first emitted value from the Flow
    }

    // ✅ Added function to fetch HabitCheckIns for a specific habit
    // ✅ Fetch HabitCheckIns for a specific habit as a List
    suspend fun getHabitCheckIns(habitId: Long): List<HabitCheckIn> = withContext(Dispatchers.IO) {
        repository.getCheckInsForHabit(habitId).first() // collect the Flow once to get the list
    }

}
