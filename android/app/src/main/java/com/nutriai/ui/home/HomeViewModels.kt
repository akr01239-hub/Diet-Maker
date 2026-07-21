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
