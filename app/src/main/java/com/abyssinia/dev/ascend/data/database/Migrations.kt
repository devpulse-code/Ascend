package com.abyssinia.dev.ascend.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {

    /**
     * Migration from version 1 → 2
     * Adds 'startDate' column to 'habits' if missing.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            val cursor = database.query("PRAGMA table_info(habits)")
            var columnExists = false
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                if (name == "startDate") {
                    columnExists = true
                    break
                }
            }
            cursor.close()
            if (!columnExists) {
                database.execSQL("ALTER TABLE habits ADD COLUMN startDate TEXT")
            }
        }
    }

    /**
     * Migration from version 2 → 3
     * Creates 'tags', 'habit_tag_cross_ref', and 'task_tag_cross_ref' if missing.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create tags table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS tags (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL
                )
            """.trimIndent())

            // Create habit_tag_cross_ref table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS habit_tag_cross_ref (
                    habitId INTEGER NOT NULL,
                    tagId INTEGER NOT NULL,
                    PRIMARY KEY(habitId, tagId),
                    FOREIGN KEY(habitId) REFERENCES habits(id) ON DELETE CASCADE,
                    FOREIGN KEY(tagId) REFERENCES tags(id) ON DELETE CASCADE
                )
            """.trimIndent())

            // Create task_tag_cross_ref table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS task_tag_cross_ref (
                    taskId INTEGER NOT NULL,
                    tagId INTEGER NOT NULL,
                    PRIMARY KEY(taskId, tagId),
                    FOREIGN KEY(taskId) REFERENCES tasks(id) ON DELETE CASCADE,
                    FOREIGN KEY(tagId) REFERENCES tags(id) ON DELETE CASCADE
                )
            """.trimIndent())
        }
    }

    /**
     * Safe helper: adds any new migration here in the future.
     */
}
