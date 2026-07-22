package com.nutriai.ui.grocery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nutriai.data.AppRepository
import com.nutriai.data.remote.dto.Grocery
import com.nutriai.data.remote.dto.GroceryCategory
import com.nutriai.ui.theme.BrandGreen
import com.nutriai.ui.theme.BrandGreenDeep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroceryState(
    val loading: Boolean = true,
    val grocery: Grocery? = null,
    val error: String? = null,
)

@HiltViewModel
class GroceryViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(GroceryState())
    val state: StateFlow<GroceryState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val r = repository.grocery()
            _state.value = if (r.isSuccess) {
                GroceryState(loading = false, grocery = r.getOrNull())
            } else {
                GroceryState(loading = false, error = "Generate a plan first (Plan tab)")
            }
        }
    }
}

@Composable
fun GroceryScreen(
    modifier: Modifier = Modifier,
    viewModel: GroceryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val grocery = state.grocery

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item { HeroHeader(totalItems = grocery?.totalItems ?: 0) }

        if (state.loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandGreen)
                }
            }
        }
        state.error?.let { err ->
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Nothing to shop yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(err, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { viewModel.load() }, shape = RoundedCornerShape(20.dp)) { Text("Refresh") }
                    }
                }
            }
        }

        grocery?.categories?.forEach { cat ->
            item { CategoryCard(cat) }
        }
    }
}

@Composable
private fun HeroHeader(totalItems: Int) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(BrandGreen, BrandGreenDeep))).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("🛒 This week's grocery", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "$totalItems raw ingredients to buy for your Sun → Sat plan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(cat: GroceryCategory) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(cat.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BrandGreen)

            val border = MaterialTheme.colorScheme.outlineVariant
            // Bordered grid: outer border + horizontal dividers between rows + vertical
            // dividers between columns. Column weights sum to 1 so it fills the full width.
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, border, RoundedCornerShape(10.dp)),
            ) {
                GroceryRow(
                    item = "Item",
                    amount = "Amount",
                    times = "×wk",
                    kcal = "kcal",
                    border = border,
                    header = true,
                )
                cat.items.forEach { line ->
                    HorizontalDivider(thickness = 1.dp, color = border)
                    GroceryRow(
                        item = line.name,
                        amount = if (line.unit == "pcs") "${line.qty} pcs" else "${line.qty} g",
                        times = "×${line.meals}",
                        kcal = if (line.kcal > 0) "${line.kcal}" else "—",
                        border = border,
                        header = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun GroceryRow(
    item: String,
    amount: String,
    times: String,
    kcal: String,
    border: Color,
    header: Boolean,
) {
    val cellStyle = MaterialTheme.typography.bodySmall
    val labelStyle = MaterialTheme.typography.labelSmall
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            item,
            Modifier.weight(0.40f).padding(horizontal = 8.dp, vertical = 8.dp),
            style = if (header) labelStyle else cellStyle,
            fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
            color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        Box(Modifier.width(1.dp).fillMaxHeight().background(border))
        Text(
            amount,
            Modifier.weight(0.24f).padding(horizontal = 6.dp, vertical = 8.dp),
            style = if (header) labelStyle else cellStyle,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else BrandGreenDeep,
        )
        Box(Modifier.width(1.dp).fillMaxHeight().background(border))
        Text(
            times,
            Modifier.weight(0.15f).padding(horizontal = 4.dp, vertical = 8.dp),
            style = if (header) labelStyle else cellStyle,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(Modifier.width(1.dp).fillMaxHeight().background(border))
        Text(
            kcal,
            Modifier.weight(0.21f).padding(horizontal = 4.dp, vertical = 8.dp),
            style = if (header) labelStyle else cellStyle,
            fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
}
