package com.anubhav.diprep.ui.screens.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anubhav.diprep.ui.theme.OnSleekBackground
import com.anubhav.diprep.ui.theme.OnSleekPrimary
import com.anubhav.diprep.ui.theme.OnSleekPrimaryContainer
import com.anubhav.diprep.ui.theme.OnSleekSurfaceVariant
import com.anubhav.diprep.ui.theme.SleekBackground
import com.anubhav.diprep.ui.theme.SleekBorderLight
import com.anubhav.diprep.ui.theme.SleekPrimary
import com.anubhav.diprep.ui.theme.SleekPrimaryContainer
import com.anubhav.diprep.ui.theme.SleekSuccess
import com.anubhav.diprep.ui.theme.SleekSuccessBg
import com.anubhav.diprep.ui.theme.SleekSurfaceContainer
import com.anubhav.diprep.ui.theme.SleekSurfaceContainerLowest

@Composable
fun WelcomeScreen(
    onStartPrep: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Sleek DI Icon Badge
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SleekPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DI",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSleekPrimaryContainer
                )
            }

            Text(
                text = "Exam Prep",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = OnSleekBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Your dedicated, offline companion for the Drug Inspector Examination.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSleekSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Feature Highlights
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurfaceContainerLowest),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SleekBorderLight, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureRowItem(
                        icon = Icons.Default.OfflinePin,
                        title = "100% Offline & Private",
                        description = "All study metrics stored locally in Room Database."
                    )
                    FeatureRowItem(
                        icon = Icons.Default.ElectricBolt,
                        title = "Android 15 Battery Safe",
                        description = "Zero background services, no persistent battery drain."
                    )
                    FeatureRowItem(
                        icon = Icons.Default.LocalPharmacy,
                        title = "Comprehensive Syllabus",
                        description = "Full coverage of Jurisprudence, Pharmacology, & Analysis."
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Name Input Field
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Enter Your Name") },
                placeholder = { Text("e.g. Anubhav") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("name_input_field"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SleekPrimary,
                    unfocusedBorderColor = SleekBorderLight
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // CTA Button
        Button(
            onClick = {
                val candidateName = if (nameInput.isNotBlank()) nameInput.trim() else "Candidate"
                onStartPrep(candidateName)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("start_prep_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
        ) {
            Text(
                text = "Begin Exam Prep",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnSleekPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = OnSleekPrimary
            )
        }
    }
}

@Composable
private fun FeatureRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SleekPrimaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SleekPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = OnSleekBackground
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = OnSleekSurfaceVariant
            )
        }
    }
}
