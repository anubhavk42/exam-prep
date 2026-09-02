package com.anubhav.diprep.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anubhav.diprep.data.datastore.PreferencesManager
import com.anubhav.diprep.data.local.db.AppDatabase
import com.anubhav.diprep.data.local.db.Topic
import com.anubhav.diprep.data.local.db.ScoreEntry
import com.anubhav.diprep.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class TopicWithStats(
    val topic: Topic,
    val avgPercent: Int?,
    val testCount: Int,
    val daysSinceLastScore: Long?  // null = no scores logged yet
)

class SubjectDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(
        AppDatabase.getDatabase(application).appDao(),
        PreferencesManager(application)
    )

    // Cache StateFlows per subject so repeated calls from recomposition don't restart the flow
    private val flowCache = mutableMapOf<String, StateFlow<List<TopicWithStats>>>()

    fun getTopicsWithStats(subject: String): StateFlow<List<TopicWithStats>> =
        flowCache.getOrPut(subject) {
            combine(
                repository.getTopicsForSubject(subject),
                repository.getScoresForSubject(subject)
            ) { topics, scores ->
                val today = LocalDate.now()
                topics.map { topic ->
                    val topicScores = scores.filter { it.topicId == topic.id }
                    val lastDate = topicScores
                        .mapNotNull { runCatching { LocalDate.parse(it.dateISO) }.getOrNull() }
                        .maxOrNull()
                    TopicWithStats(
                        topic = topic,
                        avgPercent = if (topicScores.isNotEmpty())
                            topicScores.map { it.percentage }.average().toInt()
                        else null,
                        testCount = topicScores.size,
                        daysSinceLastScore = lastDate?.let { ChronoUnit.DAYS.between(it, today).coerceAtLeast(0) }
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }

    fun addTopic(subject: String, topicName: String) {
        val trimmed = topicName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTopic(subject, trimmed)
        }
    }

    fun deleteTopic(topicId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTopic(topicId)
        }
    }
}
