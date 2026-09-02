package com.anubhav.diprep.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anubhav.diprep.data.datastore.UserProfile
import com.anubhav.diprep.data.local.db.ScoreEntry
import com.anubhav.diprep.data.local.db.TaskLog
import com.anubhav.diprep.data.local.db.TimetableCompletion
import com.anubhav.diprep.data.local.db.TimetableSlot
import com.anubhav.diprep.ui.theme.DangerCoral
import com.anubhav.diprep.ui.theme.Gold
import com.anubhav.diprep.ui.theme.SuccessGreen
import com.anubhav.diprep.ui.viewmodel.BadgeType
import com.anubhav.diprep.ui.viewmodel.ExamSummary
import com.anubhav.diprep.ui.viewmodel.StatsViewModel
import com.anubhav.diprep.ui.viewmodel.SubjectInsight
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CellAllDone = SuccessGreen
private val GreenPass = SuccessGreen
private val AmberWarn = Gold
private val RedFail = DangerCoral

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: StatsViewModel = viewModel()
) {
    BackHandler(onBack = onBack)

    val cs = MaterialTheme.colorScheme
    val haptic = com.anubhav.diprep.util.rememberSafeHaptic()
    val cellEmpty = cs.surfaceContainerHigh
    val cellSingleDone = SuccessGreen.copy(alpha = 0.35f)
    val cellPartDone = SuccessGreen.copy(alpha = 0.65f)

    val examSummary by viewModel.examSummary.collectAsStateWithLifecycle()
    val heatmapData by viewModel.heatmapData.collectAsStateWithLifecycle()
    val heatmapSlots by viewModel.allSlots.collectAsStateWithLifecycle()
    val heatmapCompletions by viewModel.heatmapCompletions.collectAsStateWithLifecycle()
    val heatmapProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val subjectInsights by viewModel.subjectInsights.collectAsStateWithLifecycle()
    val scoreHistory by viewModel.scoreHistory.collectAsStateWithLifecycle()

    var showClearAllDialog by remember { mutableStateOf(false) }
    var scoreToDelete by remember { mutableStateOf<ScoreEntry?>(null) }
    var selectedStatsTab by remember { mutableStateOf(0) } // 0: Weekly Trends, 1: Heatmap & Insights

    // Clear All Confirmation Dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear All Test Scores?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This will permanently delete all ${scoreHistory.size} logged test scores. This cannot be undone.",
                    color = cs.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.tap()
                        viewModel.clearAll()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cs.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear All", color = cs.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Single Score Confirmation Dialog
    if (scoreToDelete != null) {
        val score = scoreToDelete!!
        AlertDialog(
            onDismissRequest = { scoreToDelete = null },
            title = { Text("Delete Score Entry?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Delete score for ${score.subject} (${score.obtained}/${score.total} · ${score.percentage}%)?",
                    color = cs.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.tap()
                        viewModel.deleteScore(score.id)
                        scoreToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cs.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", color = cs.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { scoreToDelete = null }) {
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
                        text = "Weekly progress",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = cs.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { haptic.tap(); onBack() }, modifier = Modifier.testTag("stats_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = cs.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background),
                windowInsets = WindowInsets(0)
            )
        },
        containerColor = cs.background,
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Segmented Control (Weekly Trends vs Heatmap & Insights)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cs.surface)
                    .border(1.dp, cs.outline, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedStatsTab == 0) cs.primary else Color.Transparent)
                        .clickable { haptic.tap(); selectedStatsTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Weekly trends",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedStatsTab == 0) cs.onPrimary else cs.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedStatsTab == 1) cs.primary else Color.Transparent)
                        .clickable { haptic.tap(); selectedStatsTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Heatmap & Insights",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedStatsTab == 1) cs.onPrimary else cs.onSurfaceVariant
                    )
                }
            }

            if (selectedStatsTab == 0) {
                WeeklyScreen()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    // EXAM COUNTDOWN MINI CARD
                    item {
                        ExamCountdownMiniCard(
                            summary = examSummary,
                            onClick = { haptic.tap(); onNavigateToSettings() }
                        )
                    }

                    // SECTION 1: HEATMAP (70 cells, 7 cols x 10 weeks)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Consistency Heatmap (10 Weeks)",
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
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    SeventyDaysHeatmapGrid(
                                        taskLogs = heatmapData,
                                        slots = heatmapSlots,
                                        completions = heatmapCompletions,
                                        profile = heatmapProfile
                                    )

                                    // Heatmap Legend
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Less",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = cs.onSurfaceVariant
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(cellEmpty))
                                            Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(cellSingleDone))
                                            Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(cellPartDone))
                                            Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(CellAllDone))
                                        }
                                        Text(
                                            text = "All 4 Done",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = cs.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 2: SUBJECT STRENGTH
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Subject Strength Breakdown",
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
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    subjectInsights.forEachIndexed { index, insight ->
                                        SubjectInsightRow(insight = insight)
                                        if (index < subjectInsights.size - 1) {
                                            HorizontalDivider(color = cs.outline, thickness = 1.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 3: SCORE HISTORY
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Score History (${scoreHistory.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = cs.onBackground
                            )

                            if (scoreHistory.isNotEmpty()) {
                                TextButton(onClick = { haptic.tap(); showClearAllDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        tint = cs.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Clear All", color = cs.error, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (scoreHistory.isEmpty()) {
                        item {
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
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = cs.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "No scores logged yet",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = cs.onBackground
                                    )
                                    Text(
                                        text = "Take a practice test and log your marks to track progress here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cs.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(scoreHistory, key = { it.id }) { score ->
                            ScoreHistoryItemRow(
                                score = score,
                                onDeleteRequest = { haptic.tap(); scoreToDelete = score }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamCountdownMiniCard(
    summary: ExamSummary,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cs.outline, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("stats_exam_mini_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${summary.daysLeft} days to go",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = cs.primary
                    )
                    Text(
                        text = " · ${summary.stream}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Target: ${summary.formattedExamDate} · ${summary.streak}🔥 streak · ${summary.totalTests} tests",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Settings",
                tint = cs.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SeventyDaysHeatmapGrid(
    taskLogs: List<TaskLog>,
    slots: List<TimetableSlot>,
    completions: List<TimetableCompletion>,
    profile: UserProfile
) {
    val cs = MaterialTheme.colorScheme
    val cellEmpty = cs.surfaceContainerHigh
    val cellSingleDone = SuccessGreen.copy(alpha = 0.35f)
    val cellPartDone = SuccessGreen.copy(alpha = 0.65f)

    val logsByDate = remember(taskLogs) { taskLogs.associateBy { it.dateISO } }
    val slotsByDow = remember(slots) { slots.groupBy { it.dayOfWeek } }
    val completionsByDate = remember(completions) {
        completions.groupBy { it.dateISO }.mapValues { (_, list) -> list.map { it.slotId }.toSet() }
    }
    val today = remember { LocalDate.now() }
    val todayIso = remember(today) { today.toString() }

    val daysList = remember(today) {
        (69 downTo 0).map { offset -> today.minusDays(offset.toLong()) }
    }

    var selectedCellTooltip by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (week in 0 until 10) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (dayCol in 0 until 7) {
                    val index = week * 7 + dayCol
                    if (index < daysList.size) {
                        val date = daysList[index]
                        val dateIso = date.toString()
                        val isToday = dateIso == todayIso
                        val log = logsByDate[dateIso]
                        val slotsForDow = slotsByDow[date.dayOfWeek.value] ?: emptyList()
                        val doneIds = completionsByDate[dateIso] ?: emptySet()

                        // Count completed vs total expected items
                        val totalItems = slotsForDow.size +
                            (if (profile.exerciseReminderEnabled) 1 else 0) +
                            (if (profile.vitaminReminderEnabled) 1 else 0)
                        val completedItems = slotsForDow.count { it.id in doneIds } +
                            (if (profile.exerciseReminderEnabled && log?.exerciseDone == true) 1 else 0) +
                            (if (profile.vitaminReminderEnabled && log?.vitaminDone == true) 1 else 0)

                        // Map to 0–4 intensity levels
                        val completedCount = when {
                            totalItems == 0 -> 0
                            completedItems == 0 -> 0
                            completedItems == totalItems -> 4
                            completedItems.toFloat() / totalItems >= 0.5f -> 2
                            else -> 1
                        }

                        val cellColor = when (completedCount) {
                            4 -> CellAllDone
                            2, 3 -> cellPartDone
                            1 -> cellSingleDone
                            else -> cellEmpty
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(cellColor)
                                .then(
                                    if (isToday) Modifier.border(2.dp, cs.primary, RoundedCornerShape(4.dp))
                                    else Modifier
                                )
                                .combinedClickable(
                                    onClick = {
                                        val formatted = date.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
                                        selectedCellTooltip = "$formatted: $completedItems/$totalItems done"
                                    },
                                    onLongClick = {
                                        val formatted = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH))
                                        selectedCellTooltip = "$formatted: $completedItems of $totalItems tasks done"
                                    }
                                )
                        )
                    }
                }
            }
        }

        if (selectedCellTooltip != null) {
            Text(
                text = selectedCellTooltip!!,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = cs.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun SubjectInsightRow(insight: SubjectInsight) {
    val cs = MaterialTheme.colorScheme
    val cellEmpty = cs.surfaceContainerHigh
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.subject,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = cs.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (insight.count > 0) "${insight.count} tests recorded" else "No tests recorded yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }

            when (insight.badge) {
                BadgeType.STRONG -> {
                    BadgePill(text = "Strong 💪 (${insight.avg}%)", color = GreenPass, bgColor = GreenPass.copy(alpha = 0.12f))
                }
                BadgeType.IMPROVING -> {
                    BadgePill(text = "Improving 📈 (${insight.avg}%)", color = AmberWarn, bgColor = AmberWarn.copy(alpha = 0.12f))
                }
                BadgeType.WEAK -> {
                    BadgePill(text = "Needs work 🎯 (${insight.avg}%)", color = RedFail, bgColor = RedFail.copy(alpha = 0.12f))
                }
                BadgeType.UNTESTED -> {
                    BadgePill(text = "Not tested", color = cs.onSurfaceVariant, bgColor = cellEmpty)
                }
            }
        }

        val targetFraction = (insight.avg ?: 0) / 100f
        val animatedProgress by animateFloatAsState(
            targetValue = targetFraction.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            label = "insight_prog_anim"
        )

        val progressColor = when (insight.badge) {
            BadgeType.STRONG -> GreenPass
            BadgeType.IMPROVING -> AmberWarn
            BadgeType.WEAK -> RedFail
            BadgeType.UNTESTED -> cellEmpty
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = progressColor,
            trackColor = cellEmpty
        )
    }
}

@Composable
private fun BadgePill(text: String, color: Color, bgColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoreHistoryItemRow(
    score: ScoreEntry,
    onDeleteRequest: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDeleteRequest()
                false
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cs.error)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = cs.onPrimary
                )
            }
        }
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cs.outline, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = score.subject,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${score.dateISO} · ${score.obtained}/${score.total} marks",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }

                val pctColor = when {
                    score.percentage >= 75 -> GreenPass
                    score.percentage >= 50 -> AmberWarn
                    else -> RedFail
                }

                Text(
                    text = "${score.percentage}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = pctColor
                )
            }
        }
    }
}
