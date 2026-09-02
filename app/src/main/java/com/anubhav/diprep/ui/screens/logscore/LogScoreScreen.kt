package com.anubhav.diprep.ui.screens.logscore

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anubhav.diprep.data.local.db.ScoreEntry
import com.anubhav.diprep.ui.theme.DangerCoral
import com.anubhav.diprep.ui.theme.Gold
import com.anubhav.diprep.ui.theme.SuccessGreen
import com.anubhav.diprep.ui.viewmodel.LogScoreViewModel
import kotlinx.coroutines.launch
import kotlin.random.Random

private val GreenPass = SuccessGreen
private val AmberWarn = Gold
private val RedFail = DangerCoral

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScoreScreen(
    onBack: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    initialSubject: String = "",
    initialTopicId: Long = -1L,
    viewModel: LogScoreViewModel = viewModel()
) {
    BackHandler(onBack = onBack)
    val cs = MaterialTheme.colorScheme
    val haptic = com.anubhav.diprep.util.rememberSafeHaptic()

    LaunchedEffect(initialSubject, initialTopicId) {
        if (initialSubject.isNotEmpty()) {
            viewModel.setInitialValues(initialSubject, initialTopicId)
        }
    }

    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val selectedTopicName by viewModel.selectedTopicName.collectAsStateWithLifecycle()
    val selectedSubject by viewModel.selectedSubject.collectAsStateWithLifecycle()
    val obtainedInput by viewModel.obtainedInput.collectAsStateWithLifecycle()
    val totalInput by viewModel.totalInput.collectAsStateWithLifecycle()
    val derivedPct by viewModel.derivedPercentage.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val last14Scores by viewModel.last14.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var dropdownExpanded by remember { mutableStateOf(false) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var quickAddText by remember { mutableStateOf("") }
    var showConfetti by remember { mutableStateOf(false) }

    // Filter dropdown items based on typed text
    val filteredSubjects = remember(selectedSubject, subjects) {
        if (selectedSubject.isBlank()) {
            subjects
        } else {
            subjects.filter { it.contains(selectedSubject, ignoreCase = true) }
        }
    }

    // Dynamic Live Color
    val targetColor = when {
        derivedPct == null -> cs.onSurfaceVariant
        derivedPct!! >= 75 -> GreenPass
        derivedPct!! >= 50 -> AmberWarn
        else -> RedFail
    }
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300),
        label = "pct_color_anim"
    )

    // Quick Add Subject Dialog
    if (showQuickAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showQuickAddDialog = false
                quickAddText = ""
            },
            containerColor = cs.surface,
            titleContentColor = cs.onBackground,
            textContentColor = cs.onBackground,
            title = { Text("Add Subject", fontWeight = FontWeight.Bold, color = cs.onBackground) },
            text = {
                OutlinedTextField(
                    value = quickAddText,
                    onValueChange = { quickAddText = it },
                    placeholder = { Text("e.g. Pharmaceutical Jurisprudence", color = cs.onSurfaceVariant) },
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
                        if (quickAddText.trim().isNotEmpty()) {
                            viewModel.addQuickSubject(quickAddText.trim())
                            quickAddText = ""
                            showQuickAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add & Select", color = cs.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showQuickAddDialog = false
                    quickAddText = ""
                }) {
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
                        text = "Log Practice Score",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = cs.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { haptic.tap(); onBack() }, modifier = Modifier.testTag("log_score_back_button")) {
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = cs.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section: Subject Dropdown + Quick Add Button
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Subject Name",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = cs.onBackground
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedSubject,
                                onValueChange = {
                                    viewModel.updateSubject(it)
                                    dropdownExpanded = true
                                },
                                placeholder = { Text("Select or search subject...", color = cs.onSurfaceVariant) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                                },
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
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
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                                    .fillMaxWidth()
                                    .testTag("subject_dropdown_input")
                            )

                            if (filteredSubjects.isNotEmpty()) {
                                ExposedDropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false },
                                    modifier = Modifier.background(cs.surface)
                                ) {
                                    filteredSubjects.forEach { subj ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = subj,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = cs.onBackground
                                                )
                                            },
                                            onClick = {
                                                haptic.tap()
                                                viewModel.updateSubject(subj)
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Quick Add (+) Button
                        FilledTonalIconButton(
                            onClick = { haptic.tap(); showQuickAddDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = cs.surface,
                                contentColor = cs.primary
                            ),
                            modifier = Modifier
                                .size(54.dp)
                                .testTag("quick_add_subject_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Quick Add Subject",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Topic badge — shown when navigated from SubjectDetail
                if (selectedTopicName != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(cs.primary.copy(alpha = 0.12f))
                            .border(1.dp, cs.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = cs.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Topic: $selectedTopicName",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.primary
                        )
                    }
                }

                // Marks Obtained & Total Marks Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Marks Obtained",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = cs.onBackground
                        )
                        OutlinedTextField(
                            value = obtainedInput,
                            onValueChange = { viewModel.updateObtained(it) },
                            placeholder = { Text("e.g. 78", color = cs.onSurfaceVariant) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(16.dp),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("marks_obtained_input")
                        )
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Total Marks",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = cs.onBackground
                        )
                        OutlinedTextField(
                            value = totalInput,
                            onValueChange = { viewModel.updateTotal(it) },
                            placeholder = { Text("e.g. 100", color = cs.onSurfaceVariant) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(16.dp),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("total_marks_input")
                        )
                    }
                }

                // Live Percentage Preview Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cs.outline, RoundedCornerShape(20.dp))
                        .testTag("live_percentage_preview_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (derivedPct != null) "$derivedPct%" else "— %",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Black
                            ),
                            color = animatedColor,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = when {
                                derivedPct == null -> "Enter marks to see live percentage"
                                derivedPct!! >= 75 -> "Excellent Performance! 🌟 (>= 75%)"
                                derivedPct!! >= 50 -> "Good Effort! Keep Pushing 🚀 (>= 50%)"
                                else -> "Needs Immediate Review ⚠️ (< 50%)"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = animatedColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Save Score Action Button
                Button(
                    onClick = {
                        haptic.tap()
                        viewModel.saveScore(
                            onSuccess = { msg, isNewPB ->
                                if (isNewPB) {
                                    showConfetti = true
                                    haptic.celebrate()
                                }
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            },
                            onError = { err ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(err)
                                }
                            }
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_score_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = cs.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Save Practice Score",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onPrimary
                    )
                }

                // Stats Section (if tests exist)
                if (stats.count > 0) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Performance Overview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = cs.onBackground
                    )

                    // 3 Stat Cards in a row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMiniCard(
                            title = "7-Day Avg",
                            value = if (stats.avg7 != null) "${stats.avg7}%" else "—",
                            modifier = Modifier.weight(1f),
                            tint = cs.primary
                        )
                        StatMiniCard(
                            title = "All-Time Avg",
                            value = if (stats.avgAll != null) "${stats.avgAll}%" else "—",
                            modifier = Modifier.weight(1f),
                            tint = GreenPass
                        )
                        StatMiniCard(
                            title = "Tests Logged",
                            value = "${stats.count}",
                            modifier = Modifier.weight(1f),
                            tint = cs.onBackground
                        )
                    }

                    // Bar Chart of last 14 entries
                    if (last14Scores.isNotEmpty()) {
                        Text(
                            text = "Recent Test Trends (Last 14)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = cs.onBackground
                        )

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = cs.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, cs.outline, RoundedCornerShape(18.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                ScoresTrendBarChart(
                                    scores = last14Scores,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(170.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Confetti Overlay on New PB
            if (showConfetti) {
                ConfettiEffect(
                    onFinished = { showConfetti = false }
                )
            }
        }
    }
}

@Composable
private fun StatMiniCard(
    title: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        modifier = modifier.border(1.dp, cs.outline, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = tint
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ScoresTrendBarChart(
    scores: List<ScoreEntry>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val totalBars = scores.size
        if (totalBars == 0) return@Canvas

        val barSpacing = 8.dp.toPx()
        val totalSpacing = (totalBars - 1) * barSpacing
        val barWidth = ((size.width - totalSpacing) / totalBars).coerceAtLeast(10.dp.toPx())
        val chartHeight = size.height - 24.dp.toPx()

        scores.forEachIndexed { index, score ->
            val fraction = (score.percentage / 100f).coerceIn(0f, 1f)
            val barHeight = chartHeight * fraction
            val left = index * (barWidth + barSpacing)
            val top = chartHeight - barHeight

            val barColor = when {
                score.percentage >= 75 -> GreenPass
                score.percentage >= 50 -> AmberWarn
                else -> RedFail
            }

            // Draw Bar
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx())),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
        }
    }
}

@Composable
private fun ConfettiEffect(
    onFinished: () -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
        )
        onFinished()
    }

    val particles = remember {
        List(45) {
            Triple(
                Random.nextFloat(), // x
                Random.nextFloat() * 0.4f, // speed
                listOf(Color(0xFFF97316), Color(0xFF1F9E5E), Color(0xFFC9622F), Color(0xFFD4860A), Color(0xFF3B82F6)).random()
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { (startX, speed, color) ->
            val x = startX * size.width
            val y = progress.value * size.height * (1f + speed)
            if (y < size.height) {
                drawCircle(
                    color = color.copy(alpha = (1f - progress.value).coerceIn(0f, 1f)),
                    radius = 5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}
