package com.finora.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.repository.FinanceRepository
import com.finora.domain.model.AccountBalance
import com.finora.domain.model.Goal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalsViewModel(private val repository: FinanceRepository) : ViewModel() {

    val goals: StateFlow<List<Goal>> = repository.observeGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accounts: StateFlow<List<AccountBalance>> = repository.observeAccountBalances()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveGoal(
        id: Long,
        name: String,
        target: Double,
        iconKey: String,
        color: Long,
        deadline: Long?,
        saved: Double,
        linkedAccountId: Long? = null
    ) {
        if (name.isBlank() || target <= 0.0) return
        viewModelScope.launch {
            repository.addGoal(
                Goal(
                    id = id,
                    name = name.trim(),
                    targetAmount = target,
                    savedAmount = saved.coerceAtLeast(0.0),
                    iconKey = iconKey,
                    color = color,
                    deadline = deadline,
                    linkedAccountId = linkedAccountId
                )
            )
        }
    }

    /** Deposit (amount > 0) or withdraw (amount < 0) moving money to/from [accountId]. */
    fun contribute(goalId: Long, accountId: Long, amount: Double) {
        if (amount == 0.0) return
        viewModelScope.launch { repository.contributeToGoal(goalId, accountId, amount) }
    }

    fun delete(goal: Goal) {
        viewModelScope.launch { repository.deleteGoal(goal) }
    }
}
