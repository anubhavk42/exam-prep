package com.anubhav.diprep.ui.screens.subjects

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anubhav.diprep.data.model.Subject
import com.anubhav.diprep.data.model.SubjectStatus
import com.anubhav.diprep.ui.theme.DangerCoral
import com.anubhav.diprep.ui.theme.Gold
import com.anubhav.diprep.ui.theme.SuccessGreen
import com.anubhav.diprep.ui.viewmodel.PrepUiState
import com.anubhav.diprep.util.rememberSafeHaptic

private val FILTERS = listOf(
    "All Subjects" to "All",
    "Strong (>80%)" to "Strong",
    "In Progress" to "In progress",
    "Needs Work (<50%)" to "Needs work"
)

private val CardShape = RoundedCornerShape(12.dp)

@Composable
fun SubjectsScreen(
    uiState: PrepUiState,
    onFilterChanged: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSubjectDetail: (String) -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberSafeHaptic()

    val filteredSubjects = uiState.subjects.filter { subject ->
        when (uiState.activeSubjectFilter) {
            "Strong (>80%)" -> subject.completionPercent >= 80
            "In Progress" -> subject.completionPercent in 50..79
            "Needs Work (<50%)" -> subject.completionPercent < 50
            else -> true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .padding(top = 14.dp)
    ) {
        // Header: title + settings icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your subjects",
                    style = MaterialTheme.typography.titleLarge,
                    color = cs.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Track your mastery across every module.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }
            IconButton(onClick = { haptic.tap(); onNavigateToProfile() }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = cs.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FILTERS, key = { it.first }) { (key, label) ->
                val isSelected = uiState.activeSubjectFilter == key
                FilterPill(
                    text = label,
                    selected = isSelected,
                    onClick = { haptic.tap(); onFilterChanged(key) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2-column subject grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredSubjects, key = { it.id }) { subject ->
                SubjectGridCell(
                    subject = subject,
                    onClick = { haptic.tap(); onNavigateToSubjectDetail(subject.name) }
                )
            }
        }
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) cs.primary else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) cs.primary else cs.surfaceContainerHighest,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) cs.onPrimary else cs.onSurfaceVariant
        )
    }
}

private fun statusAccent(status: SubjectStatus): Color = when (status) {
    SubjectStatus.MASTERED -> SuccessGreen
    SubjectStatus.IN_PROGRESS -> Gold
    SubjectStatus.NEEDS_WORK -> DangerCoral
}

@Composable
private fun SubjectGridCell(subject: Subject, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val accent = if (subject.hasTopics) statusAccent(subject.status) else cs.onSurfaceVariant
    val pct = subject.completionPercent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(cs.surface)
            .border(1.dp, cs.outline, CardShape)
            .clickable(onClick = onClick)
            .padding(10.dp)
            .testTag("subject_card_${subject.id}")
    ) {
        // Circular progress ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(64.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 5.dp.toPx()
                val radius = (size.minDimension / 2f) - strokeWidth / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                val arcTopLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2f, radius * 2f)
                val sweepAngle = (pct / 100f) * 360f

                drawCircle(
                    color = cs.outline,
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                if (subject.hasTopics && sweepAngle > 0f) {
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
            Text(
                text = if (subject.hasTopics) "$pct%" else "+",
                style = MaterialTheme.typography.labelSmall,
                color = if (subject.hasTopics) cs.onSurface else cs.onSurfaceVariant
            )
        }

        // Subject name
        Text(
            text = subject.name,
            style = MaterialTheme.typography.labelLarge,
            color = cs.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Status badge or empty-state prompt
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(accent.copy(alpha = 0.14f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = if (subject.hasTopics) subject.status.label else "Add topics",
                style = MaterialTheme.typography.labelMedium,
                color = accent
            )
        }
    }
}
