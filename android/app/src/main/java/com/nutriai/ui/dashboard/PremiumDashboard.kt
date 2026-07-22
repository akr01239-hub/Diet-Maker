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
import androidx.compose.ui.text.style.TextOverflow
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
    steps: Long = 0,
    stepsKcal: Int = 0,
    stepsPermission: Boolean = true,
    stepsAvailable: Boolean = false,
    onConnectSteps: () -> Unit = {},
    heartRate: Int? = null,
    sleepHours: Double? = null,
    modifier: Modifier = Modifier,
) {
    val d = dashboard
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp),
    ) {
        // 1. Hero
        item { HeroCard(greetingName = greetingName, streakDays = d.streakDays) }

        // 2. Calorie ring
        item { CalorieRingCard(dashboard = d, burnedKcal = stepsKcal, onCompleteProfile = onCompleteProfile) }

        // 3. Macro row
        item { MacroRow(dashboard = d) }

        // 4. Steps (Health Connect) — above hydration
        item { StepsCard(steps = steps, stepsKcal = stepsKcal, hasPermission = stepsPermission, available = stepsAvailable, onConnect = onConnectSteps) }

        // 4b. Watch vitals (heart rate + sleep) — only when a band/watch syncs them.
        if (heartRate != null || sleepHours != null) {
            item { VitalsCard(heartRate = heartRate, sleepHours = sleepHours) }
        }

        // 5. Hydration
        item { HydrationCard(water = d.water, onAddWater = onAddWater) }

        // 6. Scores
        item { ScoresRow(dashboard = d) }

        // 6. Your journey (projection)
        if (d.projection.size > 1) {
            item { JourneyCard(dashboard = d) }
        }

        // 7. Footer: logout / disclaimer (Delete account lives in Settings)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                ) { Text("Log out") }
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "$greeting, $name 👋",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        motivation,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.22f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        "🔥 $streakDays",
                        style = MaterialTheme.typography.titleSmall,
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
private fun CalorieRingCard(dashboard: Dashboard, burnedKcal: Int = 0, onCompleteProfile: () -> Unit) {
    val cal = dashboard.calories
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                // Exercise calories add to the budget (like Fitbit / MyFitnessPal):
                // budget = goal + burned; left = budget − eaten.
                val budget = cal.target + burnedKcal
                val remaining = budget - cal.consumed
                val percent = if (budget > 0) (cal.consumed / budget * 100).coerceIn(0.0, 100.0).toFloat() else 0f
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                Box(
                    modifier = Modifier.size(158.dp),
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
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "kcal left",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // Budget breakdown: Goal + Burned − Eaten.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    BudgetPart("🎯 Goal", "${cal.target.toInt()}", MaterialTheme.colorScheme.onSurface)
                    BudgetPart("🔥 Burned", "+$burnedKcal", BrandGreen)
                    BudgetPart("🍽️ Eaten", "-${cal.consumed.toInt()}", BrandAmber)
                }
            }
        }
    }
}

@Composable
private fun BudgetPart(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RingCanvas(percent: Float, trackColor: Color, modifier: Modifier = Modifier) {
    val sweepBrush = Brush.linearGradient(colors = listOf(BrandGreenLight, BrandGreen, BrandLime))
    Canvas(modifier = modifier) {
        val strokeWidthPx = 20.dp.toPx()
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
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
// 4b. Steps card (Health Connect)
// ---------------------------------------------------------------------------

@Composable
private fun VitalsCard(heartRate: Int?, sleepHours: Double?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⌚", style = MaterialTheme.typography.headlineSmall)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("From your watch", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    heartRate?.let {
                        Text("❤️ $it bpm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                    sleepHours?.let {
                        Text("😴 ${it}h", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BrandGreenDeep)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepsCard(steps: Long, stepsKcal: Int, hasPermission: Boolean, available: Boolean, onConnect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "👟 Steps today",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (hasPermission) "%,d".format(steps) else "—",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen,
                )
            }
            if (hasPermission) {
                Text(
                    "≈ $stepsKcal kcal burned",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    if (available) {
                        "Connect Health Connect to auto-track your steps and calories burned."
                    } else {
                        "Install Health Connect (free) to auto-track your steps and calories burned."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                ) { Text(if (available) "Connect steps" else "Install Health Connect") }
            }
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScoreTile("🎯", "Adherence", "$adherence%", listOf(BrandGreen, BrandGreenDeep), Modifier.weight(1f))
        ScoreTile("💧", "Hydration", "$hydration%", listOf(Color(0xFF38BDF8), Color(0xFF0369A1)), Modifier.weight(1f))
        ScoreTile("⚖️", "BMI", bmiText, listOf(BrandAmber, Color(0xFFB45309)), Modifier.weight(1f))
    }
}

@Composable
private fun ScoreTile(
    icon: String,
    label: String,
    value: String,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(gradient))
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(icon, style = MaterialTheme.typography.titleLarge)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f),
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
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
