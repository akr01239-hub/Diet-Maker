package com.nutriai.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutriai.data.remote.dto.FoodDto
import com.nutriai.data.remote.dto.FoodLogEntry
import com.nutriai.ui.home.LogFoodViewModel
import com.nutriai.ui.theme.BrandGreen
import com.nutriai.ui.theme.BrandGreenDeep
import com.nutriai.ui.theme.BrandGreenLight

// ---------------------------------------------------------------------------
// Premium food-logging screen for NutriAI.
// One self-contained file. Reuses LogFoodViewModel. Foundation / Material3 only.
// ---------------------------------------------------------------------------

@Composable
fun LogScreen(
    modifier: Modifier = Modifier,
    viewModel: LogFoodViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingFood by remember { mutableStateOf<FoodDto?>(null) }
    var qtyText by remember { mutableStateOf("") }

    // Quantity dialog — set exact grams for THIS food before logging (accurate calories).
    pendingFood?.let { food ->
        QuantityDialog(
            food = food,
            qtyText = qtyText,
            onQtyChange = { qtyText = it.filter { c -> c.isDigit() } },
            onConfirm = {
                val g = qtyText.toDoubleOrNull()
                if (g != null && g > 0) viewModel.log(food, g)
                pendingFood = null
            },
            onDismiss = { pendingFood = null },
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp),
    ) {
        // 1. Header
        item { LogHeader(todayCount = state.today.size) }

        // 2. Meal-slot chips
        item {
            SlotChipRow(
                slots = viewModel.slots,
                selected = state.slot,
                onSelect = { viewModel.onSlot(it) },
            )
        }

        // 3. Search field + button
        item {
            SearchCard(
                query = state.query,
                onQuery = { viewModel.onQuery(it) },
                onSearch = { viewModel.search(state.query) },
            )
        }

        // 4. Status message
        state.message?.let { msg ->
            item { MessageBanner(msg) }
        }

        // 5. Loading
        if (state.loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandGreen)
                }
            }
        }

        // 6. Results
        if (state.results.isNotEmpty()) {
            item { SectionLabel("Search results", "Tap Add to log a portion") }
            items(state.results, key = { it.id }) { food ->
                ResultCard(
                    food = food,
                    onAdd = {
                        pendingFood = food
                        qtyText = food.typicalServingG.toInt().toString()
                    },
                )
            }
        }

        // 7. Today's log
        if (state.today.isNotEmpty()) {
            val totalKcal = state.today.sumOf { it.kcal }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "Today's log",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "${state.today.size} item${if (state.today.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TotalChip(kcal = totalKcal)
                }
            }
            items(state.today, key = { it.id }) { entry ->
                LogEntryCard(entry, onDelete = { viewModel.delete(entry.id) })
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------

@Composable
private fun LogHeader(todayCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(BrandGreenLight, BrandGreen, BrandGreenDeep),
                    ),
                )
                .padding(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Log food 🍽️",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    "Search, pick a portion, and track it in seconds.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                )
                if (todayCount > 0) {
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.22f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            "✓ $todayCount logged today",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Meal-slot chips
// ---------------------------------------------------------------------------

@Composable
private fun SlotChipRow(
    slots: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Meal",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            slots.forEach { slot ->
                SlotChip(
                    label = slot.replaceFirstChar { it.uppercase() },
                    selected = slot == selected,
                    onClick = { onSelect(slot) },
                )
            }
        }
    }
}

@Composable
private fun SlotChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) BrandGreen else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = fg,
        )
    }
}

// ---------------------------------------------------------------------------
// Search card
// ---------------------------------------------------------------------------

@Composable
private fun SearchCard(
    query: String,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search foods (local + USDA)…") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = BrandGreen,
                    cursorColor = BrandGreen,
                ),
            )
            Button(
                onClick = onSearch,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            ) { Text("Search") }
        }
    }
}

// ---------------------------------------------------------------------------
// Message banner
// ---------------------------------------------------------------------------

@Composable
private fun MessageBanner(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = BrandGreen.copy(alpha = 0.12f)),
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = BrandGreenDeep,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Section label
// ---------------------------------------------------------------------------

@Composable
private fun SectionLabel(title: String, subtitle: String) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// Result card
// ---------------------------------------------------------------------------

@Composable
private fun ResultCard(food: FoodDto, onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    food.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${food.kcal.toInt()} kcal · P ${food.proteinG.toInt()} / C ${food.carbG.toInt()} / F ${food.fatG.toInt()} per 100g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            ) { Text("Add") }
        }
    }
}

// ---------------------------------------------------------------------------
// Quantity dialog
// ---------------------------------------------------------------------------

@Composable
private fun QuantityDialog(
    food: FoodDto,
    qtyText: String,
    onQtyChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                "How much ${food.name}?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "${food.kcal.toInt()} kcal per 100 g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = onQtyChange,
                    label = { Text("Grams") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(18.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = BrandGreen,
                        focusedLabelColor = BrandGreen,
                        cursorColor = BrandGreen,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                val g = qtyText.toDoubleOrNull() ?: 0.0
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(BrandGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        "= ${(food.kcal * g / 100).toInt()} kcal · ${(food.proteinG * g / 100).toInt()} g protein",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreenDeep,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// ---------------------------------------------------------------------------
// Today's log entry card
// ---------------------------------------------------------------------------

private fun slotEmoji(slot: String): String = when (slot.lowercase()) {
    "breakfast" -> "🍳"
    "midmorning" -> "🍎"
    "lunch" -> "🥗"
    "eveningsnack" -> "☕"
    "dinner" -> "🍽️"
    "bedtime" -> "🥛"
    else -> "🍴"
}

@Composable
private fun LogEntryCard(entry: FoodLogEntry, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Leading meal badge.
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrandGreen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) { Text(slotEmoji(entry.mealSlot), style = MaterialTheme.typography.titleLarge) }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    entry.foodName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    MetaPill("${entry.grams.toInt()} g")
                    MetaPill(entry.mealSlot.replaceFirstChar { it.uppercase() })
                }
                Text(
                    "P ${entry.proteinG.toInt()} · C ${entry.carbG.toInt()} · F ${entry.fatG.toInt()} g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${entry.kcal.toInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreenDeep,
                )
                Text("kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onDelete() }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        "Remove",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BrandGreen.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = BrandGreenDeep,
        )
    }
}

// ---------------------------------------------------------------------------
// Daily total chip
// ---------------------------------------------------------------------------

@Composable
private fun TotalChip(kcal: Double) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(colors = listOf(BrandGreen, BrandGreenDeep)),
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${kcal.toInt()}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                " kcal",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}
