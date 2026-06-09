package com.finora.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.repository.FinanceRepository
import com.finora.domain.model.Account
import com.finora.domain.model.AccountBalance
import com.finora.domain.model.AccountType
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
        color: Long
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addAccount(
                Account(
                    id = id,
                    name = name.trim(),
                    type = type,
                    initialBalance = initialBalance,
                    color = color,
                    iconKey = iconKey
                )
            )
        }
    }

    fun delete(account: Account) {
        viewModelScope.launch { repository.deleteAccount(account) }
    }
}
