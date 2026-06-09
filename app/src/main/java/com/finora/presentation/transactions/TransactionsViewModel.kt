package com.finora.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.repository.FinanceRepository
import com.finora.domain.model.TransactionDetails
import com.finora.domain.model.TransactionType
import com.finora.presentation.util.startOfDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    val isEmpty: Boolean = true
)

class TransactionsViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val filter = MutableStateFlow(TxFilter.ALL)

    val uiState: StateFlow<TransactionsUiState> = combine(
        repository.observeTransactionDetails(),
        filter
    ) { transactions, currentFilter ->
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
        TransactionsUiState(
            filter = currentFilter,
            groups = groups,
            isEmpty = filtered.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionsUiState()
    )

    fun setFilter(value: TxFilter) {
        filter.value = value
    }

    fun delete(details: TransactionDetails) {
        viewModelScope.launch {
            repository.deleteTransaction(details.transaction)
        }
    }
}
