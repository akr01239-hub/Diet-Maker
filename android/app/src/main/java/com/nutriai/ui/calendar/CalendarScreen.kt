package com.nutriai.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nutriai.data.AppRepository
import com.nutriai.data.remote.dto.Adaptation
import com.nutriai.data.remote.dto.DayPlan
import com.nutriai.data.remote.dto.ExerciseLogDto
import com.nutriai.data.remote.dto.ExerciseLogRequest
import com.nutriai.data.remote.dto.Guidance
import com.nutriai.data.remote.dto.LastPerformance
import com.nutriai.data.remote.dto.WorkoutDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---- State ----
data class CalendarState(
    val loading: Boolean = true,
    val dietError: String? = null,
    val workoutError: String? = null,
    val dietDays: List<DayPlan> = emptyList(),
    val workoutDays: List<WorkoutDay> = emptyList(),
    val workoutBlockLabel: String? = null,
    val selectedDate: String? = null,
    /** Most recent performed set per exercise name — progression hints. */
    val lastPerf: Map<String, LastPerformance> = emptyMap(),
    /** Exercise logs the user recorded on the selected day. */
    val selectedLogs: List<ExerciseLogDto> = emptyList(),
    /** Adaptive coaching insight from recent logging + weight trend. */
    val adaptation: Adaptation? = null,
    val applying: Boolean = false,
    /** Personalized diet + exercise guidance from conditions / sex / lifestyle. */
    val guidance: com.nutriai.data.remote.dto.Guidance? = null,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = CalendarState(loading = true)
        viewModelScope.launch {
            val plan = repository.latestPlan()
            val workout = repository.exercisePlan()
            val lastPerf = repository.lastPerformance().getOrDefault(emptyMap())
            val adaptation = repository.adaptation().getOrNull()
            val guidance = repository.guidance().getOrNull()

            val dietDays = plan.getOrNull()?.days.orEmpty()
            val workoutPlan = workout.getOrNull()
            val workoutDays = workoutPlan?.days.orEmpty()

            // Default the selection to the "Today" diet day when present,
            // otherwise the first day, otherwise a "Today" workout day.
            val defaultDate = dietDays.firstOrNull { it.label == "Today" }?.date
                ?: dietDays.firstOrNull { it.date != null }?.date
                ?: workoutDays.firstOrNull { it.label == "Today" }?.date
                ?: workoutDays.firstOrNull { it.date != null }?.date

            val logs = defaultDate?.let { repository.exerciseLogs(it).getOrDefault(emptyList()) }.orEmpty()

            _state.value = CalendarState(
                loading = false,
                dietError = plan.exceptionOrNull()?.message,
                workoutError = workout.exceptionOrNull()?.message,
                dietDays = dietDays,
                workoutDays = workoutDays,
                workoutBlockLabel = workoutPlan?.blockLabel?.takeIf { it.isNotBlank() },
                selectedDate = defaultDate,
                lastPerf = lastPerf,
                selectedLogs = logs,
                adaptation = adaptation,
                guidance = guidance,
            )
        }
    }

    /** Swaps a single meal in the plan for a different dish at similar calories. */
    fun swapMeal(dayIndex: Int, slot: String) {
        viewModelScope.launch {
            repository.swapMeal(dayIndex, slot).getOrNull()?.let { plan ->
                _state.value = _state.value.copy(dietDays = plan.days)
            }
        }
    }

    /** Applies the coach's recommendation: rebuilds the plan (at the adjusted target). */
    fun applyAdaptation() {
        _state.value = _state.value.copy(applying = true)
        viewModelScope.launch {
            repository.applyAdaptation()
            val plan = repository.latestPlan().getOrNull()
            val adaptation = repository.adaptation().getOrNull()
            _state.value = _state.value.copy(
                applying = false,
                dietDays = plan?.days.orEmpty(),
                adaptation = adaptation,
            )
        }
    }

    fun selectDate(date: String?) {
        _state.value = _state.value.copy(selectedDate = date, selectedLogs = emptyList())
        viewModelScope.launch {
            val logs = date?.let { repository.exerciseLogs(it).getOrDefault(emptyList()) }.orEmpty()
            if (_state.value.selectedDate == date) _state.value = _state.value.copy(selectedLogs = logs)
        }
    }

    /** Records a performed set against the selected day, then refreshes logs + progression. */
    fun logExercise(name: String, focus: String?, sets: Int?, reps: Int?, weightKg: Double?) {
        val date = _state.value.selectedDate
        val performedAt = date?.let { "${it}T12:00:00.000Z" }
        viewModelScope.launch {
            repository.logExercise(
                ExerciseLogRequest(
                    exerciseName = name,
                    focus = focus,
                    sets = sets,
                    reps = reps,
                    weightKg = weightKg,
                    performedAt = performedAt,
                ),
            )
            val logs = date?.let { repository.exerciseLogs(it).getOrDefault(emptyList()) }.orEmpty()
            val lastPerf = repository.lastPerformance().getOrDefault(emptyMap())
            _state.value = _state.value.copy(selectedLogs = logs, lastPerf = lastPerf)
        }
    }

    fun deleteLog(id: String) {
        val date = _state.value.selectedDate
        viewModelScope.launch {
            repository.deleteExerciseLog(id)
            val logs = date?.let { repository.exerciseLogs(it).getOrDefault(emptyList()) }.orEmpty()
            _state.value = _state.value.copy(selectedLogs = logs)
        }
    }

    fun regenerate() {
        _state.value = _state.value.copy(loading = true)
        viewModelScope.launch {
            repository.generatePlan()
            load()
        }
    }
}

