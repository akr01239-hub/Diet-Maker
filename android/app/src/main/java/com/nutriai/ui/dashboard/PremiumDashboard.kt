package com.nutriai.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nutriai.data.remote.dto.Dashboard
import com.nutriai.data.remote.dto.DashMetric
import com.nutriai.ui.theme.BrandAmber
import com.nutriai.ui.theme.BrandGreen
import com.nutriai.ui.theme.BrandGreenDark
import com.nutriai.ui.theme.BrandGreenDeep
import com.nutriai.ui.theme.BrandGreenLight
import com.nutriai.ui.theme.BrandLime
import java.time.LocalTime

// ---------------------------------------------------------------------------
// Premium redesigned dashboard for NutriAI — Apple Health / WHOOP inspired.
// One self-contained file. Uses only Foundation / Material3 / Canvas.
// ---------------------------------------------------------------------------

@Composable
fun PremiumDashboard(
    dashboard: Dashboard,
    greetingName: String?,
    onAddWater: () -> Unit,
    onCompleteProfile: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = dashboard
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp),
    ) {
        // 1. Hero
        item { HeroCard(greetingName = greetingName, streakDays = d.streakDays) }

        // 2. Calorie ring
        item { CalorieRingCard(dashboard = d, onCompleteProfile = onCompleteProfile) }

        // 3. Macro row
        item { MacroRow(dashboard = d) }

        // 4. Hydration
        item { HydrationCard(water = d.water, onAddWater = onAddWater) }

        // 5. Scores
        item { ScoresRow(dashboard = d) }

        // 6. Your journey (projection)
        if (d.projection.size > 1) {
            item { JourneyCard(dashboard = d) }
        }

        // 7. Footer: logout / delete / disclaimer
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                ) { Text("Log out") }
                TextButton(
                    onClick = onDeleteAccount,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Delete account", color = MaterialTheme.colorScheme.error)
                }
                Text(
                    "Educational guidance, not medical advice — consult a professional.",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 1. Hero card
// ---------------------------------------------------------------------------

@Composable
private fun HeroCard(greetingName: String?, streakDays: Int) {
    val hour = LocalTime.now().hour
    val (greeting, motivation) = when {
        hour < 12 -> "Good morning" to "Fuel your body right — today is yours to own."
        hour < 17 -> "Good afternoon" to "Keep the momentum going. Every choice counts."
        else -> "Good evening" to "Wind down strong. Consistency beats perfection."
    }
    val name = greetingName ?: "there"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(BrandGreenLight, BrandGreen, BrandGreenDeep),
                    ),
                )
                .padding(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "$greeting, $name 👋",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    motivation,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.22f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        "🔥 $streakDays day streak",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2. Calorie ring
// ---------------------------------------------------------------------------

@Composable
private fun CalorieRingCard(dashboard: Dashboard, onCompleteProfile: () -> Unit) {
    val cal = dashboard.calories
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Today's energy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )

            if (cal.target == null) {
                Text(
                    "${cal.consumed.toInt()} kcal logged so far.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = onCompleteProfile,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                ) { Text("Set your goal") }
            } else {
                val percent = (cal.percent ?: 0.0).coerceIn(0.0, 100.0).toFloat()
                val remaining = cal.remaining ?: (cal.target - cal.consumed)
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    RingCanvas(
                        percent = percent,
                        trackColor = trackColor,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${remaining.toInt().coerceAtLeast(0)}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "kcal left",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "${cal.consumed.toInt()} / ${cal.target.toInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandGreen,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RingCanvas(percent: Float, trackColor: Color, modifier: Modifier = Modifier) {
    val sweepBrush = Brush.linearGradient(colors = listOf(BrandGreenLight, BrandGreen, BrandLime))
    Canvas(modifier = modifier) {
        val strokeWidthPx = 26.dp.toPx()
        val inset = strokeWidthPx / 2f
        val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
        val topLeft = Offset(inset, inset)
        // Track
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
        )
        // Progress
        val sweep = (percent / 100f) * 360f
        if (sweep > 0f) {
            drawArc(
                brush = sweepBrush,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 3. Macro row
// ---------------------------------------------------------------------------

@Composable
private fun MacroRow(dashboard: Dashboard) {
    val proteinConsumed = dashboard.protein.consumed ?: 0.0
    val proteinPercent = (dashboard.protein.percent ?: 0.0).coerceIn(0.0, 100.0).toFloat()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StatTile(
            label = "Protein",
            value = "${proteinConsumed.toInt()} g",
            fraction = proteinPercent / 100f,
            accent = BrandGreen,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = "Carbs",
            value = "${dashboard.macros.carbG.toInt()} g",
            fraction = null,
            accent = BrandAmber,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = "Fat",
            value = "${dashboard.macros.fatG.toInt()} g",
            fraction = null,
            accent = BrandGreenDark,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    fraction: Float?,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            LinearProgressIndicator(
                progress = { (fraction ?: 1f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.18f),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 4. Hydration card
// ---------------------------------------------------------------------------

@Composable
private fun HydrationCard(water: DashMetric, onAddWater: () -> Unit) {
    val consumed = water.consumedMl ?: water.consumed ?: 0.0
    val target = water.targetMl ?: water.target
    val fraction = (water.percent ?: 0.0).coerceIn(0.0, 100.0).toFloat() / 100f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "💧 Hydration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${consumed.toInt()}${target?.let { " / ${it.toInt()}" } ?: ""} ml",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen,
                )
            }
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = BrandGreenLight,
                trackColor = BrandGreenLight.copy(alpha = 0.18f),
            )
            Button(
                onClick = onAddWater,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            ) { Text("+ Add 250 ml") }
        }
    }
}

// ---------------------------------------------------------------------------
// 5. Scores row
// ---------------------------------------------------------------------------

@Composable
private fun ScoresRow(dashboard: Dashboard) {
    val adherence = (dashboard.calories.percent ?: 0.0).coerceIn(0.0, 100.0).toInt()
    val hydration = (dashboard.water.percent ?: 0.0).coerceIn(0.0, 100.0).toInt()
    val bmiText = dashboard.bmi?.let { String.format("%.1f", it) } ?: "—"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScoreTile("Adherence", "$adherence", "%", BrandGreen, Modifier.weight(1f))
        ScoreTile("Hydration", "$hydration", "%", BrandGreenLight, Modifier.weight(1f))
        ScoreTile("BMI", bmiText, "", BrandAmber, Modifier.weight(1f))
    }
}

@Composable
private fun ScoreTile(
    label: String,
    value: String,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                if (unit.isNotEmpty()) {
                    Text(
                        unit,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                        modifier = Modifier.padding(bottom = 4.dp, start = 1.dp),
                    )
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 6. Your journey (projection)
// ---------------------------------------------------------------------------

@Composable
private fun JourneyCard(dashboard: Dashboard) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Your journey ✨",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "Projected at your safe, steady pace",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(2.dp))
            dashboard.projection.forEach { p ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        p.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "${p.weightKg} kg · BMI ${p.bmi}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}
