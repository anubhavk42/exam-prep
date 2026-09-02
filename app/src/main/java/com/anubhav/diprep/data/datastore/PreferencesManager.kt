package com.anubhav.diprep.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray

val Context.examPrepDataStore: DataStore<Preferences> by preferencesDataStore(name = "exam_prep_prefs")

data class UserProfile(
    val name: String = "",
    val examStream: String = "Drug Inspector",
    val examDate: String = "2026-12-20",
    val examDateConfirmed: Boolean = false,
    val customSubjects: List<String> = emptyList(),
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 13,
    val reminderMinute: Int = 30,
    val streakLastDate: String = "",
    val reminderScheduledDate: String = "",
    val onboardingDone: Boolean = false,
    val themeMode: String = "DARK",
    val dynamicColor: Boolean = false,
    val vitaminReminderEnabled: Boolean = false,
    val exerciseReminderEnabled: Boolean = true,
    val homeSectionOrder: List<String> = HomeSections.DEFAULT_ORDER,
    val homeHiddenSections: List<String> = emptyList(),
    val celebratedMilestones: Set<String> = emptySet(),
    // Notification Filter (Focus Mode)
    val notificationFilterEnabled: Boolean = false,
    val mutedAppPackages: List<String> = emptyList(),
    val filterActivationMode: String = "TIMETABLE",  // "TIMETABLE" or "MANUAL"
    val isFocusSessionActive: Boolean = false,
    val isDemoMode: Boolean = false,
    val appLanguage: String = "en",  // "en" or "hi"
    val hapticFeedbackEnabled: Boolean = true
)

object HomeSections {
    const val WHATS_NEXT = "whats_next"
    const val WEAK_ALERT = "weak_alert"
    const val STREAK_PB = "streak_pb"
    const val TODAYS_GOALS = "todays_goals"
    const val LOG_BUTTON = "log_button"
    const val QUOTE = "quote"
    const val SYLLABUS_RING = "syllabus_ring"
    const val REVISION_REMINDER = "revision_reminder"

    val DEFAULT_ORDER = listOf(WHATS_NEXT, WEAK_ALERT, REVISION_REMINDER, STREAK_PB, TODAYS_GOALS, LOG_BUTTON, QUOTE, SYLLABUS_RING)

    fun label(key: String): String = when (key) {
        WHATS_NEXT -> "What's next"
        WEAK_ALERT -> "Weak topic alert"
        REVISION_REMINDER -> "Revision reminder"
        STREAK_PB -> "Streak & personal best"
        TODAYS_GOALS -> "Today's goals progress"
        LOG_BUTTON -> "Log score button"
        QUOTE -> "Daily quote"
        SYLLABUS_RING -> "Overall mastery ring"
        else -> key
    }

    fun description(key: String): String = when (key) {
        WHATS_NEXT -> "Next slot from your timetable"
        WEAK_ALERT -> "Auto-shown when a subject slips"
        REVISION_REMINDER -> "Topic you haven't revisited in 7+ days"
        STREAK_PB -> "Your consistency at a glance"
        TODAYS_GOALS -> "Checklist completion for today"
        LOG_BUTTON -> "Quick shortcut to log a test"
        QUOTE -> "A line of motivation, rotates daily"
        SYLLABUS_RING -> "Visual ring of combined subject mastery"
        else -> ""
    }
}

object ExamPresets {
    const val STREAM_DRUG_INSPECTOR = "Drug Inspector"
    const val STREAM_GPAT = "GPAT"
    const val STREAM_PHARMACIST = "Pharmacist (Government)"
    const val STREAM_RAJASTHAN_DI = "Rajasthan Drug Inspector"
    const val STREAM_CUSTOM = "Custom"

    const val STREAM_UPSC = "UPSC"
    const val STREAM_SSC = "SSC"
    const val STREAM_BANKING = "Banking"
    const val STREAM_NEET = "NEET/Medical"
    const val STREAM_STATE_PCS = "State PCS"

    // Quick-pick preset chips shown in Onboarding and Settings.
    val PRESET_CHIPS = listOf(
        STREAM_UPSC,
        STREAM_SSC,
        STREAM_BANKING,
        "Pharmacy/Drug Inspector",
        STREAM_NEET,
        STREAM_STATE_PCS
    )

