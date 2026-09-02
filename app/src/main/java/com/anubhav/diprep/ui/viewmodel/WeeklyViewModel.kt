package com.anubhav.diprep.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anubhav.diprep.data.datastore.PreferencesManager
import com.anubhav.diprep.data.local.db.AppDatabase
import com.anubhav.diprep.data.repository.AppRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class WeekSummary(
    val weekIndex: Int,
    val label: String,
    val startDate: String,
    val endDate: String,
    val avg: Int?,
    val count: Int,
    val hasData: Boolean
)

data class WeekComparison(
    val thisWeekAvg: Int?,
    val lastWeekAvg: Int?
)

@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val preferencesManager = PreferencesManager(application)
    private val repository = AppRepository(database.appDao(), preferencesManager)

    val subjects: StateFlow<List<String>> = repository.userProfile
        .map { it.customSubjects }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val examDate: StateFlow<String> = repository.userProfile
        .map { it.examDate.ifBlank { "2026-12-20" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "2026-12-20")

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

    val selectedSubject = MutableStateFlow("ALL")

    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    // Tracks which subjects have score entries in current week to control 0.5f alpha on filter chips
    val subjectsWithDataThisWeek: StateFlow<Set<String>> = repository.allScores
        .map { scores ->
            val today = LocalDate.now()
            val currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val currentSunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            val startIso = currentMonday.toString()
            val endIso = currentSunday.toString()

            scores.filter { it.dateISO in startIso..endIso }
                .map { it.subject.trim().lowercase() }
                .toSet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val weekSummaries: StateFlow<List<WeekSummary>> = combine(
        selectedSubject,
        repository.allScores
    ) { subject, _ -> subject }
        .flatMapLatest { subject ->
            flow {
                val today = LocalDate.now()
                val currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val currentSunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

                val summaries = (0..5).map { weekIndex ->
                    val start = currentMonday.minusWeeks(weekIndex.toLong())
                    val end = currentSunday.minusWeeks(weekIndex.toLong())

                    val startIso = start.toString()
                    val endIso = end.toString()

                    val label = when (weekIndex) {
                        0 -> "This week"
                        1 -> "Last week"
                        else -> "${start.format(dateFormatter)} – ${end.format(dateFormatter)}"
                    }

                    val row = repository.getWeekSummarySync(startIso, endIso, subject)
                    val hasData = row.count > 0 && row.avgPercentage != null
                    WeekSummary(
                        weekIndex = weekIndex,
                        label = label,
                        startDate = startIso,
                        endDate = endIso,
                        avg = row.avgPercentage?.toInt(),
                        count = row.count,
                        hasData = hasData
                    )
                }
                emit(summaries)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weekComparison: StateFlow<WeekComparison> = weekSummaries
        .map { list ->
            WeekComparison(
                thisWeekAvg = list.find { it.weekIndex == 0 }?.avg,
                lastWeekAvg = list.find { it.weekIndex == 1 }?.avg
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeekComparison(null, null))

    fun selectSubject(subject: String) {
        selectedSubject.value = subject
    }
}
