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
import com.finora.domain.model.Transfer
import com.finora.presentation.util.sanitizeMoneyInput
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** UI-level mode — includes TRANSFER which is not a TransactionType. */
enum class EntryMode { EXPENSE, INCOME, TRANSFER }

class AddTransactionViewModel(private val repository: FinanceRepository) : ViewModel() {

    val accounts: StateFlow<List<Account>> = repository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var mode by mutableStateOf(EntryMode.EXPENSE)
        private set

    /** Convenience accessor: EXPENSE/INCOME → corresponding TransactionType. */
    val type: TransactionType
        get() = when (mode) {
            EntryMode.EXPENSE -> TransactionType.EXPENSE
            EntryMode.INCOME -> TransactionType.INCOME
            EntryMode.TRANSFER -> TransactionType.EXPENSE // unused for transfers
        }

    var amountText by mutableStateOf("")
        private set
    var accountId by mutableStateOf<Long?>(null)
        private set
    /** Destination account for transfers. */
    var toAccountId by mutableStateOf<Long?>(null)
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
        get() = when (mode) {
            EntryMode.EXPENSE, EntryMode.INCOME -> amount > 0.0 && accountId != null
            EntryMode.TRANSFER -> amount > 0.0 && accountId != null && toAccountId != null && accountId != toAccountId
        }

    fun updateMode(value: EntryMode) {
        if (mode != value) {
            mode = value
            categoryId = null
            // Reset toAccountId when leaving transfer mode
            if (value != EntryMode.TRANSFER) toAccountId = null
        }
    }

    /** Legacy compat — called from TypeToggle when only INCOME/EXPENSE */
    fun updateType(value: TransactionType) {
        updateMode(if (value == TransactionType.INCOME) EntryMode.INCOME else EntryMode.EXPENSE)
    }

    fun setAmount(value: String) {
        amountText = sanitizeMoneyInput(value)
    }

    fun setAccount(id: Long) { accountId = id }
    fun setToAccount(id: Long) { toAccountId = id }
    fun setCategory(id: Long?) { categoryId = id }

    /** Creates a custom category of the current [type] and selects it. */
    fun createCategory(name: String, iconKey: String, color: Long) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.addCategory(
                Category(
                    name = name.trim(),
                    type = type,
                    iconKey = iconKey,
                    color = color,
                    isDefault = false
                )
            )
            categoryId = id
        }
    }

    fun updateNote(value: String) { note = value }
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
                mode = if (tx.type == TransactionType.INCOME) EntryMode.INCOME else EntryMode.EXPENSE
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
        when (mode) {
            EntryMode.EXPENSE, EntryMode.INCOME -> saveTransaction(onDone)
            EntryMode.TRANSFER -> saveTransfer(onDone)
        }
    }

    private fun saveTransaction(onDone: () -> Unit) {
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

    private fun saveTransfer(onDone: () -> Unit) {
        val fromId = accountId ?: return
        val toId = toAccountId ?: return
        if (fromId == toId || amount <= 0.0) return
        viewModelScope.launch {
            repository.addTransfer(
                Transfer(
                    fromAccountId = fromId,
                    toAccountId = toId,
                    amount = amount,
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
