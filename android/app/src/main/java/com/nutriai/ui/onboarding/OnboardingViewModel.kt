package com.nutriai.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriai.data.AppRepository
import com.nutriai.data.remote.dto.ProfileUpsertRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(val loading: Boolean = false, val error: String? = null)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun save(request: ProfileUpsertRequest, onSuccess: () -> Unit) {
        _state.value = OnboardingUiState(loading = true)
        viewModelScope.launch {
            val result = repository.saveProfile(request)
            if (result.isSuccess) {
                _state.value = OnboardingUiState()
                onSuccess()
            } else {
                _state.value = OnboardingUiState(error = result.exceptionOrNull()?.message ?: "Could not save profile")
            }
        }
    }
}
