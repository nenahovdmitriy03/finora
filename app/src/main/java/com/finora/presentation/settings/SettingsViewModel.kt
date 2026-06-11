package com.finora.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.preferences.SettingsRepository
import com.finora.data.remote.AuthRepository
import com.finora.data.remote.SyncManager
import com.finora.domain.model.AccentColor
import com.finora.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val authRepo: AuthRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val accentColor: StateFlow<AccentColor> = settings.accentColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccentColor.BLUE)

    val authState: StateFlow<AuthRepository.AuthState> = authRepo.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthRepository.AuthState.Loading)

    private val _isSigningOut = MutableStateFlow(false)
    val isSigningOut: StateFlow<Boolean> = _isSigningOut.asStateFlow()

    sealed interface DeleteStatus {
        data object Idle : DeleteStatus
        data object Deleting : DeleteStatus
        data object Done : DeleteStatus
        data class Error(val message: String) : DeleteStatus
    }

    private val _deleteStatus = MutableStateFlow<DeleteStatus>(DeleteStatus.Idle)
    val deleteStatus: StateFlow<DeleteStatus> = _deleteStatus.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setAccent(accent: AccentColor) {
        viewModelScope.launch { settings.setAccentColor(accent) }
    }

    /**
     * Lets a user who is browsing without an account go to the auth screen.
     * Clearing the "auth skipped" flag makes the nav host route to Auth.
     * Local data is preserved and will be uploaded to the cloud once the
     * user registers / logs in (handled by AuthViewModel).
     */
    fun goToRegister() {
        viewModelScope.launch { settings.setAuthSkipped(false) }
    }

    fun signOut() {
        viewModelScope.launch {
            _isSigningOut.value = true
            // Upload with 8s timeout — don't block sign-out if network is slow
            try {
                val userId = authRepo.currentUserId()
                if (userId != null) {
                    withTimeoutOrNull(8_000L) {
                        syncManager.uploadAll(userId)
                    }
                }
            } catch (_: Exception) { }
            authRepo.signOut()
            settings.clearOnboardingFlags()
            _isSigningOut.value = false
        }
    }

    /** Delete all user data from cloud + local, then sign out. */
    fun deleteAccount() {
        val userId = authRepo.currentUserId() ?: return
        _deleteStatus.value = DeleteStatus.Deleting
        viewModelScope.launch {
            try {
                syncManager.deleteAllUserData(userId)
                authRepo.signOut()
                settings.clearOnboardingFlags()
                _deleteStatus.value = DeleteStatus.Done
            } catch (e: Exception) {
                _deleteStatus.value = DeleteStatus.Error(e.message ?: "Ошибка удаления")
            }
        }
    }
}
