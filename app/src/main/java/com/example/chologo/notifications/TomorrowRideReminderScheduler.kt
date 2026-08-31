package com.example.chologo.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Schedules (or cancels) the on-device "is this ride still happening?"
 * reminder for one Tomorrow Ride leg - one call per accepted request
 * (passenger side) or matched ride (rider side).
 *
 * Rescheduling with the same [uniqueKey] is safe and cheap: WorkManager's
 * REPLACE policy just swaps the pending work, so callers can call this
 * on every snapshot update without worrying about duplicate notifications.
 */
object TomorrowRideReminderScheduler {
    private const val REMINDER_LEAD_MINUTES = 60

    fun scheduleReminder(
        context: Context,
        uniqueKey: String,
        rideDate: String,
        timeMinutes: Int,
        title: String,
        message: String
    ) {
        val targetMillis = reminderFireTimeMillis(rideDate, timeMinutes)

        if (targetMillis == null || targetMillis <= System.currentTimeMillis()) {
            // Already past the 1-hour-before point (or an unparseable
            // date) - nothing meaningful left to schedule.
            cancelReminder(context, uniqueKey)
            return
        }

        val delayMillis = targetMillis - System.currentTimeMillis()

        val request = OneTimeWorkRequestBuilder<TomorrowRideReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    TomorrowRideReminderWorker.KEY_TITLE to title,
                    TomorrowRideReminderWorker.KEY_MESSAGE to message,
                    TomorrowRideReminderWorker.KEY_UNIQUE_KEY to uniqueKey
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(uniqueKey),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelReminder(context: Context, uniqueKey: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(uniqueKey))
    }

    private fun workName(uniqueKey: String) = "tomorrow_ride_reminder_$uniqueKey"

    /** [rideDate] is "yyyy-MM-dd", [timeMinutes] is minutes since midnight - both device-local. */
    private fun reminderFireTimeMillis(rideDate: String, timeMinutes: Int): Long? {
        val parts = rideDate.split("-")
        if (parts.size != 3) return null

        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null

        return try {
            Calendar.getInstance().apply {
                set(year, month - 1, day, timeMinutes / 60, timeMinutes % 60, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MINUTE, -REMINDER_LEAD_MINUTES)
            }.timeInMillis
        } catch (e: Exception) {
            null
        }
    }
}
