package com.example.chologo.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// CholoGO's actual brand palette (matches the dark obsidian / electric-lime
// look every hand-built screen already uses) - NOT the Android Studio
// template purple this file started as. Any component that reads
// MaterialTheme.colorScheme (AlertDialog, default ripples, etc.) picks
// these up instead of an arbitrary fallback.
private val BrandLime = Color(0xFFC6F135)
private val BrandLimeDeep = Color(0xFF6F8F1A)
private val BrandBlue = Color(0xFF60A5FA)
private val BrandBg = Color(0xFF0A0D0F)
private val BrandSurface = Color(0xFF161B20)
private val BrandSurfaceVariant = Color(0xFF1C2228)
private val BrandTextHigh = Color(0xFFF1F5F9)
private val BrandTextMed = Color(0xFF8B96A5)
private val BrandOutline = Color(0xFF2A3548)
private val BrandError = Color(0xFFFF4D6A)

private val DarkColorScheme = darkColorScheme(
    primary = BrandLime,
    onPrimary = BrandBg,
    secondary = BrandBlue,
    onSecondary = BrandBg,
    tertiary = BrandLime,
    onTertiary = BrandBg,
    background = BrandBg,
    onBackground = BrandTextHigh,
    surface = BrandSurface,
    onSurface = BrandTextHigh,
    surfaceVariant = BrandSurfaceVariant,
    onSurfaceVariant = BrandTextMed,
    outline = BrandOutline,
    error = BrandError,
    onError = BrandTextHigh
)

private val LightColorScheme = lightColorScheme(
    primary = BrandLimeDeep,
    onPrimary = Color.White,
    secondary = BrandBlue,
    tertiary = BrandLimeDeep,
    error = BrandError
)

@Composable
fun CholoGOTheme(
    // Every hand-built screen in the app is dark-only, regardless of the
    // system setting - there's no light-mode styling anywhere else to
    // match, so the few components that DO read this theme (dialogs,
    // ripples) must stay dark too, or they'd flip light against a pitch-
    // dark app the moment someone's phone is in light mode.
    darkTheme: Boolean = true,
    // CholoGO has a deliberate brand look (dark + electric lime) used by
    // hand across every screen - letting Material You repaint the few
    // components that DO read the theme (dialogs, ripples) from the
    // user's wallpaper would clash with that, so this defaults off.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}