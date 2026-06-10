package com.finora.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.remote.AuthRepository
import com.finora.data.remote.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLogin: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class AuthViewModel(
    private val authRepo: AuthRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) {
        _state.value = _state.value.copy(email = value.trim(), error = null)
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value, error = null)
    }

    fun toggleMode() {
        _state.value = _state.value.copy(isLogin = !_state.value.isLogin, error = null)
    }

    fun submit() {
        val s = _state.value
        if (s.email.isBlank() || s.password.length < 6) {
            _state.value = s.copy(error = "Введите email и пароль (мин. 6 символов)")
            return
        }
        _state.value = s.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                if (s.isLogin) {
                    authRepo.signIn(s.email, s.password)
                    // After login — try to restore data from cloud
                    val userId = authRepo.currentUserId()
                    if (userId != null) {
                        try { syncManager.downloadAll(userId) } catch (_: Exception) { }
                    }
                } else {
                    authRepo.signUp(s.email, s.password)
                    // After registration — upload local data to cloud
                    val userId = authRepo.currentUserId()
                    if (userId != null) {
                        try { syncManager.uploadAll(userId) } catch (_: Exception) { }
                    }
                }
                _state.value = _state.value.copy(isLoading = false, success = true)
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("Invalid login", true) == true ->
                        "Неверный email или пароль"
                    e.message?.contains("already registered", true) == true ->
                        "Этот email уже зарегистрирован"
                    e.message?.contains("valid email", true) == true ->
                        "Некорректный email"
                    else -> e.message ?: "Ошибка авторизации"
                }
                _state.value = _state.value.copy(isLoading = false, error = msg)
            }
        }
    }
}
