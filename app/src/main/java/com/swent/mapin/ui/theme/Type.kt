package com.swent.mapin.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Material Design 3 typography scale for the application.
 *
 * This defines the text styles used throughout the app following Material Design 3 typography
 * guidelines.
 *
 * **Currently Overridden Styles:**
 * - `bodyLarge`: 16sp, FontWeight.Normal, 24sp line height, 0.5sp letter spacing
 *
 * **Default Styles (not overridden):** All other text styles (displayLarge, displayMedium,
 * displaySmall, headlineLarge, headlineMedium, headlineSmall, titleLarge, titleMedium, titleSmall,
 * bodyMedium, bodySmall, labelLarge, labelMedium, labelSmall) use Material 3 default values.
 *
 * **How to extend:** To customize additional text styles, add them as parameters to the Typography
 * constructor:
 * ```
 * val Typography = Typography(
 *     bodyLarge = TextStyle(...),
 *     titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp)
 * )
 * ```
 *
 * @see [Material 3 Typography Documentation](https://m3.material.io/styles/typography/overview)
 * @see
 *   [Compose Material 3 Typography](https://developer.android.com/jetpack/compose/designsystems/material3#typography)
 */
val Typography =
    Typography(
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp)
        /* Other default text styles to override
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
        */
        )
