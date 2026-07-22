package com.nutriai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriai.data.AppRepository
import com.nutriai.data.remote.dto.Dashboard
import com.nutriai.data.remote.dto.PlanDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---- Dashboard ----
data class DashboardState(
    val loading: Boolean = true,
    val dashboard: Dashboard? = null,
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val r = repository.dashboard()
            _state.value = if (r.isSuccess) {
                DashboardState(loading = false, dashboard = r.getOrNull())
            } else {
                DashboardState(loading = false, error = r.exceptionOrNull()?.message ?: "Failed to load")
            }
        }
    }

    fun logWater(ml: Int) {
        viewModelScope.launch {
            repository.logWater(ml)
            refresh()
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            onDone()
        }
    }

    fun deleteAccount(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAccount()
            repository.logout() // clear local tokens after server-side deletion
            onDone()
        }
    }
}

// ---- Plan ----
data class PlanState(
    val loading: Boolean = true,
    val plan: PlanDto? = null,
    val error: String? = null,
)

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(PlanState())
    val state: StateFlow<PlanState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val r = repository.latestPlan()
            _state.value = if (r.isSuccess) PlanState(loading = false, plan = r.getOrNull())
            else PlanState(loading = false, error = r.exceptionOrNull()?.message)
        }
    }

    fun generate() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val r = repository.generatePlan()
            _state.value = if (r.isSuccess) PlanState(loading = false, plan = r.getOrNull())
            else PlanState(loading = false, error = r.exceptionOrNull()?.message)
        }
    }
}

// ---- Chat ----
data class ChatMessage(val fromUser: Boolean, val text: String)

data class ChatState(val messages: List<ChatMessage> = emptyList(), val sending: Boolean = false)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(
        ChatState(messages = listOf(ChatMessage(false, "Hi! Ask me about a food, your targets, or a safe pace to lose weight."))),
    )
    val state: StateFlow<ChatState> = _state.asStateFlow()

    fun send(text: String) {
        if (text.isBlank()) return
        val history = _state.value.messages + ChatMessage(true, text)
        _state.value = ChatState(messages = history, sending = true)
        viewModelScope.launch {
            val r = repository.chat(text)
            val reply = r.getOrNull()?.reply ?: (r.exceptionOrNull()?.message ?: "Sorry, I couldn't answer that.")
            _state.value = ChatState(messages = history + ChatMessage(false, reply), sending = false)
        }
    }
}

// ---- Food logging ----
data class LogState(
    val query: String = "",
    val results: List<com.nutriai.data.remote.dto.FoodDto> = emptyList(),
    val today: List<com.nutriai.data.remote.dto.FoodLogEntry> = emptyList(),
    val slot: String = "breakfast",
    val grams: String = "150",
    val loading: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class LogFoodViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LogState())
    val state: StateFlow<LogState> = _state.asStateFlow()

    val slots = listOf("breakfast", "midmorning", "lunch", "eveningsnack", "dinner", "bedtime")

    init {
        search("")
        loadToday()
    }

    fun loadToday() {
        viewModelScope.launch {
            val r = repository.todayLogs()
            r.getOrNull()?.let { _state.value = _state.value.copy(today = it) }
        }
    }

    fun onQuery(q: String) {
        _state.value = _state.value.copy(query = q)
    }

    fun onSlot(s: String) {
        _state.value = _state.value.copy(slot = s)
    }

    fun onGrams(g: String) {
        _state.value = _state.value.copy(grams = g.filter { it.isDigit() })
    }

    fun search(q: String) {
        _state.value = _state.value.copy(loading = true, message = null)
        viewModelScope.launch {
            val r = repository.searchFoods(q)
            _state.value = _state.value.copy(loading = false, results = r.getOrDefault(emptyList()))
        }
    }

    fun log(food: com.nutriai.data.remote.dto.FoodDto, gramsOverride: Double? = null) {
        val grams = gramsOverride ?: _state.value.grams.toDoubleOrNull() ?: food.typicalServingG
        viewModelScope.launch {
            val r = repository.logFoodItem(_state.value.slot, food, grams)
            _state.value = _state.value.copy(
                message = if (r.isSuccess) {
                    "✓ Logged ${grams.toInt()} g of ${food.name} to ${_state.value.slot}"
                } else {
                    r.exceptionOrNull()?.message ?: "Could not log"
                },
            )
            if (r.isSuccess) loadToday()
        }
    }
}

// ---- Session (login state at the nav root) ----
@HiltViewModel
class SessionViewModel @Inject constructor(
    repository: AppRepository,
) : ViewModel() {
    val isLoggedIn: StateFlow<Boolean?> = kotlinx.coroutines.flow.MutableStateFlow<Boolean?>(null).also { flow ->
        viewModelScope.launch {
            repository.isLoggedIn.collect { flow.value = it }
        }
    }.asStateFlow()
}
