package com.anubhav.diprep.ui.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anubhav.diprep.R
import com.anubhav.diprep.data.datastore.HomeSections
import com.anubhav.diprep.ui.screens.ConfettiEffect
import com.anubhav.diprep.ui.theme.Gold
import com.anubhav.diprep.ui.theme.DangerCoral
import com.anubhav.diprep.ui.theme.SuccessGreen
import com.anubhav.diprep.ui.viewmodel.HomeViewModel
import com.anubhav.diprep.ui.viewmodel.RevisionDebtTopic
import com.anubhav.diprep.util.rememberSafeHaptic
import kotlinx.coroutines.flow.collectLatest

private val CardShape = RoundedCornerShape(12.dp)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToLogScore: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onExitDemo: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val todayFormatted by viewModel.todayFormatted.collectAsStateWithLifecycle()
    val quote by viewModel.todayQuote.collectAsStateWithLifecycle()
    val daysRemaining by viewModel.daysRemaining.collectAsStateWithLifecycle()
    val weeksAndDays by viewModel.weeksAndDays.collectAsStateWithLifecycle()
    val formattedExamDate by viewModel.formattedExamDate.collectAsStateWithLifecycle()
    val isDateRumoured by viewModel.isDateRumoured.collectAsStateWithLifecycle()
    val streakCount by viewModel.streakCount.collectAsStateWithLifecycle()
    val personalBest by viewModel.personalBest.collectAsStateWithLifecycle()
    val todayGoals by viewModel.todayGoals.collectAsStateWithLifecycle()
    val whatsNext by viewModel.whatsNext.collectAsStateWithLifecycle()
    val weakTopic by viewModel.weakTopic.collectAsStateWithLifecycle()
    val sections by viewModel.visibleHomeSections.collectAsStateWithLifecycle()
    val overallMastery by viewModel.overallMastery.collectAsStateWithLifecycle()
    val endOfDayRecap by viewModel.endOfDayRecap.collectAsStateWithLifecycle()
    val revisionDebt by viewModel.revisionDebt.collectAsStateWithLifecycle()
    val isDemoMode by viewModel.isDemoMode.collectAsStateWithLifecycle()

    var showMilestoneConfetti by remember { mutableStateOf(false) }
    var milestoneMessage by remember { mutableStateOf("") }

    val cs = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val haptic = rememberSafeHaptic()

    LaunchedEffect(Unit) {
        viewModel.pendingMilestone.collectLatest { message ->
            milestoneMessage = message
            showMilestoneConfetti = true
            haptic.celebrate()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .verticalScroll(scrollState)
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Demo Mode banner (persistent while active)
        if (isDemoMode) {
            DemoModeBanner(onExitDemo = { haptic.tap(); viewModel.exitDemo(onComplete = onExitDemo) })
        }

        // 1. Greeting (always shown)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_greeting, userName),
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = todayFormatted,
                    style = MaterialTheme.typography.headlineSmall,
                    color = cs.onBackground
                )
            }
            IconButton(
                onClick = { haptic.tap(); onNavigateToSettings() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(cs.surface)
                    .border(1.dp, cs.outline, CircleShape)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = cs.onSurfaceVariant
                )
            }
        }

        // 2. Countdown hero (always shown)
        CountdownCard(
            daysRemaining = daysRemaining,
            weeksAndDays = weeksAndDays,
            formattedExamDate = formattedExamDate,
            isDateRumoured = isDateRumoured
        )

        // 3+. Reorderable / toggleable sections
        sections.forEach { key ->
            when (key) {
                HomeSections.WHATS_NEXT -> whatsNext?.let { next ->
                    WhatsNextCard(label = next.label, start = next.startTime, end = next.endTime)
                }

                HomeSections.WEAK_ALERT -> weakTopic?.let { weak ->
                    WeakTopicCard(subject = weak.subject, reason = weak.reason)
                }

                HomeSections.REVISION_REMINDER -> revisionDebt?.let { debt ->
                    RevisionReminderCard(debt = debt)
                }

                HomeSections.STREAK_PB -> StreakAndBestRow(
                    streakCount = streakCount,
                    personalBest = personalBest
                )

                HomeSections.TODAYS_GOALS -> TodaysGoalsCard(
                    done = todayGoals.done,
                    total = todayGoals.total
                )

                HomeSections.LOG_BUTTON -> Button(
                    onClick = { haptic.tap(); onNavigateToLogScore() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("home_log_score_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = cs.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.home_log_test),
                        style = MaterialTheme.typography.titleLarge,
                        color = cs.onPrimary
                    )
                }

                HomeSections.QUOTE -> QuoteCard(text = quote.text, author = quote.author)

                HomeSections.SYLLABUS_RING -> SyllabusRingCard(overallMastery = overallMastery)
            }
        }

        // End-of-Day Recap: shown only in the evening (after 6 PM)
        if (viewModel.isEvening) {
            EndOfDayRecapCard(recap = endOfDayRecap)
        }
    }

    // Milestone celebration overlay
    if (showMilestoneConfetti) {
        ConfettiEffect(onDismiss = { showMilestoneConfetti = false })
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 56.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.primary)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🎉", fontSize = 20.sp)
                Text(
                    text = milestoneMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
    } // end Box
}

