package com.finora

import android.app.Application
import com.finora.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FinoraApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            container.repository.ensureSeeded()
            // Pay out any due interest on savings accounts.
            container.repository.applyInterestAccruals()
        }
        // NOTE: Auto-upload on start was REMOVED — it raced with downloadAll()
        // during login on fresh install, deleting remote data before download
        // could fetch it. Sync to cloud now happens:
        //   • On registration (AuthViewModel)
        //   • Before sign-out (SettingsViewModel)
        //   • After data changes (debounced in SyncManager)
    }
}
