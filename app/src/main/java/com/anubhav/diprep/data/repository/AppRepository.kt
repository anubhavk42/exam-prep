package com.anubhav.diprep.data.repository

import com.anubhav.diprep.data.datastore.PreferencesManager
import com.anubhav.diprep.data.datastore.UserProfile
import com.anubhav.diprep.data.local.db.AppDao
import com.anubhav.diprep.data.local.db.ScoreEntry
import com.anubhav.diprep.data.local.db.TaskLog
import com.anubhav.diprep.data.local.db.MoodEntry
import com.anubhav.diprep.data.local.db.TimetableCompletion
import com.anubhav.diprep.data.local.db.TimetableSlot
import com.anubhav.diprep.data.local.db.Topic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AppRepository(
    private val appDao: AppDao,
    private val preferencesManager: PreferencesManager
) {
    val userProfile: Flow<UserProfile> = preferencesManager.userProfileFlow

    suspend fun saveProfile(
        name: String,
        examStream: String,
        examDate: String,
        customSubjects: List<String>
    ) = withContext(Dispatchers.IO) {
        preferencesManager.saveProfile(name, examStream, examDate, customSubjects)
    }

    suspend fun getUserName(): String = withContext(Dispatchers.IO) {
        preferencesManager.getUserName()
    }

    suspend fun updateName(name: String) = withContext(Dispatchers.IO) {
        preferencesManager.updateName(name)
    }

    suspend fun updateExamDate(dateISO: String) = withContext(Dispatchers.IO) {
        preferencesManager.updateExamDate(dateISO)
    }

    suspend fun updateExamDateConfirmed(confirmed: Boolean) = withContext(Dispatchers.IO) {
        preferencesManager.updateExamDateConfirmed(confirmed)
    }

    suspend fun updateExamStreamAndSubjects(stream: String, subjects: List<String>) = withContext(Dispatchers.IO) {
        preferencesManager.updateExamStreamAndSubjects(stream, subjects)
    }

    suspend fun updateCustomSubjects(subjects: List<String>) = withContext(Dispatchers.IO) {
        preferencesManager.updateCustomSubjects(subjects)
    }

    suspend fun updateReminderEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        preferencesManager.updateReminderEnabled(enabled)
    }

    suspend fun updateReminderTime(hour: Int, minute: Int) = withContext(Dispatchers.IO) {
        preferencesManager.updateReminderTime(hour, minute)
    }

    suspend fun setOnboardingDone(done: Boolean) = withContext(Dispatchers.IO) {
        preferencesManager.setOnboardingDone(done)
    }

    suspend fun updateStreakLastDate(dateISO: String) = withContext(Dispatchers.IO) {
        preferencesManager.updateStreakLastDate(dateISO)
    }

    suspend fun updateReminderScheduledDate(dateISO: String) = withContext(Dispatchers.IO) {
        preferencesManager.updateReminderScheduledDate(dateISO)
    }

    suspend fun updateThemeMode(themeMode: String) = withContext(Dispatchers.IO) {
        preferencesManager.updateThemeMode(themeMode)
    }

    suspend fun updateDynamicColor(dynamicColor: Boolean) = withContext(Dispatchers.IO) {
        preferencesManager.updateDynamicColor(dynamicColor)
    }

    suspend fun updateVitaminReminderEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        preferencesManager.updateVitaminReminderEnabled(enabled)
    }

    suspend fun updateExerciseReminderEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        preferencesManager.updateExerciseReminderEnabled(enabled)
    }

    suspend fun updateHomeSectionOrder(order: List<String>) = withContext(Dispatchers.IO) {
        preferencesManager.updateHomeSectionOrder(order)
    }

    suspend fun updateHomeSectionHidden(key: String, hidden: Boolean) = withContext(Dispatchers.IO) {
        preferencesManager.updateHomeSectionHidden(key, hidden)
    }

    suspend fun addCelebratedMilestone(key: String) = withContext(Dispatchers.IO) {
        preferencesManager.addCelebratedMilestone(key)
    }

    suspend fun updateNotificationFilterEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        preferencesManager.updateNotificationFilterEnabled(enabled)
    }

    suspend fun updateMutedAppPackages(packages: List<String>) = withContext(Dispatchers.IO) {
        preferencesManager.updateMutedAppPackages(packages)
    }

    suspend fun updateFilterActivationMode(mode: String) = withContext(Dispatchers.IO) {
        preferencesManager.updateFilterActivationMode(mode)
    }

    suspend fun updateFocusSessionActive(active: Boolean) = withContext(Dispatchers.IO) {
        preferencesManager.updateFocusSessionActive(active)
    }

    val allScores: Flow<List<ScoreEntry>> = appDao.getAllScores()
    val distinctSubjects: Flow<List<String>> = appDao.getDistinctSubjects()
    val maxScorePercentage: Flow<Int?> = appDao.getMaxScorePercentage()
    val allTimeAvgPercentage: Flow<Double?> = appDao.getAllTimeAvgPercentage()
    val totalTestsCount: Flow<Int> = appDao.getTotalTestsCount()
    val allTaskLogsDesc: Flow<List<TaskLog>> = appDao.getAllTaskLogsDesc()
    val completedTaskDates: Flow<List<String>> = appDao.getCompletedTaskDates()

    fun getRecentScores(limit: Int = 14): Flow<List<ScoreEntry>> = appDao.getRecentScores(limit)

    fun getLastScoresChronological(limit: Int = 14): Flow<List<ScoreEntry>> =
        appDao.getLastScoresChronological(limit)

    fun getAvgPercentageSince(startDate: String): Flow<Double?> =
        appDao.getAvgPercentageSince(startDate)

    fun getScoresBetween(startDate: String, endDate: String): Flow<List<ScoreEntry>> =
        appDao.getScoresBetween(startDate, endDate)

    fun getScoresForSubject(subject: String): Flow<List<ScoreEntry>> =
        appDao.getScoresForSubject(subject)

    fun getTaskLogsSince(startDate: String): Flow<List<TaskLog>> =
        appDao.getTaskLogsSince(startDate)

    fun getTaskLogForDate(dateISO: String): Flow<TaskLog?> =
        appDao.getTaskLogForDate(dateISO)

    suspend fun getWeekSummarySync(startDate: String, endDate: String, subject: String) = withContext(Dispatchers.IO) {
        appDao.getWeekSummarySync(startDate, endDate, subject)
    }

    suspend fun logScoreAndMarkExam(
        dateISO: String,
        subject: String,
        obtained: Int,
        total: Int,
        topicId: Long? = null
    ) = withContext(Dispatchers.IO) {
        val percentage = if (total > 0) {
            ((obtained.toDouble() / total) * 100).toInt().coerceIn(0, 100)
        } else 0

        val score = ScoreEntry(
            dateISO = dateISO,
            subject = subject,
            obtained = obtained,
            total = total,
            percentage = percentage,
            topicId = topicId,
            createdAt = System.currentTimeMillis()
        )
        appDao.logScoreAndMarkExamDone(score)
    }

    suspend fun deleteScore(id: Int) = withContext(Dispatchers.IO) {
        appDao.deleteScoreById(id)
    }

    suspend fun clearAllScores() = withContext(Dispatchers.IO) {
        appDao.clearAllScores()
    }

    suspend fun resetApp() = withContext(Dispatchers.IO) {
        appDao.clearAllScores()
        appDao.clearAllTaskLogs()
        appDao.clearAllTimetableSlots()
        appDao.clearAllTimetableCompletions()
        appDao.clearAllTopics()
        appDao.clearAllMoodEntries()
        preferencesManager.clearAllData()
    }

    suspend fun updateDemoMode(enabled: Boolean) = withContext(Dispatchers.IO) {
        preferencesManager.updateDemoMode(enabled)
    }

    suspend fun updateAppLanguage(tag: String) = withContext(Dispatchers.IO) {
        preferencesManager.updateAppLanguage(tag)
    }

    suspend fun updateHapticFeedbackEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        preferencesManager.updateHapticFeedbackEnabled(enabled)
    }

    /**
     * Wipes everything, then seeds a realistic sample dataset and flags the app
     * as being in Demo Mode. Landing screen (Home / Subjects / Weekly / Stats)
     * should all show populated data, not empty states.
     */
    suspend fun loadDemoData() = withContext(Dispatchers.IO) {
        DemoDataSeeder.seed(appDao, preferencesManager)
    }

    suspend fun updateTaskLog(taskLog: TaskLog) = withContext(Dispatchers.IO) {
        appDao.insertTaskLog(taskLog)
    }

    suspend fun toggleTask(dateISO: String, taskType: String, isDone: Boolean) = withContext(Dispatchers.IO) {
        val existing = appDao.getTaskLogForDateSync(dateISO) ?: TaskLog(dateISO = dateISO)
        val updated = when (taskType) {
            "study" -> existing.copy(studyDone = isDone)
            "exam" -> existing.copy(examDone = isDone)
            "exercise" -> existing.copy(exerciseDone = isDone)
            "vitamin" -> existing.copy(vitaminDone = isDone)
            else -> existing
        }
        val allCompleted = updated.studyDone && updated.examDone && updated.exerciseDone && updated.vitaminDone
        appDao.insertTaskLog(updated.copy(allDone = allCompleted))
    }

    // --- TIMETABLE ---

    fun getSlotsForDay(dayOfWeek: Int): Flow<List<TimetableSlot>> =
        appDao.getSlotsForDay(dayOfWeek)

    fun getAllSlots(): Flow<List<TimetableSlot>> =
        appDao.getAllSlots()

    fun getCompletedSlotIdsForDate(dateISO: String): Flow<List<Long>> =
        appDao.getCompletedSlotIdsForDate(dateISO)

    fun getCompletionsSince(sinceDate: String): Flow<List<TimetableCompletion>> =
        appDao.getCompletionsSince(sinceDate)

    suspend fun insertTimetableSlot(slot: TimetableSlot) = withContext(Dispatchers.IO) {
        appDao.insertTimetableSlot(slot)
    }

    suspend fun deleteTimetableSlot(slotId: Long) = withContext(Dispatchers.IO) {
        appDao.deleteCompletionsForSlot(slotId)
        appDao.deleteTimetableSlotById(slotId)
    }

    suspend fun toggleTimetableCompletion(slotId: Long, dateISO: String) = withContext(Dispatchers.IO) {
        val existing = appDao.getCompletion(slotId, dateISO)
        if (existing != null) {
            appDao.deleteCompletion(slotId, dateISO)
        } else {
            appDao.insertCompletion(TimetableCompletion(slotId = slotId, dateISO = dateISO))
        }
    }

    // --- TOPICS ---

    val allTopics: Flow<List<Topic>> = appDao.getAllTopics()

    fun getTopicsForSubject(subject: String): Flow<List<Topic>> =
        appDao.getTopicsForSubject(subject)

    fun getScoresForTopic(topicId: Long): Flow<List<ScoreEntry>> =
        appDao.getScoresForTopic(topicId)

    suspend fun getTopicById(id: Long): Topic? = withContext(Dispatchers.IO) {
        appDao.getTopicById(id)
    }

    suspend fun insertTopic(subject: String, topicName: String): Long = withContext(Dispatchers.IO) {
        appDao.insertTopic(Topic(subject = subject, topicName = topicName))
    }

    suspend fun deleteTopic(topicId: Long) = withContext(Dispatchers.IO) {
        appDao.deleteTopicById(topicId)
    }

    // --- MOOD CHECK-IN ---

    suspend fun getLastMoodPromptDate(): String = withContext(Dispatchers.IO) {
        preferencesManager.getLastMoodPromptDate()
    }

    suspend fun updateLastMoodPromptDate(dateISO: String) = withContext(Dispatchers.IO) {
        preferencesManager.updateLastMoodPromptDate(dateISO)
    }

    suspend fun saveMoodEntry(dateISO: String, mood: String) = withContext(Dispatchers.IO) {
        appDao.insertMoodEntry(MoodEntry(dateISO = dateISO, mood = mood))
    }
}
