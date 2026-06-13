package com.finora.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.repository.FinanceRepository
import com.finora.domain.model.CategoryStat
import com.finora.domain.model.TransactionDetails
import com.finora.domain.model.TransactionType
import com.finora.presentation.util.addMonths
import com.finora.presentation.util.endOfMonth
import com.finora.presentation.util.formatMonthYear
import com.finora.presentation.util.startOfDay
import com.finora.presentation.util.startOfMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TxFilter { ALL, INCOME, EXPENSE }

data class DayGroup(
    val dayStart: Long,
    val income: Double,
    val expense: Double,
    val items: List<TransactionDetails>
)

data class TransactionsUiState(
    val filter: TxFilter = TxFilter.ALL,
    val groups: List<DayGroup> = emptyList(),
    val isEmpty: Boolean = true,
    // Current-month category breakdown (donut shown at the top of the screen).
    val monthLabel: String = "",
    val monthOffset: Int = 0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val expenseStats: List<CategoryStat> = emptyList(),
    val incomeStats: List<CategoryStat> = emptyList()
)

class TransactionsViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val filter = MutableStateFlow(TxFilter.ALL)
    private val monthOffset = MutableStateFlow(0)

    val uiState: StateFlow<TransactionsUiState> = combine(
        repository.observeTransactionDetails(),
        filter,
        monthOffset
    ) { transactions, currentFilter, offset ->
        val filtered = when (currentFilter) {
            TxFilter.ALL -> transactions
            TxFilter.INCOME -> transactions.filter { it.transaction.type == TransactionType.INCOME }
            TxFilter.EXPENSE -> transactions.filter { it.transaction.type == TransactionType.EXPENSE }
        }
        val groups = filtered
            .groupBy { startOfDay(it.transaction.date) }
            .toSortedMap(compareByDescending { it })
            .map { (day, items) ->
                DayGroup(
                    dayStart = day,
                    income = items.filter { it.transaction.type == TransactionType.INCOME }
                        .sumOf { it.transaction.amount },
                    expense = items.filter { it.transaction.type == TransactionType.EXPENSE }
                        .sumOf { it.transaction.amount },
                    items = items
                )
            }

        val monthAnchor = addMonths(System.currentTimeMillis(), offset)
        val from = startOfMonth(monthAnchor)
        val to = endOfMonth(monthAnchor)
        val monthTx = transactions.filter { it.transaction.date in from..to }
        val monthIncome = monthTx.filter { it.transaction.type == TransactionType.INCOME }
            .sumOf { it.transaction.amount }
        val monthExpense = monthTx.filter { it.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.transaction.amount }

        TransactionsUiState(
            filter = currentFilter,
            groups = groups,
            isEmpty = filtered.isEmpty(),
            monthLabel = formatMonthYear(monthAnchor),
            monthOffset = offset,
            monthIncome = monthIncome,
            monthExpense = monthExpense,
            expenseStats = buildStats(monthTx, TransactionType.EXPENSE, monthExpense),
            incomeStats = buildStats(monthTx, TransactionType.INCOME, monthIncome)
        )
    }
        // Grouping + stats over all transactions is heavy — keep it off the main thread.
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TransactionsUiState()
        )

    private fun buildStats(
        txs: List<TransactionDetails>,
        type: TransactionType,
        total: Double
    ): List<CategoryStat> {
        if (total <= 0.0) return emptyList()
        return txs.filter { it.transaction.type == type && it.category != null }
            .groupBy { it.category!! }
            .map { (category, items) ->
                val sum = items.sumOf { it.transaction.amount }
                CategoryStat(category, sum, (sum / total).toFloat(), items.size)
            }
            .sortedByDescending { it.total }
    }

    fun setFilter(value: TxFilter) {
        filter.value = value
    }

    fun previousMonth() { monthOffset.value -= 1 }
    fun nextMonth() { if (monthOffset.value < 0) monthOffset.value += 1 }

    fun delete(details: TransactionDetails) {
        viewModelScope.launch {
            repository.deleteTransaction(details.transaction)
        }
    }
}
