package com.example.chologo.notifications

import android.content.Context

/**
 * Tracks which one-off local notifications have already been shown, keyed
 * by an arbitrary event key (e.g. "accepted_<requestId>"). Backed by
 * SharedPreferences rather than in-memory Compose state, so reopening the
 * app after the process was killed doesn't re-fire a notification for a
 * condition that's still true (e.g. a request that's still "accepted") -
 * plain `remember` state resets on every process restart and would
 * otherwise re-notify on every app launch.
 */
object NotifiedEventsStore {
    private const val PREFS_NAME = "notified_events"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasNotified(context: Context, key: String): Boolean {
        return prefs(context).getBoolean(key, false)
    }

    fun markNotified(context: Context, key: String) {
        prefs(context).edit().putBoolean(key, true).apply()
    }
}