@Composable
private fun DemoModeBanner(onExitDemo: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(cs.surface)
            .border(1.dp, Gold, CardShape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.demo_mode_title),
                style = MaterialTheme.typography.labelSmall,
                color = Gold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.demo_mode_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }
        Text(
            text = stringResource(R.string.demo_mode_exit),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Gold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onExitDemo)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("exit_demo_button")
        )
    }
}

@Composable
private fun CountdownCard(
    daysRemaining: Long,
    weeksAndDays: String,
    formattedExamDate: String,
    isDateRumoured: Boolean
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(cs.surface)
            .border(1.dp, cs.outline, CardShape)
            .padding(vertical = 22.dp, horizontal = 18.dp)
            .testTag("countdown_card"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (daysRemaining > 0) {
            Text(
                text = "$daysRemaining",
                style = MaterialTheme.typography.displayLarge,
                color = cs.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.home_days_remaining),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                color = cs.onSurfaceVariant
            )
            if (weeksAndDays.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = weeksAndDays,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "Exam day — all the best",
                style = MaterialTheme.typography.headlineMedium,
                color = cs.primary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = buildString {
                append("Exam: ")
                append(formattedExamDate)
                append(if (isDateRumoured) " · rumoured" else " · confirmed")
            },
            style = MaterialTheme.typography.labelSmall,
            color = cs.primary
        )
    }
}

@Composable
private fun WhatsNextCard(label: String, start: String, end: String) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(cs.surface)
            .border(1.dp, cs.outline, CardShape)
            .padding(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = cs.primary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stringResource(R.string.home_whats_next),
                style = MaterialTheme.typography.labelSmall,
                color = cs.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$start – $end   $label",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurface
        )
    }
}

@Composable
private fun WeakTopicCard(subject: String, reason: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(cs.errorContainer)
            .border(1.dp, cs.error.copy(alpha = 0.4f), CardShape)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.WarningAmber,
            contentDescription = null,
            tint = DangerCoral,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "NEEDS ATTENTION",
                style = MaterialTheme.typography.labelSmall,
                color = DangerCoral
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subject,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurface
            )
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RevisionReminderCard(debt: RevisionDebtTopic) {
    val cs = MaterialTheme.colorScheme
    val isUrgent = debt.daysSince >= 15
    val accentColor = if (isUrgent) DangerCoral else Gold
    val bgColor = if (isUrgent) cs.errorContainer else cs.surface
    val borderColor = if (isUrgent) cs.error.copy(alpha = 0.4f) else cs.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(bgColor)
            .border(1.dp, borderColor, CardShape)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Update,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "REVISION DUE",
                style = MaterialTheme.typography.labelSmall,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${debt.topicName} · ${debt.subject}",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurface
            )
            Text(
                text = "Not reviewed in ${debt.daysSince} days — time to revisit!",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StreakAndBestRow(streakCount: Int, personalBest: Int?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            emoji = "🔥",
            value = "$streakCount",
            label = stringResource(R.string.home_day_streak),
            modifier = Modifier.weight(1f).testTag("day_streak_card")
        )
        StatCard(
            emoji = "🏆",
            value = if (personalBest != null && personalBest > 0) "$personalBest%" else "—",
            label = stringResource(R.string.home_personal_best),
            modifier = Modifier.weight(1f).testTag("personal_best_card")
        )
    }
}

