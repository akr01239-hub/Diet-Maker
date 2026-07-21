package com.nutriai.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriai.data.repository.HealthRepository
import com.nutriai.domain.model.HealthStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HealthUiState {
    data object Loading : HealthUiState
    data class Connected(val status: HealthStatus) : HealthUiState
    data class Error(val message: String) : HealthUiState
}

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val repository: HealthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HealthUiState>(HealthUiState.Loading)
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = HealthUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                HealthUiState.Connected(repository.check())
            } catch (e: Exception) {
                HealthUiState.Error(e.message ?: "Could not reach the API")
            }
        }
    }
}
