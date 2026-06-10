package com.finora.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.repository.FinanceRepository
import com.finora.domain.model.Account
import com.finora.domain.model.AccountBalance
import com.finora.domain.model.AccountType
import com.finora.domain.model.InterestPeriod
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountsUiState(
    val total: Double = 0.0,
    val inGoals: Double = 0.0,
    val accounts: List<AccountBalance> = emptyList()
) {
    val free: Double get() = (total - inGoals).coerceAtLeast(0.0)
}

class AccountsViewModel(private val repository: FinanceRepository) : ViewModel() {

    val uiState: StateFlow<AccountsUiState> = combine(
        repository.observeAccountBalances(),
        repository.observeGoals()
    ) { list, goals ->
        AccountsUiState(
            total = list.sumOf { it.balance },
            inGoals = goals.sumOf { it.savedAmount },
            accounts = list
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())

    fun saveAccount(
        id: Long,
        name: String,
        type: AccountType,
        initialBalance: Double,
        iconKey: String,
        color: Long,
        interestRate: Double = 0.0,
        interestPeriod: InterestPeriod? = null,
        interestPayoutMinute: Int = 9 * 60,
        interestPayoutDay: Int = 1,
        previousLastInterestAt: Long? = null,
        previouslyHadInterest: Boolean = false
    ) {
        if (name.isBlank()) return
        val enabled = interestPeriod != null && interestRate > 0.0
        val lastInterestAt = when {
            !enabled -> null
            previouslyHadInterest -> previousLastInterestAt ?: lastPayoutInstant(interestPayoutMinute)
            else -> lastPayoutInstant(interestPayoutMinute)
        }
        viewModelScope.launch {
            repository.addAccount(
                Account(
                    id = id,
                    name = name.trim(),
                    type = type,
                    initialBalance = initialBalance,
                    color = color,
                    iconKey = iconKey,
                    interestRate = if (enabled) interestRate else 0.0,
                    interestPeriod = if (enabled) interestPeriod else null,
                    lastInterestAt = lastInterestAt,
                    interestPayoutMinute = interestPayoutMinute,
                    interestPayoutDay = interestPayoutDay
                )
            )
            repository.applyInterestAccruals()
        }
    }

    private fun lastPayoutInstant(payoutMinute: Int): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, payoutMinute / 60)
        cal.set(java.util.Calendar.MINUTE, payoutMinute % 60)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        if (cal.timeInMillis > System.currentTimeMillis()) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        return cal.timeInMillis
    }

    fun delete(account: Account) {
        viewModelScope.launch { repository.deleteAccount(account) }
    }
}
