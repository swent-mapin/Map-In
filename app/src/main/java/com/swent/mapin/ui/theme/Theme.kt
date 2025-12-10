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
 * @param darkTheme Whether to use dark theme (defaults to system setting)
 * @param dynamicColor Whether to use dynamic color from system (Android 12+)
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
