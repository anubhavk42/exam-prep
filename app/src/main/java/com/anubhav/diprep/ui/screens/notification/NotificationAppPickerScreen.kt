package com.anubhav.diprep.ui.screens.notification

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anubhav.diprep.ui.viewmodel.SettingsViewModel

data class AppInfo(
    val packageName: String,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationAppPickerScreen(
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val haptic = com.anubhav.diprep.util.rememberSafeHaptic()
    val userProfile by settingsViewModel.userProfile.collectAsStateWithLifecycle()

    var selectedPackages by remember(userProfile.mutedAppPackages) {
        mutableStateOf(userProfile.mutedAppPackages.toSet())
    }

    val apps = remember {
        buildFilteredAppList(context.packageManager, context.packageName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Select Apps to Mute",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = cs.onBackground
                        )
                        Text(
                            text = "Calls & messages are never affected",
                            fontSize = 14.sp,
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)
            )
        },
        containerColor = cs.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = "${selectedPackages.size} app${if (selectedPackages.size != 1) "s" else ""} selected",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = cs.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    val checked = app.packageName in selectedPackages
                    AppPickerRow(
                        app = app,
                        checked = checked,
                        onToggle = {
                            selectedPackages = if (checked) {
                                selectedPackages - app.packageName
                            } else {
                                selectedPackages + app.packageName
                            }
                            settingsViewModel.updateMutedAppPackages(selectedPackages.toList())
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppPickerRow(
    app: AppInfo,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val haptic = com.anubhav.diprep.util.rememberSafeHaptic()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cs.surface)
            .border(1.dp, cs.outline, RoundedCornerShape(12.dp))
            .clickable { haptic.tap(); onToggle() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = app.label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = cs.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = checked,
            onCheckedChange = { haptic.tap(); onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = cs.primary,
                uncheckedColor = cs.onSurfaceVariant,
                checkmarkColor = cs.onPrimary
            )
        )
    }
}

private fun buildFilteredAppList(pm: PackageManager, ownPackage: String): List<AppInfo> {
    val protectedPackages = buildProtectedPackages(pm, ownPackage)
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

    return pm.queryIntentActivities(launcherIntent, 0)
        .mapNotNull { ri ->
            val pkg = ri.activityInfo.packageName
            if (pkg in protectedPackages) return@mapNotNull null
            val appInfo = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull()
                ?: return@mapNotNull null
            if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) return@mapNotNull null
            val label = pm.getApplicationLabel(appInfo).toString()
            AppInfo(packageName = pkg, label = label)
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

private fun buildProtectedPackages(pm: PackageManager, ownPackage: String): Set<String> {
    val result = mutableSetOf(ownPackage)

    fun resolvePackage(intent: Intent): String? =
        pm.resolveActivity(intent, 0)?.activityInfo?.packageName

    resolvePackage(Intent(Intent.ACTION_DIAL))?.let { result.add(it) }
    resolvePackage(Intent(Intent.ACTION_CALL, Uri.parse("tel:12345")))?.let { result.add(it) }
    resolvePackage(Intent(Intent.ACTION_VIEW, Uri.parse("sms:")))?.let { result.add(it) }
    resolvePackage(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")))?.let { result.add(it) }

    result.addAll(listOf(
        "com.android.mms",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.oneplus.mms",
        "com.android.phone",
        "com.google.android.dialer",
        "com.samsung.android.incallui"
    ))

    return result
}
