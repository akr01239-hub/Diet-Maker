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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nutriai.data.AppRepository
import com.nutriai.data.remote.dto.DayPlan
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

            val dietDays = plan.getOrNull()?.days.orEmpty()
            val workoutPlan = workout.getOrNull()
            val workoutDays = workoutPlan?.days.orEmpty()

            // Default the selection to the "Today" diet day when present,
            // otherwise the first day, otherwise a "Today" workout day.
            val defaultDate = dietDays.firstOrNull { it.label == "Today" }?.date
                ?: dietDays.firstOrNull { it.date != null }?.date
                ?: workoutDays.firstOrNull { it.label == "Today" }?.date
                ?: workoutDays.firstOrNull { it.date != null }?.date

            _state.value = CalendarState(
                loading = false,
                dietError = plan.exceptionOrNull()?.message,
                workoutError = workout.exceptionOrNull()?.message,
                dietDays = dietDays,
                workoutDays = workoutDays,
                workoutBlockLabel = workoutPlan?.blockLabel?.takeIf { it.isNotBlank() },
                selectedDate = defaultDate,
            )
        }
    }

    fun selectDate(date: String?) {
        _state.value = _state.value.copy(selectedDate = date)
    }

    fun regenerate() {
        _state.value = _state.value.copy(loading = true)
        viewModelScope.launch {
            repository.generatePlan()
            load()
        }
    }
}

// ---- UI ----
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
                            Text(
                                meal.slot.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
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
                                    workoutDay.exercises.forEach { ex ->
                                        Text(
                                            "• ${ex.name} — ${ex.sets} × ${ex.reps}",
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
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
