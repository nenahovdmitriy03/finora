package com.finora.presentation.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.local.AppDatabase
import com.finora.data.local.entity.AccountEntity
import com.finora.data.local.entity.CategoryEntity
import com.finora.data.local.entity.RecurringRuleEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecurringRulesUiState(
    val rules: List<RecurringRuleEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList()
)

class RecurringRulesViewModel(private val db: AppDatabase) : ViewModel() {

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
        periodDays: Int
    ) {
        viewModelScope.launch {
            db.recurringRuleDao().upsert(
                RecurringRuleEntity(
                    name = name,
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    periodDays = periodDays,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleRule(rule: RecurringRuleEntity) {
        viewModelScope.launch {
            db.recurringRuleDao().upsert(rule.copy(enabled = !rule.enabled))
        }
    }

    fun deleteRule(rule: RecurringRuleEntity) {
        viewModelScope.launch {
            db.recurringRuleDao().delete(rule)
        }
    }
}
