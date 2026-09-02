package com.anubhav.diprep.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anubhav.diprep.data.datastore.PreferencesManager
import com.anubhav.diprep.data.local.db.AppDatabase
import com.anubhav.diprep.data.local.db.ScoreEntry
import com.anubhav.diprep.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ScoreStats(
    val avg7: Int?,
    val avgAll: Int?,
    val count: Int
)

class LogScoreViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val preferencesManager = PreferencesManager(application)
    private val repository = AppRepository(database.appDao(), preferencesManager)

    val subjects: StateFlow<List<String>> = repository.userProfile
        .map { it.customSubjects }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedSubject = MutableStateFlow("")
    val obtainedInput = MutableStateFlow("")
    val totalInput = MutableStateFlow("")

    private val _selectedTopicId = MutableStateFlow<Long?>(null)
    val selectedTopicId: StateFlow<Long?> = _selectedTopicId

    private val _selectedTopicName = MutableStateFlow<String?>(null)
    val selectedTopicName: StateFlow<String?> = _selectedTopicName

    val derivedPercentage: StateFlow<Int?> = combine(obtainedInput, totalInput) { obtStr, totStr ->
        val obt = obtStr.trim().toIntOrNull()
        val tot = totStr.trim().toIntOrNull()
        if (obt != null && tot != null && tot > 0) {
            ((obt.toDouble() / tot) * 100).toInt()
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _newPBEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val newPBEvent: SharedFlow<Unit> = _newPBEvent.asSharedFlow()

    // 7-day average query
    val avg7: StateFlow<Int?> = repository.getAvgPercentageSince(LocalDate.now().minusDays(7).toString())
        .map { it?.toInt() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // All-time average query
    val avgAll: StateFlow<Int?> = repository.allTimeAvgPercentage
        .map { it?.toInt() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Total tests count
    val totalTests: StateFlow<Int> = repository.totalTestsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val stats: StateFlow<ScoreStats> = combine(avg7, avgAll, totalTests) { a7, aAll, cnt ->
        ScoreStats(avg7 = a7, avgAll = aAll, count = cnt)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScoreStats(null, null, 0))

    // Last 14 scores for bar chart
    val last14: StateFlow<List<ScoreEntry>> = repository.getLastScoresChronological(14)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSubject(value: String) {
        selectedSubject.value = value
    }

    fun updateObtained(value: String) {
        if (value.length <= 4 && value.all { it.isDigit() }) {
            obtainedInput.value = value
        }
    }

    fun updateTotal(value: String) {
        if (value.length <= 4 && value.all { it.isDigit() }) {
            totalInput.value = value
        }
    }

    fun setInitialValues(subject: String, topicId: Long) {
        selectedSubject.value = subject
        if (topicId > 0L) {
            _selectedTopicId.value = topicId
            viewModelScope.launch(Dispatchers.IO) {
                _selectedTopicName.value = repository.getTopicById(topicId)?.topicName
            }
        }
    }

    fun addQuickSubject(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                val current = subjects.value.toMutableList()
                if (!current.contains(trimmed)) {
                    current.add(trimmed)
                    repository.updateCustomSubjects(current)
                }
                selectedSubject.value = trimmed
            }
        }
    }

    fun saveScore(
        onSuccess: (message: String, isNewPB: Boolean) -> Unit,
        onError: (message: String) -> Unit
    ) {
        val subject = selectedSubject.value.trim()
        if (subject.isEmpty()) {
            onError("Please select or enter a subject name")
            return
        }

        val obtained = obtainedInput.value.trim().toIntOrNull()
        if (obtained == null) {
            onError("Enter valid marks obtained")
            return
        }

        val total = totalInput.value.trim().toIntOrNull()
        if (total == null || total <= 0) {
            onError("Total marks must be greater than zero")
            return
        }

        if (obtained > total) {
            onError("Marks obtained cannot exceed total marks")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val prevMax = repository.maxScorePercentage.first() ?: 0
            val percentage = ((obtained.toDouble() / total) * 100).toInt().coerceIn(0, 100)
            val todayIso = LocalDate.now().toString()

            repository.logScoreAndMarkExam(todayIso, subject, obtained, total, _selectedTopicId.value)

            val isNewPB = percentage > prevMax && prevMax > 0
            if (isNewPB) {
                _newPBEvent.emit(Unit)
            }

            // Clear inputs
            selectedSubject.value = ""
            obtainedInput.value = ""
            totalInput.value = ""
            _selectedTopicId.value = null
            _selectedTopicName.value = null

            val msg = if (isNewPB) "New personal best! 🏆" else "Score logged successfully!"
            launch(Dispatchers.Main) {
                onSuccess(msg, isNewPB)
            }
        }
    }
}
