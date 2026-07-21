package com.nutriai.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nutriai.ui.badges.BadgesScreen
import com.nutriai.ui.barcode.BarcodeScreen
import com.nutriai.ui.checkin.CheckinScreen
import com.nutriai.ui.family.FamilyScreen
import com.nutriai.ui.grocery.GroceryScreen
import com.nutriai.ui.reports.ReportsScreen

private data class MoreItem(val key: String, val icon: String, val label: String)

private val MORE_ITEMS = listOf(
    MoreItem("checkin", "⚖️", "Weekly check-in"),
    MoreItem("barcode", "📷", "Scan barcode"),
    MoreItem("grocery", "🛒", "Grocery list"),
    MoreItem("reports", "📄", "Weekly report"),
    MoreItem("badges", "🏅", "Achievements"),
    MoreItem("family", "👨‍👩‍👧", "Family"),
)

@Composable
fun MoreScreen(modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf<String?>(null) }

    when (selected) {
        null -> MoreMenu(modifier) { selected = it }
        else -> Column(modifier.fillMaxSize()) {
            TextButton(onClick = { selected = null }, modifier = Modifier.padding(4.dp)) {
                Text("← More")
            }
            when (selected) {
                "checkin" -> CheckinScreen(Modifier.fillMaxSize())
                "barcode" -> BarcodeScreen(Modifier.fillMaxSize())
                "grocery" -> GroceryScreen(Modifier.fillMaxSize())
                "reports" -> ReportsScreen(Modifier.fillMaxSize())
                "badges" -> BadgesScreen(Modifier.fillMaxSize())
                "family" -> FamilyScreen(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun MoreMenu(modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("More", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        MORE_ITEMS.forEach { item ->
            Card(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(item.key) },
            ) {
                Text(
                    "${item.icon}   ${item.label}",
                    modifier = Modifier.padding(18.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