@Composable
private fun StatCard(
    emoji: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(cs.surface)
            .border(1.dp, cs.outline, CardShape)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = emoji, fontSize = 18.sp)
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = cs.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant
        )
    }
}

@Composable
private fun TodaysGoalsCard(done: Int, total: Int) {
    val cs = MaterialTheme.colorScheme
    val ratio = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "goals_progress"
    )
    val fill = when {
        total == 0 -> cs.surfaceContainerHighest
        ratio >= 1f -> SuccessGreen
        ratio >= 0.5f -> cs.primary
        else -> DangerCoral
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(cs.surface)
            .border(1.dp, cs.outline, CardShape)
            .padding(16.dp)
            .testTag("today_goals_card"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.home_todays_goals),
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant
            )
            Text(
                text = if (total > 0) "$done of $total" else "No slots today",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurface
            )
        }
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50)),
            color = fill,
            trackColor = cs.outline
        )
    }
}

@Composable
private fun EndOfDayRecapCard(recap: com.anubhav.diprep.ui.viewmodel.EndOfDayRecap) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(cs.surface)
            .border(1.dp, cs.outline, CardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Nightlight,
                contentDescription = null,
                tint = cs.primary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stringResource(R.string.home_todays_recap),
                style = MaterialTheme.typography.labelSmall,
                color = cs.primary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val hours = recap.minutesStudied / 60
            val mins = recap.minutesStudied % 60
            val studiedText = when {
                hours > 0 && mins > 0 -> "${hours}h ${mins}m"
                hours > 0 -> "${hours}h"
                else -> "${mins}m"
            }
            RecapStatBox(value = studiedText, label = "studied", modifier = Modifier.weight(1f))
            RecapStatBox(
                value = if (recap.avgScoreToday != null) "${recap.avgScoreToday}%" else "—",
                label = "avg score",
                modifier = Modifier.weight(1f)
            )
            RecapStatBox(
                value = "${recap.tasksDone}/${recap.tasksTotal}",
                label = "goals done",
                modifier = Modifier.weight(1f)
            )
        }
        if (recap.subjectsTested.isNotEmpty()) {
            Text(
                text = "Tested: ${recap.subjectsTested.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecapStatBox(value: String, label: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(cs.background)
            .border(1.dp, cs.outline, RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = cs.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant
        )
    }
}

@Composable
private fun SyllabusRingCard(overallMastery: Int) {
    val cs = MaterialTheme.colorScheme
    val animatedSweep by animateFloatAsState(
        targetValue = (overallMastery / 100f).coerceIn(0f, 1f) * 360f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "ring_sweep"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(cs.surface)
            .border(1.dp, cs.outline, CardShape)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.DonutLarge,
                contentDescription = null,
                tint = cs.primary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stringResource(R.string.home_overall_mastery),
                style = MaterialTheme.typography.labelSmall,
                color = cs.primary
            )
        }
        val trackColor = cs.outline
        val fillColor = cs.primary
        Box(modifier = Modifier.size(112.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(
                    color = trackColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
                if (animatedSweep > 0f) {
                    drawArc(
                        color = fillColor,
                        startAngle = -90f,
                        sweepAngle = animatedSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$overallMastery%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = cs.primary
                )
                Text(
                    text = "overall",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuoteCard(text: String, author: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(cs.surface)
            .border(1.dp, cs.outline, CardShape)
            .padding(16.dp)
            .testTag("daily_quote_card"),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.FormatQuote,
            contentDescription = null,
            tint = cs.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "\"$text\"",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = cs.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "— $author",
                style = MaterialTheme.typography.labelSmall,
                color = cs.primary,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
