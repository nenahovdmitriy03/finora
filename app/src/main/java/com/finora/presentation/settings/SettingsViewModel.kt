package com.finora.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.backup.BackupManager
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
    private val syncManager: SyncManager,
    private val backupManager: BackupManager
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

    // ─── Backup / restore (JSON) ─────────────────────────────────────────

    sealed interface BackupStatus {
        data object Idle : BackupStatus
        data object Working : BackupStatus
        data class Exported(val records: Int) : BackupStatus
        data class Imported(val records: Int) : BackupStatus
        data class Error(val message: String) : BackupStatus
    }

    private val _backupStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val backupStatus: StateFlow<BackupStatus> = _backupStatus.asStateFlow()

    fun clearBackupStatus() { _backupStatus.value = BackupStatus.Idle }

    /**
     * Exports all local data to JSON. The [writer] lambda receives the JSON text
     * and is responsible for writing it to the file the user picked (the
     * Composable does the actual content-resolver IO so the VM stays Android-free).
     */
    fun exportBackup(writer: suspend (String) -> Unit) {
        viewModelScope.launch {
            _backupStatus.value = BackupStatus.Working
            try {
                val jsonText = backupManager.exportToJson()
                writer(jsonText)
                val records = backupManager.parse(jsonText).totalRecords
                _backupStatus.value = BackupStatus.Exported(records)
            } catch (e: Exception) {
                _backupStatus.value = BackupStatus.Error(e.message ?: "Не удалось сохранить файл")
            }
        }
    }

    /**
     * Restores local data from a JSON backup. The [reader] lambda returns the
     * file contents (read by the Composable from the picked Uri). On success,
     * if the user is logged in, the restored data is also pushed to the cloud.
     */
    fun importBackup(reader: suspend () -> String) {
        viewModelScope.launch {
            _backupStatus.value = BackupStatus.Working
            try {
                val jsonText = reader()
                val data = backupManager.importFromJson(jsonText)
                // Keep the cloud in sync with what we just restored.
                val userId = authRepo.currentUserId()
                if (userId != null) {
                    settings.setDataOwnerId(userId)
                    runCatching { syncManager.uploadAll(userId) }
                }
                _backupStatus.value = BackupStatus.Imported(data.totalRecords)
            } catch (e: Exception) {
                _backupStatus.value = BackupStatus.Error(
                    e.message ?: "Файл повреждён или имеет неверный формат"
                )
            }
        }
    }

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
            settings.clearDataOwnerId()
            // Wipe local data so the next account that logs in starts clean
            // and the previous user's data can't leak across accounts.
            try {
                syncManager.clearLocalData()
            } catch (_: Exception) { }
            _isSigningOut.value = false
        }
    }

    /** Delete all user data from cloud + local, then sign out. */
    fun deleteAccount() {
        val userId = authRepo.currentUserId() ?: return
        _deleteStatus.value = DeleteStatus.Deleting
        viewModelScope.launch {
            try {
                // 1. Remove the user's data rows from the cloud (also wipes local Room).
                syncManager.deleteAllUserData(userId)
                // 2. Delete the auth account itself so the email is freed and the
                //    user can't simply log back in (this was the bug — only data
                //    was removed before, the account survived).
                authRepo.deleteUser()
                // 3. End the local session and clear flags.
                authRepo.signOut()
                settings.clearOnboardingFlags()
                settings.clearDataOwnerId()
                _deleteStatus.value = DeleteStatus.Done
            } catch (e: Exception) {
                _deleteStatus.value = DeleteStatus.Error(e.message ?: "Ошибка удаления")
            }
        }
    }
}
