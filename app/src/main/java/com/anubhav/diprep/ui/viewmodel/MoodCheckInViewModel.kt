package com.anubhav.diprep.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anubhav.diprep.data.datastore.PreferencesManager
import com.anubhav.diprep.data.local.db.AppDatabase
import com.anubhav.diprep.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class MoodCheckInViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(
        AppDatabase.getDatabase(application).appDao(),
        PreferencesManager(application)
    )

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog

    init {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            if (repository.getLastMoodPromptDate() != today) {
                _showDialog.value = true
            }
        }
    }

    fun onMoodSelected(mood: String) {
        val today = LocalDate.now().toString()
        viewModelScope.launch {
            repository.saveMoodEntry(today, mood)
            repository.updateLastMoodPromptDate(today)
            _showDialog.value = false
        }
    }

    fun onSkip() {
        viewModelScope.launch {
            repository.updateLastMoodPromptDate(LocalDate.now().toString())
            _showDialog.value = false
        }
    }
}
