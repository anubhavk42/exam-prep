package com.anubhav.diprep.ui.screens.profile

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anubhav.diprep.ui.theme.OnSleekBackground
import com.anubhav.diprep.ui.theme.OnSleekPrimary
import com.anubhav.diprep.ui.theme.OnSleekPrimaryContainer
import com.anubhav.diprep.ui.theme.OnSleekSurfaceVariant
import com.anubhav.diprep.ui.theme.SleekBackground
import com.anubhav.diprep.ui.theme.SleekBorderLight
import com.anubhav.diprep.ui.theme.SleekCritical
import com.anubhav.diprep.ui.theme.SleekCriticalBg
import com.anubhav.diprep.ui.theme.SleekOutlineVariant
import com.anubhav.diprep.ui.theme.SleekPrimary
import com.anubhav.diprep.ui.theme.SleekPrimaryContainer
import com.anubhav.diprep.ui.theme.SleekSuccess
import com.anubhav.diprep.ui.theme.SleekSuccessBg
import com.anubhav.diprep.ui.theme.SleekSuccessLight
import com.anubhav.diprep.ui.theme.SleekSurfaceContainer
import com.anubhav.diprep.ui.theme.SleekSurfaceContainerHigh
import com.anubhav.diprep.ui.theme.SleekSurfaceContainerLowest
import com.anubhav.diprep.ui.viewmodel.PrepUiState
import com.anubhav.diprep.ui.viewmodel.PrepViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: PrepViewModel,
    uiState: PrepUiState,
    onBack: () -> Unit,
    onNavigateToQuiz: (String) -> Unit
) {
    // Predictive Back gesture handler for Android 15
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showExactAlarmRationaleDialog by remember { mutableStateOf(false) }

    // Pulsing green dot animation for API 35 optimization
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Candidate Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSleekBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SleekPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SleekBackground)
            )
        },
        containerColor = SleekBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Aspirant Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SleekPrimaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(OnSleekPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DI",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = SleekPrimaryContainer
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = uiState.userProfile.name.ifEmpty { "Candidate" },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSleekPrimaryContainer
                            )
                            IconButton(
                                onClick = { showEditNameDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Name",
                                    tint = OnSleekPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = "Drug Inspector Aspirant",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSleekPrimaryContainer.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(OnSleekPrimaryContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = uiState.userProfile.examStream.ifEmpty { "UPSC / State PSC Drug Inspector" },
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Stats Bento Row (Avg Score, Hours / Week, Streak)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileBentoCard(
                    modifier = Modifier.weight(1f),
                    title = "Avg Score",
                    value = "${uiState.allTimeAvg}%",
                    sub = "All Tests"
                )
                ProfileBentoCard(
                    modifier = Modifier.weight(1f),
                    title = "Weekly Study",
                    value = "28h",
                    sub = "Target 36h"
                )
                ProfileBentoCard(
                    modifier = Modifier.weight(1f),
                    title = "Streak",
                    value = "${uiState.currentStreakDays}d",
                    sub = "Active"
                )
            }

            // Weak Spots Focus Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurfaceContainerLowest),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SleekBorderLight, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Priority Weak Spot",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSleekBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Jurisprudence (30%) requires attention before the exam.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSleekSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { onNavigateToQuiz("jurisprudence") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                    ) {
                        Text("Launch Jurisprudence High-Yield Drill", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Reminders & Preferences Section
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurfaceContainerLowest),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SleekBorderLight, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "One-Time Daily Reminders",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSleekBackground
                    )

                    // Multivitamin Reminder
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SleekPrimaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Medication,
                                    contentDescription = null,
                                    tint = SleekPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Multivitamin Reminder",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSleekBackground
                                )
                                Text(
                                    text = "Scheduled for 9:00 AM on app open",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSleekSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = uiState.userProfile.reminderEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                    if (!alarmManager.canScheduleExactAlarms()) {
                                        showExactAlarmRationaleDialog = true
                                    }
                                }
                                viewModel.updateMultivitaminReminder(enabled)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SleekPrimary)
                        )
                    }
                }
            }

            // Battery / System Status Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SleekBorderLight, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(SleekSuccessLight.copy(alpha = pulseAlpha))
                            )
                            Text(
                                text = "API 35 OPTIMIZATION ACTIVE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSleekBackground
                            )
                        }

                        Text(
                            text = "Zero Drain",
                            style = MaterialTheme.typography.labelSmall,
                            color = SleekSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "100% Offline • Local Room Database • No Background Services • Battery Safe",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSleekSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showEditNameDialog) {
        var newName by remember { mutableStateOf(uiState.userProfile.name) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Candidate Name") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.saveUserName(newName.trim()) {}
                        }
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showExactAlarmRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmRationaleDialog = false },
            title = { Text("Exact Alarm Permission Required") },
            text = {
                Text("To trigger the daily multivitamin reminder precisely at your chosen time without repeating background services, Exam Prep needs exact alarm permission.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExactAlarmRationaleDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmRationaleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileBentoCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    sub: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurfaceContainerLowest),
        modifier = modifier.border(1.dp, SleekBorderLight, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = OnSleekSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SleekPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = OnSleekSurfaceVariant
            )
        }
    }
}
