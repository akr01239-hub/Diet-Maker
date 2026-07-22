package com.nutriai.ui.onboarding

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutriai.data.remote.dto.ProfileUpsertRequest
import com.nutriai.data.remote.dto.SensitiveData

private val SEX = listOf("male", "female")
private val ACTIVITY = listOf("sedentary", "light", "moderate", "active", "veryactive")
private val GOAL = listOf("lose", "maintain", "gain")
private val DIET = listOf("veg", "eggetarian", "nonveg", "vegan", "jain", "keto", "highprotein")
private val CONDITIONS = listOf("diabetes", "hypertension", "kidney_disease", "thyroid", "pcos", "heart_disease")

@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("male") }
    var activity by remember { mutableStateOf("moderate") }
    var goal by remember { mutableStateOf("lose") }
    var diet by remember { mutableStateOf("nonveg") }
    val conditions = remember { mutableStateListOf<String>() }
    var fastDay by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Your health profile", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text("We use this to compute safe, personalised targets.", style = MaterialTheme.typography.bodyMedium)

        numberField(height, { height = it }, "Height (cm)")
        numberField(weight, { weight = it }, "Current weight (kg)")
        numberField(target, { target = it }, "Target weight (kg)")
        OutlinedTextField(dob, { dob = it }, label = { Text("Date of birth (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Label("Sex")
        SingleChoiceChips(SEX, sex) { sex = it }
        Label("Activity level")
        SingleChoiceChips(ACTIVITY, activity) { activity = it }
        Label("Goal")
        SingleChoiceChips(GOAL, goal) { goal = it }
        Label("Diet")
        SingleChoiceChips(DIET, diet) { diet = it }
        Label("Goal tip: choose \"gain\" to build muscle (higher protein).")
        Label("Conditions (optional)")
        MultiChoiceChips(CONDITIONS, conditions)

        Label("Weekly fasting day (optional)")
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val fastLabels = listOf("None", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            fastLabels.forEachIndexed { idx, lbl ->
                val value = if (idx == 0) null else idx - 1
                FilterChip(selected = fastDay == value, onClick = { fastDay = value }, label = { Text(lbl) })
            }
        }

        if (state.error != null) {
            Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = {
                val h = height.toDoubleOrNull()
                val w = weight.toDoubleOrNull()
                val t = target.toDoubleOrNull()
                if (h != null && w != null && t != null && dob.isNotBlank()) {
                    viewModel.save(
                        ProfileUpsertRequest(
                            heightCm = h,
                            activityLevel = activity,
                            goal = goal,
                            dietType = diet,
                            sensitive = SensitiveData(
                                sex = sex,
                                dob = dob.trim(),
                                currentWeightKg = w,
                                targetWeightKg = t,
                                conditions = conditions.toList(),
                                fastDayOfWeek = fastDay,
                            ),
                        ),
                        onDone,
                    )
                }
            },
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.loading) CircularProgressIndicator(Modifier.padding(4.dp)) else Text("See my plan")
        }
    }
}

@Composable
private fun numberField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun SingleChoiceChips(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { opt ->
            FilterChip(selected = opt == selected, onClick = { onSelect(opt) }, label = { Text(opt) })
        }
    }
}

@Composable
private fun MultiChoiceChips(options: List<String>, selected: MutableList<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { opt ->
            val isSel = selected.contains(opt)
            FilterChip(
                selected = isSel,
                onClick = { if (isSel) selected.remove(opt) else selected.add(opt) },
                label = { Text(opt.replace('_', ' ')) },
            )
        }
    }
}
