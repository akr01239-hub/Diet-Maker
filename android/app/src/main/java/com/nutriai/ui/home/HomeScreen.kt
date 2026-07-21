package com.nutriai.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutriai.data.remote.dto.Dashboard

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onCompleteProfile: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("🏠" to "Home", "🍽️" to "Plan", "💬" to "Coach")

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, (icon, label) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(icon) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> DashboardTab(onLogout = onLogout, onCompleteProfile = onCompleteProfile)
                1 -> PlanTab()
                else -> ChatTab()
            }
        }
    }
}

@Composable
private fun DashboardTab(
    onLogout: () -> Unit,
    onCompleteProfile: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Dashboard", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)

        when {
            state.loading -> CircularProgressIndicator()
            state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
            state.dashboard != null -> DashboardBody(state.dashboard!!, onCompleteProfile, { viewModel.logWater(250) })
        }

        OutlinedButton(onClick = { viewModel.logout(onLogout) }, modifier = Modifier.fillMaxWidth()) {
            Text("Log out")
        }
        Text(
            "Educational guidance, not medical advice — consult a professional.",
            style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DashboardBody(d: Dashboard, onCompleteProfile: () -> Unit, onAddWater: () -> Unit) {
    if (d.calories.target == null) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Finish setting up", style = MaterialTheme.typography.titleMedium)
                Text("Complete your health profile to get personalised targets.")
                Button(onClick = onCompleteProfile) { Text("Complete profile") }
            }
        }
        return
    }

    metricCard("Calories", "${d.calories.consumed.toInt()} / ${d.calories.target?.toInt()} kcal", d.calories.percent)
    metricCard("Protein", "${d.protein.consumed?.toInt() ?: 0} / ${d.protein.target?.toInt() ?: 0} g", d.protein.percent)
    metricCard("Water", "${d.water.consumedMl?.toInt() ?: 0} / ${d.water.targetMl?.toInt() ?: 0} ml", d.water.percent)
    Button(onClick = onAddWater) { Text("+ 250 ml water") }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        statCard("Streak", "${d.streakDays} 🔥", Modifier.weight(1f))
        statCard("BMI", d.bmi?.let { it.toString() } ?: "—", Modifier.weight(1f))
    }
    d.weight.latestKg?.let { latest ->
        statCard("Weight", "$latest kg (${d.weight.deltaKg ?: 0.0} kg)", Modifier.fillMaxWidth())
    }
}

@Composable
private fun metricCard(title: String, value: String, percent: Double?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.titleLarge)
            if (percent != null) {
                LinearProgressIndicator(
                    progress = { (percent / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun statCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun PlanTab(viewModel: PlanViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Today's plan", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        when {
            state.loading -> CircularProgressIndicator()
            state.plan?.days?.isNotEmpty() == true -> {
                val day = state.plan!!.days.first()
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(day.meals) { meal ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(meal.slot.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall)
                                meal.items.forEach { item ->
                                    Text("• ${item.name} — ${item.grams.toInt()} g (${item.kcal.toInt()} kcal)")
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                Text(state.error ?: "No plan yet.")
                Button(onClick = { viewModel.generate() }) { Text("Generate my 7-day plan") }
            }
        }
    }
}

@Composable
private fun ChatTab(viewModel: ChatViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.messages) { m ->
                Card(
                    Modifier.fillMaxWidth(),
                ) {
                    Text(
                        m.text,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about a food…") },
                singleLine = true,
            )
            Button(
                onClick = { viewModel.send(input); input = "" },
                enabled = !state.sending && input.isNotBlank(),
                modifier = Modifier.padding(start = 8.dp),
            ) { Text("Send") }
        }
    }
}
