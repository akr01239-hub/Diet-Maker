package com.nutriai.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Deep-green brand gradient used on splash + auth screens. */
val DarkBrandGradient: Brush = Brush.verticalGradient(
    listOf(
        Color(0xFF06251A),
        BrandGreenDeep,
        Color(0xFF0D5433),
    ),
)

/** A full-screen gradient background wrapper. */
@Composable
fun BrandGradientBox(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBrandGradient),
        content = content,
    )
}
