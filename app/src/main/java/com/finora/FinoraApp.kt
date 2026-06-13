package com.finora

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.finora.data.ai.DailyInsightWorker
import com.finora.data.recurring.RecurringRulesManager
import com.finora.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class FinoraApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Notification channel for daily insights
        DailyInsightWorker.createNotificationChannel(this)

        appScope.launch {
            // 1. Seed defaults (fast — only if Room is empty)
            container.repository.ensureSeeded()
            // 2. Accrue interest on savings accounts
            container.repository.applyInterestAccruals()
            // 3. Execute overdue recurring transaction rules
            RecurringRulesManager(container.db).executePending()
            // NOTE: no auto-upload here. Cloud sync only happens:
            //   • after data changes (triggerCloudSync → scheduleUpload)
            //   • on login flow (download; if remote empty → upload)
            //   • before sign-out (uploadAll in SettingsViewModel)
        }

        // Schedule daily AI insight (runs ~once every 24h, keeps existing)
        scheduleDailyInsight()
    }

    private fun scheduleDailyInsight() {
        val request = PeriodicWorkRequestBuilder<DailyInsightWorker>(
            repeatInterval = 24, repeatIntervalTimeUnit = TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DailyInsightWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
