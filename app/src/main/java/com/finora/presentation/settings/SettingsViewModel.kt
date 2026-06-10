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

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val authRepo: AuthRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val accentColor: StateFlow<AccentColor> = settings.accentColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccentColor.VIOLET)

    val authState: StateFlow<AuthRepository.AuthState> = authRepo.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthRepository.AuthState.Loading)

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

    fun signOut() {
        viewModelScope.launch {
            // Auto-upload before signing out
            try {
                val userId = authRepo.currentUserId()
                if (userId != null) syncManager.uploadAll(userId)
            } catch (_: Exception) { }
            authRepo.signOut()
            settings.clearOnboardingFlags()
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
