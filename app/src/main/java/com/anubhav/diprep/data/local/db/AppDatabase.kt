package com.anubhav.diprep.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ScoreEntry::class, TaskLog::class, TimetableSlot::class, TimetableCompletion::class, MoodEntry::class, Topic::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `timetable_slots` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dayOfWeek` INTEGER NOT NULL,
                        `startTime` TEXT NOT NULL,
                        `endTime` TEXT NOT NULL,
                        `label` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )"""
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `timetable_completions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `slotId` INTEGER NOT NULL,
                        `dateISO` TEXT NOT NULL
                    )"""
                )
                database.execSQL(
                    """CREATE UNIQUE INDEX IF NOT EXISTS `index_timetable_completions_slotId_dateISO`
                       ON `timetable_completions` (`slotId`, `dateISO`)"""
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `mood_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dateISO` TEXT NOT NULL,
                        `mood` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )"""
                )
                database.execSQL(
                    """CREATE UNIQUE INDEX IF NOT EXISTS `index_mood_entries_dateISO`
                       ON `mood_entries` (`dateISO`)"""
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `topics` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `topicName` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )"""
                )
                database.execSQL(
                    "ALTER TABLE `scores` ADD COLUMN `topicId` INTEGER DEFAULT NULL"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `timetable_slots` ADD COLUMN `focusModeEnabled` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "exam_prep_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
