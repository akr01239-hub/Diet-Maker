package com.nutriai.ui.move

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nutriai.data.AppRepository
import com.nutriai.data.remote.dto.ExerciseLogDto
import com.nutriai.data.remote.dto.WellnessHistory
import com.nutriai.data.remote.dto.WellnessSessionDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MoveLogState(
    val loading: Boolean = true,
    val todayExercise: List<ExerciseLogDto> = emptyList(),
    val history: WellnessHistory = WellnessHistory(),
)

@HiltViewModel
class MoveLogViewModel @Inject constructor(private val repository: AppRepository) : ViewModel() {
    private val _state = MutableStateFlow(MoveLogState())
    val state: StateFlow<MoveLogState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(loading = true)
        viewModelScope.launch {
            val logs = repository.exerciseLogs(null).getOrDefault(emptyList())
            val history = repository.wellnessHistory().getOrDefault(WellnessHistory())
            _state.value = MoveLogState(loading = false, todayExercise = logs, history = history)
        }
    }
}

@Composable
fun MoveLogScreen(modifier: Modifier = Modifier, viewModel: MoveLogViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = remember { java.time.LocalDate.now().toString() }
    val todayWellness = state.history.sessions.filter { it.completedAt.startsWith(today) }
    val burnedToday = state.todayExercise.sumOf { it.kcal ?: 0 } + todayWellness.sumOf { it.kcal }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item { StreakHero(streak = state.history.streakDays, weekCount = state.history.weekCount, burnedToday = burnedToday) }

        if (state.loading) {
            item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }

        item { SectionLabel("Today · exercise") }
        if (state.todayExercise.isEmpty()) {
            item { EmptyCard("No sets logged yet. Log one from the Exercise tab.") }
        } else {
            items(state.todayExercise.size) { i ->
                val e = state.todayExercise[i]
                ActivityRow(
                    title = e.exerciseName,
                    detail = buildString {
                        if (e.sets != null) append("${e.sets} sets")
                        if (e.reps != null) append(if (isEmpty()) "${e.reps} reps" else " × ${e.reps}")
                        if (e.weightKg != null) append(" · ${e.weightKg} kg")
                    }.ifBlank { "Session" },
                    kcal = e.kcal ?: 0,
                )
            }
        }

        item { SectionLabel("Today · meditation & yoga") }
        if (todayWellness.isEmpty()) {
            item { EmptyCard("No sessions yet. Complete one from the Meditation tab.") }
        } else {
            items(todayWellness.size) { i -> WellnessRow(todayWellness[i]) }
        }

        if (state.history.sessions.any { !it.completedAt.startsWith(today) }) {
            item { SectionLabel("Earlier sessions") }
            val earlier = state.history.sessions.filter { !it.completedAt.startsWith(today) }
            items(earlier.size) { i -> WellnessRow(earlier[i]) }
        }

        item {
            Text(
                "Calories burned are educational estimates, not measurements.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun StreakHero(streak: Int, weekCount: Int, burnedToday: Int) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (streak > 0) "🔥 $streak-day wellness streak" else "Start a wellness streak today",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "$weekCount session${if (weekCount == 1) "" else "s"} this week  ·  ~$burnedToday kcal burned today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun EmptyCard(text: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Text(text, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun WellnessRow(s: WellnessSessionDto) {
    val icon = if (s.type == "yoga") "🧘" else "🌬️"
    ActivityRow(title = "$icon ${s.refName}", detail = "${s.durationMin} min · ${s.type}", kcal = s.kcal)
}

@Composable
private fun ActivityRow(title: String, detail: String, kcal: Int) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "🔥 $kcal",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}
