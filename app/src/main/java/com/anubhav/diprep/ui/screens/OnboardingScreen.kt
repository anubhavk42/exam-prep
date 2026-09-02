package com.anubhav.diprep.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anubhav.diprep.R
import com.anubhav.diprep.data.datastore.ExamPresets
import com.anubhav.diprep.ui.viewmodel.OnboardingViewModel
import com.anubhav.diprep.util.rememberSafeHaptic
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onNavigateToHome: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val name by viewModel.nameInput.collectAsStateWithLifecycle()
    val selectedStream by viewModel.selectedStream.collectAsStateWithLifecycle()
    val examDate by viewModel.examDate.collectAsStateWithLifecycle()
    val subjectList by viewModel.subjectList.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }
    val focusRequester = remember { FocusRequester() }

    fun triggerShake() {
        coroutineScope.launch {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    -20f at 50
                    20f at 100
                    -15f at 150
                    15f at 200
                    -10f at 250
                    10f at 300
                    -5f at 350
                    0f at 400
                }
            )
        }
    }

    LaunchedEffect(currentStep) {
        if (currentStep == 1) {
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step Progress Indicator at Top
            StepProgressBar(currentStep = currentStep, totalSteps = 3)

            Spacer(modifier = Modifier.height(24.dp))

            // Animated Screen Step Content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "onboarding_step_anim"
            ) { step ->
                when (step) {
                    1 -> StepOneName(
                        name = name,
                        onNameChange = { viewModel.updateName(it) },
                        shakeOffsetX = shakeOffset.value,
                        focusRequester = focusRequester,
                        onNext = {
                            if (!viewModel.nextStep()) {
                                triggerShake()
                            }
                        },
                        onTryDemo = { viewModel.loadDemo(onSuccess = onNavigateToHome) }
                    )
                    2 -> StepTwoExamStream(
                        selectedStream = selectedStream,
                        onSelectStream = { viewModel.onStreamSelected(it) },
                        onUpdateStreamName = { viewModel.updateStreamName(it) },
                        onBack = { viewModel.previousStep() },
                        onNext = { viewModel.nextStep() }
                    )
                    3 -> StepThreeExamDateAndSubjects(
                        examDateIso = examDate,
                        subjectList = subjectList,
                        onDateChange = { viewModel.updateExamDate(it) },
                        onAddSubject = { viewModel.addSubject(it) },
                        onRemoveSubject = { viewModel.removeSubject(it) },
                        onBack = { viewModel.previousStep() },
                        onComplete = {
                            viewModel.completeOnboarding(onSuccess = onNavigateToHome)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepProgressBar(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..totalSteps) {
            val isActive = i <= currentStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            )
            if (i < totalSteps) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun StepOneName(
    name: String,
    onNameChange: (String) -> Unit,
    shakeOffsetX: Float,
    focusRequester: FocusRequester,
    onNext: () -> Unit,
    onTryDemo: () -> Unit
) {
    val scrollState = rememberScrollState()
    val haptic = rememberSafeHaptic()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 28.dp)
        ) {
            Text(
                text = "🎯",
                fontSize = 54.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = stringResource(R.string.onboarding_welcome_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.onboarding_welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            Box(
                modifier = Modifier
                    .offset { IntOffset(x = shakeOffsetX.roundToInt(), y = 0) }
                    .fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    placeholder = { Text(stringResource(R.string.onboarding_name_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onNext() }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("onboarding_name_input")
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { haptic.tap(); onNext() },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("step1_next_button")
            ) {
                Text(
                    text = stringResource(R.string.action_next),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { haptic.tap(); onTryDemo() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("try_demo_button")
            ) {
                Text(
                    text = stringResource(R.string.onboarding_try_demo),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepTwoExamStream(
    selectedStream: String,
    onSelectStream: (String) -> Unit,
    onUpdateStreamName: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val scrollState = rememberScrollState()
    val haptic = rememberSafeHaptic()
    val presetChips = ExamPresets.PRESET_CHIPS
    val isCustom = selectedStream.isNotEmpty() && selectedStream !in presetChips

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.padding(top = 12.dp)) {
            Text(
                text = stringResource(R.string.onboarding_exam_question),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.onboarding_exam_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetChips.forEach { chip ->
                    val isSelected = selectedStream == chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(50)
                            )
                            .clickable { haptic.tap(); onSelectStream(chip) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("stream_chip_${chip.lowercase().replace(Regex("[^a-z0-9]"), "_")}")
                    ) {
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.onboarding_custom_exam_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = if (isCustom) selectedStream else "",
                onValueChange = onUpdateStreamName,
                placeholder = { Text("e.g. RRB NTPC, CAT, GATE", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_stream_input")
            )
        }

        // Navigation Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { haptic.tap(); onBack() },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Text(text = stringResource(R.string.action_back), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = { haptic.tap(); onNext() },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = selectedStream.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("step2_next_button")
            ) {
                Text(
                    text = stringResource(R.string.action_next),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun StepThreeExamDateAndSubjects(
    examDateIso: String,
    subjectList: List<String>,
    onDateChange: (String) -> Unit,
    onAddSubject: (String) -> Unit,
    onRemoveSubject: (String) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val scrollState = rememberScrollState()
    val haptic = rememberSafeHaptic()
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newSubjectText by remember { mutableStateOf("") }

    val formattedDisplayDate = remember(examDateIso) {
        try {
            val parsed = LocalDate.parse(examDateIso)
            parsed.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH))
        } catch (_: Exception) {
            examDateIso
        }
    }

    if (showDatePicker) {
        val initialEpoch = remember(examDateIso) {
            try {
                LocalDate.parse(examDateIso).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
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
                            onDateChange(selectedLocalDate.toString())
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                newSubjectText = ""
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onBackground,
            title = {
                Text(
                    text = "Add Custom Subject",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                OutlinedTextField(
                    value = newSubjectText,
                    onValueChange = { newSubjectText = it },
                    placeholder = { Text("e.g. Clinical Pharmacology", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.tap()
                        if (newSubjectText.trim().isNotEmpty()) {
                            onAddSubject(newSubjectText.trim())
                            newSubjectText = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        newSubjectText = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.padding(top = 12.dp)) {
            Text(
                text = stringResource(R.string.onboarding_exam_date_question),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tappable Date Field
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .clickable { haptic.tap(); showDatePicker = true }
                    .testTag("exam_date_picker_trigger")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.onboarding_exam_target_date),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formattedDisplayDate,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.onboarding_change),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Your Subject List Section
            Text(
                text = stringResource(R.string.onboarding_your_subjects, subjectList.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subjectList.forEach { subj ->
                    InputChip(
                        selected = true,
                        onClick = { /* Keep selected */ },
                        label = { Text(text = subj, style = MaterialTheme.typography.bodySmall) },
                        trailingIcon = {
                            if (subjectList.size > 1) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove $subj",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { haptic.tap(); onRemoveSubject(subj) }
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surface,
                            selectedLabelColor = MaterialTheme.colorScheme.onBackground,
                            selectedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = InputChipDefaults.inputChipBorder(
                            enabled = true,
                            selected = true,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = MaterialTheme.colorScheme.outline,
                            borderWidth = 1.dp
                        )
                    )
                }

                // Add Subject Chip
                InputChip(
                    selected = false,
                    onClick = { haptic.tap(); showAddDialog = true },
                    label = { Text(stringResource(R.string.onboarding_add_subject), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    shape = RoundedCornerShape(12.dp),
                    colors = InputChipDefaults.inputChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.primary
                    ),
                    border = InputChipDefaults.inputChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        borderWidth = 1.dp
                    ),
                    modifier = Modifier.testTag("add_subject_chip")
                )
            }
        }

        // Action Row: Back & Start
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { haptic.tap(); onBack() },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
            ) {
                Text(text = stringResource(R.string.action_back), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = { haptic.tap(); onComplete() },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = subjectList.isNotEmpty(),
                modifier = Modifier
                    .weight(1.5f)
                    .height(54.dp)
                    .testTag("start_prep_button")
            ) {
                Icon(
                    imageVector = Icons.Default.RocketLaunch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.onboarding_start_prep),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
