package com.example.chologo.utils

import java.util.Calendar

/**
 * The time-of-day greeting shown above a user's name on the dashboards.
 *
 * One place, because the same three-line `when` was pasted into
 * PassengerHeroCard, RiderHeroCard and LevelCard - and all three carried
 * the same bug: `hour < 12 -> "Good Morning"` starts at midnight, so
 * opening the app at 1 AM was greeted with "GOOD MORNING". Small, but it
 * is the first line on the screen and it reads as broken.
 *
 * The small hours now get no time-of-day claim at all. There is no
 * graceful thing to call 2 AM - "good night" is a farewell, "good
 * morning" is a lie - so the app just says hello and moves on.
 */
object Greeting {

    /** 05:00 until 12:00. */
    private const val MORNING_START = 5

    /** 12:00 until 17:00. Afternoon ends earlier than the old 18:00 cut, so
     *  5 PM - which is dark for much of the year in Dhaka - reads as
     *  evening rather than afternoon. */
    private const val AFTERNOON_START = 12

    /** 17:00 until midnight. */
    private const val EVENING_START = 17

    /**
     * Greeting for [hour] (0-23), defaulting to the device's current hour.
     *
     * Returned in sentence case; the hero cards uppercase it themselves.
     */
    fun forHour(hour: Int = currentHour()): String {
        return when {
            hour < MORNING_START -> "Hello"
            hour < AFTERNOON_START -> "Good morning"
            hour < EVENING_START -> "Good afternoon"
            else -> "Good evening"
        }
    }

    private fun currentHour(): Int {
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    }
}
