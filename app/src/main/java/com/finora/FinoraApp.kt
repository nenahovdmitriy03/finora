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
        // Auto-sync: if authenticated, upload local data to cloud on start
        appScope.launch {
            try {
                val userId = container.authRepository.currentUserId()
                if (userId != null) {
                    container.syncManager.uploadAll(userId)
                }
            } catch (_: Exception) { /* offline or no session — skip */ }
        }
    }
}
