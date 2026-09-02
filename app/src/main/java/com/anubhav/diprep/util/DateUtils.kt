package com.anubhav.diprep.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateUtils {

    private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)
    private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun todayISO(): String = LocalDate.now().format(isoFormatter)

    fun formatFullDate(localDate: LocalDate = LocalDate.now()): String {
        return localDate.format(fullDateFormatter)
    }

    fun formatShortDate(localDate: LocalDate): String {
        return localDate.format(shortDateFormatter)
    }

    fun daysUntil(targetDateIso: String): Long {
        return try {
            val target = LocalDate.parse(targetDateIso, isoFormatter)
            val today = LocalDate.now()
            ChronoUnit.DAYS.between(today, target).coerceAtLeast(0)
        } catch (_: Exception) {
            0
        }
    }

    fun weeksAndDaysRemaining(targetDateIso: String): String {
        val totalDays = daysUntil(targetDateIso)
        val weeks = totalDays / 7
        val remDays = totalDays % 7
        return when {
            weeks > 0 && remDays > 0 -> "$weeks weeks, $remDays days left"
            weeks > 0 -> "$weeks weeks left"
            else -> "$remDays days left"
        }
    }
}
