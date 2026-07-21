package com.nutriai.ui.family

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nutriai.data.AppRepository
import com.nutriai.data.remote.dto.CalcResult
import com.nutriai.data.remote.dto.FamilyMemberDto
import com.nutriai.data.remote.dto.FamilyMemberRequest
import com.nutriai.data.remote.dto.SensitiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val SEX = listOf("male", "female")
private val ACTIVITY = listOf("sedentary", "light", "moderate", "active", "veryactive")
private val GOAL = listOf("lose", "maintain", "gain")
private val DIET = listOf("veg", "eggetarian", "nonveg", "vegan", "jain", "keto", "highprotein")

data class FamilyState(
    val members: List<FamilyMemberDto> = emptyList(),
    val loading: Boolean = true,
    val message: String? = null,
    val selectedCalc: CalcResult? = null,
)

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(FamilyState())
    val state: StateFlow<FamilyState> = _state.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        _state.value = _state.value.copy(loading = true)
        viewModelScope.launch {
            val r = repository.family()
            _state.value = if (r.isSuccess) {
                _state.value.copy(loading = false, members = r.getOrDefault(emptyList()))
            } else {
                _state.value.copy(loading = false, message = r.exceptionOrNull()?.message ?: "Failed to load family")
            }
        }
    }

    fun addMember(req: FamilyMemberRequest) {
        _state.value = _state.value.copy(loading = true, message = null)
        viewModelScope.launch {
            val r = repository.addFamilyMember(req)
            if (r.isSuccess) {
                _state.value = _state.value.copy(message = "Added ${req.firstName}")
                reload()
            } else {
                _state.value = _state.value.copy(
                    loading = false,
                    message = r.exceptionOrNull()?.message ?: "Could not add member",
                )
            }
        }
    }

    fun viewCalc(id: String) {
        viewModelScope.launch {
            val r = repository.familyCalc(id)
            _state.value = _state.value.copy(selectedCalc = r.getOrNull())
        }
    }
}

@Composable
fun FamilyScreen(
    modifier: Modifier = Modifier,
    viewModel: FamilyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var firstName by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("male") }
    var activity by remember { mutableStateOf("moderate") }
    var goal by remember { mutableStateOf("lose") }
    var diet by remember { mutableStateOf("nonveg") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Family", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)

        if (state.loading) CircularProgressIndicator()

        state.members.forEach { member ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.viewCalc(member.id) },
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(member.firstName, style = MaterialTheme.typography.titleMedium)
                    member.relation?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }

        state.selectedCalc?.let { calc ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Targets", style = MaterialTheme.typography.titleSmall)
                    Text("BMI: ${calc.bmi}")
                    Text("TDEE: ${calc.tdee.toInt()} kcal")
                    Text("Daily: ${calc.dailyKcal.toInt()} kcal")
                    Text("Protein: ${calc.proteinG.toInt()} g")
                    if (calc.requiresSupervision) {
                        Text(
                            "Requires professional supervision.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        Text("Add member", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = relation,
            onValueChange = { relation = it },
            label = { Text("Relation") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        NumberField(height, { height = it }, "Height (cm)")
        Label("Sex")
        SingleChoiceChips(SEX, sex) { sex = it }
        OutlinedTextField(
            value = dob,
            onValueChange = { dob = it },
            label = { Text("Date of birth (YYYY-MM-DD)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        NumberField(weight, { weight = it }, "Current weight (kg)")
        NumberField(target, { target = it }, "Target weight (kg)")
        Label("Activity level")
        SingleChoiceChips(ACTIVITY, activity) { activity = it }
        Label("Goal")
        SingleChoiceChips(GOAL, goal) { goal = it }
        Label("Diet")
        SingleChoiceChips(DIET, diet) { diet = it }

        state.message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }

        val h = height.toDoubleOrNull()
        val w = weight.toDoubleOrNull()
        val t = target.toDoubleOrNull()
        val valid = firstName.isNotBlank() && dob.isNotBlank() && h != null && w != null && t != null

        Button(
            onClick = {
                if (h != null && w != null && t != null) {
                    viewModel.addMember(
                        FamilyMemberRequest(
                            firstName = firstName.trim(),
                            relation = relation.trim().ifBlank { null },
                            heightCm = h,
                            activityLevel = activity,
                            goal = goal,
                            dietType = diet,
                            sensitive = SensitiveData(
                                sex = sex,
                                dob = dob.trim(),
                                currentWeightKg = w,
                                targetWeightKg = t,
                                conditions = emptyList(),
                                allergies = emptyList(),
                            ),
                        ),
                    )
                }
            },
            enabled = valid && !state.loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add member")
        }
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String) {
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
