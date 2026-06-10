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
    val info: String? = null,
    val success: Boolean = false
)

class AuthViewModel(
    private val authRepo: AuthRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) {
        _state.value = _state.value.copy(email = value.trim(), error = null, info = null)
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value, error = null, info = null)
    }

    fun toggleMode() {
        _state.value = _state.value.copy(isLogin = !_state.value.isLogin, error = null, info = null)
    }

    fun submit() {
        val s = _state.value
        if (s.email.isBlank() || s.password.length < 6) {
            _state.value = s.copy(error = "Введите email и пароль (мин. 6 символов)")
            return
        }
        _state.value = s.copy(isLoading = true, error = null, info = null)

        viewModelScope.launch {
            try {
                if (s.isLogin) {
                    authRepo.signIn(s.email, s.password)
                    // After login — try to restore data from cloud
                    val userId = authRepo.currentUserId()
                    if (userId != null) {
                        try { syncManager.downloadAll(userId) } catch (_: Exception) { }
                    }
                    _state.value = _state.value.copy(isLoading = false, success = true)
                } else {
                    authRepo.signUp(s.email, s.password)
                    // Check if user got auto-confirmed (email confirmation disabled)
                    val userId = authRepo.currentUserId()
                    if (userId != null) {
                        // Auto-confirmed — upload data and proceed
                        try { syncManager.uploadAll(userId) } catch (_: Exception) { }
                        _state.value = _state.value.copy(isLoading = false, success = true)
                    } else {
                        // Email confirmation required — tell user to check inbox
                        _state.value = _state.value.copy(
                            isLoading = false,
                            info = "Письмо для подтверждения отправлено на ${s.email}. Проверьте почту и перейдите по ссылке, затем нажмите «Войти»."
                        )
                    }
                }
            } catch (e: Exception) {
                val msg = translateError(e.message)
                _state.value = _state.value.copy(isLoading = false, error = msg)
            }
        }
    }

    private fun translateError(raw: String?): String {
        if (raw == null) return "Ошибка авторизации"
        val lower = raw.lowercase()
        return when {
            "invalid login" in lower || "invalid_credentials" in lower ->
                "Неверный email или пароль"
            "already registered" in lower || "already been registered" in lower ->
                "Этот email уже зарегистрирован. Нажмите «Войти»."
            "valid email" in lower || "invalid email" in lower ->
                "Некорректный формат email"
            "email not confirmed" in lower ->
                "Email не подтверждён. Проверьте почту и перейдите по ссылке."
            "rate" in lower || "security purposes" in lower || "after 45 seconds" in lower
                    || "request this after" in lower || "too many requests" in lower ->
                "Слишком частые запросы. Подождите минуту и попробуйте снова."
            "network" in lower || "unable to resolve" in lower || "timeout" in lower
                    || "connect" in lower ->
                "Нет подключения к интернету. Проверьте сеть."
            "weak password" in lower || "at least" in lower ->
                "Пароль слишком простой. Минимум 6 символов."
            "user not found" in lower ->
                "Пользователь не найден"
            "signup is disabled" in lower ->
                "Регистрация временно отключена"
            else -> raw
        }
    }
}
