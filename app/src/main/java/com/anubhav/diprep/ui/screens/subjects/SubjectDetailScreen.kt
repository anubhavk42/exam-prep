package com.anubhav.diprep.ui.screens.subjects

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anubhav.diprep.ui.theme.DangerCoral
import com.anubhav.diprep.ui.theme.Gold
import com.anubhav.diprep.ui.theme.SuccessGreen
import com.anubhav.diprep.ui.viewmodel.SubjectDetailViewModel
import com.anubhav.diprep.ui.viewmodel.TopicWithStats
import com.anubhav.diprep.util.rememberSafeHaptic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subjectName: String,
    onBack: () -> Unit,
    onNavigateToLogScore: (subject: String, topicId: Long) -> Unit,
    viewModel: SubjectDetailViewModel = viewModel()
) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberSafeHaptic()
    val topicsWithStats by viewModel.getTopicsWithStats(subjectName)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val overallPct: Int? = topicsWithStats
        .mapNotNull { it.avgPercent }
        .takeIf { it.isNotEmpty() }
        ?.average()?.toInt()

    var showAddDialog by remember { mutableStateOf(false) }
    var newTopicName by remember { mutableStateOf("") }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; newTopicName = "" },
            containerColor = cs.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Add Topic",
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface
                )
            },
            text = {
                OutlinedTextField(
                    value = newTopicName,
                    onValueChange = { newTopicName = it },
                    placeholder = { Text("e.g. Pharmacokinetics", color = cs.onSurfaceVariant) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = cs.onSurface,
                        unfocusedTextColor = cs.onSurface,
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
                        viewModel.addTopic(subjectName, newTopicName)
                        newTopicName = ""
                        showAddDialog = false
                    },
                    enabled = newTopicName.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add", color = cs.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; newTopicName = "" }) {
                    Text("Cancel", color = cs.onSurfaceVariant)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = subjectName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = cs.onBackground
                        )
                        Text(
                            text = if (overallPct != null) "Overall: $overallPct%" else "No scores yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { haptic.tap(); onBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = cs.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { haptic.tap(); showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add topic",
                            tint = cs.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)
            )
        },
        containerColor = cs.background
    ) { paddingValues ->
        if (topicsWithStats.isEmpty()) {
            EmptyTopicsState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                onAddTopic = { haptic.tap(); showAddDialog = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = 16.dp,
                    bottom = 32.dp
                )
            ) {
                items(topicsWithStats, key = { it.topic.id }) { item ->
                    TopicRow(
                        item = item,
                        onClick = {
                            haptic.tap()
                            onNavigateToLogScore(subjectName, item.topic.id)
                        },
                        onDelete = { haptic.tap(); viewModel.deleteTopic(item.topic.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable { haptic.tap(); showAddDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add topic",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Gold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicRow(
    item: TopicWithStats,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val pct = item.avgPercent
    val accent = when {
        pct == null -> cs.onSurfaceVariant
        pct >= 75 -> SuccessGreen
        pct >= 50 -> Gold
        else -> DangerCoral
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cs.surface)
            .border(1.dp, cs.outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 4.dp.toPx()
                val r = (size.minDimension / 2f) - stroke / 2f
                val c = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = accent.copy(alpha = 0.2f),
                    radius = r,
                    center = c,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                val sweep = ((pct ?: 0) / 100f) * 360f
                if (sweep > 0f) {
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(c.x - r, c.y - r),
                        size = Size(r * 2f, r * 2f),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Text(
                text = if (pct != null) "$pct%" else "—",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurface,
                fontWeight = FontWeight.Bold
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = item.topic.topicName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface
            )
            Text(
                text = "${item.testCount} test${if (item.testCount != 1) "s" else ""} logged · Tap to log score",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
            DebtBadge(daysSince = item.daysSinceLastScore)
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete topic",
                tint = DangerCoral.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = cs.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun DebtBadge(daysSince: Long?) {
    val cs = MaterialTheme.colorScheme
    val (label, color) = when {
        daysSince == null -> "Not started" to cs.onSurfaceVariant
        daysSince == 0L -> "Fresh" to SuccessGreen
        daysSince < 7 -> "${daysSince}d ago" to SuccessGreen
        daysSince < 15 -> "${daysSince}d ago" to Gold
        else -> "${daysSince}d ago" to DangerCoral
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyTopicsState(
    modifier: Modifier = Modifier,
    onAddTopic: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No topics yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = cs.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add topics to start tracking your performance per chapter or concept.",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddTopic,
            colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = cs.onPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Add first topic",
                color = cs.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
