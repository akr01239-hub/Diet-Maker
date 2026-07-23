package com.nutriai.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.nutriai.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val fade by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(600), label = "fade")

    LaunchedEffect(Unit) {
        visible = true
        // Just long enough for the brand fade to land — not a gratuitous blocking wait.
        delay(1200)
        onFinished()
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Image(
            painter = painterResource(R.drawable.splash_nutriai),
            contentDescription = "NutriAI — Diet · Exercise · Discipline",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(fade),
        )
    }
}
