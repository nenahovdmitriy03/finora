package com.finora.presentation

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.finora.FinoraApp
import com.finora.presentation.accounts.AccountsViewModel
import com.finora.presentation.addtransaction.AddTransactionViewModel
import com.finora.presentation.goals.GoalsViewModel
import com.finora.presentation.home.AiInsightViewModel
import com.finora.presentation.home.HomeViewModel
import com.finora.presentation.settings.SettingsViewModel
import com.finora.presentation.statistics.StatisticsViewModel
import com.finora.presentation.transactions.TransactionsViewModel

private fun creationApp(extras: androidx.lifecycle.viewmodel.CreationExtras): FinoraApp =
    extras[APPLICATION_KEY] as FinoraApp

/** Central factory wiring all ViewModels to the shared repository. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { HomeViewModel(creationApp(this).container.repository) }
        initializer { AiInsightViewModel(creationApp(this).container.repository) }
        initializer { TransactionsViewModel(creationApp(this).container.repository) }
        initializer { AddTransactionViewModel(creationApp(this).container.repository) }
        initializer { StatisticsViewModel(creationApp(this).container.repository) }
        initializer { GoalsViewModel(creationApp(this).container.repository) }
        initializer { AccountsViewModel(creationApp(this).container.repository) }
        initializer { SettingsViewModel(creationApp(this).container.settings) }
    }
}
