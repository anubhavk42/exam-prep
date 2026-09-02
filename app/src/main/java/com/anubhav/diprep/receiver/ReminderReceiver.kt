package com.anubhav.diprep.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.anubhav.diprep.MainActivity

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REMINDER = "com.anubhav.diprep.action.REMINDER"
        const val EXTRA_TITLE = "extra_reminder_title"
        const val EXTRA_MESSAGE = "extra_reminder_message"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val CHANNEL_ID = "exam_prep_reminders"
        const val CHANNEL_NAME = "Daily Reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Only handle ACTION_REMINDER — no other intents
        if (intent.action != ACTION_REMINDER) return

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "💊 Multivitamin time!"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Don't forget your afternoon multivitamin!"
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 2001)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel with IMPORTANCE_DEFAULT (battery-friendly, no vibration loop)
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Battery-efficient daily reminders for Drug Inspector preparation"
            enableVibration(false)
            enableLights(false)
        }
        notificationManager.createNotificationChannel(channel)

        // Single tap intent to open app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        notificationManager.notify(notificationId, notification)

        // Piggyback the once-a-day alarm to refresh the home-screen widget.
        // No new scheduling is introduced by this call.
        com.anubhav.diprep.widget.CountdownWidget.requestRefresh(context)
    }
}
