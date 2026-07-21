package com.nutriai.ui.barcode

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
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

    /** Called by the camera when a barcode is detected — guarded against repeats. */
    fun onScanned(code: String) {
        val digits = code.filter { it.isDigit() }
        val st = _state.value
        if (digits.isBlank() || st.loading || st.food != null || digits == st.code) return
        _state.value = st.copy(code = digits)
        lookup()
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
        val food = _state.value.food ?: return
        val grams = _state.value.grams.toDoubleOrNull() ?: 100.0
        val slot = _state.value.slot
        _state.value = _state.value.copy(loading = true, message = null)
        viewModelScope.launch {
            val r = repository.logBarcodeFood(slot, food, grams)
            _state.value = _state.value.copy(
                loading = false,
                message = if (r.isSuccess) {
                    "Logged ${grams.toInt()} g of ${food.name} to $slot"
                } else {
                    r.exceptionOrNull()?.message ?: "Could not log"
                },
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScreen(
    modifier: Modifier = Modifier,
    viewModel: BarcodeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Scan barcode",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        if (cameraPermission.status.isGranted) {
            Card(Modifier.fillMaxWidth()) {
                CameraScanner(
                    onBarcode = viewModel::onScanned,
                    modifier = Modifier.fillMaxWidth().height(230.dp),
                )
            }
            Text("Point your camera at a barcode", style = MaterialTheme.typography.bodySmall)
        } else {
            OutlinedButton(
                onClick = { cameraPermission.launchPermissionRequest() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("📷  Enable camera to scan")
            }
        }

        OutlinedTextField(
            value = state.code,
            onValueChange = viewModel::onCode,
            label = { Text("Or enter barcode number") },
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

/** CameraX preview + ML Kit barcode analysis. Calls [onBarcode] with the first detected value. */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraScanner(onBarcode: (String) -> Unit, modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            val scanner = BarcodeScanning.getClient()
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { proxy ->
                    val media = proxy.image
                    if (media != null) {
                        val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                barcodes.firstOrNull()?.rawValue?.let(onBarcode)
                            }
                            .addOnCompleteListener { proxy.close() }
                    } else {
                        proxy.close()
                    }
                }
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                } catch (_: Exception) {
                    // Camera unavailable — the manual-entry fallback still works.
                }
            }, executor)
            previewView
        },
    )
}
