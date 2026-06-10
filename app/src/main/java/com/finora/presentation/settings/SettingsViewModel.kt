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

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    sealed interface SyncStatus {
        data object Idle : SyncStatus
        data object Syncing : SyncStatus
        data object Success : SyncStatus
        data class Error(val message: String) : SyncStatus
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setAccent(accent: AccentColor) {
        viewModelScope.launch { settings.setAccentColor(accent) }
    }

    fun signOut() {
        viewModelScope.launch { authRepo.signOut() }
    }

    /** Upload local data to cloud. */
    fun syncToCloud() {
        val userId = authRepo.currentUserId() ?: return
        _syncStatus.value = SyncStatus.Syncing
        viewModelScope.launch {
            try {
                syncManager.uploadAll(userId)
                _syncStatus.value = SyncStatus.Success
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error(e.message ?: "Ошибка синхронизации")
            }
        }
    }

    /** Restore data from cloud (overwrite local). */
    fun restoreFromCloud() {
        val userId = authRepo.currentUserId() ?: return
        _syncStatus.value = SyncStatus.Syncing
        viewModelScope.launch {
            try {
                syncManager.downloadAll(userId)
                _syncStatus.value = SyncStatus.Success
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error(e.message ?: "Ошибка восстановления")
            }
        }
    }
}
