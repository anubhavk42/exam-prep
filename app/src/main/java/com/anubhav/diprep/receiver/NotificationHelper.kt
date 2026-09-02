package com.anubhav.diprep.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object NotificationHelper {

    const val ID_MULTIVITAMIN = 2001

    /**
     * Schedules a one-time exact alarm for today at 1:30 PM (13:30).
     * Fires once, shows the notification, and stops.
     * Never repeats on its own in background.
     */
    fun scheduleMultivitaminReminder(
        context: Context,
        userName: String = "Anubhav",
        targetHour: Int = 13,
        targetMinute: Int = 30
    ): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
            putExtra(ReminderReceiver.EXTRA_TITLE, "💊 Multivitamin time!")
            putExtra(
                ReminderReceiver.EXTRA_MESSAGE,
                "Don't forget your afternoon multivitamin, $userName."
            )
            putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, ID_MULTIVITAMIN)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ID_MULTIVITAMIN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If target time today has already passed, schedule for today + tomorrow or trigger if within window
        var triggerAtMillis = calendar.timeInMillis
        if (triggerAtMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            triggerAtMillis = calendar.timeInMillis
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    true
                } else {
                    // Fallback to inexact idle alarm if exact permission not granted
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    false
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                true
            }
        } catch (e: SecurityException) {
            // Never crash if permission denied - degrade gracefully
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } catch (ignored: Exception) { }
            false
        }
    }

    fun cancelReminder(context: Context, notificationId: Int = ID_MULTIVITAMIN) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        scheduleMultivitaminReminder(context = context, targetHour = hour, targetMinute = minute)
    }

    fun cancelDailyReminder(context: Context) {
        cancelReminder(context, ID_MULTIVITAMIN)
    }
}
