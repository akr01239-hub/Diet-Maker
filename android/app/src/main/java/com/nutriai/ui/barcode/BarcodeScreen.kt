package com.nutriai.ui.barcode

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nutriai.data.AppRepository
import com.nutriai.data.remote.dto.BarcodeFood
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BarcodeState(
    val code: String = "",
    val food: BarcodeFood? = null,
    val grams: String = "100",
    val slot: String = "breakfast",
    val loading: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class BarcodeViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(BarcodeState())
    val state: StateFlow<BarcodeState> = _state.asStateFlow()

    val slots = listOf("breakfast", "midmorning", "lunch", "eveningsnack", "dinner", "bedtime")

    fun onCode(c: String) {
        _state.value = _state.value.copy(code = c.filter { it.isDigit() })
    }

    fun onGrams(g: String) {
        _state.value = _state.value.copy(grams = g.filter { it.isDigit() })
    }

    fun onSlot(s: String) {
        _state.value = _state.value.copy(slot = s)
    }

    fun lookup() {
        val code = _state.value.code
        if (code.isBlank()) return
        _state.value = _state.value.copy(loading = true, message = null, food = null)
        viewModelScope.launch {
            val r = repository.barcode(code)
            _state.value = if (r.isSuccess) {
                _state.value.copy(loading = false, food = r.getOrNull())
            } else {
                _state.value.copy(
                    loading = false,
                    food = null,
                    message = r.exceptionOrNull()?.message ?: "No food found for that barcode",
                )
            }
        }
    }

    fun logIt() {
        val code = _state.value.code
        val grams = _state.value.grams.toDoubleOrNull() ?: 100.0
        val slot = _state.value.slot
        _state.value = _state.value.copy(loading = true, message = null)
        viewModelScope.launch {
            val r = repository.logFood(slot, "barcode:$code", grams)
            _state.value = _state.value.copy(
                loading = false,
                message = if (r.isSuccess) {
                    "Logged ${grams.toInt()} g to $slot"
                } else {
                    r.exceptionOrNull()?.message ?: "Could not log"
                },
            )
        }
    }
}

@Composable
fun BarcodeScreen(
    modifier: Modifier = Modifier,
    viewModel: BarcodeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Scan barcode (enter number)",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        OutlinedTextField(
            value = state.code,
            onValueChange = viewModel::onCode,
            label = { Text("Barcode number") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { viewModel.lookup() },
            enabled = state.code.isNotBlank() && !state.loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Look up")
        }

        if (state.loading) CircularProgressIndicator()

        state.food?.let { food ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(food.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${food.per100g.kcal.toInt()} kcal / 100 g",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            OutlinedTextField(
                value = state.grams,
                onValueChange = viewModel::onGrams,
                label = { Text("Grams") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Meal", style = MaterialTheme.typography.labelLarge)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                viewModel.slots.forEach { s ->
                    FilterChip(
                        selected = s == state.slot,
                        onClick = { viewModel.onSlot(s) },
                        label = { Text(s) },
                    )
                }
            }

            Button(
                onClick = { viewModel.logIt() },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Log it")
            }
        }

        state.message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
