package com.anubhav.diprep

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.anubhav.diprep.data.datastore.PreferencesManager
import com.anubhav.diprep.data.datastore.UserProfile
import com.anubhav.diprep.ui.navigation.AppNavGraph
import com.anubhav.diprep.ui.theme.MyApplicationTheme
import com.anubhav.diprep.ui.viewmodel.PrepViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_SHORTCUT_LOG_SCORE = "com.anubhav.diprep.action.SHORTCUT_LOG_SCORE"
        const val ACTION_SHORTCUT_ADD_SLOT = "com.anubhav.diprep.action.SHORTCUT_ADD_SLOT"
    }

    private val viewModel: PrepViewModel by viewModels()

    // One-shot signal for a launcher shortcut that opened the app.
    private val shortcutAction = mutableStateOf<String?>(null)

    private fun captureShortcutAction(intent: Intent?) {
        val action = intent?.action ?: return
        if (action == ACTION_SHORTCUT_LOG_SCORE || action == ACTION_SHORTCUT_ADD_SLOT) {
            shortcutAction.value = action
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureShortcutAction(intent)
    }

    override fun onResume() {
        super.onResume()
        // Foreground-only widget refresh — no background scheduling.
        com.anubhav.diprep.widget.CountdownWidget.requestRefresh(applicationContext)
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result handled gracefully without crash
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        captureShortcutAction(intent)

        val preferencesManager = PreferencesManager(applicationContext)

        setContent {
            val userProfile by preferencesManager.userProfileFlow.collectAsStateWithLifecycle(initialValue = UserProfile())
            val isSystemDark = isSystemInDarkTheme()
            val darkTheme = when (userProfile.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemDark
            }

            MyApplicationTheme(
                darkTheme = darkTheme,
                dynamicColor = userProfile.dynamicColor
            ) {
              androidx.compose.runtime.CompositionLocalProvider(
                com.anubhav.diprep.util.LocalHapticEnabled provides userProfile.hapticFeedbackEnabled
              ) {
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        val controller = WindowCompat.getInsetsController(window, view)
                        controller.isAppearanceLightStatusBars = !darkTheme
                        controller.isAppearanceLightNavigationBars = !darkTheme
                    }
                }

                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()
                var showNotificationRationaleDialog by remember { mutableStateOf(false) }

                fun requestNotificationPermission() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val isGranted = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!isGranted) {
                            if (ActivityCompat.shouldShowRequestPermissionRationale(
                                    this@MainActivity,
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            ) {
                                showNotificationRationaleDialog = true
                            } else {
                                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }
                }

                fun openExactAlarmSettings() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${packageName}")
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${packageName}")
                                }
                                startActivity(intent)
                            } catch (ignored: Exception) { }
                        }
                    }
                }

                // Check permissions & schedule today's one-time exact 1:30 PM reminder on app open
                LaunchedEffect(Unit) {
                    requestNotificationPermission()

                    viewModel.checkAndScheduleDailyMultivitaminReminder(
                        onExactAlarmDenied = {
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Enable exact alarms in Settings to get your Multivitamin reminder",
                                    actionLabel = "Settings",
                                    duration = SnackbarDuration.Long
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    openExactAlarmSettings()
                                }
                            }
                        }
                    )
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    containerColor = MaterialTheme.colorScheme.background,
                    // Each destination owns its own Scaffold / window-inset handling.
                    // Applying insets here too double-pads the top (visible gap).
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AppNavGraph(
                            navController = navController,
                            viewModel = viewModel,
                            shortcutAction = shortcutAction.value,
                            onShortcutHandled = { shortcutAction.value = null }
                        )
                    }
                }

                if (showNotificationRationaleDialog) {
                    AlertDialog(
                        onDismissRequest = { showNotificationRationaleDialog = false },
                        title = { Text("Daily Reminders") },
                        text = {
                            Text(
                                "Exam Prep sends a single daily notification at 1:30 PM for your multivitamin and exam readiness. Notifications never run background loops or wake locks."
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showNotificationRationaleDialog = false
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Allow")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showNotificationRationaleDialog = false }) {
                                Text("Not Now")
                            }
                        }
                    )
                }
              }
            }
        }
    }
}
