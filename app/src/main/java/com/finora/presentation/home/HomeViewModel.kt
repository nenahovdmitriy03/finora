package com.finora.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.repository.FinanceRepository
import com.finora.domain.model.AccountBalance
import com.finora.domain.model.Goal
import com.finora.domain.model.TransactionDetails
import com.finora.domain.model.TransactionType
import com.finora.presentation.util.startOfMonth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val totalBalance: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val accounts: List<AccountBalance> = emptyList(),
    val recentTransactions: List<TransactionDetails> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val loading: Boolean = true
)

class HomeViewModel(repository: FinanceRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeAccountBalances(),
        repository.observeTransactionDetails(),
        repository.observeGoals()
    ) { accounts, transactions, goals ->
        val monthStart = startOfMonth(System.currentTimeMillis())
        val monthTx = transactions.filter { it.transaction.date >= monthStart }
        HomeUiState(
            totalBalance = accounts.sumOf { it.balance },
            monthIncome = monthTx.filter { it.transaction.type == TransactionType.INCOME }
                .sumOf { it.transaction.amount },
            monthExpense = monthTx.filter { it.transaction.type == TransactionType.EXPENSE }
                .sumOf { it.transaction.amount },
            accounts = accounts,
            recentTransactions = transactions.take(5),
            goals = goals.take(3),
            loading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )
}
