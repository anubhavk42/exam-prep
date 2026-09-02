package com.anubhav.diprep.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anubhav.diprep.data.local.db.TimetableSlot
import com.anubhav.diprep.ui.theme.SuccessGreen
import com.anubhav.diprep.ui.viewmodel.TimetableViewModel
import com.anubhav.diprep.util.rememberSafeHaptic

// Subtle "completed" accent tints — design details layered on top of the
// Dark Premium palette that MaterialTheme.colorScheme drives everywhere else.
private val DoneCardTint = Color(0xFF1A241D)
private val WellnessIconBg = Color(0xFF251F1A)
private val WellnessIconDoneBg = Color(0xFF25382D)

@Composable
fun TimetableScreen(
    modifier: Modifier = Modifier,
    openAddSlotSignal: Boolean = false,
    onAddSlotConsumed: () -> Unit = {},
    viewModel: TimetableViewModel = viewModel()
) {
    val todaySlots by viewModel.todaySlots.collectAsStateWithLifecycle()
    val completedSlotIds by viewModel.completedSlotIds.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val todayLog by viewModel.todayLog.collectAsStateWithLifecycle()
    val whatsNext by viewModel.whatsNext.collectAsStateWithLifecycle()
    val isFocusSessionActive by viewModel.isFocusSessionActive.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val cs = MaterialTheme.colorScheme
    val haptic = rememberSafeHaptic()

    LaunchedEffect(openAddSlotSignal) {
        if (openAddSlotSignal) {
            showAddDialog = true
            onAddSlotConsumed()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(cs.background)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp)
        ) {
            // Header
            Text(
                text = "Today's schedule",
                style = MaterialTheme.typography.titleLarge,
                color = cs.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${todaySlots.size} slot${if (todaySlots.size != 1) "s" else ""} planned",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))

            // What's next banner
            whatsNext?.let { next ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cs.outline, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp)) {
                        Text(
                            text = "WHAT'S NEXT",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${next.label} · ${next.startTime}–${next.endTime}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Timetable slots for today
            if (todaySlots.isEmpty()) {
                Text(
                    text = "No time slots yet — add your first below",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                todaySlots.forEach { slot ->
                    val isDone = completedSlotIds.contains(slot.id)
                    SlotRow(
                        slot = slot,
                        isDone = isDone,
                        onToggle = { haptic.tap(); viewModel.toggleSlotCompletion(slot.id) },
                        onDelete = { haptic.tap(); viewModel.deleteSlot(slot.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Add slot button
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.surfaceContainerHighest, RoundedCornerShape(12.dp))
                    .clickable { haptic.tap(); showAddDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = cs.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add time slot",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant
                    )
                }
            }

            // Optional wellness habits — only shown if toggled on in Settings
            if (userProfile.exerciseReminderEnabled || userProfile.vitaminReminderEnabled) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "WELLNESS",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (userProfile.exerciseReminderEnabled) {
                    WellnessRow(
                        emoji = "🏃",
                        title = "Exercise",
                        description = "Any physical activity",
                        isDone = todayLog.exerciseDone,
                        onToggle = { haptic.tap(); viewModel.toggleTask("exercise") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (userProfile.vitaminReminderEnabled) {
                    WellnessRow(
                        emoji = "💊",
                        title = "Multivitamin",
                        description = "Optional daily reminder",
                        isDone = todayLog.vitaminDone,
                        onToggle = { haptic.tap(); viewModel.toggleTask("vitamin") }
                    )
                }
            }

            // Focus Session toggle — only shown when activation mode is MANUAL
            if (userProfile.notificationFilterEnabled && userProfile.filterActivationMode == "MANUAL") {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "FOCUS MODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFocusSessionActive) cs.primary.copy(alpha = 0.15f) else cs.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isFocusSessionActive) cs.primary else cs.outline,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { haptic.tap(); viewModel.toggleFocusSession() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isFocusSessionActive) "Focus Session Active" else "Start Focus Session",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isFocusSessionActive) cs.primary else cs.onSurface
                            )
                            Text(
                                text = if (isFocusSessionActive) "Distracting apps are muted" else "Tap to mute distracting apps",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isFocusSessionActive,
                            onCheckedChange = { haptic.tap(); viewModel.toggleFocusSession() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = cs.background,
                                checkedTrackColor = cs.primary,
                                uncheckedThumbColor = cs.onSurfaceVariant,
                                uncheckedTrackColor = cs.outline
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showAddDialog) {
        AddSlotDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { day, start, end, label, focusMode ->
                viewModel.addSlot(day, start, end, label, focusMode)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SlotRow(
    slot: TimetableSlot,
    isDone: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val cardBg by animateColorAsState(
        targetValue = if (isDone) DoneCardTint else cs.surface,
        animationSpec = tween(300), label = "slot_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isDone) SuccessGreen.copy(alpha = 0.4f) else cs.outline,
        animationSpec = tween(300), label = "slot_border"
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 12.dp)
                .clickable { onToggle() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isDone) SuccessGreen else Color.Transparent)
                    .border(
                        width = if (isDone) 0.dp else 1.5.dp,
                        color = cs.surfaceContainerHighest,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = cs.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slot.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDone) SuccessGreen else cs.onSurface
                )
                Text(
                    text = "${slot.startTime} – ${slot.endTime}",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete slot",
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun WellnessRow(
    emoji: String,
    title: String,
    description: String,
    isDone: Boolean,
    onToggle: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val cardBg by animateColorAsState(
        targetValue = if (isDone) DoneCardTint else cs.surface,
        animationSpec = tween(300), label = "wellness_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isDone) SuccessGreen.copy(alpha = 0.4f) else cs.outline,
        animationSpec = tween(300), label = "wellness_border"
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (isDone) WellnessIconDoneBg else WellnessIconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 14.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDone) SuccessGreen else cs.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (isDone) SuccessGreen else Color.Transparent)
                    .border(
                        width = if (isDone) 0.dp else 1.5.dp,
                        color = cs.surfaceContainerHighest,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = cs.onPrimary,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddSlotDialog(
    onDismiss: () -> Unit,
    onConfirm: (dayOfWeek: Int, startTime: String, endTime: String, label: String, focusModeEnabled: Boolean) -> Unit
) {
    var selectedDay by remember { mutableIntStateOf(1) }
    var startHour by remember { mutableStateOf("6") }
    var startMinute by remember { mutableStateOf("00") }
    var startIsAm by remember { mutableStateOf(true) }
    var endHour by remember { mutableStateOf("8") }
    var endMinute by remember { mutableStateOf("00") }
    var endIsAm by remember { mutableStateOf(true) }
    var label by remember { mutableStateOf("") }
    var focusModeEnabled by remember { mutableStateOf(false) }

    // Single-letter labels: M T W T F S S  (Mon=1 … Sun=7)
    val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")
    val cs = MaterialTheme.colorScheme
    val haptic = rememberSafeHaptic()

    fun to24hr(hourStr: String, minStr: String, isAm: Boolean): String {
        val h12 = hourStr.toIntOrNull()?.coerceIn(1, 12) ?: 12
        val h24 = when {
            isAm && h12 == 12 -> 0
            !isAm && h12 != 12 -> h12 + 12
            else -> h12
        }
        val m = minStr.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return "${h24.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cs.surface,
        title = {
            Text("Add time slot", color = cs.onSurface, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                // ── DAY OF WEEK ──────────────────────────────────────────────
                Text(
                    "DAY OF WEEK",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    dayLetters.forEachIndexed { index, letter ->
                        val dayValue = index + 1
                        val selected = selectedDay == dayValue
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) cs.primary else Color.Transparent)
                                .border(1.dp, if (selected) cs.primary else cs.outline, RoundedCornerShape(8.dp))
                                .clickable { haptic.tap(); selectedDay = dayValue }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = letter,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) cs.onPrimary else cs.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── START TIME ───────────────────────────────────────────────
                Text(
                    "START TIME",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                TimeInputRow(
                    hour = startHour, onHourChange = { startHour = it },
                    minute = startMinute, onMinuteChange = { startMinute = it },
                    isAm = startIsAm, onAmPmToggle = { startIsAm = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ── END TIME ─────────────────────────────────────────────────
                Text(
                    "END TIME",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                TimeInputRow(
                    hour = endHour, onHourChange = { endHour = it },
                    minute = endMinute, onMinuteChange = { endMinute = it },
                    isAm = endIsAm, onAmPmToggle = { endIsAm = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ── LABEL ────────────────────────────────────────────────────
                Text(
                    "LABEL",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    placeholder = { Text("e.g. Pharmacology, Mock test", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ── FOCUS MODE ───────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Focus Mode during this slot",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = cs.onSurface
                        )
                        Text(
                            "Muted apps stay silenced automatically",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = focusModeEnabled,
                        onCheckedChange = { focusModeEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = cs.onPrimary,
                            checkedTrackColor = cs.primary,
                            uncheckedThumbColor = cs.onSurfaceVariant,
                            uncheckedTrackColor = cs.surfaceVariant
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    haptic.tap()
                    if (label.isNotBlank()) {
                        onConfirm(
                            selectedDay,
                            to24hr(startHour, startMinute, startIsAm),
                            to24hr(endHour, endMinute, endIsAm),
                            label.trim(),
                            focusModeEnabled
                        )
                    }
                }
            ) {
                Text("Add", color = cs.primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = { haptic.tap(); onDismiss() }) {
                Text("Cancel", color = cs.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun TimeInputRow(
    hour: String, onHourChange: (String) -> Unit,
    minute: String, onMinuteChange: (String) -> Unit,
    isAm: Boolean, onAmPmToggle: (Boolean) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberSafeHaptic()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = hour,
            onValueChange = { v ->
                val d = v.filter { it.isDigit() }
                if (d.length <= 2) onHourChange(d)
            },
            label = { Text("HH", fontSize = 10.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(62.dp),
            singleLine = true
        )
        Text(":", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = cs.onSurface)
        OutlinedTextField(
            value = minute,
            onValueChange = { v ->
                val d = v.filter { it.isDigit() }
                if (d.length <= 2) onMinuteChange(d)
            },
            label = { Text("MM", fontSize = 10.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(62.dp),
            singleLine = true
        )
        Spacer(modifier = Modifier.width(6.dp))
        // Stacked AM / PM selector
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(true to "AM", false to "PM").forEach { (amOption, lbl) ->
                val sel = isAm == amOption
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (sel) cs.primary else Color.Transparent)
                        .border(1.dp, if (sel) cs.primary else cs.outline, RoundedCornerShape(6.dp))
                        .clickable { haptic.tap(); onAmPmToggle(amOption) }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lbl,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (sel) cs.onPrimary else cs.onSurfaceVariant
                    )
                }
            }
        }
    }
}
