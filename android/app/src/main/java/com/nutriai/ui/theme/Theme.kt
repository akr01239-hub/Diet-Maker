package com.nutriai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = BrandMint,
    onPrimaryContainer = BrandGreenDeep,
    secondary = BrandGreenDark,
    onSecondary = Color.White,
    secondaryContainer = BrandMint,
    onSecondaryContainer = BrandGreenDeep,
    tertiary = BrandAmber,
    onTertiary = Color.White,
    tertiaryContainer = BrandAmberContainer,
    background = AppBackgroundLight,
    onBackground = OnSurfaceLight,
    surface = AppSurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = AppSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF3A5347),
    outline = Color(0xFF9DBCA9),
)

private val DarkColors = darkColorScheme(
    primary = BrandGreenLight,
    onPrimary = Color(0xFF042713),
    primaryContainer = BrandGreenDark,
    onPrimaryContainer = BrandMint,
    secondary = BrandGreenLight,
    onSecondary = Color(0xFF042713),
    secondaryContainer = Color(0xFF1D3327),
    onSecondaryContainer = BrandMint,
    tertiary = BrandAmber,
    onTertiary = Color(0xFF3A2A00),
    tertiaryContainer = Color(0xFF6B4E00),
    background = AppBackgroundDark,
    onBackground = OnSurfaceDark,
    surface = AppSurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = AppSurfaceVariantDark,
    onSurfaceVariant = Color(0xFFB9CFC0),
    outline = Color(0xFF3E5A49),
)

@Composable
fun NutriAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Fixed brand palette (no dynamic color) so every screen looks consistent + colorful.
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NutriTypography,
        content = content,
    )
}
