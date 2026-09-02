package com.anubhav.diprep.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.anubhav.diprep.MainActivity
import com.anubhav.diprep.R
import com.anubhav.diprep.data.datastore.PreferencesManager
import com.anubhav.diprep.data.local.db.AppDatabase
import com.anubhav.diprep.util.DateUtils
import com.anubhav.diprep.util.StreakCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Home-screen widget: days-to-exam + current streak.
 *
 * ARCHITECTURE NOTE — battery safety:
 * This widget has NO recurring update mechanism. `updatePeriodMillis` is 0 in
 * countdown_widget_info.xml and there is no WorkManager / AlarmManager tied to it.
 * It refreshes only on three foreground-driven events:
 *   1. Widget added / resized      → AppWidgetProvider.onUpdate
 *   2. App opened                  → MainActivity.onResume → requestRefresh()
 *   3. The existing daily reminder → ReminderReceiver → requestRefresh()
 * All three are moments the process is already awake for another reason.
 */
object CountdownWidget {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Recompute + redraw every placed instance. Safe to call from any thread. */
    fun updateAll(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val ids = manager.getAppWidgetIds(
            ComponentName(appContext, CountdownWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return

        scope.launch {
            val prefs = PreferencesManager(appContext)
            val profile = prefs.userProfileFlow.first()
            val dao = AppDatabase.getDatabase(appContext).appDao()

            val days = DateUtils.daysUntil(profile.examDate)
            val streak = if (profile.onboardingDone) {
                StreakCalculator.calculate(
                    logs = dao.getAllTaskLogsDesc().first(),
                    slots = dao.getAllSlots().first(),
                    completions = dao.getCompletionsSince("2020-01-01").first(),
                    profile = profile
                )
            } else 0

            val views = RemoteViews(appContext.packageName, R.layout.widget_countdown).apply {
                setTextViewText(R.id.widget_days, days.toString())
                setTextViewText(
                    R.id.widget_streak,
                    if (streak > 0) "🔥 $streak day streak" else "Start your streak"
                )
                setOnClickPendingIntent(R.id.widget_root, openAppIntent(appContext))
            }
            ids.forEach { manager.updateAppWidget(it, views) }
        }
    }

    /** Alias used by foreground callers (MainActivity, ReminderReceiver). */
    fun requestRefresh(context: Context) = updateAll(context)

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
