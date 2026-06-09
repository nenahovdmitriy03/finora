package com.finora.presentation.addtransaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.repository.FinanceRepository
import com.finora.domain.model.Account
import com.finora.domain.model.Category
import com.finora.domain.model.Transaction
import com.finora.domain.model.TransactionType
import com.finora.presentation.util.sanitizeMoneyInput
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddTransactionViewModel(private val repository: FinanceRepository) : ViewModel() {

    val accounts: StateFlow<List<Account>> = repository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var type by mutableStateOf(TransactionType.EXPENSE)
        private set
    var amountText by mutableStateOf("")
        private set
    var accountId by mutableStateOf<Long?>(null)
        private set
    var categoryId by mutableStateOf<Long?>(null)
        private set
    var note by mutableStateOf("")
        private set
    var dateMillis by mutableLongStateOf(System.currentTimeMillis())
        private set

    var editingId by mutableStateOf<Long?>(null)
        private set
    private var loaded = false

    val amount: Double
        get() = amountText.replace(',', '.').replace("\u00A0", "").replace(" ", "").toDoubleOrNull() ?: 0.0

    val canSave: Boolean
        get() = amount > 0.0 && accountId != null

    fun setType(value: TransactionType) {
        if (type != value) {
            type = value
            categoryId = null
        }
    }

    fun setAmount(value: String) {
        amountText = sanitizeMoneyInput(value)
    }

    fun setAccount(id: Long) { accountId = id }
    fun setCategory(id: Long?) { categoryId = id }
    fun setNote(value: String) { note = value }
    fun setDate(millis: Long) { dateMillis = millis }

    fun load(id: Long) {
        if (loaded || id <= 0L) {
            loaded = true
            return
        }
        loaded = true
        viewModelScope.launch {
            repository.getTransaction(id)?.let { tx ->
                editingId = tx.id
                type = tx.type
                amountText = if (tx.amount % 1.0 == 0.0) tx.amount.toLong().toString() else tx.amount.toString()
                accountId = tx.accountId
                categoryId = tx.categoryId
                note = tx.note
                dateMillis = tx.date
            }
        }
    }

    fun ensureDefaultAccount() {
        if (accountId == null) {
            accounts.value.firstOrNull()?.let { accountId = it.id }
        }
    }

    fun save(onDone: () -> Unit) {
        val accId = accountId ?: return
        if (amount <= 0.0) return
        viewModelScope.launch {
            repository.addTransaction(
                Transaction(
                    id = editingId ?: 0L,
                    amount = amount,
                    type = type,
                    accountId = accId,
                    categoryId = categoryId,
                    note = note.trim(),
                    date = dateMillis,
                    createdAt = System.currentTimeMillis()
                )
            )
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = editingId ?: return
        viewModelScope.launch {
            repository.getTransaction(id)?.let { repository.deleteTransaction(it) }
            onDone()
        }
    }
}
