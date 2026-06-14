package com.finora.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.repository.FinanceRepository
import com.finora.domain.model.AccountBalance
import com.finora.domain.model.Goal
import com.finora.domain.model.GoalAccountSummary
import com.finora.domain.model.GoalContribution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Per-goal breakdown: which accounts contributed how much (net).
 */
data class GoalWithSources(
    val goal: Goal,
    val sources: List<GoalAccountSummary>,
    val monthlyPace: Double = 0.0
)

class GoalsViewModel(private val repository: FinanceRepository) : ViewModel() {
    private companion object {
        const val MONTH_MS = 30L * 24L * 60L * 60L * 1000L
    }

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
        val recentSince = System.currentTimeMillis() - MONTH_MS
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
            val monthlyPace = goalContribs
                .filter { it.date >= recentSince && it.amount > 0.0 }
                .sumOf { it.amount }
            GoalWithSources(goal = goal, sources = byAccount, monthlyPace = monthlyPace)
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveGoal(
        id: Long,
        name: String,
        target: Double,
        iconKey: String,
        color: Long,
        deadline: Long?,
        saved: Double,
        planMonths: Int? = null,
        plannedMonthlyAmount: Double? = null,
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
                    planMonths = planMonths,
                    plannedMonthlyAmount = plannedMonthlyAmount,
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

    fun savePlan(goal: Goal, months: Int?, monthlyAmount: Double?) {
        viewModelScope.launch {
            repository.updateGoal(
                goal.copy(
                    planMonths = months?.takeIf { it > 0 },
                    plannedMonthlyAmount = monthlyAmount?.takeIf { it > 0.0 }
                )
            )
        }
    }

    fun delete(goal: Goal) {
        viewModelScope.launch { repository.deleteGoal(goal) }
    }
}
