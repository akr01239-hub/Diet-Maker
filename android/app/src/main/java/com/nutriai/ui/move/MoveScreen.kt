package com.nutriai.ui.move

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nutriai.data.AppRepository
import com.nutriai.data.remote.dto.WeeklyWorkout
import com.nutriai.data.remote.dto.WorkoutDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MoveState(val loading: Boolean = true, val plan: WeeklyWorkout? = null, val error: String? = null)

@HiltViewModel
class MoveViewModel @Inject constructor(private val repository: AppRepository) : ViewModel() {
    private val _state = MutableStateFlow(MoveState())
    val state: StateFlow<MoveState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val r = repository.exercisePlan()
            _state.value = if (r.isSuccess) MoveState(loading = false, plan = r.getOrNull())
            else MoveState(loading = false, error = "Generate a plan first (Diet tab)")
        }
    }
}

@Composable
fun MoveScreen(modifier: Modifier = Modifier, viewModel: MoveViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val plan = state.plan
    val today = plan?.days?.firstOrNull { it.label == "Today" } ?: plan?.days?.firstOrNull { !it.rest }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
    ) {
        item { Hero(plan) }

        if (state.loading) {
            item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }
        state.error?.let { err ->
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Text(err, Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Week strip: train vs rest days.
        plan?.days?.takeIf { it.isNotEmpty() }?.let { days ->
            item {
                Text("This week", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item { WeekStrip(days) }
        }

        // Today's session.
        today?.let { day ->
            item { Text(if (day.label == "Today") "Today · ${day.focus}" else day.focus, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (day.rest || day.exercises.isEmpty()) {
                item { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) { Text("Rest & recovery day — light movement, stretch, hydrate.", Modifier.padding(18.dp)) } }
            } else {
                items(day.exercises.size) { i ->
                    val ex = day.exercises[i]
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                                Text("${i + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(ex.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${ex.sets} × ${ex.reps}",
                                Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        plan?.disclaimer?.takeIf { it.isNotBlank() }?.let { d ->
            item {
                Text(d, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun Hero(plan: WeeklyWorkout?) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("🏋️ Move", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                plan?.let { "${it.blockLabel.ifBlank { "Training block" }} · ${it.location} · ${it.goal}" } ?: "Your training plan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun WeekStrip(days: List<WorkoutDay>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.take(7).forEach { d ->
            val rest = d.rest
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .background(if (rest) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(if (rest) "😴" else "💪", style = MaterialTheme.typography.bodyMedium)
                Text(
                    d.label?.take(3) ?: "D${d.dayIndex + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
