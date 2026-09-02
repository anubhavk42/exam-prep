package com.anubhav.diprep.util

import com.anubhav.diprep.data.datastore.UserProfile
import com.anubhav.diprep.data.local.db.TaskLog
import com.anubhav.diprep.data.local.db.TimetableCompletion
import com.anubhav.diprep.data.local.db.TimetableSlot
import java.time.LocalDate

/**
 * Single source of truth for the "day complete" / streak rule.
 *
 * A day counts as complete when:
 *  1. Every TimetableSlot scheduled for that weekday has a matching TimetableCompletion
 *     (a day with zero slots is automatically satisfied), AND
 *  2. Each wellness habit the student has enabled in Settings is done for that day.
 *
 * "examDone" (a test being logged) is deliberately NOT part of this — test frequency
 * is tracked separately.
 */
object StreakCalculator {

    fun isDayComplete(
        slotsForDow: List<TimetableSlot>,
        completedSlotIds: Set<Long>,
        taskLog: TaskLog?,
        profile: UserProfile
    ): Boolean {
        val timetableOk = slotsForDow.isEmpty() ||
            slotsForDow.all { it.id in completedSlotIds }
        val exerciseOk = !profile.exerciseReminderEnabled ||
            taskLog?.exerciseDone == true
        val vitaminOk = !profile.vitaminReminderEnabled ||
            taskLog?.vitaminDone == true
        return timetableOk && exerciseOk && vitaminOk
    }

    fun calculate(
        logs: List<TaskLog>,
        slots: List<TimetableSlot>,
        completions: List<TimetableCompletion>,
        profile: UserProfile
    ): Int {
        val logsByDate = logs.associateBy { it.dateISO }
        val slotsByDow = slots.groupBy { it.dayOfWeek }
        val completionsByDate = completions.groupBy { it.dateISO }
            .mapValues { (_, list) -> list.map { it.slotId }.toSet() }

        val today = LocalDate.now()
        // Today always counts (grace period — the day isn't over yet).
        var streak = 1

        fun isDone(date: LocalDate): Boolean {
            val iso = date.toString()
            val slotsForDow = slotsByDow[date.dayOfWeek.value] ?: emptyList()
            val doneIds = completionsByDate[iso] ?: emptySet()
            return isDayComplete(slotsForDow, doneIds, logsByDate[iso], profile)
        }

        var check = today.minusDays(1)
        while (isDone(check)) {
            streak++
            check = check.minusDays(1)
        }
        return streak
    }
}
