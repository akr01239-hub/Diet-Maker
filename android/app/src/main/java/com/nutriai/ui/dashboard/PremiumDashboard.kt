package com.nutriai.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
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
    manualHeartRate: Int? = null,
    stress: Int? = null,
    onSaveVitals: (Int?, Int?) -> Unit = { _, _ -> },
    safetyFlags: List<com.nutriai.data.remote.dto.Flag> = emptyList(),
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

        // 2b. Health & safety notes (guardrail flags: conditions, medication interactions…)
        if (safetyFlags.isNotEmpty()) {
            item { SafetyCard(flags = safetyFlags) }
        }

        // 3. Macro row
        item { MacroRow(dashboard = d) }

        // 4. Steps (Health Connect) — above hydration
        item { StepsCard(steps = steps, stepsKcal = stepsKcal, hasPermission = stepsPermission, available = stepsAvailable, onConnect = onConnectSteps) }

        // 4b. Vitals — Health Connect if a band syncs, else manual entry (many watches don't sync).
        item {
            VitalsCard(
                heartRate = heartRate,
                sleepHours = sleepHours,
                manualHeartRate = manualHeartRate,
                stress = stress,
                onSaveVitals = onSaveVitals,
            )
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatTile(
            label = "Protein",
            value = "${proteinConsumed.toInt()} g",
            icon = "💪",
            fraction = proteinPercent / 100f,
            accent = BrandGreen,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = "Carbs",
            value = "${dashboard.macros.carbG.toInt()} g",
            icon = "🌾",
            fraction = null,
            accent = BrandAmber,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = "Fat",
            value = "${dashboard.macros.fatG.toInt()} g",
            icon = "🥑",
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
    icon: String,
    fraction: Float?,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(accent))
                Text(
                    "$icon $label",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
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
                    .height(5.dp)
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
private fun VitalsCard(
    heartRate: Int?,
    sleepHours: Double?,
    manualHeartRate: Int?,
    stress: Int?,
    onSaveVitals: (Int?, Int?) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    // Prefer a live Health Connect reading; fall back to the manually-entered value.
    val hr = heartRate ?: manualHeartRate
    val hrFromWatch = heartRate != null
    val stressLabel = stress?.let { listOf("", "😌 Calm", "🙂 Low", "😐 Medium", "😣 High", "😖 Very high").getOrElse(it) { "" } }

    if (editing) {
        VitalsEntryDialog(
            initialHr = manualHeartRate,
            initialStress = stress,
            onDismiss = { editing = false },
            onSave = { newHr, newStress -> onSaveVitals(newHr, newStress); editing = false },
        )
    }

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
                Text(
                    if (hrFromWatch || sleepHours != null) "Vitals · via Health Connect" else "Vitals · tap edit to log",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (hr != null || sleepHours != null || stressLabel != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        hr?.let {
                            Text(
                                "❤️ $it bpm" + if (!hrFromWatch) "*" else "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        sleepHours?.let {
                            Text("😴 ${it}h", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BrandGreenDeep)
                        }
                        stressLabel?.let {
                            Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BrandGreenDeep)
                        }
                    }
                    if (hr != null && !hrFromWatch) {
                        Text("*manually logged", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text(
                        "Your watch is connected to its OWN app (e.g. Fastrack Smart) — not to NutriAI. " +
                            "For it to show here, that app must sync to Health Connect: open Fastrack Smart → " +
                            "Settings and enable Health Connect / Google Fit. Many watch apps don't support this — " +
                            "if so, just tap Edit to log manually.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = { editing = true }) { Text("Edit") }
        }
    }
}

@Composable
private fun VitalsEntryDialog(
    initialHr: Int?,
    initialStress: Int?,
    onDismiss: () -> Unit,
    onSave: (Int?, Int?) -> Unit,
) {
    var hrText by remember { mutableStateOf(initialHr?.toString() ?: "") }
    var stress by remember { mutableStateOf(initialStress) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log your vitals") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = hrText,
                    onValueChange = { hrText = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Resting heart rate (bpm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                Text("How stressed do you feel today?", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val faces = listOf(1 to "😌", 2 to "🙂", 3 to "😐", 4 to "😣", 5 to "😖")
                    faces.forEach { (lvl, face) ->
                        val selected = stress == lvl
                        Box(
                            Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) BrandGreen else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { stress = if (selected) null else lvl },
                            contentAlignment = Alignment.Center,
                        ) { Text(face, style = MaterialTheme.typography.titleLarge) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(hrText.toIntOrNull(), stress) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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
            val onC = MaterialTheme.colorScheme.onPrimaryContainer
            val line = onC.copy(alpha = 0.25f)
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, line, RoundedCornerShape(10.dp)),
            ) {
                JourneyRow("When", "Weight", "BMI", line, onC, header = true)
                dashboard.projection.forEach { p ->
                    HorizontalDivider(thickness = 1.dp, color = line)
                    JourneyRow(p.label, "${p.weightKg} kg", "${p.bmi}", line, onC, header = false)
                }
            }
        }
    }
}

@Composable
private fun JourneyRow(when_: String, weight: String, bmi: String, line: Color, onC: Color, header: Boolean) {
    val style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        Text(
            when_,
            Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 8.dp),
            style = style,
            fontWeight = if (header) FontWeight.SemiBold else FontWeight.Medium,
            color = if (header) onC.copy(alpha = 0.75f) else onC,
        )
        Box(Modifier.width(1.dp).fillMaxHeight().background(line))
        Text(
            weight,
            Modifier.weight(0.6f).padding(horizontal = 10.dp, vertical = 8.dp),
            style = style,
            fontWeight = if (header) FontWeight.SemiBold else FontWeight.SemiBold,
            textAlign = TextAlign.End,
            color = if (header) onC.copy(alpha = 0.75f) else onC,
        )
        Box(Modifier.width(1.dp).fillMaxHeight().background(line))
        Text(
            bmi,
            Modifier.weight(0.5f).padding(horizontal = 10.dp, vertical = 8.dp),
            style = style,
            textAlign = TextAlign.End,
            color = if (header) onC.copy(alpha = 0.75f) else onC,
        )
    }
}

// ---------------------------------------------------------------------------
// 2b. Health & safety notes (guardrail flags)
// ---------------------------------------------------------------------------

@Composable
private fun SafetyCard(flags: List<com.nutriai.data.remote.dto.Flag>) {
    // Most serious first: critical -> warning -> info.
    val order = mapOf("critical" to 0, "warning" to 1, "info" to 2)
    val sorted = flags.sortedBy { order[it.severity] ?: 3 }
    val hasCritical = flags.any { it.severity == "critical" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🩺", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Health & safety notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (hasCritical) {
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.error).padding(horizontal = 8.dp, vertical = 2.dp),
                    ) { Text("See a doctor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold) }
                }
            }
            sorted.forEach { f ->
                val (icon, tint) = when (f.severity) {
                    "critical" -> "⛔" to MaterialTheme.colorScheme.error
                    "warning" -> "⚠️" to Color(0xFFB45309)
                    else -> "ℹ️" to BrandGreenDeep
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(tint.copy(alpha = 0.10f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(icon, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        f.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
