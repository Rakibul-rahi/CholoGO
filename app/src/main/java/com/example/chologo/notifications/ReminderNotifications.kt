package com.example.chologo.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.chologo.MainActivity
import com.example.chologo.R

/**
 * One shared channel for all Tomorrow Ride reminders. Safe to call
 * repeatedly - creating a channel with the same id is a no-op on Android,
 * so this can be called on every app start without checking first.
 */
object ReminderNotifications {
    const val CHANNEL_ID = "tomorrow_ride_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Tomorrow Ride reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminds you an hour before a confirmed Tomorrow Ride so you can confirm it's still happening."
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    /**
     * Shows a local notification immediately (unlike
     * [TomorrowRideReminderWorker], which fires on a schedule) - used e.g.
     * when a rider cancels an already-accepted Tomorrow Ride and the
     * passenger needs to know right away, even if the app is backgrounded.
     */
    fun showNow(
        context: Context,
        uniqueKey: String,
        title: String,
        message: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            uniqueKey.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.chologologo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(uniqueKey.hashCode(), notification)
    }
}
