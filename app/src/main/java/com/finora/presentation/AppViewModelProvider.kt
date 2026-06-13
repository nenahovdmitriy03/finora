package com.finora.presentation

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.finora.FinoraApp
import com.finora.presentation.accounts.AccountsViewModel
import com.finora.presentation.addtransaction.AddTransactionViewModel
import com.finora.presentation.ai.AiChatViewModel
import com.finora.presentation.auth.AuthViewModel
import com.finora.presentation.goals.GoalsViewModel
import com.finora.presentation.home.AiInsightViewModel
import com.finora.presentation.home.HomeViewModel
import com.finora.presentation.recurring.RecurringRulesViewModel
import com.finora.presentation.scan.ScanReceiptViewModel
import com.finora.presentation.settings.SettingsViewModel
import com.finora.presentation.tips.RecommendationsViewModel
import com.finora.presentation.tips.TaxViewModel
import com.finora.presentation.transactions.TransactionsViewModel

private fun creationApp(extras: androidx.lifecycle.viewmodel.CreationExtras): FinoraApp =
    extras[APPLICATION_KEY] as FinoraApp

/** Central factory wiring all ViewModels to the shared repository. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { HomeViewModel(creationApp(this).container.repository) }
        initializer { AiInsightViewModel(creationApp(this).container.settings) }
        initializer {
            AiChatViewModel(
                creationApp(this).container.repository,
                creationApp(this).container.settings
            )
        }
        initializer { TransactionsViewModel(creationApp(this).container.repository) }
        initializer { AddTransactionViewModel(creationApp(this).container.repository) }
        initializer {
            ScanReceiptViewModel(
                creationApp(this).container.repository,
                creationApp(this).container.receiptScanner
            )
        }
        initializer { GoalsViewModel(creationApp(this).container.repository) }
        initializer { AccountsViewModel(creationApp(this).container.repository) }
        initializer {
            SettingsViewModel(
                creationApp(this).container.settings,
                creationApp(this).container.authRepository,
                creationApp(this).container.syncManager,
                creationApp(this).container.backupManager
            )
        }
        initializer {
            AuthViewModel(
                creationApp(this).container.authRepository,
                creationApp(this).container.syncManager,
                creationApp(this).container.settings
            )
        }
        initializer { RecurringRulesViewModel(creationApp(this).container.db, creationApp(this).container.repository) }
        initializer { TaxViewModel(creationApp(this).container.repository) }
        initializer { RecommendationsViewModel(creationApp(this).container.repository) }
    }
}
