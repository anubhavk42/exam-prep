package com.anubhav.diprep.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anubhav.diprep.data.datastore.ExamPresets
import com.anubhav.diprep.data.datastore.PreferencesManager
import com.anubhav.diprep.data.datastore.UserProfile
import com.anubhav.diprep.data.local.db.AppDatabase
import com.anubhav.diprep.data.repository.AppRepository
import com.anubhav.diprep.receiver.NotificationHelper
import com.anubhav.diprep.util.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(application)
    private val preferencesManager = PreferencesManager(application)
    private val repository = AppRepository(database.appDao(), preferencesManager)

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val totalScoresCount: StateFlow<Int> = repository.totalTestsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val currentLanguage: StateFlow<String> = repository.userProfile
        .map { it.appLanguage }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocaleHelper.currentTag(context))

    fun updateLanguage(tag: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAppLanguage(tag)
        }
        // Applies immediately (framework recreates the activity) — no manual restart.
        LocaleHelper.setLanguage(context, tag)
    }

    fun updateName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty() && trimmed.length <= 20) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateName(trimmed)
            }
        }
    }

    fun updateExamDate(dateIso: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateExamDate(dateIso)
        }
    }

    fun toggleExamDateConfirmed(confirmed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateExamDateConfirmed(confirmed)
        }
    }

    fun updateStream(stream: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val presets = ExamPresets.getPresetSubjects(stream)
            if (presets.isNotEmpty()) {
                repository.updateExamStreamAndSubjects(stream, presets)
            } else {
                repository.updateExamStreamAndSubjects(stream, userProfile.value.customSubjects)
            }
        }
    }

    /** Free-text custom exam name — keeps the existing subject list. */
    fun updateStreamName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateExamStreamAndSubjects(trimmed, userProfile.value.customSubjects)
        }
    }

    fun updateCustomSubjects(subjects: List<String>) {
        if (subjects.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateCustomSubjects(subjects)
            }
        }
    }

    fun toggleReminder(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateReminderEnabled(enabled)
            if (enabled) {
                val profile = userProfile.value
                NotificationHelper.scheduleDailyReminder(context, profile.reminderHour, profile.reminderMinute)
            } else {
                NotificationHelper.cancelDailyReminder(context)
            }
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateReminderTime(hour, minute)
            if (userProfile.value.reminderEnabled) {
                NotificationHelper.scheduleDailyReminder(context, hour, minute)
            }
        }
    }

    fun updateThemeMode(themeMode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateThemeMode(themeMode)
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDynamicColor(enabled)
        }
    }

    fun toggleHapticFeedback(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateHapticFeedbackEnabled(enabled)
        }
    }

    fun toggleVitaminReminder(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateVitaminReminderEnabled(enabled)
        }
    }

    fun toggleExerciseReminder(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateExerciseReminderEnabled(enabled)
        }
    }

    fun setHomeSectionHidden(key: String, hidden: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateHomeSectionHidden(key, hidden)
        }
    }

    fun moveHomeSection(key: String, up: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val order = userProfile.value.homeSectionOrder.toMutableList()
            val index = order.indexOf(key)
            if (index < 0) return@launch
            val target = if (up) index - 1 else index + 1
            if (target !in order.indices) return@launch
            order[index] = order[target].also { order[target] = order[index] }
            repository.updateHomeSectionOrder(order)
        }
    }

    // --- Focus Mode / Notification Filter ---

    fun toggleNotificationFilter(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateNotificationFilterEnabled(enabled)
        }
    }

    fun updateMutedAppPackages(packages: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMutedAppPackages(packages)
        }
    }

    fun updateFilterActivationMode(mode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFilterActivationMode(mode)
        }
    }

    fun clearScores() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllScores()
        }
    }

    fun loadDemoData(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.loadDemoData()
            launch(Dispatchers.Main) { onComplete() }
        }
    }

    fun resetApp(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            NotificationHelper.cancelDailyReminder(context)
            repository.resetApp()
            launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}
