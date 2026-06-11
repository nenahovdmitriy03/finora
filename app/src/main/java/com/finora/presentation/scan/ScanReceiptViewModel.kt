package com.finora.presentation.scan

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.ai.ReceiptScanner
import com.finora.data.ai.ScannedTransaction
import com.finora.domain.model.Account
import com.finora.domain.model.Category
import com.finora.domain.model.Transaction
import com.finora.domain.model.TransactionType
import com.finora.data.repository.FinanceRepository
import com.finora.presentation.util.sanitizeMoneyInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One editable row on the confirmation screen. */
data class ReceiptDraft(
    val type: TransactionType,
    val amountText: String,
    val date: Long,
    val categoryId: Long?,
    val note: String,
    val include: Boolean = true
) {
    val amount: Double
        get() = amountText.replace(',', '.').replace("\u00A0", "").replace(" ", "").toDoubleOrNull() ?: 0.0
}

enum class ScanPhase { IDLE, SCANNING, REVIEW, SAVING, DONE }

data class ScanUiState(
    val available: Boolean = true,
    val phase: ScanPhase = ScanPhase.IDLE,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedAccountId: Long? = null,
    val drafts: List<ReceiptDraft> = emptyList(),
    val error: String? = null
) {
    val includedCount: Int get() = drafts.count { it.include && it.amount > 0.0 }
    val canSave: Boolean get() = selectedAccountId != null && includedCount > 0 && phase == ScanPhase.REVIEW

    fun categoriesFor(type: TransactionType): List<Category> = categories.filter { it.type == type }
}

class ScanReceiptViewModel(
    private val repository: FinanceRepository,
    private val scanner: ReceiptScanner
) : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState(available = scanner.isAvailable))
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val accounts = repository.observeAccounts().first()
            val categories = repository.observeCategories().first()
            _state.update {
                it.copy(
                    accounts = accounts,
                    categories = categories,
                    selectedAccountId = accounts.firstOrNull()?.id
                )
            }
        }
    }

    fun scan(bitmap: Bitmap) {
        if (_state.value.phase == ScanPhase.SCANNING) return
        _state.update { it.copy(phase = ScanPhase.SCANNING, error = null, drafts = emptyList()) }
        viewModelScope.launch {
            try {
                val cats = _state.value.categories
                val scanned = scanner.scan(bitmap, cats.map { it.name })
                if (scanned.isEmpty()) {
                    _state.update {
                        it.copy(phase = ScanPhase.IDLE, error = "Не удалось распознать операции. Попробуй другое фото.")
                    }
                    return@launch
                }
                val drafts = scanned.map { it.toDraft(cats) }
                _state.update { it.copy(phase = ScanPhase.REVIEW, drafts = drafts) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(phase = ScanPhase.IDLE, error = e.message ?: "Ошибка распознавания")
                }
            }
        }
    }

    fun selectAccount(id: Long) = _state.update { it.copy(selectedAccountId = id) }

    fun setAmount(index: Int, value: String) = updateDraft(index) { it.copy(amountText = sanitizeMoneyInput(value)) }

    fun setCategory(index: Int, categoryId: Long?) = updateDraft(index) { it.copy(categoryId = categoryId) }

    fun setType(index: Int, type: TransactionType) = updateDraft(index) {
        // Reset category when switching type, since categories are type-specific.
        it.copy(type = type, categoryId = null)
    }

    fun toggleInclude(index: Int) = updateDraft(index) { it.copy(include = !it.include) }

    fun reset() = _state.update {
        it.copy(phase = ScanPhase.IDLE, drafts = emptyList(), error = null)
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        val accId = s.selectedAccountId ?: return
        if (!s.canSave) return
        _state.update { it.copy(phase = ScanPhase.SAVING) }
        viewModelScope.launch {
            try {
                s.drafts.filter { it.include && it.amount > 0.0 }.forEach { d ->
                    repository.addTransaction(
                        Transaction(
                            amount = d.amount,
                            type = d.type,
                            accountId = accId,
                            categoryId = d.categoryId,
                            note = d.note,
                            date = d.date
                        )
                    )
                }
                _state.update { it.copy(phase = ScanPhase.DONE) }
                onDone()
            } catch (e: Exception) {
                _state.update { it.copy(phase = ScanPhase.REVIEW, error = e.message ?: "Не удалось сохранить") }
            }
        }
    }

    private fun updateDraft(index: Int, transform: (ReceiptDraft) -> ReceiptDraft) {
        _state.update { st ->
            val list = st.drafts.toMutableList()
            if (index in list.indices) list[index] = transform(list[index])
            st.copy(drafts = list)
        }
    }

    private fun ScannedTransaction.toDraft(categories: List<Category>): ReceiptDraft {
        val match = categoryName?.let { name ->
            categories.firstOrNull { it.type == type && it.name.equals(name, ignoreCase = true) }
                ?: categories.firstOrNull { it.type == type && it.name.contains(name, ignoreCase = true) }
                ?: categories.firstOrNull { it.type == type && name.contains(it.name, ignoreCase = true) }
        }
        val amountStr = if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
        return ReceiptDraft(
            type = type,
            amountText = amountStr,
            date = date,
            categoryId = match?.id,
            note = note
        )
    }
}
