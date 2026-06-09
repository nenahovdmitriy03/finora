package com.finora.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.repository.FinanceRepository
import com.finora.domain.model.CategoryStat
import com.finora.domain.model.PeriodPoint
import com.finora.domain.model.TransactionDetails
import com.finora.domain.model.TransactionType
import com.finora.presentation.util.addMonths
import com.finora.presentation.util.endOfMonth
import com.finora.presentation.util.formatMonthYear
import com.finora.presentation.util.formatShortMonth
import com.finora.presentation.util.startOfMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StatisticsUiState(
    val monthLabel: String = "",
    val monthOffset: Int = 0,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val expenseStats: List<CategoryStat> = emptyList(),
    val incomeStats: List<CategoryStat> = emptyList(),
    val trend: List<PeriodPoint> = emptyList(),
    val hasData: Boolean = false
)

class StatisticsViewModel(repository: FinanceRepository) : ViewModel() {

    private val monthOffset = MutableStateFlow(0)

    val uiState: StateFlow<StatisticsUiState> = combine(
        repository.observeTransactionDetails(),
        monthOffset
    ) { transactions, offset ->
        val now = System.currentTimeMillis()
        val monthAnchor = addMonths(now, offset)
        val from = startOfMonth(monthAnchor)
        val to = endOfMonth(monthAnchor)
        val monthTx = transactions.filter { it.transaction.date in from..to }

        val income = monthTx.filter { it.transaction.type == TransactionType.INCOME }
            .sumOf { it.transaction.amount }
        val expense = monthTx.filter { it.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.transaction.amount }

        StatisticsUiState(
            monthLabel = formatMonthYear(monthAnchor),
            monthOffset = offset,
            income = income,
            expense = expense,
            expenseStats = buildStats(monthTx, TransactionType.EXPENSE, expense),
            incomeStats = buildStats(monthTx, TransactionType.INCOME, income),
            trend = buildTrend(transactions, now),
            hasData = monthTx.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatisticsUiState()
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
                CategoryStat(category, sum, (sum / total).toFloat())
            }
            .sortedByDescending { it.total }
    }

    private fun buildTrend(all: List<TransactionDetails>, now: Long): List<PeriodPoint> {
        return (5 downTo 0).map { back ->
            val anchor = addMonths(now, -back)
            val from = startOfMonth(anchor)
            val to = endOfMonth(anchor)
            val monthTx = all.filter { it.transaction.date in from..to }
            PeriodPoint(
                label = formatShortMonth(anchor),
                income = monthTx.filter { it.transaction.type == TransactionType.INCOME }
                    .sumOf { it.transaction.amount },
                expense = monthTx.filter { it.transaction.type == TransactionType.EXPENSE }
                    .sumOf { it.transaction.amount }
            )
        }
    }

    fun previousMonth() { monthOffset.value -= 1 }
    fun nextMonth() { if (monthOffset.value < 0) monthOffset.value += 1 }
}
