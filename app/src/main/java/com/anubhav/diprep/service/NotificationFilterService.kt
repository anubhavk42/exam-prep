package com.anubhav.diprep.service

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.anubhav.diprep.data.datastore.PreferencesManager
import com.anubhav.diprep.data.local.db.AppDatabase
import com.anubhav.diprep.data.local.db.TimetableSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Deliberate architectural exception to the zero-background-service rule.
 * See CLAUDE.md "Custom Notification Filter (Focus Mode)" for rationale.
 * This is the ONLY background service in the app and must remain isolated.
 */
class NotificationFilterService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    // Cached from DataStore — updated reactively
    @Volatile private var cachedFilterEnabled = false
    @Volatile private var cachedMutedPackages = emptySet<String>()
    @Volatile private var cachedActivationMode = "TIMETABLE"
    @Volatile private var cachedFocusSessionActive = false

    // Cached from Room — today's slots for the timetable check
    @Volatile private var cachedTodaySlots = emptyList<TimetableSlot>()

    // Built once on connect — packages that must never be suppressed
    private var protectedPackages = emptySet<String>()

    override fun onListenerConnected() {
        protectedPackages = buildProtectedPackages()

        val prefs = PreferencesManager(applicationContext)
        val db = AppDatabase.getDatabase(applicationContext)
        val dayOfWeek = LocalDate.now().dayOfWeek.value

        scope.launch {
            prefs.userProfileFlow.collectLatest { profile ->
                cachedFilterEnabled = profile.notificationFilterEnabled
                cachedMutedPackages = profile.mutedAppPackages.toSet()
                cachedActivationMode = profile.filterActivationMode
                cachedFocusSessionActive = profile.isFocusSessionActive
            }
        }

        scope.launch {
            db.appDao().getSlotsForDay(dayOfWeek).collectLatest { slots ->
                cachedTodaySlots = slots
            }
        }
    }

    override fun onListenerDisconnected() {
        scope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!cachedFilterEnabled) return

        val pkg = sbn.packageName ?: return

        // HARD EXCLUSION: phone, dialer, SMS, MMS — never suppressed under any circumstances
        if (pkg in protectedPackages) return

        // Only act on packages the student deliberately selected
        if (pkg !in cachedMutedPackages) return

        val active = when (cachedActivationMode) {
            "MANUAL"    -> cachedFocusSessionActive
            "TIMETABLE" -> isAnySlotActive()
            else        -> false
        }

        if (active) cancelNotification(sbn.key)
    }

    private fun isAnySlotActive(): Boolean {
        val now = LocalTime.now()
        return cachedTodaySlots.any { slot ->
            try {
                val start = LocalTime.parse(slot.startTime, timeFmt)
                val end   = LocalTime.parse(slot.endTime,   timeFmt)
                !now.isBefore(start) && now.isBefore(end)
            } catch (_: Exception) { false }
        }
    }

    private fun buildProtectedPackages(): Set<String> {
        val pm = packageManager
        val protected = mutableSetOf<String>()

        fun resolvePackage(intent: Intent): String? =
            pm.resolveActivity(intent, 0)?.activityInfo?.packageName

        // Phone / dialer
        resolvePackage(Intent(Intent.ACTION_DIAL))?.let { protected.add(it) }
        resolvePackage(Intent(Intent.ACTION_CALL, Uri.parse("tel:12345")))?.let { protected.add(it) }

        // SMS / MMS
        resolvePackage(Intent(Intent.ACTION_VIEW, Uri.parse("sms:")))?.let { protected.add(it) }
        resolvePackage(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")))?.let { protected.add(it) }

        // Android's default SMS app (API 19+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Telephony.Sms.getDefaultSmsPackage(applicationContext)?.let { protected.add(it) }
        }

        // Own package — never filter ourselves
        protected.add(packageName)

        return protected
    }
}
