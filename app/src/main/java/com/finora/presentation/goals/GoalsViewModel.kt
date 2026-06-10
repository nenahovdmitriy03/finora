package com.finora.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.repository.FinanceRepository
import com.finora.domain.model.AccountBalance
import com.finora.domain.model.Goal
import com.finora.domain.model.GoalAccountSummary
import com.finora.domain.model.GoalContribution
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Per-goal breakdown: which accounts contributed how much (net).
 */
data class GoalWithSources(
    val goal: Goal,
    val sources: List<GoalAccountSummary>
)

class GoalsViewModel(private val repository: FinanceRepository) : ViewModel() {

    val goals: StateFlow<List<Goal>> = repository.observeGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accounts: StateFlow<List<AccountBalance>> = repository.observeAccountBalances()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Goals enriched with ALL contributing accounts (not just the last one).
     * Contributions are aggregated: net amount per account per goal.
     */
    val goalsWithSources: StateFlow<List<GoalWithSources>> = combine(
        repository.observeGoals(),
        repository.observeGoalContributions(),
        repository.observeAccounts()
    ) { goals, contributions, accounts ->
        val accountMap = accounts.associateBy { it.id }
        val contribsByGoal = contributions.groupBy { it.goalId }
        goals.map { goal ->
            val goalContribs = contribsByGoal[goal.id].orEmpty()
            // Aggregate net amount per account
            val byAccount = goalContribs
                .groupBy { it.accountId }
                .mapNotNull { (accId, items) ->
                    val acc = accountMap[accId] ?: return@mapNotNull null
                    val net = items.sumOf { it.amount }
                    if (net == 0.0) return@mapNotNull null
                    GoalAccountSummary(account = acc, netAmount = net)
                }
                .sortedByDescending { it.netAmount }
            GoalWithSources(goal = goal, sources = byAccount)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
