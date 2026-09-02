package com.anubhav.diprep.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anubhav.diprep.data.datastore.PreferencesManager
import com.anubhav.diprep.data.datastore.UserProfile
import com.anubhav.diprep.data.local.db.AppDatabase
import com.anubhav.diprep.data.local.db.ScoreEntry
import com.anubhav.diprep.data.local.db.TaskLog
import com.anubhav.diprep.data.local.db.TimetableCompletion
import com.anubhav.diprep.data.local.db.TimetableSlot
import com.anubhav.diprep.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class Quote(
    val text: String,
    val author: String
)

data class WhatsNext(
    val label: String,
    val startTime: String,
    val endTime: String
)

data class WeakTopic(
    val subject: String,
    val reason: String
)

data class TodayGoals(
    val done: Int,
    val total: Int
)

data class RevisionDebtTopic(
    val subject: String,
    val topicName: String,
    val daysSince: Long
)

data class EndOfDayRecap(
    val minutesStudied: Int,
    val subjectsTested: List<String>,
    val avgScoreToday: Int?,
    val tasksDone: Int,
    val tasksTotal: Int
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val DEFAULT_RUMOURED_DATE = "2026-12-20"

        val QUOTES = listOf(
            Quote("The difference between ordinary and extraordinary is that little extra.", "Jimmy Johnson"),
            Quote("Continuous effort – not strength or intelligence – is the key to unlocking our potential.", "Winston Churchill"),
            Quote("Mastery of pharmacology and jurisprudence begins with daily consistency.", "Exam Prep Guide"),
            Quote("Success is the sum of small efforts, repeated day in and day out.", "Robert Collier"),
            Quote("The expert in anything was once a beginner.", "Helen Hayes"),
            Quote("Quality is not an act, it is a habit.", "Aristotle"),
            Quote("Discipline is the bridge between goals and accomplishment.", "Jim Rohn"),
            Quote("Study hard, for the health and safety of the public relies on vigilant professionals.", "Inspector Code"),
            Quote("Believe you can and you're halfway there.", "Theodore Roosevelt"),
            Quote("Action is the foundational key to all success.", "Pablo Picasso"),
            Quote("Little by little, one travels far.", "J.R.R. Tolkien"),
            Quote("Precision in study builds excellence in regulatory inspection and pharmacy practice.", "Prep Academy"),
            Quote("Focus on the process, and the score will take care of itself.", "Anonymous"),
            Quote("Hard work beats talent when talent doesn't work hard.", "Tim Notke"),
            Quote("Your daily dedication today safeguards healthcare tomorrow.", "Pharmacy Aspirant")
        )

        fun isDayComplete(
            slotsForDow: List<TimetableSlot>,
            completedSlotIds: Set<Long>,
            taskLog: TaskLog?,
            profile: UserProfile
        ): Boolean = com.anubhav.diprep.util.StreakCalculator.isDayComplete(
            slotsForDow, completedSlotIds, taskLog, profile
        )
    }

    private val database = AppDatabase.getDatabase(application)
    private val preferencesManager = PreferencesManager(application)
    private val repository = AppRepository(database.appDao(), preferencesManager)

    val userName: StateFlow<String> = repository.userProfile
        .map { it.name.ifBlank { "Candidate" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Candidate")

    val examDate: StateFlow<String> = repository.userProfile
        .map { it.examDate.ifBlank { DEFAULT_RUMOURED_DATE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_RUMOURED_DATE)

    val isDateRumoured: StateFlow<Boolean> = examDate
        .map { it == DEFAULT_RUMOURED_DATE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val formattedExamDate: StateFlow<String> = examDate
        .map { dateIso ->
            try {
                val parsed = LocalDate.parse(dateIso)
                parsed.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
            } catch (_: Exception) {
                dateIso
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "20 Dec 2026")

    val daysRemaining: StateFlow<Long> = examDate
        .map { dateIso ->
            try {
                val exam = LocalDate.parse(dateIso)
                val today = LocalDate.now()
                ChronoUnit.DAYS.between(today, exam).coerceAtLeast(0)
            } catch (_: Exception) {
                0L
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val weeksAndDays: StateFlow<String> = daysRemaining
        .map { days ->
            val weeks = days / 7
            val remDays = days % 7
            when {
                weeks > 0 && remDays > 0 -> "$weeks weeks, $remDays days left"
                weeks > 0 -> "$weeks weeks left"
                else -> "$remDays days left"
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _todayFormatted = MutableStateFlow(formatTodayFullDate())
    val todayFormatted: StateFlow<String> = _todayFormatted.asStateFlow()

    private val _todayQuote = MutableStateFlow(calculateTodayQuote())
    val todayQuote: StateFlow<Quote> = _todayQuote.asStateFlow()

    val personalBest: StateFlow<Int?> = repository.maxScorePercentage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayProgress: StateFlow<Int> = repository.getTaskLogForDate(LocalDate.now().toString())
        .map { log ->
            if (log != null) {
                var count = 0
                if (log.studyDone) count++
                if (log.examDone) count++
                if (log.exerciseDone) count++
                if (log.vitaminDone) count++
                count
            } else {
                0
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val streakCount: StateFlow<Int> = combine(
        repository.allTaskLogsDesc,
        repository.getAllSlots(),
        repository.getCompletionsSince("2020-01-01"),
        repository.userProfile
    ) { logs, slots, completions, profile ->
        calculateStreakFromLogs(logs, slots, completions, profile)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- Demo Mode ---
    val isDemoMode: StateFlow<Boolean> = repository.userProfile
        .map { it.isDemoMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun exitDemo(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetApp()
            launch(Dispatchers.Main) { onComplete() }
        }
    }

    // --- Home personalization: ordered list of visible section keys ---
    val visibleHomeSections: StateFlow<List<String>> = repository.userProfile
        .map { profile -> profile.homeSectionOrder.filter { it !in profile.homeHiddenSections } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- What's next: the next timetable slot for today that hasn't started yet ---
    private val todayDayOfWeek = LocalDate.now().dayOfWeek.value // 1=Mon..7=Sun
    private val slotTimeFmt = DateTimeFormatter.ofPattern("HH:mm")

    val whatsNext: StateFlow<WhatsNext?> = repository.getSlotsForDay(todayDayOfWeek)
        .map { slots -> pickWhatsNext(slots) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Today's goals: today's timetable slots completed vs planned ---
    val todayGoals: StateFlow<TodayGoals> = combine(
        repository.getSlotsForDay(todayDayOfWeek),
        repository.getCompletedSlotIdsForDate(LocalDate.now().toString())
    ) { slots, completedIds ->
        val completedSet = completedIds.toSet()
        TodayGoals(done = slots.count { it.id in completedSet }, total = slots.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayGoals(0, 0))

    // --- Overall mastery: average of all subjects' score averages (for Syllabus Ring) ---
    val overallMastery: StateFlow<Int> = combine(
        repository.allScores,
        repository.userProfile
    ) { scores, profile ->
        val bySubject = scores.groupBy { it.subject }
        val avgs = profile.customSubjects.map { subject ->
            bySubject[subject]?.map { it.percentage }?.average()?.toInt() ?: 0
        }
        if (avgs.isEmpty()) 0 else avgs.average().toInt()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- Weak topic alert: worst subject that is below 50% avg OR untouched 5+ days ---
    val weakTopic: StateFlow<WeakTopic?> = combine(
        repository.allScores,
        repository.userProfile
    ) { scores, profile ->
        calculateWeakTopic(scores, profile)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Revision debt: the single topic with the highest days-since-last-score (7+ days only) ---
    val revisionDebt: StateFlow<RevisionDebtTopic?> = combine(
        repository.allTopics,
        repository.allScores
    ) { topics, scores ->
        if (topics.isEmpty()) return@combine null
        val today = LocalDate.now()
        topics.mapNotNull { topic ->
            val topicScores = scores.filter { it.topicId == topic.id }
            if (topicScores.isEmpty()) return@mapNotNull null
            val lastDate = topicScores
                .mapNotNull { runCatching { LocalDate.parse(it.dateISO) }.getOrNull() }
                .maxOrNull() ?: return@mapNotNull null
            val daysSince = ChronoUnit.DAYS.between(lastDate, today).coerceAtLeast(0)
            if (daysSince < 7) return@mapNotNull null
            RevisionDebtTopic(topic.subject, topic.topicName, daysSince)
        }.maxByOrNull { it.daysSince }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- End-of-Day Recap (shown after 6 PM) ---
    val isEvening: Boolean = LocalTime.now().hour >= 18

    val endOfDayRecap: StateFlow<EndOfDayRecap> = combine(
        repository.getSlotsForDay(todayDayOfWeek),
        repository.getCompletedSlotIdsForDate(LocalDate.now().toString()),
        repository.allScores
    ) { slots, completedIds, scores ->
        val completedSet = completedIds.toSet()
        val minutesStudied = slots.filter { it.id in completedSet }.sumOf { slot ->
            try {
                val start = LocalTime.parse(slot.startTime, slotTimeFmt)
                val end = LocalTime.parse(slot.endTime, slotTimeFmt)
                ChronoUnit.MINUTES.between(start, end).coerceAtLeast(0)
            } catch (_: Exception) { 0L }
        }.toInt()
        val todayStr = LocalDate.now().toString()
        val todayScores = scores.filter { it.dateISO == todayStr }
        val avgToday = if (todayScores.isEmpty()) null else todayScores.map { it.percentage }.average().toInt()
        EndOfDayRecap(
            minutesStudied = minutesStudied,
            subjectsTested = todayScores.map { it.subject }.distinct(),
            avgScoreToday = avgToday,
            tasksDone = slots.count { it.id in completedSet },
            tasksTotal = slots.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EndOfDayRecap(0, emptyList(), null, 0, 0))

    // --- Milestone Celebrations ---
    private val _pendingMilestone = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val pendingMilestone: SharedFlow<String> = _pendingMilestone.asSharedFlow()

    init {
        refreshDailyMetrics()
        viewModelScope.launch(Dispatchers.IO) {
            checkMilestones()
        }
    }

    private suspend fun checkMilestones() {
        val count = repository.totalTestsCount.first()
        val scores = repository.allScores.first()
        val profile = repository.userProfile.first()
        val logs = repository.allTaskLogsDesc.first()
        val slots = repository.getAllSlots().first()
        val completions = repository.getCompletionsSince("2020-01-01").first()
        val celebrated = profile.celebratedMilestones
        val streak = calculateStreakFromLogs(logs, slots, completions, profile)

        val milestone: Pair<String, String>? = when {
            count >= 100 && "tests_100" !in celebrated -> "tests_100" to "100 tests logged — exam-ready mindset!"
            count >= 50 && "tests_50" !in celebrated -> "tests_50" to "50 tests logged — incredible consistency!"
            count >= 25 && "tests_25" !in celebrated -> "tests_25" to "25 tests down — keep pushing!"
            count >= 10 && "tests_10" !in celebrated -> "tests_10" to "10 tests logged — great start!"
            else -> {
                val bySubject = scores.groupBy { it.subject }
                val hasHighMastery = bySubject.any { (_, entries) ->
                    entries.isNotEmpty() && entries.map { it.percentage }.average() >= 90.0
                }
                when {
                    hasHighMastery && "mastery_90" !in celebrated -> "mastery_90" to "A subject hit 90%+ mastery — outstanding!"
                    streak >= 100 && "streak_100" !in celebrated -> "streak_100" to "100-day streak — legendary dedication!"
                    streak >= 30 && "streak_30" !in celebrated -> "streak_30" to "30-day streak — unstoppable!"
                    streak >= 7 && "streak_7" !in celebrated -> "streak_7" to "7-day streak — great momentum!"
                    else -> null
                }
            }
        }

        milestone?.let { (key, message) ->
            repository.addCelebratedMilestone(key)
            _pendingMilestone.emit(message)
        }
    }

    private fun refreshDailyMetrics() {
        _todayQuote.value = calculateTodayQuote()
        _todayFormatted.value = formatTodayFullDate()
    }

    private fun calculateTodayQuote(): Quote {
        val dayOfYear = LocalDate.now().dayOfYear
        val index = (dayOfYear % QUOTES.size).coerceIn(0, QUOTES.size - 1)
        return QUOTES[index]
    }

    private fun formatTodayFullDate(): String {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)
        return today.format(formatter)
    }

    private fun pickWhatsNext(slots: List<TimetableSlot>): WhatsNext? {
        val now = LocalTime.now()
        val upcoming = slots
            .mapNotNull { slot ->
                runCatching { LocalTime.parse(slot.startTime, slotTimeFmt) }.getOrNull()?.let { it to slot }
            }
            .filter { it.first.isAfter(now) }
            .minByOrNull { it.first }
            ?.second
            ?: slots.firstOrNull() // nothing left today — surface the first slot as a preview
        return upcoming?.let { WhatsNext(it.label, it.startTime, it.endTime) }
    }

    private fun calculateWeakTopic(scores: List<ScoreEntry>, profile: UserProfile): WeakTopic? {
        if (scores.isEmpty()) return null
        val today = LocalDate.now()
        val bySubject = scores.groupBy { it.subject }

        data class Candidate(val subject: String, val avg: Int, val daysSince: Long, val severity: Int)

        val candidates = bySubject.mapNotNull { (subject, entries) ->
            if (entries.isEmpty()) return@mapNotNull null
            val avg = entries.map { it.percentage }.average().toInt()
            val lastDate = entries.mapNotNull { runCatching { LocalDate.parse(it.dateISO) }.getOrNull() }.maxOrNull()
                ?: return@mapNotNull null
            val daysSince = ChronoUnit.DAYS.between(lastDate, today).coerceAtLeast(0)
            val isWeak = avg < 50 || daysSince >= 5
            if (!isWeak) return@mapNotNull null
            // Lower score = more severe; long gaps add urgency.
            val severity = (100 - avg) + (daysSince * 3).toInt()
            Candidate(subject, avg, daysSince, severity)
        }

        val worst = candidates.maxByOrNull { it.severity } ?: return null
        val reason = buildString {
            append("${worst.avg}% average")
            if (worst.daysSince >= 5) append(" · not studied in ${worst.daysSince} days")
        }
        return WeakTopic(worst.subject, reason)
    }

    private fun calculateStreakFromLogs(
        logs: List<TaskLog>,
        slots: List<TimetableSlot>,
        completions: List<TimetableCompletion>,
        profile: UserProfile
    ): Int = com.anubhav.diprep.util.StreakCalculator.calculate(logs, slots, completions, profile)

}
