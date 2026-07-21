package com.nutriai.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutriai.R
import com.nutriai.ui.theme.BrandGradientBox
import com.nutriai.ui.theme.BrandGreenLight
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val fade by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700),
        label = "fade",
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(2000)
        onFinished()
    }

    BrandGradientBox {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .alpha(fade)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "NutriAI",
                modifier = Modifier.size(140.dp),
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) { append("Nutri") }
                    withStyle(SpanStyle(color = BrandGreenLight, fontWeight = FontWeight.Bold)) { append("AI") }
                },
                fontSize = 44.sp,
            )
            Text(
                "— Smarter Nutrition. Healthier You. —",
                color = Color(0xFFCFE9D9),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp),
            )

            Row(
                Modifier.fillMaxWidth().padding(top = 40.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Feature("🥗", "Personalized\nDiet Plans")
                Feature("📈", "Track\nProgress")
                Feature("❤️", "Improve\nHealth")
                Feature("🧠", "AI Powered\nInsights")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .alpha(fade)
                .padding(bottom = 56.dp, start = 40.dp, end = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Your Health Journey,",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Powered by AI ✨",
                color = BrandGreenLight,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp),
            )
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = BrandGreenLight,
                trackColor = Color(0x33FFFFFF),
            )
            Spacer(Modifier.height(12.dp))
            Text("Loading your best self…", color = Color(0x99FFFFFF), fontSize = 13.sp)
        }
    }
}

@Composable
private fun Feature(icon: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 26.sp)
        Text(
            label,
            color = Color(0xFFCFE9D9),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
