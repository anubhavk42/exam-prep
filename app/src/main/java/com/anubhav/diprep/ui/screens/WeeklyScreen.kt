package com.anubhav.diprep.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anubhav.diprep.ui.theme.DangerCoral
import com.anubhav.diprep.ui.theme.Gold
import com.anubhav.diprep.ui.theme.SuccessGreen
import com.anubhav.diprep.ui.viewmodel.WeekComparison
import com.anubhav.diprep.ui.viewmodel.WeekSummary
import com.anubhav.diprep.ui.viewmodel.WeeklyViewModel

private val AmberPillText = Gold
private val GreenPass = SuccessGreen
private val AmberWarn = Gold
private val RedFail = DangerCoral
private val SelectedChipBg = Gold

@Composable
fun WeeklyScreen(
    modifier: Modifier = Modifier,
    viewModel: WeeklyViewModel = viewModel()
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val formattedExamDate by viewModel.formattedExamDate.collectAsStateWithLifecycle()
    val daysRemaining by viewModel.daysRemaining.collectAsStateWithLifecycle()
    val selectedSubject by viewModel.selectedSubject.collectAsStateWithLifecycle()
    val subjectsWithDataThisWeek by viewModel.subjectsWithDataThisWeek.collectAsStateWithLifecycle()
    val weekSummaries by viewModel.weekSummaries.collectAsStateWithLifecycle()
    val weekComparison by viewModel.weekComparison.collectAsStateWithLifecycle()

    val cs = MaterialTheme.colorScheme
    val haptic = com.anubhav.diprep.util.rememberSafeHaptic()
    val amberPillBg = Gold.copy(alpha = 0.15f)

    val verticalScrollState = rememberScrollState()
    val chipsScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(cs.background)
            .verticalScroll(verticalScrollState)
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sub-header: exam target + days-left pill (parent screen owns the title)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Target: $formattedExamDate",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(amberPillBg)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$daysRemaining days left",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AmberPillText
                )
            }
        }

        // Horizontally Scrollable Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(chipsScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // First Chip: "All subjects"
            val isAllSelected = selectedSubject == "ALL"
            FilterChip(
                selected = isAllSelected,
                onClick = { haptic.tap(); viewModel.selectSubject("ALL") },
                label = { Text("All subjects", fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium) },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SelectedChipBg,
                    selectedLabelColor = cs.onPrimary,
                    containerColor = cs.surfaceContainerLowest,
                    labelColor = cs.onBackground
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isAllSelected,
                    borderColor = cs.outline,
                    selectedBorderColor = SelectedChipBg,
                    borderWidth = 1.dp
                ),
                modifier = Modifier.testTag("filter_chip_all")
            )

            // Chips for each configured subject in custom_subjects
            subjects.forEach { subj ->
                val isSelected = selectedSubject.equals(subj, ignoreCase = true)
                val hasDataThisWeek = subjectsWithDataThisWeek.contains(subj.trim().lowercase())
                val alphaModifier = if (!hasDataThisWeek && !isSelected) Modifier.alpha(0.5f) else Modifier

                FilterChip(
                    selected = isSelected,
                    onClick = { haptic.tap(); viewModel.selectSubject(subj) },
                    label = { Text(subj, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SelectedChipBg,
                        selectedLabelColor = cs.onPrimary,
                        containerColor = cs.surfaceContainerLowest,
                        labelColor = cs.onBackground
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = cs.outline,
                        selectedBorderColor = SelectedChipBg,
                        borderWidth = 1.dp
                    ),
                    modifier = alphaModifier.testTag("filter_chip_${subj.lowercase().replace(" ", "_")}")
                )
            }
        }

        // Section: This week vs last week comparison
        ComparisonCard(comparison = weekComparison)

        // Section: 6 Weeks Breakdown Cards
        val hasAnyData = weekSummaries.any { it.count > 0 }
        if (!hasAnyData) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cs.surface)
                    .border(1.dp, cs.outline, RoundedCornerShape(16.dp))
                    .padding(vertical = 40.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = cs.onSurfaceVariant
                )
                Text(
                    text = "No tests logged yet",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onBackground
                )
                Text(
                    text = "Log a practice test to start tracking your weekly progress.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                weekSummaries.forEach { summary ->
                    WeekProgressCard(summary = summary)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun ComparisonCard(comparison: WeekComparison) {
    val cs = MaterialTheme.colorScheme
    val thisAvg = comparison.thisWeekAvg
    val lastAvg = comparison.lastWeekAvg

    val displayText: String
    val displayColor: androidx.compose.ui.graphics.Color
    val arrowIcon: androidx.compose.ui.graphics.vector.ImageVector?

    when {
        thisAvg == null -> {
            displayText = "No tests this week yet"
            displayColor = cs.onSurfaceVariant
            arrowIcon = null
        }
        lastAvg == null -> {
            displayText = "First week — no comparison yet"
            displayColor = cs.onSurfaceVariant
            arrowIcon = null
        }
        else -> {
            val diff = thisAvg - lastAvg
            when {
                diff > 0 -> {
                    displayText = "+$diff% vs last week"
                    displayColor = GreenPass
                    arrowIcon = Icons.Default.TrendingUp
                }
                diff < 0 -> {
                    displayText = "$diff% vs last week"
                    displayColor = RedFail
                    arrowIcon = Icons.Default.TrendingDown
                }
                else -> {
                    displayText = "Same as last week — $thisAvg%"
                    displayColor = cs.onSurfaceVariant
                    arrowIcon = null
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(cs.surface)
            .border(1.dp, cs.outline, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "THIS WEEK VS LAST",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant
            )
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = displayColor
            )
        }
        arrowIcon?.let {
            Icon(imageVector = it, contentDescription = null, tint = displayColor, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun WeekProgressCard(summary: WeekSummary) {
    val cs = MaterialTheme.colorScheme
    val trackBg = cs.surfaceContainerHigh
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cs.outline, RoundedCornerShape(18.dp))
            .testTag("week_card_${summary.weekIndex}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onBackground
                    )
                    Text(
                        text = if (summary.count > 0) "${summary.count} test${if (summary.count > 1) "s" else ""} logged" else "No tests logged",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }

                val scoreColor = when {
                    summary.avg == null -> cs.onSurfaceVariant
                    summary.avg >= 75 -> GreenPass
                    summary.avg >= 50 -> AmberWarn
                    else -> RedFail
                }

                Text(
                    text = if (summary.avg != null) "${summary.avg}%" else "—",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = scoreColor
                )
            }

            val targetFraction = if (summary.avg != null) (summary.avg / 100f).coerceIn(0f, 1f) else 0f
            val animatedFraction by animateFloatAsState(
                targetValue = targetFraction,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "week_progress_bar_anim"
            )

            val barColor = when {
                summary.avg == null -> trackBg
                summary.avg >= 75 -> GreenPass
                summary.avg >= 50 -> AmberWarn
                else -> RedFail
            }

            LinearProgressIndicator(
                progress = { animatedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(50)),
                color = barColor,
                trackColor = trackBg
            )
        }
    }
}
