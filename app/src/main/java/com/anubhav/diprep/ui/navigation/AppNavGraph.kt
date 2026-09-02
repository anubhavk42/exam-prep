package com.anubhav.diprep.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anubhav.diprep.MainActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.anubhav.diprep.ui.screens.OnboardingScreen
import com.anubhav.diprep.ui.screens.SettingsScreen
import com.anubhav.diprep.ui.screens.logscore.LogScoreScreen
import com.anubhav.diprep.ui.screens.main.MainScreen
import com.anubhav.diprep.ui.screens.notification.NotificationAppPickerScreen
import com.anubhav.diprep.ui.screens.privacy.PrivacyPolicyScreen
import com.anubhav.diprep.ui.screens.subjects.SubjectDetailScreen
import com.anubhav.diprep.ui.viewmodel.PrepViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: PrepViewModel,
    shortcutAction: String? = null,
    onShortcutHandled: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val startDestination = if (uiState.userProfile.onboardingDone) {
        Screen.Home.route
    } else {
        Screen.Onboarding.route
    }

    // Handle a launcher shortcut that opened the app. Only meaningful once
    // onboarding is done — otherwise ignore and clear the signal.
    LaunchedEffect(shortcutAction, uiState.userProfile.onboardingDone) {
        val action = shortcutAction ?: return@LaunchedEffect
        if (!uiState.userProfile.onboardingDone) {
            onShortcutHandled()
            return@LaunchedEffect
        }
        when (action) {
            MainActivity.ACTION_SHORTCUT_LOG_SCORE -> {
                viewModel.onTabSelected(MainTab.HOME)
                navController.navigate(Screen.LogScore.route)
            }
            MainActivity.ACTION_SHORTCUT_ADD_SLOT -> {
                navController.popBackStack(Screen.Home.route, inclusive = false)
                viewModel.onTabSelected(MainTab.GOALS)
                viewModel.requestAddSlot()
            }
        }
        onShortcutHandled()
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Onboarding / First-launch setup route
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // Backward-compatible alias for welcome
        composable(Screen.Welcome.route) {
            OnboardingScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        // Home / Main dashboard route
        composable(Screen.Home.route) {
            MainScreen(
                viewModel = viewModel,
                onNavigateToLogScore = { navController.navigate(Screen.LogScore.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToSubjectDetail = { subject ->
                    navController.navigate(Screen.SubjectDetail.createRoute(subject))
                },
                onExitDemo = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.LogScore.route) {
            LogScoreScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Screen.Profile.route) }
            )
        }

        composable(
            route = Screen.LogScoreTopic.route,
            arguments = listOf(
                navArgument("subject") { type = NavType.StringType },
                navArgument("topicId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val subject = Uri.decode(backStackEntry.arguments?.getString("subject") ?: "")
            val topicId = backStackEntry.arguments?.getLong("topicId") ?: -1L
            LogScoreScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Screen.Profile.route) },
                initialSubject = subject,
                initialTopicId = topicId
            )
        }

        composable(
            route = Screen.SubjectDetail.route,
            arguments = listOf(
                navArgument("subjectName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subjectName = Uri.decode(backStackEntry.arguments?.getString("subjectName") ?: "")
            SubjectDetailScreen(
                subjectName = subjectName,
                onBack = { navController.popBackStack() },
                onNavigateToLogScore = { subject, topicId ->
                    navController.navigate(Screen.LogScoreTopic.createRoute(subject, topicId))
                }
            )
        }

        composable(Screen.Profile.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onResetAppNav = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToAppPicker = {
                    navController.navigate(Screen.NotificationAppPicker.route)
                },
                onNavigateToPrivacyPolicy = {
                    navController.navigate(Screen.PrivacyPolicy.route)
                }
            )
        }

        composable(Screen.NotificationAppPicker.route) {
            NotificationAppPickerScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