/** Which exercise the log dialog is currently open for, with sensible pre-filled values. */
private data class PendingLog(
    val name: String,
    val focus: String?,
    val weight: String,
    val reps: String,
    val sets: String,
)

// ---- UI ----
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<PendingLog?>(null) }

    pending?.let { p ->
        LogDialog(
            pending = p,
            onDismiss = { pending = null },
            onConfirm = { weight, reps, sets ->
                viewModel.logExercise(p.name, p.focus, sets, reps, weight)
                pending = null
            },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "This week",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        item {
            Button(onClick = { viewModel.regenerate() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.dietDays.isEmpty()) "Generate my 7-day plan" else "🔄 Regenerate week")
            }
        }

        state.adaptation?.takeIf { it.status != "insufficient_data" }?.let { adapt ->
            item {
                AdaptiveInsightCard(
                    adaptation = adapt,
                    applying = state.applying,
                    onApply = { viewModel.applyAdaptation() },
                )
            }
        }

        state.guidance?.takeIf { it.dietTips.isNotEmpty() || it.exerciseTips.isNotEmpty() }?.let { g ->
            item { GuidanceCard(g) }
        }

        if (state.loading) {
            item { CircularProgressIndicator() }
        }

        if (!state.loading && state.dietDays.isEmpty() && state.workoutDays.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Nothing scheduled yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            state.dietError
                                ?: state.workoutError
                                ?: "Complete your profile and generate a plan to see your week here.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        // Week strip built from the diet-plan days.
        if (state.dietDays.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.dietDays) { day ->
                        DayPill(
                            label = day.label,
                            date = day.date,
                            isToday = day.label == "Today",
                            isSelected = day.date != null && day.date == state.selectedDate,
                            onClick = { day.date?.let { viewModel.selectDate(it) } },
                        )
                    }
                }
            }
        }

        // Selected day's combined report.
        val selected = state.selectedDate
        val dietDay = state.dietDays.firstOrNull { it.date != null && it.date == selected }
        val workoutDay = state.workoutDays.firstOrNull { it.date != null && it.date == selected }

        if (!state.loading && (dietDay != null || workoutDay != null)) {
            item {
                Text(
                    (dietDay?.label ?: workoutDay?.label ?: "Selected day") +
                        (selected?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Diet section.
            item { Text("🍽️ Diet", style = MaterialTheme.typography.titleSmall) }
            if (dietDay == null || dietDay.meals.isEmpty()) {
                item {
                    Text(
                        state.dietError ?: "No meals planned for this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(dietDay.meals) { meal ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    meal.slot.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    "🔄 Swap",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { viewModel.swapMeal(dietDay.dayIndex, meal.slot) },
                                )
                            }
                            meal.items.forEach { mealItem ->
                                Text(
                                    "• ${mealItem.name} — ${mealItem.grams.toInt()}g (${mealItem.kcal.toInt()}kcal)",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
                item {
                    Text(
                        "Total: ${dietDay.totals.kcal.toInt()} kcal · P ${dietDay.totals.proteinG.toInt()}g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Workout section.
            item { Text("🏋️ Workout", style = MaterialTheme.typography.titleSmall) }
            state.workoutBlockLabel?.let { block ->
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ) {
                        Text(
                            "🔄 $block — new exercises next month",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            }
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        when {
                            workoutDay == null -> Text(
                                state.workoutError ?: "No workout scheduled for this day.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            workoutDay.rest -> {
                                Text(
                                    workoutDay.focus.ifBlank { "Rest day" },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text("Rest & recovery", style = MaterialTheme.typography.bodyMedium)
                            }
                            else -> {
                                Text(
                                    workoutDay.focus.ifBlank { "Workout" },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                if (workoutDay.exercises.isEmpty()) {
                                    Text("No exercises listed.", style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    val focus = workoutDay.focus.ifBlank { null }
                                    workoutDay.exercises.forEach { ex ->
                                        val last = state.lastPerf[ex.name]
                                        val done = state.selectedLogs.any { it.exerciseName == ex.name }
                                        ExerciseRow(
                                            name = ex.name,
                                            prescription = "${ex.sets} × ${ex.reps}",
                                            last = last,
                                            done = done,
                                            onLog = {
                                                pending = PendingLog(
                                                    name = ex.name,
                                                    focus = focus,
                                                    weight = last?.weightKg?.let { fmtNum(it) } ?: "",
                                                    reps = (last?.reps?.toString() ?: ex.reps.filter { c -> c.isDigit() }),
                                                    sets = (last?.sets ?: ex.sets).toString(),
                                                )
                                            },
                                        )
                                    }
                                    if (state.selectedLogs.isNotEmpty()) {
                                        Text(
                                            "✅ Logged this day",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 6.dp),
                                        )
                                        state.selectedLogs.forEach { log ->
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    buildString {
                                                        append(log.exerciseName)
                                                        log.weightKg?.let { append(" — ${fmtNum(it)} kg") }
                                                        log.reps?.let { append(" × $it") }
                                                        log.sets?.let { append(" · ${it} set${if (it == 1) "" else "s"}") }
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                                Text(
                                                    "✕",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier
                                                        .clickable { viewModel.deleteLog(log.id) }
                                                        .padding(start = 8.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Educational guidance, not medical advice — consult a professional.",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun GuidanceCard(g: Guidance) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("🎯 Personalized for you", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        g.summary.ifBlank { "Diet & exercise tips for your profile" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    )
                }
                Text(if (expanded) "▲" else "▼", style = MaterialTheme.typography.bodyMedium)
            }
            if (expanded) {
                if (g.dietTips.isNotEmpty()) {
                    Text("🥗 Diet", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    g.dietTips.forEach { Text("•  $it", style = MaterialTheme.typography.bodySmall) }
                }
                if (g.exerciseTips.isNotEmpty()) {
                    Text("🏋️ Exercise", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    g.exerciseTips.forEach { Text("•  $it", style = MaterialTheme.typography.bodySmall) }
                }
                Text(
                    "Educational guidance, not medical advice.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                )
            } else {
                Text(
                    "Tap to see your diet & exercise tips",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun AdaptiveInsightCard(
    adaptation: Adaptation,
    applying: Boolean,
    onApply: () -> Unit,
) {
    val onTrack = adaptation.status == "on_track"
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (onTrack) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            },
        ),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (onTrack) "🎯 Coach: on track" else "🧭 Coach insight",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(adaptation.message, style = MaterialTheme.typography.bodyMedium)
            if (adaptation.status == "adjust_target") {
                Button(
                    onClick = onApply,
                    enabled = !applying,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (applying) {
                        CircularProgressIndicator(Modifier.width(20.dp))
                    } else {
                        val sign = if (adaptation.suggestedKcalDelta >= 0) "+" else ""
                        Text("Apply ($sign${adaptation.suggestedKcalDelta} kcal) & rebuild plan")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(
    name: String,
    prescription: String,
    last: LastPerformance?,
    done: Boolean,
    onLog: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                (if (done) "✓ " else "") + name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (done) FontWeight.SemiBold else FontWeight.Normal,
            )
            val hint = buildString {
                append("Target: $prescription")
                last?.weightKg?.let {
                    append("   ·   last ${fmtNum(it)} kg")
                    last.reps?.let { r -> append(" × $r") }
                }
            }
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = onLog, modifier = Modifier.padding(start = 8.dp)) {
            Text(if (done) "Log again" else "Log")
        }
    }
}

@Composable
private fun LogDialog(
    pending: PendingLog,
    onDismiss: () -> Unit,
    onConfirm: (weightKg: Double?, reps: Int?, sets: Int?) -> Unit,
) {
    var weight by remember(pending) { mutableStateOf(pending.weight) }
    var reps by remember(pending) { mutableStateOf(pending.reps) }
    var sets by remember(pending) { mutableStateOf(pending.sets) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log ${pending.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("What did you actually do? Leave blank what doesn't apply.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Weight (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = sets,
                        onValueChange = { sets = it.filter { c -> c.isDigit() } },
                        label = { Text("Sets") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it.filter { c -> c.isDigit() } },
                    label = { Text("Reps (per set)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(weight.toDoubleOrNull(), reps.toIntOrNull(), sets.toIntOrNull())
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Formats a Double without a trailing ".0" (40 not 40.0). */
private fun fmtNum(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

@Composable
private fun DayPill(
    label: String?,
    date: String?,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val dayNumber = date?.substringAfterLast('-')?.trimStart('0')?.ifBlank { "0" } ?: "—"
    val container = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.width(64.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                label ?: "—",
                style = MaterialTheme.typography.labelMedium,
                color = content,
                maxLines = 1,
            )
            Text(
                dayNumber,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = content,
            )
        }
    }
}
