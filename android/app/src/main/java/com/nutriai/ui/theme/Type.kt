package com.nutriai.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * NutriAI type scale. Built on the Material 3 metrics (so nothing re-lays-out) but with
 * bolder headings/titles and slightly tighter tracking on large text for a more confident,
 * premium feel across every screen. Uses the system font family (no bundled font yet).
 */
private val Default = Typography()

val NutriTypography = Default.copy(
    displayLarge = Default.displayLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    displayMedium = Default.displayMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    displaySmall = Default.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineLarge = Default.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = Default.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    headlineSmall = Default.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = Default.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = Default.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Default.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Default.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = Default.labelMedium.copy(fontWeight = FontWeight.Medium),
)
