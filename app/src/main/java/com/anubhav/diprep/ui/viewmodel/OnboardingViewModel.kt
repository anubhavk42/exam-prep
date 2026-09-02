package com.anubhav.diprep.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anubhav.diprep.data.datastore.ExamPresets
import com.anubhav.diprep.data.datastore.PreferencesManager
import com.anubhav.diprep.data.local.db.AppDatabase
import com.anubhav.diprep.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val preferencesManager = PreferencesManager(application)
    private val repository = AppRepository(database.appDao(), preferencesManager)

    val currentStep = MutableStateFlow(1)
    val nameInput = MutableStateFlow("")
    val selectedStream = MutableStateFlow(ExamPresets.STREAM_DRUG_INSPECTOR)
    val examDate = MutableStateFlow("2026-12-20")
    val subjectList = MutableStateFlow(ExamPresets.getPresetSubjects(ExamPresets.STREAM_DRUG_INSPECTOR))

    fun updateName(name: String) {
        if (name.length <= 20) {
            nameInput.value = name
        }
    }

    fun onStreamSelected(stream: String) {
        selectedStream.value = stream
        val presets = ExamPresets.getPresetSubjects(stream)
        if (presets.isNotEmpty()) {
            subjectList.value = presets
        }
    }

    /** Free-text custom exam name — does not touch the subject list. */
    fun updateStreamName(name: String) {
        selectedStream.value = name
    }

    fun updateExamDate(dateIso: String) {
        examDate.value = dateIso
    }

    fun addSubject(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty() && !subjectList.value.contains(trimmed)) {
            subjectList.value = subjectList.value + trimmed
        }
    }

    fun removeSubject(name: String) {
        if (subjectList.value.size > 1) {
            subjectList.value = subjectList.value.filter { it != name }
        }
    }

    fun nextStep(): Boolean {
        return when (currentStep.value) {
            1 -> {
                if (nameInput.value.trim().isNotEmpty()) {
                    currentStep.value = 2
                    true
                } else {
                    false
                }
            }
            2 -> {
                if (selectedStream.value.isNotEmpty()) {
                    currentStep.value = 3
                    true
                } else {
                    false
                }
            }
            else -> true
        }
    }

    fun previousStep() {
        if (currentStep.value > 1) {
            currentStep.value = currentStep.value - 1
        }
    }

    private var isCompleting = false

    fun completeOnboarding(onSuccess: () -> Unit) {
        if (isCompleting) return
        isCompleting = true
        val name = nameInput.value.trim().ifEmpty { "Aspirant" }
        val stream = selectedStream.value
        val date = examDate.value
        val subjects = subjectList.value

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProfile(
                name = name,
                examStream = stream,
                examDate = date,
                customSubjects = subjects
            )
            launch(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    /** "Try Demo" — seed sample data and jump straight to Home. */
    fun loadDemo(onSuccess: () -> Unit) {
        if (isCompleting) return
        isCompleting = true
        viewModelScope.launch(Dispatchers.IO) {
            repository.loadDemoData()
            launch(Dispatchers.Main) {
                onSuccess()
            }
        }
    }
}
