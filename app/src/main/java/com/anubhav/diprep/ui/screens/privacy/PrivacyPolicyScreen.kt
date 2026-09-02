package com.anubhav.diprep.ui.screens.privacy

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Canonical online copy of this policy — the app itself never fetches this;
// it only opens in the user's browser when they explicitly tap "View online".
private const val PRIVACY_POLICY_URL =
    "https://claude.ai/code/artifact/b3cd2737-5f66-42ae-8aa5-e48b17a55822"

private const val PRIVACY_POLICY_TEXT = """Privacy Policy for Exam Prep

Last updated: 2 September 2026

Exam Prep is designed to work entirely offline. This policy explains exactly what data the app handles and how.

DATA WE COLLECT: None.
Exam Prep does not collect, transmit, or share any personal data with us or any third party. The app has no servers, no analytics, no advertising, and no internet permission requested.

DATA STORED ON YOUR DEVICE:
The app stores the following information locally on your phone only, never transmitted anywhere:
- Your name and exam preparation details (exam name, date, subjects)
- Test scores and study progress you log
- Your custom timetable and study schedule
- App preferences (theme, notification settings)

This data never leaves your device unless you choose to export it yourself. Uninstalling the app permanently deletes all this data.

NOTIFICATION ACCESS PERMISSION:
If you enable the optional Focus Mode feature, Exam Prep requests Android's Notification Access permission. This is used solely to temporarily suppress notifications from apps you select, during time periods you schedule. The app only checks which app sent a notification (not its content) to decide whether to suppress it. No notification content is ever read, stored, or transmitted. Phone calls and SMS/messaging apps cannot be muted under any circumstance. This permission can be revoked at any time in your phone's Settings.

CAMERA/PHOTOS:
Exam Prep does not currently request camera or photo access.

CONTACT US:
If you have questions about this privacy policy, contact: anubhavk42@gmail.com"""

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val cs = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = cs.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = PRIVACY_POLICY_TEXT,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onBackground
            )


            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
