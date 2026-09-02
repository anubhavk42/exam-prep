package com.anubhav.diprep.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Welcome : Screen("welcome")
    data object Home : Screen("home")
    data object Main : Screen("home")
    data object LogScore : Screen("log_score")
    data object LogScoreTopic : Screen("log_score/{subject}/{topicId}") {
        fun createRoute(subject: String, topicId: Long) =
            "log_score/${android.net.Uri.encode(subject)}/$topicId"
    }
    data object SubjectDetail : Screen("subject_detail/{subjectName}") {
        fun createRoute(subjectName: String) =
            "subject_detail/${android.net.Uri.encode(subjectName)}"
    }
    data object Profile : Screen("profile")
    data object NotificationAppPicker : Screen("notification_app_picker")
    data object PrivacyPolicy : Screen("privacy_policy")
}

enum class MainTab(val label: String, val iconName: String) {
    HOME("Home", "home"),
    SUBJECTS("Subjects", "menu_book"),
    GOALS("Goals", "checklist_rtl"),
    STATS("Stats", "bar_chart")
}
