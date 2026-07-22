package com.nutriai.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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
    val tabs = listOf("🏠" to "Home", "🍽️" to "Plan", "➕" to "Log", "💬" to "Coach", "☰" to "More")

    Scaffold(
        bottomBar = {
            Column {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                ) {
                    tabs.forEachIndexed { i, (icon, label) ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Text(icon) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> DashboardTab(onLogout = onLogout, onCompleteProfile = onCompleteProfile)
                1 -> com.nutriai.ui.calendar.CalendarScreen(Modifier.fillMaxSize())
                2 -> com.nutriai.ui.log.LogScreen(Modifier.fillMaxSize())
                3 -> com.nutriai.ui.coach.CoachScreen(Modifier.fillMaxSize())
                else -> MoreScreen(Modifier.fillMaxSize())
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
    var showDelete by remember { mutableStateOf(false) }

    // Refresh every time the Home tab becomes visible (e.g. after logging food).
    LaunchedEffect(Unit) { viewModel.refresh() }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete account?") },
            text = { Text("This permanently deletes your account and all your data from our servers. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    viewModel.deleteAccount(onLogout)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }

    when {
        state.dashboard != null -> com.nutriai.ui.dashboard.PremiumDashboard(
            dashboard = state.dashboard!!,
            greetingName = state.firstName,
            onAddWater = { viewModel.logWater(250) },
            onCompleteProfile = onCompleteProfile,
            onLogout = { viewModel.logout(onLogout) },
            onDeleteAccount = { showDelete = true },
            modifier = Modifier.fillMaxSize(),
        )
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(state.error ?: "Couldn't load your dashboard.", color = MaterialTheme.colorScheme.error)
            Button(onClick = onCompleteProfile, modifier = Modifier.fillMaxWidth()) { Text("Complete profile") }
            OutlinedButton(onClick = { viewModel.logout(onLogout) }, modifier = Modifier.fillMaxWidth()) { Text("Log out") }
        }
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

    // Full nutrition breakdown for today.
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Nutrition today", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            nutritionRow("Calories", "${d.calories.consumed.toInt()} kcal")
            nutritionRow("Protein", "${d.protein.consumed?.toInt() ?: 0} g")
            nutritionRow("Carbs", "${d.macros.carbG.toInt()} g")
            nutritionRow("Fat", "${d.macros.fatG.toInt()} g")
            nutritionRow("Fiber", "${d.macros.fiberG.toInt()} g")
            nutritionRow("Sugar", "${d.macros.sugarG.toInt()} g")
            nutritionRow("Sodium", "${d.macros.sodiumMg.toInt()} mg")
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        statCard("Streak", "${d.streakDays} 🔥", Modifier.weight(1f))
        statCard("BMI", d.bmi?.let { it.toString() } ?: "—", Modifier.weight(1f))
    }
    d.weight.latestKg?.let { latest ->
        statCard("Weight", "$latest kg (${d.weight.deltaKg ?: 0.0} kg)", Modifier.fillMaxWidth())
    }

    // Free "how you'll look" forecast — projected weight/BMI over time.
    if (d.projection.size > 1) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Your journey ✨",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Projected at your safe, steady pace",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                d.projection.forEach { p ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(p.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            "${p.weightKg} kg · BMI ${p.bmi}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun nutritionRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
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
    val days = state.plan?.days.orEmpty()

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Your 7-day plan", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
        if (state.loading) item { CircularProgressIndicator() }

        if (days.isNotEmpty()) {
            item {
                Button(onClick = { viewModel.generate() }, modifier = Modifier.fillMaxWidth()) {
                    Text("🔄  Regenerate plan")
                }
            }
            days.forEach { day ->
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "${day.label ?: "Day ${day.dayIndex + 1}"}${day.date?.let { " · $it" } ?: ""}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                "${day.totals.kcal.toInt()} kcal · P ${day.totals.proteinG.toInt()}g",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
                items(day.meals) { meal ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(meal.slot.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            meal.items.forEach { item ->
                                Text("• ${item.name} — ${item.grams.toInt()} g (${item.kcal.toInt()} kcal)", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        } else if (!state.loading) {
            item {
                Text(state.error ?: "No plan yet — generate your personalised 7-day plan.")
            }
            item {
                Button(onClick = { viewModel.generate() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Generate my 7-day plan")
                }
            }
        }
    }
}

@Composable
private fun LogTab(viewModel: LogFoodViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingFood by remember { mutableStateOf<com.nutriai.data.remote.dto.FoodDto?>(null) }
    var qtyText by remember { mutableStateOf("") }

    // Quantity dialog — set the exact grams for THIS food before logging (accurate calories).
    pendingFood?.let { food ->
        AlertDialog(
            onDismissRequest = { pendingFood = null },
            title = { Text("How much ${food.name}?") },
            text = {
                Column {
                    Text("${food.kcal.toInt()} kcal per 100 g", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it.filter { c -> c.isDigit() } },
                        label = { Text("Grams") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    val g = qtyText.toDoubleOrNull() ?: 0.0
                    Text(
                        "= ${(food.kcal * g / 100).toInt()} kcal · ${(food.proteinG * g / 100).toInt()} g protein",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val g = qtyText.toDoubleOrNull()
                    if (g != null && g > 0) viewModel.log(food, g)
                    pendingFood = null
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { pendingFood = null }) { Text("Cancel") } },
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Log food", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Meal", style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    viewModel.slots.forEach { s ->
                        FilterChip(selected = s == state.slot, onClick = { viewModel.onSlot(s) }, label = { Text(s) })
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQuery,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search foods (local + USDA)…") },
                    singleLine = true,
                )
                Button(onClick = { viewModel.search(state.query) }, modifier = Modifier.padding(start = 8.dp)) {
                    Text("Search")
                }
            }
        }
        state.message?.let { msg ->
            item { Text(msg, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
        }
        if (state.loading) item { CircularProgressIndicator() }

        if (state.results.isNotEmpty()) {
            item { Text("Results — tap Add", style = MaterialTheme.typography.labelLarge) }
            items(state.results) { food ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(food.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${food.kcal.toInt()} kcal · P ${food.proteinG.toInt()} · C ${food.carbG.toInt()} · F ${food.fatG.toInt()} /100g",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Button(onClick = {
                            pendingFood = food
                            qtyText = food.typicalServingG.toInt().toString()
                        }) { Text("+ Add") }
                    }
                }
            }
        }

        if (state.today.isNotEmpty()) {
            item {
                Text(
                    "Today's log (${state.today.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(state.today) { e ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${e.foodName} — ${e.grams.toInt()} g · ${e.mealSlot}", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${e.kcal.toInt()} kcal · P ${e.proteinG.toInt()}g · C ${e.carbG.toInt()}g · F ${e.fatG.toInt()}g",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
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
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start,
                ) {
                    Card(
                        modifier = Modifier.widthIn(max = 300.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (m.fromUser) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                    ) {
                        Text(
                            m.text,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (m.fromUser) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
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
        // Disclaimer shown ONCE here, not on every message.
        Text(
            "Educational guidance, not medical advice — consult a professional.",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
    }
}
