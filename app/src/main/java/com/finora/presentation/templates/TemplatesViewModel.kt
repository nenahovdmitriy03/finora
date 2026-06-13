package com.finora.presentation.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.repository.FinanceRepository
import com.finora.domain.model.Account
import com.finora.domain.model.Category
import com.finora.domain.model.Template
import com.finora.domain.model.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TemplatesUiState(
    val templates: List<Template> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList()
)

class TemplatesViewModel(private val repository: FinanceRepository) : ViewModel() {

    val uiState: StateFlow<TemplatesUiState> = combine(
        repository.observeTemplates(),
        repository.observeCategories(),
        repository.observeAccounts()
    ) { templates, cats, accounts ->
        TemplatesUiState(templates, cats, accounts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TemplatesUiState())

    fun addTemplate(
        name: String,
        amount: Double,
        type: TransactionType,
        categoryId: Long?,
        accountId: Long?,
        note: String
    ) {
        if (name.isBlank() || amount <= 0) return
        viewModelScope.launch {
            repository.addTemplate(
                Template(
                    name = name.trim(),
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    note = note.trim(),
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteTemplate(template: Template) {
        viewModelScope.launch { repository.deleteTemplate(template) }
    }
}
