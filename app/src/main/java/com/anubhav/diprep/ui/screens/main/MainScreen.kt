package com.anubhav.diprep.ui.screens.main

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChecklistRtl
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anubhav.diprep.ui.navigation.MainTab
import com.anubhav.diprep.ui.screens.StatsScreen
import com.anubhav.diprep.ui.screens.TimetableScreen
import com.anubhav.diprep.ui.screens.WeeklyScreen
import com.anubhav.diprep.ui.screens.home.HomeScreen
import com.anubhav.diprep.ui.screens.subjects.SubjectsScreen
import com.anubhav.diprep.ui.theme.Gold
import com.anubhav.diprep.ui.theme.NavUnselected
import com.anubhav.diprep.util.rememberSafeHaptic
import com.anubhav.diprep.ui.viewmodel.HomeViewModel
import com.anubhav.diprep.ui.viewmodel.MoodCheckInViewModel
import com.anubhav.diprep.ui.viewmodel.PrepViewModel

@Composable
fun MainScreen(
    viewModel: PrepViewModel,
    onNavigateToLogScore: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSubjectDetail: (String) -> Unit = {},
    onExitDemo: () -> Unit = {},
    homeViewModel: HomeViewModel = viewModel(),
    moodViewModel: MoodCheckInViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showMoodDialog by moodViewModel.showDialog.collectAsStateWithLifecycle()
    val pendingAddSlot by viewModel.pendingAddSlot.collectAsStateWithLifecycle()

    if (showMoodDialog) {
        MoodCheckInDialog(
            onMoodSelected = { moodViewModel.onMoodSelected(it) },
            onSkip = { moodViewModel.onSkip() }
        )
    }

    Scaffold(
        bottomBar = {
            SleekNavigationBar(
                selectedTab = uiState.selectedTab,
                onTabSelected = { viewModel.onTabSelected(it) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                MainTab.HOME -> {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToLogScore = onNavigateToLogScore,
                        onNavigateToSettings = onNavigateToProfile,
                        onExitDemo = onExitDemo
                    )
                }
                MainTab.SUBJECTS -> {
                    SubjectsScreen(
                        uiState = uiState,
                        onFilterChanged = { viewModel.setSubjectFilter(it) },
                        onNavigateToProfile = onNavigateToProfile,
                        onNavigateToSubjectDetail = onNavigateToSubjectDetail
                    )
                }
                MainTab.GOALS -> {
                    TimetableScreen(
                        openAddSlotSignal = pendingAddSlot,
                        onAddSlotConsumed = { viewModel.consumeAddSlot() }
                    )
                }
                MainTab.STATS -> {
                    StatsScreen(
                        onNavigateToSettings = onNavigateToProfile
                    )
                }
            }
        }
    }
}

private data class MoodOption(val emoji: String, val label: String, val key: String)

private val MOOD_OPTIONS = listOf(
    MoodOption("😄", "Excited", "EXCITED"),
    MoodOption("😐", "Neutral", "NEUTRAL"),
    MoodOption("😔", "Low", "LOW")
)

@Composable
private fun MoodCheckInDialog(
    onMoodSelected: (String) -> Unit,
    onSkip: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberSafeHaptic()
    AlertDialog(
        onDismissRequest = onSkip,
        containerColor = cs.surface,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "How are you feeling today?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MOOD_OPTIONS.forEach { option ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Gold.copy(alpha = 0.08f))
                            .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .clickable { haptic.tap(); onMoodSelected(option.key) }
                            .padding(vertical = 14.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = option.emoji, fontSize = 28.sp)
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { haptic.tap(); onSkip() }) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.labelLarge,
                    color = cs.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun SleekNavigationBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    val haptic = rememberSafeHaptic()
    val tabs = listOf(
        Pair(MainTab.HOME, Icons.Default.Home),
        Pair(MainTab.SUBJECTS, Icons.Default.MenuBook),
        Pair(MainTab.GOALS, Icons.Default.ChecklistRtl),
        Pair(MainTab.STATS, Icons.Default.BarChart)
    )

    val cs = MaterialTheme.colorScheme
    NavigationBar(
        containerColor = cs.background,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = cs.outline)
    ) {
        tabs.forEach { (tab, tabIcon) ->
            val isSelected = selectedTab == tab

            NavigationBarItem(
                selected = isSelected,
                onClick = { haptic.tap(); onTabSelected(tab) },
                icon = {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(cs.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tabIcon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(20.dp),
                                tint = cs.primary
                            )
                        }
                    } else {
                        Icon(
                            imageVector = tabIcon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(22.dp),
                            tint = NavUnselected
                        )
                    }
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = cs.primary,
                    selectedTextColor = cs.primary,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = NavUnselected,
                    unselectedTextColor = NavUnselected
                ),
                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
            )
        }
    }
}
