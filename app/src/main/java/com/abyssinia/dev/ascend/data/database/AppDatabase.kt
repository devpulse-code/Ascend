package com.abyssinia.dev.ascend.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.abyssinia.dev.ascend.data.dao.HabitCheckInDao
import com.abyssinia.dev.ascend.data.dao.HabitDao
import com.abyssinia.dev.ascend.data.dao.TaskDao
import com.abyssinia.dev.ascend.data.dao.TagDao
import com.abyssinia.dev.ascend.data.model.*

@Database(
    entities = [
        Habit::class,
        Task::class,
        HabitCheckIn::class,
        Tag::class,
        HabitTagCrossRef::class,
        TaskTagCrossRef::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun taskDao(): TaskDao
    abstract fun habitCheckInDao(): HabitCheckInDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rise_db"
                )
                    .addMigrations(
                        Migrations.MIGRATION_1_2,
                        Migrations.MIGRATION_2_3
                    )
                    .fallbackToDestructiveMigration() // optional safety fallback
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
