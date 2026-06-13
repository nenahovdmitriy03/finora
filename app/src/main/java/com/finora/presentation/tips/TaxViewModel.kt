package com.finora.presentation.tips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.repository.FinanceRepository
import com.finora.domain.model.TransactionType
import com.finora.presentation.util.formatMoney
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * A single deductible spending category matched from user data.
 */
data class DeductionItem(
    val categoryName: String,
    val deductionType: DeductionType,
    val spent: Double,
    val emoji: String
)

enum class DeductionType(val label: String, val emoji: String) {
    MEDICAL("Лечение и медикаменты", "🏥"),
    EDUCATION("Обучение", "🎓"),
    FITNESS("Спорт и фитнес", "🏋️"),
    CHARITY("Благотворительность", "❤️"),
    INSURANCE("Страхование жизни", "🛡️")
}

data class TaxUiState(
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val items: List<DeductionItem> = emptyList(),
    val totalDeductible: Double = 0.0,
    val socialCap: Double = 150_000.0,
    val effectiveDeductible: Double = 0.0,
    val potentialRefund: Double = 0.0,
    val loading: Boolean = true
)

/**
 * Analyses user transactions and maps known expense categories to
 * tax-deductible groups per Russian Tax Code (НК РФ, ст. 219).
 *
 * Social deduction cap since 2024: 150 000 ₽/year → refund up to 19 500 ₽ (13 %).
 */
class TaxViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    companion object {
        /** Category name → DeductionType mapping (case-insensitive prefix match). */
        private val CATEGORY_MAP = mapOf(
            "здоров" to DeductionType.MEDICAL,
            "медиц" to DeductionType.MEDICAL,
            "лечен" to DeductionType.MEDICAL,
            "аптек" to DeductionType.MEDICAL,
            "стомат" to DeductionType.MEDICAL,
            "клиник" to DeductionType.MEDICAL,
            "врач" to DeductionType.MEDICAL,
            "образов" to DeductionType.EDUCATION,
            "обучен" to DeductionType.EDUCATION,
            "курс" to DeductionType.EDUCATION,
            "школ" to DeductionType.EDUCATION,
            "универс" to DeductionType.EDUCATION,
            "репетит" to DeductionType.EDUCATION,
            "спорт" to DeductionType.FITNESS,
            "фитнес" to DeductionType.FITNESS,
            "трениров" to DeductionType.FITNESS,
            "бассейн" to DeductionType.FITNESS,
            "благотвор" to DeductionType.CHARITY,
            "пожертв" to DeductionType.CHARITY,
            "донат" to DeductionType.CHARITY,
            "страхов" to DeductionType.INSURANCE
        )

        private const val SOCIAL_CAP = 150_000.0
        private const val TAX_RATE = 0.13
    }

    private val _state = MutableStateFlow(TaxUiState())
    val state: StateFlow<TaxUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }

            val year = _state.value.year
            val cal = Calendar.getInstance()
            cal.set(year, Calendar.JANUARY, 1, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val yearStart = cal.timeInMillis
            cal.set(year + 1, Calendar.JANUARY, 1, 0, 0, 0)
            val yearEnd = cal.timeInMillis

            val transactions = repository.observeTransactionDetails().first()
            val yearTx = transactions.filter {
                it.transaction.type == TransactionType.EXPENSE &&
                    it.transaction.date in yearStart until yearEnd
            }

            // Group by matched deduction type
            val matched = mutableMapOf<String, Pair<DeductionType, Double>>()
            for (tx in yearTx) {
                val catName = tx.category?.name ?: continue
                val lower = catName.lowercase()
                val type = CATEGORY_MAP.entries.firstOrNull { (prefix, _) ->
                    lower.startsWith(prefix)
                }?.value ?: continue

                val current = matched[catName]
                if (current != null) {
                    matched[catName] = current.copy(second = current.second + tx.transaction.amount)
                } else {
                    matched[catName] = type to tx.transaction.amount
                }
            }

            val items = matched.map { (name, pair) ->
                DeductionItem(
                    categoryName = name,
                    deductionType = pair.first,
                    spent = pair.second,
                    emoji = pair.first.emoji
                )
            }.sortedByDescending { it.spent }

            val total = items.sumOf { it.spent }
            val effective = total.coerceAtMost(SOCIAL_CAP)
            val refund = effective * TAX_RATE

            _state.update {
                it.copy(
                    items = items,
                    totalDeductible = total,
                    effectiveDeductible = effective,
                    potentialRefund = refund,
                    loading = false
                )
            }
        }
    }
}
