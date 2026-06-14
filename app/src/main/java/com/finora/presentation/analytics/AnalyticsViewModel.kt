package com.finora.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.repository.FinanceRepository
import com.finora.domain.model.AccountBalance
import com.finora.domain.model.Category
import com.finora.domain.model.Goal
import com.finora.domain.model.TransactionDetails
import com.finora.domain.model.TransactionType
import com.finora.presentation.util.addMonths
import com.finora.presentation.util.endOfMonth
import com.finora.presentation.util.startOfMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import kotlin.math.round

data class CategoryAnalytics(
    val category: Category,
    val amount: Double,
    val share: Float,
    val previousAmount: Double
) {
    val delta: Double get() = amount - previousAmount
}

data class SavingsYield(
    val account: AccountBalance,
    val monthlyIncome: Double
)

data class AnalyticsUiState(
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val netFlow: Double = 0.0,
    val projectedEndBalance: Double = 0.0,
    val topExpenseCategories: List<CategoryAnalytics> = emptyList(),
    val savingsYields: List<SavingsYield> = emptyList(),
    val totalSavingsMonthlyYield: Double = 0.0,
    val activeGoals: List<Goal> = emptyList(),
    val goalsRemaining: Double = 0.0,
    val loading: Boolean = true
)

class AnalyticsViewModel(repository: FinanceRepository) : ViewModel() {

    val uiState: StateFlow<AnalyticsUiState> = combine(
        repository.observeTransactionDetails(),
        repository.observeAccountBalances(),
        repository.observeGoals()
    ) { transactions, accounts, goals ->
        buildState(transactions, accounts, goals)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    private fun buildState(
        transactions: List<TransactionDetails>,
        accounts: List<AccountBalance>,
        goals: List<Goal>
    ): AnalyticsUiState {
        val now = System.currentTimeMillis()
        val monthStart = startOfMonth(now)
        val monthEnd = endOfMonth(now)
        val previousMonthStart = addMonths(monthStart, -1)
        val previousMonthEnd = monthStart - 1

        val currentMonth = transactions.filter { it.transaction.date in monthStart..monthEnd }
        val previousMonth = transactions.filter { it.transaction.date in previousMonthStart..previousMonthEnd }

        val income = currentMonth
            .filter { it.transaction.type == TransactionType.INCOME }
            .sumOf { it.transaction.amount }
        val expense = currentMonth
            .filter { it.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.transaction.amount }
        val net = income - expense
        val projectedEndBalance = accounts.sumOf { it.balance } + projectedRemainingNet(now, net)

        val previousExpenseByCategory = previousMonth
            .filter { it.transaction.type == TransactionType.EXPENSE && it.category != null }
            .groupBy { it.category!!.id }
            .mapValues { (_, rows) -> rows.sumOf { it.transaction.amount } }

        val expenseRows = currentMonth.filter {
            it.transaction.type == TransactionType.EXPENSE && it.category != null
        }
        val totalExpense = expenseRows.sumOf { it.transaction.amount }.coerceAtLeast(0.0)
        val categories = expenseRows
            .groupBy { it.category!! }
            .map { (category, rows) ->
                val amount = rows.sumOf { it.transaction.amount }
                CategoryAnalytics(
                    category = category,
                    amount = amount,
                    share = if (totalExpense > 0.0) (amount / totalExpense).toFloat() else 0f,
                    previousAmount = previousExpenseByCategory[category.id] ?: 0.0
                )
            }
            .sortedByDescending { it.amount }
            .take(5)

        val savings = accounts
            .filter { it.account.hasInterest && it.balance > 0.0 }
            .map { account ->
                SavingsYield(
                    account = account,
                    monthlyIncome = round2(account.balance * account.account.interestRate / 100.0 / 12.0)
                )
            }
            .sortedByDescending { it.monthlyIncome }

        val activeGoals = goals.filter { it.progress < 1f }
        val remaining = activeGoals.sumOf { (it.targetAmount - it.savedAmount).coerceAtLeast(0.0) }

        return AnalyticsUiState(
            monthIncome = income,
            monthExpense = expense,
            netFlow = net,
            projectedEndBalance = projectedEndBalance,
            topExpenseCategories = categories,
            savingsYields = savings,
            totalSavingsMonthlyYield = savings.sumOf { it.monthlyIncome },
            activeGoals = activeGoals.sortedByDescending { it.progress }.take(3),
            goalsRemaining = remaining,
            loading = false
        )
    }

    private fun projectedRemainingNet(now: Long, currentNet: Double): Double {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val day = cal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(day)
        val daysLeft = (daysInMonth - day).coerceAtLeast(0)
        if (day <= 0 || daysLeft <= 0) return 0.0
        return currentNet / day.toDouble() * daysLeft.toDouble()
    }

    private fun round2(value: Double): Double = round(value * 100.0) / 100.0
}
