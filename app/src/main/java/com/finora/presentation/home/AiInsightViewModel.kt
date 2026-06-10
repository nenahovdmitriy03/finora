package com.finora.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.ai.AiEngine
import com.finora.data.ai.AiProviders
import com.finora.data.repository.FinanceRepository
import com.finora.domain.model.TransactionType
import com.finora.presentation.util.formatMoney
import com.finora.presentation.util.startOfMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiInsightUiState(
    val configured: Boolean = true,
    val provider: String? = null,
    val loading: Boolean = false,
    val insight: String? = null,
    val error: String? = null
)

/**
 * Drives the "AI-аналитика" card on Home. Builds a compact, privacy-friendly
 * summary (aggregates only — no raw transaction list) and asks the configured
 * AI provider (OpenRouter / Groq / Gemini) to analyse it.
 */
class AiInsightViewModel(
    private val repository: FinanceRepository,
    private val engines: List<AiEngine> = AiProviders.configured()
) : ViewModel() {

    private val _state = MutableStateFlow(
        AiInsightUiState(configured = engines.isNotEmpty(), provider = engines.firstOrNull()?.label)
    )
    val state: StateFlow<AiInsightUiState> = _state.asStateFlow()

    fun analyze() {
        if (_state.value.loading) return
        if (engines.isEmpty()) {
            _state.update { it.copy(configured = false) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val prompt = buildPrompt()
            val errors = mutableListOf<String>()
            // Try each configured provider in turn; first success wins.
            for (engine in engines) {
                try {
                    val text = engine.generate(prompt)
                    _state.update {
                        it.copy(
                            loading = false,
                            provider = engine.label,
                            error = null,
                            insight = text.ifBlank { "Модель вернула пустой ответ. Попробуй ещё раз." }
                        )
                    }
                    return@launch
                } catch (e: Exception) {
                    errors += "${engine.label}: ${e.message ?: "ошибка"}"
                }
            }
            val combined = if (errors.size > 1) {
                "Все провайдеры недоступны.\n" + errors.joinToString("\n")
            } else {
                errors.firstOrNull() ?: "Не удалось получить ответ"
            }
            _state.update { it.copy(loading = false, error = combined) }
        }
    }

    private suspend fun buildPrompt(): String {
        val accounts = repository.observeAccountBalances().first()
        val transactions = repository.observeTransactionDetails().first()
        val goals = repository.observeGoals().first()

        val monthStart = startOfMonth(System.currentTimeMillis())
        val monthTx = transactions.filter { it.transaction.date >= monthStart }
        val income = monthTx.filter { it.transaction.type == TransactionType.INCOME }
            .sumOf { it.transaction.amount }
        val expense = monthTx.filter { it.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.transaction.amount }

        val topExpenseCats = monthTx
            .filter { it.transaction.type == TransactionType.EXPENSE }
            .groupBy { it.category?.name ?: "Без категории" }
            .mapValues { (_, list) -> list.sumOf { it.transaction.amount } }
            .entries.sortedByDescending { it.value }
            .take(6)

        val sb = StringBuilder()
        sb.appendLine("Ты — персональный финансовый аналитик в приложении учёта личных финансов.")
        sb.appendLine("Проанализируй данные пользователя и дай полезные выводы на русском языке.")
        sb.appendLine("Формат ответа: 3–6 коротких пунктов с эмодзи. Будь конкретным, опирайся на цифры,")
        sb.appendLine("отметь риски (например, расходы превышают доходы), дай 1–2 практичных совета.")
        sb.appendLine("Не выдумывай данные, которых нет. Без вступлений и заключений — только пункты.")
        sb.appendLine()
        sb.appendLine("=== Данные (валюта — рубли) ===")
        sb.appendLine("Общий баланс: ${formatMoney(accounts.sumOf { it.balance })}")
        sb.appendLine("Доходы за текущий месяц: ${formatMoney(income)}")
        sb.appendLine("Расходы за текущий месяц: ${formatMoney(expense)}")
        sb.appendLine("Сальдо за месяц: ${formatMoney(income - expense)}")
        sb.appendLine()
        sb.appendLine("Счета:")
        if (accounts.isEmpty()) {
            sb.appendLine("- нет")
        } else {
            accounts.forEach { ab ->
                val acc = ab.account
                val savings = if (acc.hasInterest) " (накопительный, ${acc.interestRate}% годовых)" else ""
                sb.appendLine("- ${acc.name}: ${formatMoney(ab.balance)}$savings")
            }
        }
        sb.appendLine()
        sb.appendLine("Топ категорий расходов за месяц:")
        if (topExpenseCats.isEmpty()) {
            sb.appendLine("- нет расходов в этом месяце")
        } else {
            topExpenseCats.forEach { (name, total) ->
                sb.appendLine("- $name: ${formatMoney(total)}")
            }
        }
        sb.appendLine()
        sb.appendLine("Цели накоплений:")
        if (goals.isEmpty()) {
            sb.appendLine("- нет")
        } else {
            goals.forEach { g ->
                val pct = (g.progress * 100).toInt()
                sb.appendLine("- ${g.name}: ${formatMoney(g.savedAmount)} из ${formatMoney(g.targetAmount)} ($pct%)")
            }
        }
        return sb.toString()
    }
}
