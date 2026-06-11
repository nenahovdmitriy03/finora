package com.finora

import android.app.Application
import android.util.Log
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
            // 1. Seed defaults (fast — only if Room is empty)
            container.repository.ensureSeeded()
            // 2. Accrue interest on savings accounts
            container.repository.applyInterestAccruals()
            // 3. If user is authenticated, schedule a background upload.
            //    This is safe: SyncManager uses a Mutex, so this upload
            //    won't conflict with any download in AuthViewModel.
            //    It ensures data gets backed up on every app start.
            val userId = container.authRepository.currentUserId()
            if (userId != null) {
                Log.d("FinoraApp", "User authenticated ($userId) — scheduling background sync")
                container.syncManager.scheduleUpload(userId)
            }
        }
    }
}
