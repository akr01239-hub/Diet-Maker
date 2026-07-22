package com.nutriai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriai.data.AppRepository
import com.nutriai.data.remote.dto.PublicUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val loading: Boolean = true,
    val user: PublicUser? = null,
    val deleting: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val u = repository.me().getOrNull()
            _state.value = _state.value.copy(loading = false, user = u)
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            onDone()
        }
    }

    fun deleteAccount(onDone: () -> Unit) {
        _state.value = _state.value.copy(deleting = true)
        viewModelScope.launch {
            repository.deleteAccount()
            repository.logout()
            onDone()
        }
    }
}
