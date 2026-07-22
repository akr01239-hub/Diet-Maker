package com.nutriai.ui.cycle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.nutriai.data.remote.dto.Cycle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

data class CycleUiState(val loading: Boolean = true, val cycle: Cycle? = null, val submitting: Boolean = false)

@HiltViewModel
class CycleViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CycleUiState())
    val state: StateFlow<CycleUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            val c = repository.cycle().getOrNull()
            _state.value = _state.value.copy(loading = false, cycle = c)
        }
    }

    /** Logs a period that started `daysAgo` days ago (0 = today), then refreshes. */
    fun logPeriod(daysAgo: Int) {
        _state.value = _state.value.copy(submitting = true)
        viewModelScope.launch {
            val iso = LocalDate.now().minusDays(daysAgo.toLong())
                .atTime(12, 0).atOffset(ZoneOffset.UTC).toInstant().toString()
            repository.logPeriod(iso)
            val c = repository.cycle().getOrNull()
            _state.value = _state.value.copy(submitting = false, cycle = c)
        }
    }
}

@Composable
fun CycleSection(modifier: Modifier = Modifier, viewModel: CycleViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cycle = state.cycle ?: return
    if (!cycle.applicable) return

    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (cycle.needsSetup) {
                Text("🌸 Track your cycle", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Log when your last period started and I'll tailor your diet, workouts and yoga to each phase.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text("When did it start?", style = MaterialTheme.typography.labelMedium)
                DayAgoChips(enabled = !state.submitting) { viewModel.logPeriod(it) }
            } else {
                Text(
                    "🌸 ${cycle.phaseLabel ?: "Your cycle"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    buildString {
                        cycle.cycleDay?.let { append("Day $it") }
                        cycle.nextPeriodInDays?.let { append("  ·  next period ~$it day${if (it == 1) "" else "s"}") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                )

                if (cycle.shouldPromptPeriod) {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Your period is due around now — has it started?", style = MaterialTheme.typography.bodyMedium)
                            Button(onClick = { viewModel.logPeriod(0) }, enabled = !state.submitting) {
                                Text("Yes — log today")
                            }
                        }
                    }
                }

                cycle.guidance?.let { g -> CyclePhaseTips(g.summary, g.dietTips, g.exerciseTips, g.yogaTips) }

                var showLog by remember { mutableStateOf(false) }
                Text(
                    if (showLog) "Cancel" else "＋ Log a new period start",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { showLog = !showLog },
                )
                if (showLog) DayAgoChips(enabled = !state.submitting) { viewModel.logPeriod(it); showLog = false }
            }
        }
    }
}

@Composable
private fun DayAgoChips(enabled: Boolean, onPick: (Int) -> Unit) {
    val options = listOf("Today" to 0, "3 days ago" to 3, "1 week ago" to 7, "2 weeks ago" to 14)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (label, days) ->
            AssistChip(onClick = { if (enabled) onPick(days) }, label = { Text(label) })
        }
    }
}

@Composable
private fun CyclePhaseTips(
    summary: String,
    diet: List<String>,
    exercise: List<String>,
    yoga: List<String>,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable { expanded = !expanded },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            summary.ifBlank { "This phase's diet, exercise & yoga" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Text(if (expanded) "▲" else "▼", style = MaterialTheme.typography.bodyMedium)
    }
    if (expanded) {
        TipGroup("🥗 Diet", diet)
        TipGroup("🏃 Exercise", exercise)
        TipGroup("🧘 Yoga", yoga)
    }
}

@Composable
private fun TipGroup(title: String, tips: List<String>) {
    if (tips.isEmpty()) return
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    tips.forEach { Text("•  $it", style = MaterialTheme.typography.bodySmall) }
}
