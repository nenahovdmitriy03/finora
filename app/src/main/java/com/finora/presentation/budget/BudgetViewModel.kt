package com.finora.presentation.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.repository.FinanceRepository
import com.finora.domain.model.Budget
import com.finora.domain.model.BudgetProgress
import com.finora.domain.model.Category
import com.finora.domain.model.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BudgetUiState(
    val budgetProgress: List<BudgetProgress> = emptyList(),
    /** Expense categories that don't yet have a budget assigned. */
    val availableCategories: List<Category> = emptyList()
)

class BudgetViewModel(private val repository: FinanceRepository) : ViewModel() {

    val uiState: StateFlow<BudgetUiState> = combine(
        repository.observeBudgetProgress(),
        repository.observeCategories(TransactionType.EXPENSE),
        repository.observeBudgets()
    ) { progress, cats, budgets ->
        val budgetedIds = budgets.map { it.categoryId }.toSet()
        BudgetUiState(
            budgetProgress = progress,
            availableCategories = cats.filter { it.id !in budgetedIds }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState())

    fun addBudget(categoryId: Long, limit: Double, periodDays: Int = 30) {
        if (limit <= 0) return
        viewModelScope.launch {
            repository.addBudget(
                Budget(
                    categoryId = categoryId,
                    limitAmount = limit,
                    periodDays = periodDays,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch { repository.deleteBudget(budget) }
    }
}
