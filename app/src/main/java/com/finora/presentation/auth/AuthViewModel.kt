package com.finora.presentation.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.preferences.SettingsRepository
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
    val success: Boolean = false,
    val skipped: Boolean = false
)

class AuthViewModel(
    private val authRepo: AuthRepository,
    private val syncManager: SyncManager,
    private val settings: SettingsRepository
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

    /** Skip authentication — use app locally without an account. */
    fun skipAuth() {
        viewModelScope.launch {
            settings.setAuthSkipped(true)
            _state.value = _state.value.copy(skipped = true)
        }
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
                    // ─── Login flow ──────────────────────────────────────
                    authRepo.signIn(s.email, s.password)
                    val userId = authRepo.currentUserId()
                    if (userId != null) {
                        // Who owns the data currently in local Room?
                        val owner = settings.dataOwnerId()
                        val switchingAccount = owner != null && owner != userId

                        // If the local data belongs to a DIFFERENT account, wipe it
                        // first so it can't leak into this account.
                        if (switchingAccount) {
                            Log.d("AuthVM", "Different account ($owner → $userId) — clearing local data")
                            _state.value = _state.value.copy(info = "Подготовка…")
                            syncManager.clearLocalData()
                        }

                        // Cloud is the source of truth — always download this user's data.
                        _state.value = _state.value.copy(
                            isLoading = true,
                            info = "Загрузка данных из облака…"
                        )
                        val hadRemoteData = syncManager.downloadAll(userId)

                        if (!hadRemoteData && !switchingAccount) {
                            // Remote empty AND local data is this user's (or unclaimed,
                            // e.g. registered with email confirmation / used app without
                            // an account) → push local data up to claim/back it up.
                            Log.d("AuthVM", "Remote empty — uploading local data for $userId")
                            _state.value = _state.value.copy(info = "Сохранение данных в облако…")
                            syncManager.uploadAll(userId)
                        }
                        // else (switchingAccount && empty remote): genuinely new/empty
                        // account on this device → keep the clean slate from clearLocalData().

                        // Mark this user as the owner of the local data.
                        settings.setDataOwnerId(userId)
                    }
                    _state.value = _state.value.copy(isLoading = false, info = null, success = true)
                } else {
                    // ─── Registration flow ───────────────────────────────
                    authRepo.signUp(s.email, s.password)
                    val userId = authRepo.currentUserId()
                    if (userId != null) {
                        // If local data belonged to another account, wipe before claiming.
                        val owner = settings.dataOwnerId()
                        if (owner != null && owner != userId) {
                            syncManager.clearLocalData()
                        }
                        _state.value = _state.value.copy(
                            isLoading = true,
                            info = "Сохранение данных в облако…"
                        )
                        syncManager.uploadAll(userId)
                        settings.setDataOwnerId(userId)
                        _state.value = _state.value.copy(isLoading = false, info = null, success = true)
                    } else {
                        // Email confirmation required — upload will happen on first login
                        // (the login flow detects empty remote and uploads)
                        _state.value = _state.value.copy(
                            isLoading = false,
                            info = "Письмо для подтверждения отправлено на ${s.email}. " +
                                    "Проверьте почту и перейдите по ссылке, затем нажмите «Войти»."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthVM", "Auth error", e)
                val msg = translateError(e.message)
                _state.value = _state.value.copy(isLoading = false, info = null, error = msg)
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
