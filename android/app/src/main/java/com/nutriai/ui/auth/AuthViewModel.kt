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

/** Forgot-password flow: verify (email+DOB) then set a new password. */
data class ForgotState(
    val loading: Boolean = false,
    val verified: Boolean = false,
    val error: String? = null,
)

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

    private val _forgot = MutableStateFlow(ForgotState())
    val forgot: StateFlow<ForgotState> = _forgot.asStateFlow()

    fun resetForgot() { _forgot.value = ForgotState() }

    /** Step 1 — verify the account by email + date of birth (yyyy-MM-dd). */
    fun verifyIdentity(email: String, dob: String) {
        _forgot.value = ForgotState(loading = true)
        viewModelScope.launch {
            val r = repository.forgotVerify(email, dob)
            _forgot.value = when {
                r.isSuccess && r.getOrDefault(false) -> ForgotState(verified = true)
                r.isSuccess -> ForgotState(error = "That email and date of birth don't match an account.")
                else -> ForgotState(error = r.exceptionOrNull()?.message ?: "Something went wrong")
            }
        }
    }

    /** Step 2 — set the new password (server re-verifies email + DOB). */
    fun resetPassword(email: String, dob: String, newPassword: String, onDone: () -> Unit) {
        _forgot.value = _forgot.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val r = repository.resetPassword(email, dob, newPassword)
            if (r.isSuccess) {
                _forgot.value = ForgotState()
                onDone()
            } else {
                _forgot.value = _forgot.value.copy(loading = false, error = r.exceptionOrNull()?.message ?: "Something went wrong")
            }
        }
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
