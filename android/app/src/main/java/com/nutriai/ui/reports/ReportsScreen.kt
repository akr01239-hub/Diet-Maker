package com.nutriai.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nutriai.data.AppRepository
import com.nutriai.data.remote.dto.WeeklyReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportsState(
    val loading: Boolean = true,
    val report: WeeklyReport? = null,
    val error: String? = null,
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val r = repository.weeklyReport()
            _state.value = if (r.isSuccess) {
                ReportsState(loading = false, report = r.getOrNull())
            } else {
                ReportsState(loading = false, error = r.exceptionOrNull()?.message ?: "Failed to load")
            }
        }
    }
}

@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Weekly report",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        when {
            state.loading -> CircularProgressIndicator()
            state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
            state.report != null -> ReportBody(state.report!!)
        }
    }
}

@Composable
private fun ReportBody(report: WeeklyReport) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            summaryRow("Calorie target", report.targets?.dailyKcal?.let { "${it.toInt()} kcal" } ?: "—")
            summaryRow("Protein target", report.targets?.proteinG?.let { "${it.toInt()} g" } ?: "—")
            summaryRow("BMI", report.bmi?.toString() ?: "—")
            summaryRow("Latest weight", report.latestWeightKg?.let { "$it kg" } ?: "—")
            summaryRow("Weight change", report.weightDeltaKg?.let { "$it kg" } ?: "—")
            summaryRow("Avg calories", report.avgKcal?.let { "${it.toInt()} kcal" } ?: "—")
            summaryRow("Adherence", report.adherencePct?.let { "${it.toInt()}%" } ?: "—")
        }
    }

    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(report.days) { day ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(day.date, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${day.kcal.toInt()} kcal · ${day.proteinG.toInt()} g protein",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    Text(report.disclaimer, style = MaterialTheme.typography.labelSmall)
}

@Composable
private fun summaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
