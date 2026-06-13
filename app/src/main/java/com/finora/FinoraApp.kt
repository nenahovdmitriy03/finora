package com.finora

import android.app.Application
import com.finora.data.recurring.RecurringRulesManager
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
            // 3. Execute overdue recurring transaction rules
            RecurringRulesManager(container.db).executePending()
        }
    }
}
