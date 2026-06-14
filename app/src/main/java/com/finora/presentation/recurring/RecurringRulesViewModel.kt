package com.finora.presentation.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.local.AppDatabase
import com.finora.data.local.entity.AccountEntity
import com.finora.data.local.entity.CategoryEntity
import com.finora.data.local.entity.RecurringRuleEntity
import com.finora.data.repository.FinanceRepository
import com.finora.domain.model.Category
import com.finora.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class RecurringRulesUiState(
    val rules: List<RecurringRuleEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList()
)

class RecurringRulesViewModel(
    private val db: AppDatabase,
    private val repository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringRulesUiState())
    val uiState: StateFlow<RecurringRulesUiState> = _uiState.asStateFlow()

    init {
        combine(
            db.recurringRuleDao().observeAll(),
            db.categoryDao().observeAll(),
            db.accountDao().observeAll()
        ) { rules, cats, accounts ->
            RecurringRulesUiState(rules = rules, categories = cats, accounts = accounts)
        }.onEach { state -> _uiState.value = state }
            .launchIn(viewModelScope)
    }

    fun createRule(
        name: String,
        amount: Double,
        type: String,
        categoryId: Long,
        accountId: Long,
        periodDays: Int,
        startDateMillis: Long
    ) {
        viewModelScope.launch {
            repository.addRecurringRule(
                RecurringRuleEntity(
                    name = name,
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    periodDays = periodDays,
                    createdAt = startDateMillis
                )
            )
        }
    }

    fun createCategory(name: String, iconKey: String, color: Long, type: TransactionType): Long {
        var newId = 0L
        viewModelScope.launch {
            newId = repository.addCategory(
                Category(
                    name = name.trim(),
                    type = type,
                    iconKey = iconKey,
                    color = color,
                    isDefault = false
                )
            )
        }
        return newId
    }

    fun toggleRule(rule: RecurringRuleEntity) {
        viewModelScope.launch {
            repository.updateRecurringRule(rule.copy(enabled = !rule.enabled))
        }
    }

    fun deleteRule(rule: RecurringRuleEntity) {
        viewModelScope.launch {
            repository.deleteRecurringRule(rule)
        }
    }
}
