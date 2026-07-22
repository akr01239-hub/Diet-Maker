package com.nutriai.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriai.data.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(val loading: Boolean = false, val error: String? = null)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    /** Logs in, then routes: onNeedsProfile if the profile is missing/incomplete, else onHome. */
    fun login(email: String, password: String, onHome: () -> Unit, onNeedsProfile: () -> Unit) {
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            val result = repository.login(email.trim(), password)
            if (result.isSuccess) {
                val profile = repository.getProfile().getOrNull()
                _state.value = AuthUiState()
                if (profile?.sensitive == null) onNeedsProfile() else onHome()
            } else {
                _state.value = AuthUiState(error = result.exceptionOrNull()?.message ?: "Something went wrong")
            }
        }
    }

    fun register(email: String, password: String, first: String, last: String, onSuccess: () -> Unit) {
        execute(onSuccess) { repository.register(email.trim(), password, first.trim(), last.trim()) }
    }

    private fun execute(onSuccess: () -> Unit, block: suspend () -> Result<Unit>) {
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            val result = block()
            _state.value = if (result.isSuccess) {
                AuthUiState()
            } else {
                AuthUiState(error = result.exceptionOrNull()?.message ?: "Something went wrong")
            }
            if (result.isSuccess) onSuccess()
        }
    }
}
