package com.anubhav.diprep.data.local.db

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "scores",
    indices = [Index(value = ["dateISO"])]
)
data class ScoreEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateISO: String, // format: "YYYY-MM-DD"
    val subject: String,
    val obtained: Int,
    val total: Int,
    val percentage: Int, // stored, not computed at query time
    val topicId: Long? = null, // null = subject-level score (legacy), non-null = topic-level
    val createdAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "topics")
data class Topic(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val topicName: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "task_logs")
data class TaskLog(
    @PrimaryKey val dateISO: String, // format: "YYYY-MM-DD"
    val studyDone: Boolean = false,
    val examDone: Boolean = false,
    val exerciseDone: Boolean = false,
    val vitaminDone: Boolean = false,
    val allDone: Boolean = false // stored flag for heatmap efficiency
)

@Immutable
@Entity(tableName = "timetable_slots")
data class TimetableSlot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int,    // 1=Monday…7=Sunday (java.time.DayOfWeek.value)
    val startTime: String, // "HH:mm"
    val endTime: String,   // "HH:mm"
    val label: String,
    val focusModeEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "timetable_completions",
    indices = [Index(value = ["slotId", "dateISO"], unique = true)]
)
data class TimetableCompletion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val slotId: Long,
    val dateISO: String  // "YYYY-MM-DD"
)

@Immutable
@Entity(
    tableName = "mood_entries",
    indices = [Index(value = ["dateISO"], unique = true)]
)
data class MoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateISO: String,  // "YYYY-MM-DD", unique per day
    val mood: String,     // "EXCITED", "NEUTRAL", "LOW"
    val createdAt: Long = System.currentTimeMillis()
)
