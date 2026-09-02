package com.anubhav.diprep.data.repository

import com.anubhav.diprep.data.datastore.PreferencesManager
import com.anubhav.diprep.data.local.db.AppDao
import com.anubhav.diprep.data.local.db.MoodEntry
import com.anubhav.diprep.data.local.db.ScoreEntry
import com.anubhav.diprep.data.local.db.TaskLog
import com.anubhav.diprep.data.local.db.TimetableCompletion
import com.anubhav.diprep.data.local.db.TimetableSlot
import com.anubhav.diprep.data.local.db.Topic
import java.time.LocalDate
import kotlin.random.Random

/**
 * Seeds a realistic sample dataset for Demo Mode. Deterministic (fixed seed) so
 * the demo looks the same every time. Wipes all existing data first.
 */
internal object DemoDataSeeder {

    private data class SubjectPlan(
        val name: String,
        val topics: List<String>,
        val weekOneAvg: Int,
        val weeklyDelta: Int   // + trending up, - trending down
    )

    private val PLAN = listOf(
        SubjectPlan("Pharmacology",
            listOf("Autonomic Nervous System", "Cardiovascular Drugs", "Antimicrobials", "CNS Drugs"),
            weekOneAvg = 64, weeklyDelta = 3),
        SubjectPlan("Pharmaceutical Chemistry",
            listOf("Medicinal Chemistry", "Stereochemistry", "Named Reactions"),
            weekOneAvg = 48, weeklyDelta = 4),
        SubjectPlan("Drug Laws & Acts",
            listOf("D&C Act 1940", "Schedules (M, X, H)", "Inspector Powers", "NDPS Act"),
            weekOneAvg = 38, weeklyDelta = 3),
        SubjectPlan("Pharmacognosy",
            listOf("Alkaloids", "Glycosides", "Volatile Oils"),
            weekOneAvg = 60, weeklyDelta = 2),
        SubjectPlan("Biochemistry",
            listOf("Enzyme Kinetics", "Carbohydrate Metabolism", "Lipid Metabolism"),
            weekOneAvg = 66, weeklyDelta = -4),   // trending DOWN
        SubjectPlan("Microbiology",
            listOf("Sterilization", "Bacterial Endotoxins", "Antibiotic Assays"),
            weekOneAvg = 80, weeklyDelta = 1)
    )

    private val WEEKDAY_SLOTS = listOf(
        Triple("06:30", "08:00", "Morning revision"),
        Triple("18:00", "19:30", "Mock test practice"),
        Triple("21:00", "22:00", "Flashcards")
    )
    private val WEEKEND_SLOTS = listOf(
        Triple("09:00", "11:00", "Full-length mock"),
        Triple("16:00", "17:30", "Weak-area drilling")
    )

    // Days (offset from today) the student "missed" — keeps the streak/heatmap imperfect.
    private val SKIPPED_OFFSETS = setOf(3, 8, 9, 15, 19)

    suspend fun seed(dao: AppDao, prefs: PreferencesManager) {
        val rng = Random(42)
        val today = LocalDate.now()

        // 1. Wipe everything
        dao.clearAllScores()
        dao.clearAllTaskLogs()
        dao.clearAllTimetableSlots()
        dao.clearAllTimetableCompletions()
        dao.clearAllTopics()
        dao.clearAllMoodEntries()

        val subjectNames = PLAN.map { it.name }

        // 2. Profile
        prefs.writeDemoProfile(
            name = "Demo Student",
            examStream = "Drug Inspector",
            examDate = today.plusDays(90).toString(),
            customSubjects = subjectNames
        )

        // 3. Topics + 4. Scores across the last 8 weeks
        val scores = mutableListOf<ScoreEntry>()
        for (plan in PLAN) {
            for (topicName in plan.topics) {
                val topicId = dao.insertTopic(
                    Topic(subject = plan.name, topicName = topicName,
                        createdAt = System.currentTimeMillis())
                )
                // one score per week for 8 weeks (skip a couple at random for realism)
                for (week in 0 until 8) {
                    if (week >= 5 && rng.nextInt(100) < 25) continue
                    val jitter = rng.nextInt(0, 4)
                    val date = today.minusWeeks((7 - week).toLong()).minusDays(jitter.toLong())
                    if (date.isAfter(today)) continue
                    val trend = plan.weekOneAvg + plan.weeklyDelta * week
                    val pct = (trend + rng.nextInt(-6, 7)).coerceIn(8, 99)
                    val total = 50
                    val obtained = (pct * total / 100.0).toInt().coerceIn(0, total)
                    scores.add(
                        ScoreEntry(
                            dateISO = date.toString(),
                            subject = plan.name,
                            obtained = obtained,
                            total = total,
                            percentage = pct,
                            topicId = topicId,
                            createdAt = date.toEpochDay() * 86_400_000L + week
                        )
                    )
                }
            }
        }
        dao.insertScores(scores)

        // 5. Timetable slots — every day of the week
        val slotIdsByDow = mutableMapOf<Int, MutableList<Long>>()
        for (dow in 1..7) {
            val defs = if (dow >= 6) WEEKEND_SLOTS else WEEKDAY_SLOTS
            for ((start, end, label) in defs) {
                val id = dao.insertTimetableSlot(
                    TimetableSlot(
                        dayOfWeek = dow, startTime = start, endTime = end,
                        label = label, focusModeEnabled = false,
                        createdAt = System.currentTimeMillis()
                    )
                )
                slotIdsByDow.getOrPut(dow) { mutableListOf() }.add(id)
            }
        }

        // 6. Completions + task logs for the last 21 days (semi-consistent)
        val completions = mutableListOf<TimetableCompletion>()
        val taskLogs = mutableListOf<TaskLog>()
        for (offset in 1..21) {
            if (offset in SKIPPED_OFFSETS) continue
            val date = today.minusDays(offset.toLong())
            val iso = date.toString()
            val dow = date.dayOfWeek.value
            val slotIds = slotIdsByDow[dow].orEmpty()

            // complete most (sometimes all) of the day's slots
            val completeAll = rng.nextInt(100) < 70
            slotIds.forEach { sid ->
                if (completeAll || rng.nextInt(100) < 60) {
                    completions.add(TimetableCompletion(slotId = sid, dateISO = iso))
                }
            }
            val exercise = rng.nextInt(100) < 80
            val vitamin = rng.nextInt(100) < 75
            val allSlotsDone = slotIds.isNotEmpty() &&
                completions.count { it.dateISO == iso } == slotIds.size
            taskLogs.add(
                TaskLog(
                    dateISO = iso,
                    studyDone = false,
                    examDone = scores.any { it.dateISO == iso },
                    exerciseDone = exercise,
                    vitaminDone = vitamin,
                    allDone = allSlotsDone && exercise && vitamin
                )
            )
        }
        completions.forEach { dao.insertCompletion(it) }
        dao.insertTaskLogs(taskLogs)

        // 7. A few mood entries
        val moods = listOf("EXCITED", "NEUTRAL", "LOW", "NEUTRAL", "EXCITED")
        moods.forEachIndexed { i, mood ->
            val date = today.minusDays((i * 2 + 1).toLong())
            dao.insertMoodEntry(
                MoodEntry(dateISO = date.toString(), mood = mood,
                    createdAt = System.currentTimeMillis())
            )
        }
    }
}
