package com.nutriai.ui.checkin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nutriai.data.AppRepository
import com.nutriai.data.remote.dto.CheckinDto
import com.nutriai.data.remote.dto.CheckinRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.round

data class CheckinState(
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val checkins: List<CheckinDto> = emptyList(),
    val message: String? = null,
)

@HiltViewModel
class CheckinViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CheckinState())
    val state: StateFlow<CheckinState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = _state.value.copy(loading = true)
        viewModelScope.launch {
            val r = repository.checkins()
            _state.value = _state.value.copy(
                loading = false,
                checkins = r.getOrDefault(emptyList()),
            )
        }
    }

    fun submit(
        weightKg: Double,
        waistCm: Double?,
        energy: Int?,
        sleepHours: Double?,
        mood: Int?,
        notes: String?,
    ) {
        _state.value = _state.value.copy(submitting = true, message = null)
        viewModelScope.launch {
            val r = repository.createCheckin(
                CheckinRequest(
                    weightKg = weightKg,
                    waistCm = waistCm,
                    energy = energy,
                    sleepHours = sleepHours,
                    mood = mood,
                    notes = notes,
                ),
            )
            if (r.isSuccess) {
                _state.value = _state.value.copy(submitting = false, message = "Check-in saved")
                load()
            } else {
                _state.value = _state.value.copy(
                    submitting = false,
                    message = r.exceptionOrNull()?.message ?: "Could not save check-in",
                )
            }
        }
    }
}

@Composable
fun CheckinScreen(
    modifier: Modifier = Modifier,
    viewModel: CheckinViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var weight by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var sleep by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var energy by remember { mutableStateOf<Int?>(null) }
    var mood by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Weekly check-in",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("Weight (kg)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = waist,
            onValueChange = { waist = it },
            label = { Text("Waist (cm, optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Energy", style = MaterialTheme.typography.labelLarge)
        RatingChips(selected = energy) { energy = it }

        Text("Mood", style = MaterialTheme.typography.labelLarge)
        RatingChips(selected = mood) { mood = it }

        OutlinedTextField(
            value = sleep,
            onValueChange = { sleep = it },
            label = { Text("Sleep hours (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                val w = weight.toDoubleOrNull()
                if (w != null) {
                    viewModel.submit(
                        weightKg = w,
                        waistCm = waist.toDoubleOrNull(),
                        energy = energy,
                        sleepHours = sleep.toDoubleOrNull(),
                        mood = mood,
                        notes = notes.trim().ifBlank { null },
                    )
                }
            },
            enabled = weight.isNotBlank() && !state.submitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.submitting) CircularProgressIndicator(Modifier.padding(4.dp)) else Text("Save check-in")
        }

        state.message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }

        if (state.loading) {
            CircularProgressIndicator()
        } else {
            WeightTrend(state.checkins)

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.checkins) { c ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(c.date, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Weight: ${c.measurements?.weightKg?.let { "$it kg" } ?: "—"}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "Energy: ${c.energy ?: "—"}  ·  Mood: ${c.mood ?: "—"}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingChips(selected: Int?, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (1..5).forEach { n ->
            FilterChip(
                selected = n == selected,
                onClick = { onSelect(n) },
                label = { Text(n.toString()) },
            )
        }
    }
}

@Composable
private fun WeightTrend(checkins: List<CheckinDto>) {
    if (checkins.isEmpty()) return
    val latest = checkins.first().measurements?.weightKg
    val first = checkins.last().measurements?.weightKg

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Weight trend", style = MaterialTheme.typography.titleMedium)
            if (latest != null) {
                Text("Latest: $latest kg", style = MaterialTheme.typography.bodyMedium)
            }
            if (checkins.size >= 2 && latest != null && first != null) {
                val delta = round((latest - first) * 10.0) / 10.0
                Text(
                    "Change since first: ${if (delta > 0) "+$delta" else "$delta"} kg",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
