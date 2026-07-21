package com.nutriai.ui.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutriai.domain.model.HealthStatus
import com.nutriai.ui.theme.NutriAiTheme

@Composable
fun HealthScreen(
    modifier: Modifier = Modifier,
    viewModel: HealthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HealthContent(state = state, onRetry = viewModel::refresh, modifier = modifier)
}

@Composable
private fun HealthContent(
    state: HealthUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "NutriAI",
            style = MaterialTheme.typography.headlineMedium,
        )

        when (state) {
            is HealthUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(24.dp)
                        .size(40.dp),
                )
                Text("Connecting to API…")
            }

            is HealthUiState.Connected -> {
                Text(
                    text = "API connected ✓",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    text = "${state.status.service} v${state.status.version} · AI: ${state.status.aiProvider}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            is HealthUiState.Error -> {
                Text(
                    text = "Could not connect",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Retry")
                }
            }
        }

        Text(
            text = "Educational guidance, not medical advice — consult a professional.",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(PaddingValues(top = 40.dp)),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HealthContentConnectedPreview() {
    NutriAiTheme(dynamicColor = false) {
        HealthContent(
            state = HealthUiState.Connected(
                HealthStatus(service = "nutriai-api", version = "0.1.0", aiProvider = "rules"),
            ),
            onRetry = {},
        )
    }
}
