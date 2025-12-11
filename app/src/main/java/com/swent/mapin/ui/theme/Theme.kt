package com.swent.mapin.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Dark color scheme using the app's custom colors */
private val DarkColorScheme =
    darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

/** Light color scheme using the app's custom colors */
private val LightColorScheme =
    lightColorScheme(
        primary = Purple40, secondary = PurpleGrey40, tertiary = Pink40

        /* Other default colors to override
        background = Color(0xFFFFFBFE),
        surface = Color(0xFFFFFBFE),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFF1C1B1F),
        onSurface = Color(0xFF1C1B1F),
        */
        )

/**
 * Main theme composable for the Map-In application.
 *
 * Applies Material Design 3 theming with support for:
 * - Light/Dark theme switching
 * - Dynamic color on Android 12+ (Material You)
 * - Custom color schemes for older Android versions
 *
 * **Platform-Specific Behavior:**
 * - **Android 12+ (API 31+)** with `dynamicColor=true`: Uses system's Material You colors extracted
 *   from the user's wallpaper for a personalized experience
 * - **Android 11 and below** OR `dynamicColor=false`: Falls back to app-defined color schemes
 *   (DarkColorScheme/LightColorScheme) using placeholder colors from Color.kt
 *
 * **Fallback Hierarchy:**
 * 1. If Android 12+ AND dynamicColor enabled → Dynamic system colors
 * 2. If darkTheme enabled → DarkColorScheme (Purple80, PurpleGrey80, Pink80)
 * 3. Otherwise → LightColorScheme (Purple40, PurpleGrey40, Pink40)
 *
 * @param darkTheme Whether to use dark theme (defaults to system setting via [isSystemInDarkTheme])
 * @param dynamicColor Whether to use dynamic color from system. Only effective on Android 12+ (API
 *   31+). On older versions, this parameter is ignored and fallback colors are used. Default is
 *   true to enable Material You when available.
 * @param content The composable content to be themed
 */
@Composable
fun MapInTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
  val colorScheme =
      when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
          val context = LocalContext.current
          if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
      }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
