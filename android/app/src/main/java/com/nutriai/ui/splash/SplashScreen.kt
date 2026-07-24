package com.nutriai.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutriai.R
import com.nutriai.ui.theme.BrandGreen
import com.nutriai.ui.theme.BrandGreenDeep
import com.nutriai.ui.theme.BrandGreenLight
import kotlinx.coroutines.delay

/**
 * Kaizen splash. The brand is drawn in Compose over a pastel-green gradient, so it never depends on
 * a raster asset that could fall out of date — the wordmark and tagline always read "Kaizen".
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val fade by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(700), label = "fade")

    LaunchedEffect(Unit) {
        visible = true
        // Just long enough for the brand fade to land — not a gratuitous blocking wait.
        delay(1300)
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BrandGreenDeep, BrandGreen, BrandGreenLight))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.alpha(fade),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
            )
            Text("Kaizen", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            Text(
                "Small Habits. Big Results.",
                color = Color(0xFFEAF7EF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
