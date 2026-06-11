package com.finora.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.repository.FinanceRepository
import com.finora.domain.model.AccountBalance
import com.finora.domain.model.Goal
import com.finora.domain.model.TransactionDetails
import com.finora.presentation.util.startOfMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val totalBalance: Double = 0.0,
    val inGoals: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val accounts: List<AccountBalance> = emptyList(),
    val recentTransactions: List<TransactionDetails> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val loading: Boolean = true
) {
    val freeBalance: Double get() = (totalBalance - inGoals).coerceAtLeast(0.0)
}

class HomeViewModel(repository: FinanceRepository) : ViewModel() {

    // Start of the current month (used for income/expense aggregates).
    private val monthStart = startOfMonth(System.currentTimeMillis())

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeAccountBalances(),
        // Only the 5 most recent transactions are loaded — not the whole table.
        repository.observeRecentTransactionDetails(5),
        // Income/expense totals are aggregated in SQL, not summed in memory.
        repository.observeMonthTotals(monthStart),
        repository.observeGoals()
    ) { accounts, recent, totals, goals ->
        HomeUiState(
            totalBalance = accounts.sumOf { it.balance },
            inGoals = goals.sumOf { it.savedAmount },
            monthIncome = totals.income,
            monthExpense = totals.expense,
            accounts = accounts,
            recentTransactions = recent,
            goals = goals.take(3),
            loading = false
        )
    }
        // Run all merging/aggregation off the main thread to keep the UI smooth.
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )
}
