package com.anubhav.diprep.ui.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anubhav.diprep.data.datastore.PreferencesManager
import com.anubhav.diprep.data.datastore.UserProfile
import com.anubhav.diprep.data.local.db.AppDatabase
import com.anubhav.diprep.data.local.db.ScoreEntry
import com.anubhav.diprep.data.local.db.TaskLog
import com.anubhav.diprep.data.local.db.Topic
import com.anubhav.diprep.data.model.ExamDataConstants
import com.anubhav.diprep.data.model.HabitLogItem
import com.anubhav.diprep.data.model.Subject
import com.anubhav.diprep.data.model.SubjectStatus
import com.anubhav.diprep.data.repository.AppRepository
import com.anubhav.diprep.receiver.NotificationHelper
import com.anubhav.diprep.ui.navigation.MainTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Immutable
data class HeatmapCell(
    val date: LocalDate,
    val dayOfWeekIndex: Int, // 0 = Mon, 6 = Sun
    val intensityLevel: Int, // 0 (empty), 1 (low), 2 (medium), 3 (high/max)
    val isToday: Boolean,
    val isSelected: Boolean
)

@Immutable
data class WeeklySummary(
    val title: String,
    val dateRange: String,
    val averageScore: Int,
    val testsLoggedCount: Int,
    val statusColorType: StatusColorType
)

enum class StatusColorType {
    SUCCESS,
    WARNING,
    CRITICAL
}

@Immutable
data class PrepUiState(
    val userProfile: UserProfile = UserProfile(),
    val selectedDate: LocalDate = LocalDate.now(),
    val dailyTasks: List<Any> = emptyList(),
    val dailyHabits: List<HabitLogItem> = emptyList(),
    val allScores: List<ScoreEntry> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val selectedTab: MainTab = MainTab.HOME,
    val activeSubjectFilter: String = "All",
    val activeWeeklyFilter: String = "All",
    val dailyQuote: String = "Precision in jurisprudence, excellence in inspection.",
    val currentStreakDays: Int = 0,
    val personalBestPercent: Int? = null,
    val sevenDayAvg: Int? = null,
    val allTimeAvg: Int? = null,
    val totalTestsLogged: Int = 0,
    val daysRemaining: Long = 0,
    val weeksRemaining: Long = 0,
    val extraDaysRemaining: Long = 0,
    val heatmapGrid: List<HeatmapCell> = emptyList(),
    val recent14DaysPercentages: List<Int> = emptyList(),
    val weeklySummaries: List<WeeklySummary> = emptyList()
)

class PrepViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val preferencesManager = PreferencesManager(application)
    val repository = AppRepository(database.appDao(), preferencesManager)

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _selectedTab = MutableStateFlow(MainTab.HOME)

    // One-shot signal: open the "Add time slot" dialog on the Goals/Timetable screen
    // (triggered by the "Add Time Slot" launcher shortcut).
    private val _pendingAddSlot = MutableStateFlow(false)
    val pendingAddSlot: StateFlow<Boolean> = _pendingAddSlot

    fun requestAddSlot() { _pendingAddSlot.value = true }
    fun consumeAddSlot() { _pendingAddSlot.value = false }

    private val _activeSubjectFilter = MutableStateFlow("All")
    private val _activeWeeklyFilter = MutableStateFlow("All")
    private val _completedHabitIds = MutableStateFlow(setOf("jurisprudence_m", "dpco"))

    // Start date for the 70-day heatmap query
    private val seventyDaysAgoStr = LocalDate.now().minusDays(70).format(DateTimeFormatter.ISO_LOCAL_DATE)

    private data class InternalFilters(
        val selectedDate: LocalDate,
        val tab: MainTab,
        val subjectFilter: String,
        val weeklyFilter: String,
        val completedHabits: Set<String>
    )

    private val _filterState = combine(
        _selectedDate,
        _selectedTab,
        _activeSubjectFilter,
        _activeWeeklyFilter,
        _completedHabitIds
    ) { date, tab, subjFilter, weekFilter, habitIds ->
        InternalFilters(date, tab, subjFilter, weekFilter, habitIds)
    }

    val uiState: StateFlow<PrepUiState> = combine(
        repository.userProfile,
        repository.allScores,
        repository.allTopics,
        repository.getTaskLogsSince(seventyDaysAgoStr),
        _filterState
    ) { profile, scores, topics, taskLogs, filters ->
        buildUiState(profile, scores, topics, taskLogs, filters)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PrepUiState()
    )

    private fun buildUiState(
        profile: UserProfile,
        scores: List<ScoreEntry>,
        topics: List<Topic>,
        taskLogs: List<TaskLog>,
        filters: InternalFilters
    ): PrepUiState {
        val today = LocalDate.now()
        val examTargetLocalDate = try {
            if (profile.examDate.isNotBlank()) LocalDate.parse(profile.examDate) else today.plusDays(120)
        } catch (e: Exception) {
            today.plusDays(120)
        }
        val totalDaysRemaining = ChronoUnit.DAYS.between(today, examTargetLocalDate).coerceAtLeast(0)
        val weeks = totalDaysRemaining / 7
        val days = totalDaysRemaining % 7

        val completedDates = taskLogs.filter { it.allDone || it.studyDone || it.examDone || it.exerciseDone || it.vitaminDone }
            .map { it.dateISO }

        val streak = calculateStreak(completedDates, 0)
        val personalBest: Int? = if (scores.isNotEmpty()) scores.maxOf { it.percentage } else null
        val recentScores = scores.take(7)
        val sevenDayAvg: Int? = if (recentScores.isNotEmpty()) {
            recentScores.map { it.percentage }.average().toInt()
        } else null
        val allTimeAvg: Int? = if (scores.isNotEmpty()) {
            scores.map { it.percentage }.average().toInt()
        } else null

        // Single O(1) query generated heatmap
        val heatmapCells = generateHeatmapCells(today, filters.selectedDate, taskLogs)
        val recent14Scores = if (scores.isNotEmpty()) {
            scores.take(14).map { it.percentage }.reversed()
        } else {
            emptyList()
        }

        val weeklySummaries = generateWeeklySummaries(scores)

        val completedHabitIds = filters.completedHabits

        // Build subjects from the user's custom subject list using topic-rollup percentages.
        val subjectNames = profile.customSubjects.ifEmpty {
            ExamDataConstants.ALL_SUBJECTS.map { it.name }
        }
        val dynamicSubjects = subjectNames.map { subjectName ->
            val subjectTopics = topics.filter { it.subject == subjectName }
            val hasTopics = subjectTopics.isNotEmpty()
            val topicAvgs = subjectTopics.mapNotNull { topic ->
                val topicScores = scores.filter { it.topicId == topic.id }
                if (topicScores.isNotEmpty()) topicScores.map { it.percentage }.average().toInt()
                else null
            }
            val pct = if (topicAvgs.isNotEmpty()) topicAvgs.average().toInt() else 0
            val testCount = subjectTopics.sumOf { topic ->
                scores.count { it.topicId == topic.id }
            }
            val status = when {
                !hasTopics || pct == 0 -> SubjectStatus.NEEDS_WORK
                pct >= 75 -> SubjectStatus.MASTERED
                pct >= 50 -> SubjectStatus.IN_PROGRESS
                else -> SubjectStatus.NEEDS_WORK
            }
            Subject(
                id = subjectName.lowercase().replace(" ", "_"),
                name = subjectName,
                status = status,
                completionPercent = pct,
                testCount = testCount,
                hasTopics = hasTopics
            )
        }

        val baseHabits = listOf(
            HabitLogItem("jurisprudence_m", "Schedule M (GMP Standards)", "Jurisprudence • 45 min session", completedHabitIds.contains("jurisprudence_m")),
            HabitLogItem("dpco", "Drug Price Control Order (DPCO)", "Economics & Laws • Pending", completedHabitIds.contains("dpco")),
            HabitLogItem("multivitamin", "Daily Multivitamin (1:30 PM)", "Cognitive stamina & immunity", completedHabitIds.contains("multivitamin")),
            HabitLogItem("exercise", "Evening 20-min brisk walk", "Physical recovery & stress relief", completedHabitIds.contains("exercise"))
        )

        val todayQuote = MotivationalQuotes.getQuoteForDay(today.dayOfYear)

        return PrepUiState(
            userProfile = profile,
            selectedDate = filters.selectedDate,
            dailyTasks = emptyList(),
            dailyHabits = baseHabits,
            allScores = scores,
            subjects = dynamicSubjects,
            selectedTab = filters.tab,
            activeSubjectFilter = filters.subjectFilter,
            activeWeeklyFilter = filters.weeklyFilter,
            dailyQuote = todayQuote,
            currentStreakDays = streak,
            personalBestPercent = personalBest,
            sevenDayAvg = sevenDayAvg,
            allTimeAvg = allTimeAvg,
            totalTestsLogged = scores.size,
            daysRemaining = totalDaysRemaining,
            weeksRemaining = weeks,
            extraDaysRemaining = days,
            heatmapGrid = heatmapCells,
            recent14DaysPercentages = recent14Scores,
            weeklySummaries = weeklySummaries
        )
    }

    private fun calculateStreak(completedDates: List<String>, fallback: Int): Int {
        if (completedDates.isEmpty()) return fallback
        val today = LocalDate.now()
        var streak = 0
        var checkDate = today
        val dateSet = completedDates.toSet()

        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val yesterdayStr = today.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        if (!dateSet.contains(todayStr) && !dateSet.contains(yesterdayStr)) {
            return 0
        }

        if (!dateSet.contains(todayStr)) {
            checkDate = today.minusDays(1)
        }

        while (dateSet.contains(checkDate.format(DateTimeFormatter.ISO_LOCAL_DATE))) {
            streak++
            checkDate = checkDate.minusDays(1)
        }
        return streak
    }

    private fun generateHeatmapCells(
        today: LocalDate,
        selectedDate: LocalDate,
        taskLogs: List<TaskLog>
    ): List<HeatmapCell> {
        val cells = mutableListOf<HeatmapCell>()
        val startOfGrid = today.minusWeeks(9).with(DayOfWeek.MONDAY)
        val logMap = taskLogs.associateBy { it.dateISO }

        for (i in 0 until 70) {
            val cellDate = startOfGrid.plusDays(i.toLong())
            val dateStr = cellDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val log = logMap[dateStr]

            val intensity = when {
                log?.allDone == true -> 3
                log != null && (log.studyDone || log.examDone) -> 2
                log != null && (log.exerciseDone || log.vitaminDone) -> 1
                cellDate.isAfter(today) -> 0
                else -> 0
            }

            cells.add(
                HeatmapCell(
                    date = cellDate,
                    dayOfWeekIndex = cellDate.dayOfWeek.value - 1,
                    intensityLevel = intensity,
                    isToday = cellDate.isEqual(today),
                    isSelected = cellDate.isEqual(selectedDate)
                )
            )
        }
        return cells
    }

    private fun generateWeeklySummaries(scores: List<ScoreEntry>): List<WeeklySummary> {
        val today = LocalDate.now()
        val summaries = mutableListOf<WeeklySummary>()
        val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")

        for (i in 0 until 6) {
            val weekStart = today.minusWeeks(i.toLong()).with(DayOfWeek.MONDAY)
            val weekEnd = weekStart.plusDays(6)
            val startDateStr = weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val endDateStr = weekEnd.format(DateTimeFormatter.ISO_LOCAL_DATE)

            val weekScores = scores.filter { it.dateISO in startDateStr..endDateStr }
            val avgScore = if (weekScores.isNotEmpty()) {
                weekScores.map { it.percentage }.average().toInt()
            } else {
                0
            }

            val status = when {
                avgScore >= 80 -> StatusColorType.SUCCESS
                avgScore in 60..79 -> StatusColorType.WARNING
                else -> StatusColorType.CRITICAL
            }

            val title = when (i) {
                0 -> "This Week (Current)"
                1 -> "Last Week"
                else -> "$i Weeks Ago"
            }

            val dateRange = "${weekStart.format(dateFormatter)} - ${weekEnd.format(dateFormatter)}"

            summaries.add(
                WeeklySummary(
                    title = title,
                    dateRange = dateRange,
                    averageScore = avgScore,
                    testsLoggedCount = weekScores.size,
                    statusColorType = status
                )
            )
        }
        return summaries
    }

    fun onTabSelected(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun toggleHabit(habitId: String) {
        val currentSet = _completedHabitIds.value.toMutableSet()
        val isDone = !currentSet.contains(habitId)
        if (isDone) {
            currentSet.add(habitId)
        } else {
            currentSet.remove(habitId)
        }
        _completedHabitIds.value = currentSet

        viewModelScope.launch {
            val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val taskType = when (habitId) {
                "jurisprudence_m", "dpco" -> "study"
                "exercise" -> "exercise"
                "multivitamin" -> "vitamin"
                else -> "study"
            }
            repository.toggleTask(todayStr, taskType, isDone)
        }
    }

    fun logScore(
        subjectName: String,
        marksObtained: Int,
        totalMarks: Int,
        notes: String = "",
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            repository.logScoreAndMarkExam(todayStr, subjectName, marksObtained, totalMarks)
            onSuccess()
        }
    }

    fun saveUserName(name: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateName(name)
            onComplete()
        }
    }

    fun updateMultivitaminReminder(enabled: Boolean, hour: Int = 13, minute: Int = 30) {
        viewModelScope.launch {
            repository.updateReminderEnabled(enabled)
            repository.updateReminderTime(hour, minute)
            val context = getApplication<Application>().applicationContext
            if (enabled) {
                val profile = repository.userProfile.first()
                NotificationHelper.scheduleMultivitaminReminder(
                    context = context,
                    userName = profile.name,
                    targetHour = hour,
                    targetMinute = minute
                )
            } else {
                NotificationHelper.cancelReminder(context, NotificationHelper.ID_MULTIVITAMIN)
            }
        }
    }

    fun updateExamTarget(dateISO: String) {
        viewModelScope.launch {
            repository.updateExamDate(dateISO)
        }
    }

    fun setSubjectFilter(filter: String) {
        _activeSubjectFilter.value = filter
    }

    fun setWeeklyFilter(filter: String) {
        _activeWeeklyFilter.value = filter
    }

    /**
     * Checks DataStore flag (key: "reminder_scheduled_date").
     * If today's date != stored date, schedules new one-time exact alarm for today at 1:30 PM,
     * stores today's date in DataStore, fires once and stops.
     */
    fun checkAndScheduleDailyMultivitaminReminder(
        onExactAlarmDenied: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val profile = repository.userProfile.first()
            val lastScheduledDate = profile.reminderScheduledDate

            if (todayStr != lastScheduledDate && profile.reminderEnabled) {
                val context = getApplication<Application>().applicationContext
                val isExactScheduled = NotificationHelper.scheduleMultivitaminReminder(
                    context = context,
                    userName = profile.name,
                    targetHour = profile.reminderHour,
                    targetMinute = profile.reminderMinute
                )
                repository.updateReminderScheduledDate(todayStr)

                if (!isExactScheduled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    if (!alarmManager.canScheduleExactAlarms()) {
                        onExactAlarmDenied()
                    }
                }
            }
        }
    }
}

object MotivationalQuotes {
    private val quotes = listOf(
        "Precision in jurisprudence, excellence in inspection.",
        "Quality is never an accident; it is always the result of intelligent effort.",
        "Master the schedules today, protect public health tomorrow.",
        "Schedule M compliance begins with rigorous individual discipline.",
        "Pharmacology demands clarity, dedication, and clinical accuracy.",
        "Every page of jurisprudence studied is a step closer to the inspector's badge.",
        "The Drug Inspector ensures efficacy, safety, and uncompromising quality."
    )

    fun getQuoteForDay(dayOfYear: Int): String {
        return quotes[dayOfYear % quotes.size]
    }
}
