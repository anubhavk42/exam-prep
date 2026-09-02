package com.anubhav.diprep.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.anubhav.diprep.R
import com.anubhav.diprep.util.LocaleHelper
import com.anubhav.diprep.util.rememberSafeHaptic
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.compose.ui.platform.LocalContext
import com.anubhav.diprep.data.datastore.ExamPresets
import com.anubhav.diprep.data.datastore.HomeSections
import com.anubhav.diprep.ui.theme.SuccessGreen
import com.anubhav.diprep.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onResetAppNav: () -> Unit = {},
    onNavigateToAppPicker: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    BackHandler(onBack = onBack)

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val totalScoresCount by viewModel.totalScoresCount.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()

    val cs = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val haptic = rememberSafeHaptic()

    // Dialog & BottomSheet States
    var showNameDialog by remember { mutableStateOf(false) }
    var editNameText by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showStreamSheet by remember { mutableStateOf(false) }

    var isEditingSubjects by remember { mutableStateOf(false) }
    val localSubjectList = remember(userProfile.customSubjects) {
        mutableStateListOf<String>().apply { addAll(userProfile.customSubjects) }
    }
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var newSubjectText by remember { mutableStateOf("") }

    var showClearScoresDialog by remember { mutableStateOf(false) }
    var showResetAppDialog by remember { mutableStateOf(false) }
    var showLoadDemoDialog by remember { mutableStateOf(false) }
    var showFocusModeExplanation by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val formattedExamDate = remember(userProfile.examDate) {
        try {
            val parsed = LocalDate.parse(userProfile.examDate)
            parsed.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH))
        } catch (_: Exception) {
            userProfile.examDate
        }
    }

    val formattedReminderTime = remember(userProfile.reminderHour, userProfile.reminderMinute) {
        val h = userProfile.reminderHour
        val m = userProfile.reminderMinute
        val amPm = if (h >= 12) "PM" else "AM"
        val displayHour = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        String.format(Locale.ENGLISH, "%d:%02d %s", displayHour, m, amPm)
    }

    // Name Edit Dialog
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            containerColor = cs.surface,
            titleContentColor = cs.onBackground,
            textContentColor = cs.onBackground,
            title = { Text("Edit Your Name", fontWeight = FontWeight.Bold, color = cs.onBackground) },
            text = {
                OutlinedTextField(
                    value = editNameText,
                    onValueChange = { if (it.length <= 20) editNameText = it },
                    placeholder = { Text("Enter first name", color = cs.onSurfaceVariant) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = cs.onBackground,
                        unfocusedTextColor = cs.onBackground,
                        focusedPlaceholderColor = cs.onSurfaceVariant,
                        unfocusedPlaceholderColor = cs.onSurfaceVariant,
                        cursorColor = cs.primary,
                        focusedBorderColor = cs.primary,
                        unfocusedBorderColor = cs.outline,
                        focusedContainerColor = cs.surface,
                        unfocusedContainerColor = cs.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.tap()
                        viewModel.updateName(editNameText)
                        showNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", color = cs.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // DatePicker Dialog
    if (showDatePicker) {
        val initialEpoch = remember(userProfile.examDate) {
            try {
                LocalDate.parse(userProfile.examDate).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialEpoch)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.tap()
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val selectedLocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                            viewModel.updateExamDate(selectedLocalDate.toString())
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = cs.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // TimePicker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = userProfile.reminderHour,
            initialMinute = userProfile.reminderMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = cs.surface,
            titleContentColor = cs.onBackground,
            textContentColor = cs.onBackground,
            title = { Text("Reminder Time", fontWeight = FontWeight.Bold, color = cs.onBackground) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.tap()
                        viewModel.updateReminderTime(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", color = cs.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Stream Selection BottomSheet
    if (showStreamSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showStreamSheet = false },
            sheetState = sheetState,
            containerColor = cs.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Exam name",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = cs.onBackground
                )
                Text(
                    text = "Pick a preset (reloads its default subjects) or type your own — your subject list stays as-is.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExamPresets.PRESET_CHIPS.forEach { chip ->
                        val isSelected = userProfile.examStream == chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) cs.primary else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) cs.primary else cs.outline,
                                    RoundedCornerShape(50)
                                )
                                .clickable {
                                    haptic.tap()
                                    viewModel.updateStream(chip)
                                    coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                                        showStreamSheet = false
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = chip,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) cs.onPrimary else cs.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                var customStreamText by remember { mutableStateOf(userProfile.examStream) }
                OutlinedTextField(
                    value = customStreamText,
                    onValueChange = { customStreamText = it },
                    placeholder = { Text("Type any exam name", color = cs.onSurfaceVariant) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = cs.onBackground,
                        unfocusedTextColor = cs.onBackground,
                        focusedPlaceholderColor = cs.onSurfaceVariant,
                        unfocusedPlaceholderColor = cs.onSurfaceVariant,
                        cursorColor = cs.primary,
                        focusedBorderColor = cs.primary,
                        unfocusedBorderColor = cs.outline,
                        focusedContainerColor = cs.surface,
                        unfocusedContainerColor = cs.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        haptic.tap()
                        viewModel.updateStreamName(customStreamText)
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            showStreamSheet = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save exam name", color = cs.onPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Add Custom Subject Dialog
    if (showAddSubjectDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddSubjectDialog = false
                newSubjectText = ""
            },
            containerColor = cs.surface,
            titleContentColor = cs.onBackground,
            textContentColor = cs.onBackground,
            title = { Text("Add Subject", fontWeight = FontWeight.Bold, color = cs.onBackground) },
            text = {
                OutlinedTextField(
                    value = newSubjectText,
                    onValueChange = { newSubjectText = it },
                    placeholder = { Text("Subject name", color = cs.onSurfaceVariant) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = cs.onBackground,
                        unfocusedTextColor = cs.onBackground,
                        focusedPlaceholderColor = cs.onSurfaceVariant,
                        unfocusedPlaceholderColor = cs.onSurfaceVariant,
                        cursorColor = cs.primary,
                        focusedBorderColor = cs.primary,
                        unfocusedBorderColor = cs.outline,
                        focusedContainerColor = cs.surface,
                        unfocusedContainerColor = cs.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.tap()
                        val trimmed = newSubjectText.trim()
                        if (trimmed.isNotEmpty() && !localSubjectList.contains(trimmed)) {
                            localSubjectList.add(trimmed)
                            newSubjectText = ""
                            showAddSubjectDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add", color = cs.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddSubjectDialog = false
                    newSubjectText = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Scores Confirmation Dialog
    if (showClearScoresDialog) {
        AlertDialog(
            onDismissRequest = { showClearScoresDialog = false },
            containerColor = cs.surface,
            titleContentColor = cs.onBackground,
            textContentColor = cs.onBackground,
            title = { Text("Clear All Scores?", fontWeight = FontWeight.Bold, color = cs.onBackground) },
            text = {
                Text(
                    text = "This will delete all $totalScoresCount score records permanently. This action cannot be undone.",
                    color = cs.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.tap()
                        viewModel.clearScores()
                        showClearScoresDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cs.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete Records", color = cs.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearScoresDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Load Demo Data Confirmation Dialog
    if (showLoadDemoDialog) {
        AlertDialog(
            onDismissRequest = { showLoadDemoDialog = false },
            containerColor = cs.surface,
            titleContentColor = cs.onBackground,
            textContentColor = cs.onBackground,
            title = { Text("Load Demo Data?", fontWeight = FontWeight.Bold, color = cs.onBackground) },
            text = {
                Text(
                    text = "This replaces your current data with a realistic sample dataset " +
                        "so you can explore the app. You can exit Demo Mode from the Home " +
                        "screen banner at any time.",
                    color = cs.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.tap()
                        showLoadDemoDialog = false
                        viewModel.loadDemoData(onComplete = onBack)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Load Demo", color = cs.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoadDemoDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset App Confirmation Dialog
    if (showResetAppDialog) {
        AlertDialog(
            onDismissRequest = { showResetAppDialog = false },
            containerColor = cs.surface,
            titleContentColor = cs.error,
            textContentColor = cs.onBackground,
            title = { Text("Reset Entire Application?", fontWeight = FontWeight.Bold, color = cs.error) },
            text = {
                Text(
                    text = "This will erase all your profile data, subjects, habit logs, and test scores. The app will return to the welcome onboarding flow.",
                    color = cs.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.tap()
                        showResetAppDialog = false
                        viewModel.resetApp(onComplete = onResetAppNav)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cs.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reset Everything", color = cs.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAppDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = cs.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = cs.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)
            )
        },
        containerColor = cs.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // SECTION 1: Profile
            SettingsSectionHeader(title = stringResource(R.string.settings_section_profile))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline, RoundedCornerShape(12.dp))
            ) {
                SettingsClickableRow(
                    icon = Icons.Default.Person,
                    label = "Your Name",
                    value = userProfile.name.ifBlank { "Candidate" },
                    onClick = {
                        editNameText = userProfile.name
                        showNameDialog = true
                    }
                )
            }

            // SECTION 2: Exam Details
            SettingsSectionHeader(title = stringResource(R.string.settings_section_exam))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline, RoundedCornerShape(12.dp))
            ) {
                Column {
                    SettingsClickableRow(
                        icon = Icons.Default.CalendarMonth,
                        label = "Exam Target Date",
                        value = formattedExamDate,
                        onClick = { showDatePicker = true }
                    )

                    HorizontalDivider(color = cs.outline, thickness = 1.dp)

                    // Exam Confirmed Switch Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = cs.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Exam Confirmed?",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = cs.onBackground
                                )
                                Text(
                                    text = if (userProfile.examDateConfirmed) "Shows (confirmed) on Home" else "Shows (rumoured) on Home",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = userProfile.examDateConfirmed,
                            onCheckedChange = { haptic.tap(); viewModel.toggleExamDateConfirmed(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = cs.onPrimary,
                                checkedTrackColor = cs.primary,
                                uncheckedThumbColor = cs.onSurfaceVariant,
                                uncheckedTrackColor = cs.outline
                            )
                        )
                    }

                    HorizontalDivider(color = cs.outline, thickness = 1.dp)

                    SettingsClickableRow(
                        icon = Icons.Default.School,
                        label = "Exam Stream",
                        value = userProfile.examStream,
                        onClick = { showStreamSheet = true }
                    )
                }
            }

            // SECTION 3: Subject List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsSectionHeader(title = "Your Subjects (${if (isEditingSubjects) localSubjectList.size else userProfile.customSubjects.size})")

                TextButton(
                    onClick = {
                        haptic.tap()
                        if (isEditingSubjects) {
                            if (localSubjectList.isNotEmpty()) {
                                viewModel.updateCustomSubjects(localSubjectList.toList())
                                isEditingSubjects = false
                            }
                        } else {
                            localSubjectList.clear()
                            localSubjectList.addAll(userProfile.customSubjects)
                            isEditingSubjects = true
                        }
                    }
                ) {
                    Text(
                        text = if (isEditingSubjects) "Save Changes" else "Edit",
                        fontWeight = FontWeight.Bold,
                        color = cs.primary
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val displaySubjects = if (isEditingSubjects) localSubjectList else userProfile.customSubjects

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        displaySubjects.forEach { subj ->
                            InputChip(
                                selected = true,
                                onClick = { },
                                label = { Text(text = subj, style = MaterialTheme.typography.bodySmall) },
                                trailingIcon = {
                                    if (isEditingSubjects && localSubjectList.size > 1) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { haptic.tap(); localSubjectList.remove(subj) }
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = if (isEditingSubjects) cs.primaryContainer else cs.background,
                                    selectedLabelColor = cs.onBackground,
                                    selectedTrailingIconColor = cs.onSurfaceVariant
                                ),
                                border = InputChipDefaults.inputChipBorder(
                                    enabled = true,
                                    selected = true,
                                    borderColor = cs.outline,
                                    selectedBorderColor = if (isEditingSubjects) cs.primary else cs.outline,
                                    borderWidth = 1.dp
                                )
                            )
                        }

                        if (isEditingSubjects) {
                            InputChip(
                                selected = false,
                                onClick = { haptic.tap(); showAddSubjectDialog = true },
                                label = { Text("Add subject +", fontWeight = FontWeight.Bold, color = cs.primary) },
                                shape = RoundedCornerShape(12.dp),
                                colors = InputChipDefaults.inputChipColors(
                                    containerColor = cs.primaryContainer,
                                    labelColor = cs.primary
                                ),
                                border = InputChipDefaults.inputChipBorder(
                                    enabled = true,
                                    selected = false,
                                    borderColor = cs.primary.copy(alpha = 0.4f),
                                    borderWidth = 1.dp
                                )
                            )
                        }
                    }
                }
            }

            // SECTION 4: Reminders
            SettingsSectionHeader(title = stringResource(R.string.settings_section_reminders))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline, RoundedCornerShape(12.dp))
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = cs.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Multivitamin Reminder",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = cs.onBackground
                                )
                                Text(
                                    text = "Exact alarm for daily afternoon dose",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = userProfile.reminderEnabled,
                            onCheckedChange = { haptic.tap(); viewModel.toggleReminder(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = cs.onPrimary,
                                checkedTrackColor = cs.primary,
                                uncheckedThumbColor = cs.onSurfaceVariant,
                                uncheckedTrackColor = cs.outline
                            )
                        )
                    }

                    if (userProfile.reminderEnabled) {
                        HorizontalDivider(color = cs.outline, thickness = 1.dp)

                        SettingsClickableRow(
                            icon = Icons.Default.AccessTime,
                            label = "Reminder Time",
                            value = formattedReminderTime,
                            onClick = { showTimePicker = true }
                        )
                    }
                }
            }

            // SECTION 5: Home Screen customization
            SettingsSectionHeader(title = stringResource(R.string.settings_section_home))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline, RoundedCornerShape(12.dp))
            ) {
                Column {
                    HomeSectionRow(
                        label = "Greeting",
                        description = "Always shown",
                        fixed = true,
                        visible = true,
                        canMoveUp = false,
                        canMoveDown = false,
                        onToggle = {},
                        onMoveUp = {},
                        onMoveDown = {}
                    )
                    HorizontalDivider(color = cs.outline, thickness = 1.dp)
                    HomeSectionRow(
                        label = "Exam countdown",
                        description = "Always shown",
                        fixed = true,
                        visible = true,
                        canMoveUp = false,
                        canMoveDown = false,
                        onToggle = {},
                        onMoveUp = {},
                        onMoveDown = {}
                    )
                    val order = userProfile.homeSectionOrder
                    order.forEachIndexed { index, key ->
                        HorizontalDivider(color = cs.outline, thickness = 1.dp)
                        HomeSectionRow(
                            label = HomeSections.label(key),
                            description = HomeSections.description(key),
                            fixed = false,
                            visible = key !in userProfile.homeHiddenSections,
                            canMoveUp = index > 0,
                            canMoveDown = index < order.lastIndex,
                            onToggle = { nowVisible -> viewModel.setHomeSectionHidden(key, !nowVisible) },
                            onMoveUp = { viewModel.moveHomeSection(key, up = true) },
                            onMoveDown = { viewModel.moveHomeSection(key, up = false) }
                        )
                    }
                }
            }

            // SECTION 5b: Appearance
            SettingsSectionHeader(title = stringResource(R.string.settings_section_appearance))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline, RoundedCornerShape(12.dp))
            ) {
                Column {
                    listOf(
                        Triple("LIGHT", stringResource(R.string.theme_light), Icons.Default.LightMode),
                        Triple("DARK", stringResource(R.string.theme_dark), Icons.Default.DarkMode),
                        Triple("SYSTEM", stringResource(R.string.theme_system), Icons.Default.BrightnessAuto)
                    ).forEachIndexed { index, (mode, modeLabel, modeIcon) ->
                        if (index > 0) HorizontalDivider(color = cs.outline, thickness = 1.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { haptic.tap(); viewModel.updateThemeMode(mode) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = modeIcon,
                                    contentDescription = null,
                                    tint = if (userProfile.themeMode == mode) cs.primary else cs.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = modeLabel,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = cs.onBackground
                                )
                            }
                            RadioButton(
                                selected = userProfile.themeMode == mode,
                                onClick = { haptic.tap(); viewModel.updateThemeMode(mode) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = cs.primary,
                                    unselectedColor = cs.onSurfaceVariant
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = cs.outline, thickness = 1.dp)

                    // Haptic Feedback master toggle (app-wide preference)
                    SettingsToggleRow(
                        icon = Icons.Default.Vibration,
                        label = "Haptic Feedback",
                        description = "Subtle vibration on taps, toggles and buttons",
                        checked = userProfile.hapticFeedbackEnabled,
                        onCheckedChange = { viewModel.toggleHapticFeedback(it) }
                    )
                }
            }

            // SECTION 5b-ii: App Language
            SettingsSectionHeader(title = stringResource(R.string.settings_section_language))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline, RoundedCornerShape(12.dp))
            ) {
                Column {
                    listOf(
                        LocaleHelper.LANG_ENGLISH to stringResource(R.string.settings_language_english),
                        LocaleHelper.LANG_HINDI to stringResource(R.string.settings_language_hindi)
                    ).forEachIndexed { index, (tag, langLabel) ->
                        if (index > 0) HorizontalDivider(color = cs.outline, thickness = 1.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { haptic.tap(); viewModel.updateLanguage(tag) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = langLabel,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = cs.onBackground
                            )
                            RadioButton(
                                selected = currentLanguage == tag,
                                onClick = { haptic.tap(); viewModel.updateLanguage(tag) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = cs.primary,
                                    unselectedColor = cs.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // SECTION 5c: Wellness habits (optional, separate from timetable)
            SettingsSectionHeader(title = stringResource(R.string.settings_section_wellness))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline, RoundedCornerShape(12.dp))
            ) {
                Column {
                    SettingsToggleRow(
                        icon = Icons.Default.FitnessCenter,
                        label = "Exercise reminder",
                        description = "A daily nudge to move — on by default",
                        checked = userProfile.exerciseReminderEnabled,
                        onCheckedChange = { viewModel.toggleExerciseReminder(it) }
                    )
                    HorizontalDivider(color = cs.outline, thickness = 1.dp)
                    SettingsToggleRow(
                        icon = Icons.Default.Medication,
                        label = "Multivitamin (optional)",
                        description = "Off by default — turn on to show it in today's checklist",
                        checked = userProfile.vitaminReminderEnabled,
                        onCheckedChange = { viewModel.toggleVitaminReminder(it) }
                    )
                }
            }

            // SECTION 5d: Focus Mode / Notification Filter
            SettingsSectionHeader(title = "Notification Filter (Focus Mode)")
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline, RoundedCornerShape(12.dp))
            ) {
                Column {
                    SettingsToggleRow(
                        icon = Icons.Default.NotificationsActive,
                        label = "Enable Focus Mode",
                        description = "Suppress selected app notifications during study time",
                        checked = userProfile.notificationFilterEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !userProfile.notificationFilterEnabled) {
                                showFocusModeExplanation = true
                            } else {
                                viewModel.toggleNotificationFilter(enabled)
                            }
                        }
                    )
                    HorizontalDivider(color = cs.outline, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { haptic.tap(); onNavigateToAppPicker() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = cs.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Apps to mute",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = cs.onBackground
                                )
                                val mutedCount = userProfile.mutedAppPackages.size
                                Text(
                                    if (mutedCount == 0) "None selected" else "$mutedCount app${if (mutedCount != 1) "s" else ""} selected",
                                    fontSize = 13.sp,
                                    color = cs.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = cs.onBackground.copy(alpha = 0.5f)
                        )
                    }
                    HorizontalDivider(color = cs.outline, thickness = 1.dp)
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Activation mode",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onBackground.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(8.dp))
                        listOf(
                            "TIMETABLE" to "Auto — during Timetable slots",
                            "MANUAL" to "Manual — Start/Stop on Goals screen"
                        ).forEach { (mode, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { haptic.tap(); viewModel.updateFilterActivationMode(mode) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = userProfile.filterActivationMode == mode,
                                    onClick = { haptic.tap(); viewModel.updateFilterActivationMode(mode) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = cs.primary,
                                        unselectedColor = cs.onBackground.copy(alpha = 0.4f)
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(label, fontSize = 14.sp, color = cs.onBackground)
                            }
                        }
                    }
                }
            }

            // Permission explanation dialog for Focus Mode
            if (showFocusModeExplanation) {
                AlertDialog(
                    onDismissRequest = { showFocusModeExplanation = false },
                    containerColor = cs.surface,
                    titleContentColor = cs.onBackground,
                    textContentColor = cs.onBackground,
                    title = { Text("Notification Access Required", fontWeight = FontWeight.Bold, color = cs.onBackground) },
                    text = {
                        Text(
                            "Focus Mode needs Notification Access permission to suppress " +
                            "distracting apps during study time.\n\n" +
                            "Calls and messages are never affected — only the apps you select.\n\n" +
                            "Tap \"Open Settings\" to grant access, then come back and enable Focus Mode.",
                            color = cs.onBackground
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            haptic.tap()
                            showFocusModeExplanation = false
                            viewModel.toggleNotificationFilter(true)
                            context.startActivity(
                                Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }) {
                            Text("Open Settings", color = cs.primary, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showFocusModeExplanation = false }) {
                            Text("Cancel", color = cs.onBackground)
                        }
                    }
                )
            }

            // SECTION 5e: About
            SettingsSectionHeader(title = stringResource(R.string.settings_section_about))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline, RoundedCornerShape(12.dp))
            ) {
                Column {
                    SettingsClickableRow(
                        icon = Icons.Default.PrivacyTip,
                        label = "Privacy",
                        value = stringResource(R.string.settings_privacy_policy),
                        onClick = onNavigateToPrivacyPolicy
                    )
                    HorizontalDivider(color = cs.outline, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.tap()
                                val shareText = "I'm using Exam Prep to stay consistent with my exam " +
                                    "preparation — it's a simple, fully offline study tracker with zero ads " +
                                    "and zero data collection. Give it a try: " +
                                    "https://play.google.com/store/apps/details?id=com.anubhav.diprep"
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                runCatching {
                                    context.startActivity(
                                        Intent.createChooser(sendIntent, "Invite Friends")
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = cs.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_invite_friends),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = cs.onBackground
                                )
                                Text(
                                    text = "Share Exam Prep with someone preparing for exams too",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = cs.onSurfaceVariant
                        )
                    }
                }
            }

            // SECTION 6: Data Management
            SettingsSectionHeader(title = stringResource(R.string.settings_section_data))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.error.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { haptic.tap(); showLoadDemoDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = cs.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Load Demo Data",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = cs.onBackground
                                )
                                Text(
                                    text = "Replace data with a sample dataset to explore",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = cs.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(color = cs.outline, thickness = 1.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { haptic.tap(); showClearScoresDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = cs.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Clear All Scores",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = cs.error
                                )
                                Text(
                                    text = "$totalScoresCount score entries saved",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = cs.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(color = cs.outline, thickness = 1.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { haptic.tap(); showResetAppDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                tint = cs.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Reset App",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = cs.error
                                )
                                Text(
                                    text = "Wipe all data and restart onboarding",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = cs.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val cs = MaterialTheme.colorScheme
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = cs.onBackground
    )
}

@Composable
private fun SettingsClickableRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberSafeHaptic()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { haptic.tap(); onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = cs.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onBackground
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = cs.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberSafeHaptic()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = cs.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onBackground
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = { haptic.tap(); onCheckedChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = cs.onPrimary,
                checkedTrackColor = cs.primary,
                uncheckedThumbColor = cs.onSurfaceVariant,
                uncheckedTrackColor = cs.outline
            )
        )
    }
}

@Composable
private fun HomeSectionRow(
    label: String,
    description: String,
    fixed: Boolean,
    visible: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberSafeHaptic()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = null,
            tint = if (fixed) cs.outline else cs.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = cs.onBackground
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }
        if (fixed) {
            Text(
                text = "Fixed",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = cs.onSurfaceVariant
            )
        } else {
            IconButton(onClick = { haptic.tap(); onMoveUp() }, enabled = canMoveUp, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Move up",
                    tint = if (canMoveUp) cs.onBackground else cs.outline
                )
            }
            IconButton(onClick = { haptic.tap(); onMoveDown() }, enabled = canMoveDown, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Move down",
                    tint = if (canMoveDown) cs.onBackground else cs.outline
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Switch(
                checked = visible,
                onCheckedChange = { haptic.tap(); onToggle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = cs.onPrimary,
                    checkedTrackColor = cs.primary,
                    uncheckedThumbColor = cs.onSurfaceVariant,
                    uncheckedTrackColor = cs.outline
                )
            )
        }
    }
}
