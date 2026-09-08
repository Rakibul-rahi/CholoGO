package com.example.chologo.ui.theme

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

private const val PREFS_NAME = "cholo_go_theme_prefs"
private const val KEY_DARK_THEME = "dark_theme_enabled"

// Every hand-built screen reads its colors from here rather than the system
// theme, since the whole app started dark-only (see CholoGOTheme). This is
// the single source of truth for the user's chosen mode, persisted across
// launches with plain SharedPreferences (no need for DataStore for one bool).
object ThemeController {
    var isDarkTheme by mutableStateOf(true)
        private set

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isDarkTheme = prefs.getBoolean(KEY_DARK_THEME, true)
    }

    fun setDarkTheme(context: Context, value: Boolean) {
        isDarkTheme = value
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_THEME, value)
            .apply()
    }

    fun toggle(context: Context) {
        setDarkTheme(context, !isDarkTheme)
    }
}

// Every screen's private color tokens read this to pick their light/dark
// variant, instead of each screen threading a `darkTheme: Boolean` parameter
// through every composable.
val LocalIsDarkTheme = compositionLocalOf { true }
