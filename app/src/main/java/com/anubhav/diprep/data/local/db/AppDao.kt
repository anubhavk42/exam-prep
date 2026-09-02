package com.anubhav.diprep.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class WeekSummaryRow(
    val avgPercentage: Double?,
    val count: Int
)

@Dao
interface AppDao {

    // --- SCORES QUERIES ---

    @Query("SELECT * FROM scores ORDER BY createdAt DESC")
    fun getAllScores(): Flow<List<ScoreEntry>>

    // Score history: fetch with LIMIT for display, not full table scan
    @Query("SELECT * FROM scores ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentScores(limit: Int): Flow<List<ScoreEntry>>

    // Fetch chronological for bar charts
    @Query("SELECT * FROM (SELECT * FROM scores ORDER BY createdAt DESC LIMIT :limit) ORDER BY createdAt ASC")
    fun getLastScoresChronological(limit: Int): Flow<List<ScoreEntry>>

    // Weekly query: date range filter using indexed dateISO column
    @Query("SELECT * FROM scores WHERE dateISO BETWEEN :startDate AND :endDate ORDER BY createdAt DESC")
    fun getScoresBetween(startDate: String, endDate: String): Flow<List<ScoreEntry>>

    @Query("SELECT * FROM scores WHERE subject = :subject ORDER BY createdAt DESC")
    fun getScoresForSubject(subject: String): Flow<List<ScoreEntry>>

    @Query("SELECT DISTINCT subject FROM scores WHERE subject != '' ORDER BY subject ASC")
    fun getDistinctSubjects(): Flow<List<String>>

    // Personal best: MAX(percentage) from ScoreEntry table - single Room query
    @Query("SELECT MAX(percentage) FROM scores")
    fun getMaxScorePercentage(): Flow<Int?>

    @Query("SELECT AVG(percentage) FROM scores WHERE dateISO >= :startDate")
    fun getAvgPercentageSince(startDate: String): Flow<Double?>

    @Query("SELECT AVG(percentage) FROM scores")
    fun getAllTimeAvgPercentage(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM scores")
    fun getTotalTestsCount(): Flow<Int>

    @Query("SELECT AVG(percentage) as avgPercentage, COUNT(*) as count FROM scores WHERE dateISO BETWEEN :startDate AND :endDate AND (:subject = 'ALL' OR subject = :subject)")
    fun getWeekSummary(startDate: String, endDate: String, subject: String): Flow<WeekSummaryRow>

    @Query("SELECT AVG(percentage) as avgPercentage, COUNT(*) as count FROM scores WHERE dateISO BETWEEN :startDate AND :endDate AND (:subject = 'ALL' OR subject = :subject)")
    suspend fun getWeekSummarySync(startDate: String, endDate: String, subject: String): WeekSummaryRow

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: ScoreEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScores(scores: List<ScoreEntry>)

    @Query("DELETE FROM scores WHERE id = :id")
    suspend fun deleteScoreById(id: Int)

    @Query("DELETE FROM scores")
    suspend fun clearAllScores()

    @Query("DELETE FROM task_logs")
    suspend fun clearAllTaskLogs()

    @Query("DELETE FROM timetable_slots")
    suspend fun clearAllTimetableSlots()

    @Query("DELETE FROM timetable_completions")
    suspend fun clearAllTimetableCompletions()


    // --- TASK LOGS & HEATMAP QUERIES ---

    // Heatmap query: fetch last 70 days in ONE single query (not 70 individual queries)
    @Query("SELECT * FROM task_logs WHERE dateISO >= :startDate ORDER BY dateISO ASC")
    fun getTaskLogsSince(startDate: String): Flow<List<TaskLog>>

    // Weekly query: date range filter using primary key dateISO
    @Query("SELECT * FROM task_logs WHERE dateISO BETWEEN :startDate AND :endDate ORDER BY dateISO ASC")
    fun getTaskLogsBetween(startDate: String, endDate: String): Flow<List<TaskLog>>

    @Query("SELECT * FROM task_logs WHERE dateISO = :dateISO")
    fun getTaskLogForDate(dateISO: String): Flow<TaskLog?>

    @Query("SELECT * FROM task_logs WHERE dateISO = :dateISO")
    suspend fun getTaskLogForDateSync(dateISO: String): TaskLog?

    @Query("SELECT * FROM task_logs ORDER BY dateISO DESC")
    fun getAllTaskLogsDesc(): Flow<List<TaskLog>>

    @Query("SELECT dateISO FROM task_logs WHERE allDone = 1 OR studyDone = 1 OR examDone = 1 OR exerciseDone = 1 OR vitaminDone = 1")
    fun getCompletedTaskDates(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskLog(taskLog: TaskLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskLogs(taskLogs: List<TaskLog>)

    @Update
    suspend fun updateTaskLog(taskLog: TaskLog)

    // Multi-table write transaction for atomicity and battery efficiency
    @Transaction
    suspend fun logScoreAndMarkExamDone(score: ScoreEntry) {
        insertScore(score)
        val existing = getTaskLogForDateSync(score.dateISO) ?: TaskLog(dateISO = score.dateISO)
        val updated = existing.copy(
            examDone = true,
            allDone = existing.studyDone && true && existing.exerciseDone && existing.vitaminDone
        )
        insertTaskLog(updated)
    }


    // --- TIMETABLE QUERIES ---

    @Query("SELECT * FROM timetable_slots WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    fun getSlotsForDay(dayOfWeek: Int): Flow<List<TimetableSlot>>

    @Query("SELECT * FROM timetable_slots ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllSlots(): Flow<List<TimetableSlot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableSlot(slot: TimetableSlot): Long

    @Query("DELETE FROM timetable_slots WHERE id = :slotId")
    suspend fun deleteTimetableSlotById(slotId: Long)

    @Query("DELETE FROM timetable_completions WHERE slotId = :slotId")
    suspend fun deleteCompletionsForSlot(slotId: Long)

    @Query("SELECT slotId FROM timetable_completions WHERE dateISO = :dateISO")
    fun getCompletedSlotIdsForDate(dateISO: String): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: TimetableCompletion)

    @Query("DELETE FROM timetable_completions WHERE slotId = :slotId AND dateISO = :dateISO")
    suspend fun deleteCompletion(slotId: Long, dateISO: String)

    @Query("SELECT * FROM timetable_completions WHERE slotId = :slotId AND dateISO = :dateISO LIMIT 1")
    suspend fun getCompletion(slotId: Long, dateISO: String): TimetableCompletion?

    @Query("SELECT * FROM timetable_completions WHERE dateISO >= :sinceDate")
    fun getCompletionsSince(sinceDate: String): Flow<List<TimetableCompletion>>


    // --- MOOD ENTRIES ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodEntry(entry: MoodEntry)

    @Query("SELECT * FROM mood_entries WHERE dateISO = :dateISO LIMIT 1")
    suspend fun getMoodEntryForDate(dateISO: String): MoodEntry?

    @Query("DELETE FROM mood_entries")
    suspend fun clearAllMoodEntries()


    // --- TOPICS ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: Topic): Long

    @Query("SELECT * FROM topics ORDER BY subject ASC, createdAt ASC")
    fun getAllTopics(): Flow<List<Topic>>

    @Query("SELECT * FROM topics WHERE subject = :subject ORDER BY createdAt ASC")
    fun getTopicsForSubject(subject: String): Flow<List<Topic>>

    @Query("SELECT * FROM topics WHERE id = :id LIMIT 1")
    suspend fun getTopicById(id: Long): Topic?

    @Query("DELETE FROM topics WHERE id = :topicId")
    suspend fun deleteTopicById(topicId: Long)

    @Query("DELETE FROM topics")
    suspend fun clearAllTopics()

    @Query("SELECT * FROM scores WHERE topicId = :topicId ORDER BY createdAt DESC")
    fun getScoresForTopic(topicId: Long): Flow<List<ScoreEntry>>
}
