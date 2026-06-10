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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountsUiState(
    val total: Double = 0.0,
    val accounts: List<AccountBalance> = emptyList()
)

class AccountsViewModel(private val repository: FinanceRepository) : ViewModel() {

    val uiState: StateFlow<AccountsUiState> = repository.observeAccountBalances()
        .map { list -> AccountsUiState(total = list.sumOf { it.balance }, accounts = list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())

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
        previousLastInterestAt: Long? = null,
        previouslyHadInterest: Boolean = false
    ) {
        if (name.isBlank()) return
        val enabled = interestPeriod != null && interestRate > 0.0
        // Start accruing from the most recent payout time when interest is newly
        // enabled (so payouts land at the chosen time of day), otherwise keep the
        // prior clock so we don't backfill or lose progress.
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
                    interestPayoutMinute = interestPayoutMinute
                )
            )
            repository.applyInterestAccruals()
        }
    }

    /** Most recent wall-clock occurrence of [payoutMinute] (today, or yesterday if not yet reached). */
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