    val STREAMS = listOf(
        STREAM_DRUG_INSPECTOR,
        STREAM_GPAT,
        STREAM_PHARMACIST,
        STREAM_RAJASTHAN_DI,
        STREAM_CUSTOM
    )

    fun getPresetSubjects(stream: String): List<String> = when (stream) {
        STREAM_UPSC -> listOf(
            "History", "Geography", "Polity", "Economy", "Environment & Ecology",
            "Science & Tech", "Current Affairs", "Ethics (GS4)", "CSAT"
        )
        STREAM_SSC -> listOf(
            "Quantitative Aptitude", "General Intelligence & Reasoning",
            "English Language", "General Awareness"
        )
        STREAM_BANKING -> listOf(
            "Quantitative Aptitude", "Reasoning Ability", "English Language",
            "Banking & Financial Awareness", "Computer Aptitude"
        )
        STREAM_NEET -> listOf("Physics", "Chemistry", "Botany", "Zoology")
        STREAM_STATE_PCS -> listOf(
            "State GK & History", "Indian Polity", "Geography", "Economy",
            "Current Affairs", "CSAT / Aptitude"
        )
        "Pharmacy/Drug Inspector" -> getPresetSubjects(STREAM_DRUG_INSPECTOR)
        STREAM_DRUG_INSPECTOR -> listOf(
            "Pharmacology",
            "Pharmaceutical Chemistry",
            "Drug Laws & Acts",
            "Pharmacognosy",
            "Biochemistry",
            "Microbiology",
            "Pathophysiology",
            "Dispensing & Hospital Pharmacy",
            "Quality Control & GMP",
            "Toxicology"
        )
        STREAM_GPAT -> listOf(
            "Pharmaceutics",
            "Pharmacology",
            "Pharmaceutical Chemistry",
            "Pharmacognosy",
            "Pharmacokinetics",
            "Biopharmaceutics",
            "Clinical Pharmacy",
            "Medicinal Chemistry"
        )
        STREAM_PHARMACIST -> listOf(
            "Pharmacology",
            "Pharmaceutics",
            "Drug Store Management",
            "Pharmacognosy",
            "Pharmaceutical Chemistry",
            "Hospital & Clinical Pharmacy",
            "Pharmacy Law & Ethics"
        )
        STREAM_RAJASTHAN_DI -> listOf(
            "Pharmacology",
            "Pharmaceutical Chemistry",
            "Drug Laws & Acts",
            "Pharmacognosy",
            "Rajasthan Drug Rules",
            "Biochemistry",
            "Quality Control",
            "Dispensing Pharmacy"
        )
        STREAM_CUSTOM -> emptyList()
        else -> listOf("Pharmacology", "Pharmaceutics", "Pharmaceutical Chemistry", "Drug Laws & Acts")
    }
}

class PreferencesManager(private val context: Context) {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val EXAM_STREAM = stringPreferencesKey("exam_stream")
        val EXAM_DATE = stringPreferencesKey("exam_date")
        val EXAM_DATE_CONFIRMED = booleanPreferencesKey("exam_date_confirmed")
        val CUSTOM_SUBJECTS = stringPreferencesKey("custom_subjects")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_time_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_time_minute")
        val STREAK_LAST_DATE = stringPreferencesKey("streak_last_date")
        val REMINDER_SCHEDULED_DATE = stringPreferencesKey("reminder_scheduled_date")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val VITAMIN_REMINDER_ENABLED = booleanPreferencesKey("vitamin_reminder_enabled")
        val EXERCISE_REMINDER_ENABLED = booleanPreferencesKey("exercise_reminder_enabled")
        val HOME_SECTION_ORDER = stringPreferencesKey("home_section_order")
        val HOME_HIDDEN_SECTIONS = stringPreferencesKey("home_hidden_sections")
        val CELEBRATED_MILESTONES = stringPreferencesKey("celebrated_milestones")
        val LAST_MOOD_PROMPT_DATE = stringPreferencesKey("last_mood_prompt_date")
        val NOTIFICATION_FILTER_ENABLED = booleanPreferencesKey("notification_filter_enabled")
        val MUTED_APP_PACKAGES = stringPreferencesKey("muted_app_packages")
        val FILTER_ACTIVATION_MODE = stringPreferencesKey("filter_activation_mode")
        val IS_FOCUS_SESSION_ACTIVE = booleanPreferencesKey("is_focus_session_active")
        val IS_DEMO_MODE = booleanPreferencesKey("is_demo_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
    }

