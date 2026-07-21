package com.nutriai.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nutriai.data.AppRepository
import java.io.File
import com.nutriai.data.remote.dto.ReportDay
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

    /** Downloads the server-generated PDF and opens the Android share sheet. */
    fun sharePdf(context: Context, onError: (String) -> Unit) {
        viewModelScope.launch {
            val r = repository.reportPdfBytes()
            val bytes = r.getOrNull()
            if (bytes == null || bytes.isEmpty()) {
                onError(r.exceptionOrNull()?.message ?: "Could not download the PDF")
                return@launch
            }
            try {
                val file = File(context.cacheDir, "nutriai-weekly.pdf")
                file.writeBytes(bytes)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(intent, "Share weekly report")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (e: Exception) {
                onError(e.message ?: "Could not open the PDF")
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
    val context = LocalContext.current
    var pdfMsg by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Weekly report",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        item {
            Button(
                onClick = { pdfMsg = null; viewModel.sharePdf(context) { pdfMsg = it } },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("📄  Download / Share PDF")
            }
        }
        pdfMsg?.let { msg ->
            item { Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
        }
        if (state.loading) {
            item { CircularProgressIndicator() }
        }
        state.error?.let { err ->
            item { Text(err, color = MaterialTheme.colorScheme.error) }
        }
        state.report?.let { report ->
            item { SummaryCard(report) }
            items(report.days) { day -> DayCard(day) }
            item {
                Text(report.disclaimer, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SummaryCard(report: WeeklyReport) {
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
}

@Composable
private fun DayCard(day: ReportDay) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(day.date, style = MaterialTheme.typography.titleSmall)
            Text(
                "${day.kcal.toInt()} kcal · ${day.proteinG.toInt()} g protein",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun summaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
