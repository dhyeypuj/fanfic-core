package com.dhyey.fanfic.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyey.fanfic.auth.AuthService
import com.dhyey.fanfic.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signUp(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email and password cannot be empty")
            return
        }
        
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            authService.signUp(email, password)
                .onSuccess {
                    syncManager.sync()
                    _uiState.value = AuthUiState.Success
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.localizedMessage ?: "Sign up failed")
                }
        }
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email and password cannot be empty")
            return
        }
        
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            authService.signIn(email, password)
                .onSuccess {
                    syncManager.sync()
                    _uiState.value = AuthUiState.Success
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.localizedMessage ?: "Login failed")
                }
        }
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
}

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}
