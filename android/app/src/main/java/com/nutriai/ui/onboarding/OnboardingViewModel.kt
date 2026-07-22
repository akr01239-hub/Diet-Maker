package com.nutriai.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriai.data.AppRepository
import com.nutriai.data.remote.dto.ProfileDto
import com.nutriai.data.remote.dto.ProfileUpsertRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val loading: Boolean = false,
    val error: String? = null,
    /** Existing profile used to pre-fill the form when editing (null on first setup). */
    val prefill: ProfileDto? = null,
    val prefillLoaded: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        // Pre-fill from the saved profile so "Edit profile" isn't a full re-entry.
        viewModelScope.launch {
            val existing = repository.getProfile().getOrNull()
            _state.value = _state.value.copy(prefill = existing, prefillLoaded = true)
        }
    }

    fun save(request: ProfileUpsertRequest, onSuccess: () -> Unit) {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val result = repository.saveProfile(request)
            if (result.isSuccess) {
                _state.value = _state.value.copy(loading = false, error = null)
                onSuccess()
            } else {
                _state.value = _state.value.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.message ?: "Could not save profile",
                )
            }
        }
    }
}
