package com.anubhav.diprep.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anubhav.diprep.data.datastore.PreferencesManager
import com.anubhav.diprep.data.datastore.UserProfile
import com.anubhav.diprep.data.local.db.AppDatabase
import com.anubhav.diprep.data.local.db.TaskLog
import com.anubhav.diprep.data.local.db.TimetableSlot
import com.anubhav.diprep.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class TimetableViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val preferencesManager = PreferencesManager(application)
    private val repository = AppRepository(database.appDao(), preferencesManager)

    private val todayIso = LocalDate.now().toString()
    private val todayDayOfWeek = LocalDate.now().dayOfWeek.value // 1=Mon…7=Sun

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val todayLog: StateFlow<TaskLog> = repository.getTaskLogForDate(todayIso)
        .map { it ?: TaskLog(dateISO = todayIso) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskLog(dateISO = todayIso))

    val todaySlots: StateFlow<List<TimetableSlot>> =
        repository.getSlotsForDay(todayDayOfWeek)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSlots: StateFlow<List<TimetableSlot>> =
        repository.getAllSlots()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedSlotIds: StateFlow<Set<Long>> =
        repository.getCompletedSlotIdsForDate(todayIso)
            .map { it.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    val whatsNext: StateFlow<TimetableSlot?> =
        repository.getSlotsForDay(todayDayOfWeek)
            .map { slots ->
                val now = LocalTime.now()
                slots.firstOrNull { slot ->
                    try { LocalTime.parse(slot.startTime, timeFmt).isAfter(now) }
                    catch (_: Exception) { false }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addSlot(dayOfWeek: Int, startTime: String, endTime: String, label: String, focusModeEnabled: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTimetableSlot(
                TimetableSlot(
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime,
                    label = label,
                    focusModeEnabled = focusModeEnabled
                )
            )
        }
    }

    fun deleteSlot(slotId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTimetableSlot(slotId)
        }
    }

    fun toggleSlotCompletion(slotId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleTimetableCompletion(slotId, todayIso)
        }
    }

    val isFocusSessionActive: StateFlow<Boolean> = repository.userProfile
        .map { it.isFocusSessionActive }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleFocusSession() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.userProfile.first()
            repository.updateFocusSessionActive(!current.isFocusSessionActive)
        }
    }

    fun toggleTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getTaskLogForDate(todayIso).first()
                ?: TaskLog(dateISO = todayIso)
            val updated = when (taskId) {
                "exercise" -> current.copy(exerciseDone = !current.exerciseDone)
                "vitamin"  -> current.copy(vitaminDone  = !current.vitaminDone)
                else       -> current
            }
            repository.updateTaskLog(updated)
        }
    }
}
