package com.example.chologo.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.example.chologo.MainActivity
import com.example.chologo.R

/**
 * Fires once, exactly one hour before a confirmed Tomorrow Ride, and shows
 * a local notification asking whether the ride is still happening.
 * Runs entirely on-device (no server round trip) - see
 * [TomorrowRideReminderScheduler] for how the fire time is computed.
 */
class TomorrowRideReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "Tomorrow Ride reminder"
        val message = inputData.getString(KEY_MESSAGE)
            ?: "Your Tomorrow Ride is in 1 hour. Still happening?"
        val uniqueKey = inputData.getString(KEY_UNIQUE_KEY) ?: return Result.success()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Nothing we can do without the permission - don't retry
            // forever, this attempt is simply skipped.
            return Result.success()
        }

        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            uniqueKey.hashCode(),
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, ReminderNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.chologologo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(uniqueKey.hashCode(), notification)

        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"
        const val KEY_UNIQUE_KEY = "unique_key"
    }
}
