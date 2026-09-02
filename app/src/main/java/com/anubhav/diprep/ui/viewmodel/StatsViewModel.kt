package com.anubhav.diprep.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anubhav.diprep.data.datastore.PreferencesManager
import com.anubhav.diprep.data.local.db.AppDatabase
import com.anubhav.diprep.data.local.db.ScoreEntry
import com.anubhav.diprep.data.local.db.TaskLog
import com.anubhav.diprep.data.local.db.TimetableCompletion
import com.anubhav.diprep.data.local.db.TimetableSlot
import com.anubhav.diprep.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class BadgeType {
    STRONG,
    IMPROVING,
    WEAK,
    UNTESTED
}

data class SubjectInsight(
    val subject: String,
    val avg: Int?,
    val count: Int,
    val badge: BadgeType
)

data class ExamSummary(
    val daysLeft: Long = 0,
    val stream: String = "Drug Inspector",
    val examDate: String = "2026-12-20",
    val formattedExamDate: String = "20 Dec 2026",
    val streak: Int = 0,
    val totalTests: Int = 0
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val preferencesManager = PreferencesManager(application)
    private val repository = AppRepository(database.appDao(), preferencesManager)

    private val seventyDaysAgoIso = LocalDate.now().minusDays(70).toString()

    val heatmapData: StateFlow<List<TaskLog>> = repository.getTaskLogsSince(seventyDaysAgoIso)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSlots: StateFlow<List<TimetableSlot>> = repository.getAllSlots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val heatmapCompletions: StateFlow<List<TimetableCompletion>> =
        repository.getCompletionsSince(seventyDaysAgoIso)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<com.anubhav.diprep.data.datastore.UserProfile> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.anubhav.diprep.data.datastore.UserProfile())

    val allSubjects: StateFlow<List<String>> = repository.userProfile
        .map { it.customSubjects }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scoreHistory: StateFlow<List<ScoreEntry>> = repository.allScores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalTests: StateFlow<Int> = repository.totalTestsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val streakCount: StateFlow<Int> = combine(
        repository.allTaskLogsDesc,
        repository.getAllSlots(),
        repository.getCompletionsSince("2020-01-01"),
        repository.userProfile
    ) { logs, slots, completions, profile ->
        calculateStreak(logs, slots, completions, profile)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val examSummary: StateFlow<ExamSummary> = combine(
        repository.userProfile,
        streakCount,
        totalTests
    ) { profile, streak, total ->
        val days = try {
            val exam = LocalDate.parse(profile.examDate)
            val today = LocalDate.now()
            ChronoUnit.DAYS.between(today, exam).coerceAtLeast(0)
        } catch (_: Exception) {
            0L
        }

        val formattedDate = try {
            val parsed = LocalDate.parse(profile.examDate)
            parsed.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
        } catch (_: Exception) {
            profile.examDate
        }

        ExamSummary(
            daysLeft = days,
            stream = profile.examStream,
            examDate = profile.examDate,
            formattedExamDate = formattedDate,
            streak = streak,
            totalTests = total
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExamSummary())

    val subjectInsights: StateFlow<List<SubjectInsight>> = combine(
        allSubjects,
        repository.allScores
    ) { subjects, scores ->
        val scoresBySubject = scores.groupBy { it.subject.trim().lowercase() }

        val insights = subjects.map { subj ->
            val matchingScores = scoresBySubject[subj.trim().lowercase()] ?: emptyList()
            if (matchingScores.isNotEmpty()) {
                val avgPct = matchingScores.map { it.percentage }.average().toInt()
                val badge = when {
                    avgPct >= 75 -> BadgeType.STRONG
                    avgPct >= 50 -> BadgeType.IMPROVING
                    else -> BadgeType.WEAK
                }
                SubjectInsight(
                    subject = subj,
                    avg = avgPct,
                    count = matchingScores.size,
                    badge = badge
                )
            } else {
                SubjectInsight(
                    subject = subj,
                    avg = null,
                    count = 0,
                    badge = BadgeType.UNTESTED
                )
            }
        }

        // Sort: Tested subjects by avg descending, untested at bottom
        insights.sortedWith(
            compareByDescending<SubjectInsight> { it.count > 0 }
                .thenByDescending { it.avg ?: -1 }
                .thenBy { it.subject }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun calculateStreak(
        logs: List<TaskLog>,
        slots: List<TimetableSlot>,
        completions: List<TimetableCompletion>,
        profile: com.anubhav.diprep.data.datastore.UserProfile
    ): Int {
        val logsByDate = logs.associateBy { it.dateISO }
        val slotsByDow = slots.groupBy { it.dayOfWeek }
        val completionsByDate = completions.groupBy { it.dateISO }
            .mapValues { (_, list) -> list.map { it.slotId }.toSet() }

        val today = LocalDate.now()
        var streak = 1  // Today always counts (grace period)

        fun isDone(date: LocalDate): Boolean {
            val iso = date.toString()
            val slotsForDow = slotsByDow[date.dayOfWeek.value] ?: emptyList()
            val doneIds = completionsByDate[iso] ?: emptySet()
            return HomeViewModel.isDayComplete(slotsForDow, doneIds, logsByDate[iso], profile)
        }

        var check = today.minusDays(1)
        while (isDone(check)) {
            streak++
            check = check.minusDays(1)
        }
        return streak
    }

    fun deleteScore(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteScore(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllScores()
        }
    }
}