    private fun parseJsonStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    val userProfileFlow: Flow<UserProfile> = context.examPrepDataStore.data.map { prefs ->
        val customSubjJson = prefs[Keys.CUSTOM_SUBJECTS] ?: "[]"
        val subjectList = try {
            val arr = JSONArray(customSubjJson)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }

        UserProfile(
            name = prefs[Keys.USER_NAME] ?: "",
            examStream = prefs[Keys.EXAM_STREAM] ?: ExamPresets.STREAM_DRUG_INSPECTOR,
            examDate = prefs[Keys.EXAM_DATE] ?: "2026-12-20",
            examDateConfirmed = prefs[Keys.EXAM_DATE_CONFIRMED] ?: (prefs[Keys.EXAM_DATE] != null && prefs[Keys.EXAM_DATE] != "2026-12-20"),
            customSubjects = subjectList,
            reminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: true,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 13,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 30,
            streakLastDate = prefs[Keys.STREAK_LAST_DATE] ?: "",
            reminderScheduledDate = prefs[Keys.REMINDER_SCHEDULED_DATE] ?: "",
            onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: false,
            themeMode = prefs[Keys.THEME_MODE] ?: "DARK",
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            vitaminReminderEnabled = prefs[Keys.VITAMIN_REMINDER_ENABLED] ?: false,
            exerciseReminderEnabled = prefs[Keys.EXERCISE_REMINDER_ENABLED] ?: true,
            homeSectionOrder = run {
                val stored = parseJsonStringList(prefs[Keys.HOME_SECTION_ORDER])
                    .filter { it in HomeSections.DEFAULT_ORDER }
                // Keep stored order, append any sections added in a later app version.
                (stored + HomeSections.DEFAULT_ORDER.filter { it !in stored })
                    .ifEmpty { HomeSections.DEFAULT_ORDER }
            },
            homeHiddenSections = parseJsonStringList(prefs[Keys.HOME_HIDDEN_SECTIONS])
                .filter { it in HomeSections.DEFAULT_ORDER },
            celebratedMilestones = parseJsonStringList(prefs[Keys.CELEBRATED_MILESTONES]).toSet(),
            notificationFilterEnabled = prefs[Keys.NOTIFICATION_FILTER_ENABLED] ?: false,
            mutedAppPackages = parseJsonStringList(prefs[Keys.MUTED_APP_PACKAGES]),
            filterActivationMode = prefs[Keys.FILTER_ACTIVATION_MODE] ?: "TIMETABLE",
            isFocusSessionActive = prefs[Keys.IS_FOCUS_SESSION_ACTIVE] ?: false,
            isDemoMode = prefs[Keys.IS_DEMO_MODE] ?: false,
            appLanguage = prefs[Keys.APP_LANGUAGE] ?: "en",
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK_ENABLED] ?: true
        )
    }

    suspend fun getUserName(): String {
        return context.examPrepDataStore.data.map { it[Keys.USER_NAME] ?: "" }.first()
    }

    suspend fun saveProfile(
        name: String,
        examStream: String,
        examDate: String,
        customSubjects: List<String>
    ) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = name.trim()
            prefs[Keys.EXAM_STREAM] = examStream
            prefs[Keys.EXAM_DATE] = examDate
            prefs[Keys.EXAM_DATE_CONFIRMED] = (examDate != "2026-12-20")
            val jsonArr = JSONArray(customSubjects)
            prefs[Keys.CUSTOM_SUBJECTS] = jsonArr.toString()
            prefs[Keys.ONBOARDING_DONE] = true
        }
    }

    suspend fun updateName(name: String) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = name.trim()
        }
    }

    suspend fun updateExamDate(dateISO: String) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.EXAM_DATE] = dateISO
        }
    }

    suspend fun updateExamDateConfirmed(confirmed: Boolean) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.EXAM_DATE_CONFIRMED] = confirmed
        }
    }

    suspend fun updateExamStreamAndSubjects(stream: String, subjects: List<String>) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.EXAM_STREAM] = stream
            val jsonArr = JSONArray(subjects)
            prefs[Keys.CUSTOM_SUBJECTS] = jsonArr.toString()
        }
    }

    suspend fun updateCustomSubjects(subjects: List<String>) {
        context.examPrepDataStore.edit { prefs ->
            val jsonArr = JSONArray(subjects)
            prefs[Keys.CUSTOM_SUBJECTS] = jsonArr.toString()
        }
    }

    suspend fun updateReminderEnabled(enabled: Boolean) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.REMINDER_ENABLED] = enabled
        }
    }

    suspend fun updateReminderTime(hour: Int, minute: Int) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.REMINDER_HOUR] = hour
            prefs[Keys.REMINDER_MINUTE] = minute
        }
    }

    suspend fun updateStreakLastDate(dateISO: String) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.STREAK_LAST_DATE] = dateISO
        }
    }

    suspend fun updateReminderScheduledDate(dateISO: String) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.REMINDER_SCHEDULED_DATE] = dateISO
        }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_DONE] = done
        }
    }

    suspend fun updateThemeMode(themeMode: String) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode
        }
    }

    suspend fun updateDynamicColor(dynamicColor: Boolean) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.DYNAMIC_COLOR] = dynamicColor
        }
    }

    suspend fun updateVitaminReminderEnabled(enabled: Boolean) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.VITAMIN_REMINDER_ENABLED] = enabled
        }
    }

    suspend fun updateExerciseReminderEnabled(enabled: Boolean) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.EXERCISE_REMINDER_ENABLED] = enabled
        }
    }

    suspend fun updateHomeSectionOrder(order: List<String>) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.HOME_SECTION_ORDER] = JSONArray(order).toString()
        }
    }

    suspend fun updateHomeSectionHidden(key: String, hidden: Boolean) {
        context.examPrepDataStore.edit { prefs ->
            val current = parseJsonStringList(prefs[Keys.HOME_HIDDEN_SECTIONS]).toMutableSet()
            if (hidden) current.add(key) else current.remove(key)
            prefs[Keys.HOME_HIDDEN_SECTIONS] = JSONArray(current.toList()).toString()
        }
    }

    suspend fun addCelebratedMilestone(key: String) {
        context.examPrepDataStore.edit { prefs ->
            val current = parseJsonStringList(prefs[Keys.CELEBRATED_MILESTONES]).toMutableSet()
            current.add(key)
            prefs[Keys.CELEBRATED_MILESTONES] = JSONArray(current.toList()).toString()
        }
    }

    suspend fun getLastMoodPromptDate(): String {
        return context.examPrepDataStore.data.map { it[Keys.LAST_MOOD_PROMPT_DATE] ?: "" }.first()
    }

    suspend fun updateLastMoodPromptDate(dateISO: String) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.LAST_MOOD_PROMPT_DATE] = dateISO
        }
    }

    suspend fun updateNotificationFilterEnabled(enabled: Boolean) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.NOTIFICATION_FILTER_ENABLED] = enabled
        }
    }

    suspend fun updateMutedAppPackages(packages: List<String>) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.MUTED_APP_PACKAGES] = JSONArray(packages).toString()
        }
    }

    suspend fun updateFilterActivationMode(mode: String) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.FILTER_ACTIVATION_MODE] = mode
        }
    }

    suspend fun updateFocusSessionActive(active: Boolean) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.IS_FOCUS_SESSION_ACTIVE] = active
        }
    }

    suspend fun updateDemoMode(enabled: Boolean) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.IS_DEMO_MODE] = enabled
        }
    }

    suspend fun updateAppLanguage(tag: String) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.APP_LANGUAGE] = tag
        }
    }

    suspend fun updateHapticFeedbackEnabled(enabled: Boolean) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }

    /**
     * Writes a full sample profile in one edit. Used by Demo Mode so the app
     * lands on a populated Home instead of empty states.
     */
    suspend fun writeDemoProfile(
        name: String,
        examStream: String,
        examDate: String,
        customSubjects: List<String>
    ) {
        context.examPrepDataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = name
            prefs[Keys.EXAM_STREAM] = examStream
            prefs[Keys.EXAM_DATE] = examDate
            prefs[Keys.EXAM_DATE_CONFIRMED] = true
            prefs[Keys.CUSTOM_SUBJECTS] = JSONArray(customSubjects).toString()
            prefs[Keys.ONBOARDING_DONE] = true
            prefs[Keys.IS_DEMO_MODE] = true
            prefs[Keys.VITAMIN_REMINDER_ENABLED] = true
            prefs[Keys.EXERCISE_REMINDER_ENABLED] = true
        }
    }

    suspend fun clearAllData() {
        context.examPrepDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
