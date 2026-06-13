package com.finora.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.ai.AiEngine
import com.finora.data.ai.AiProviders
import com.finora.data.ai.ChatTurn
import com.finora.data.preferences.SettingsRepository
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
import org.json.JSONArray
import org.json.JSONObject

/** A single visible chat bubble. */
data class ChatMessage(val role: String, val content: String) // "user" | "assistant"

data class AiChatUiState(
    val configured: Boolean = true,
    val provider: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

/**
 * Drives the dedicated AI screen: runs the initial analysis, then lets the user
 * ask follow-up questions. The conversation (plus the data snapshot it is grounded
 * on) is persisted in settings so it survives restarts.
 */
class AiChatViewModel(
    private val repository: FinanceRepository,
    private val settings: SettingsRepository,
    private val engines: List<AiEngine> = AiProviders.configured()
) : ViewModel() {

    private val _state = MutableStateFlow(
        AiChatUiState(configured = engines.isNotEmpty(), provider = engines.firstOrNull()?.label)
    )
    val state: StateFlow<AiChatUiState> = _state.asStateFlow()

    /** Data summary the model is grounded on; rebuilt for each fresh analysis. */
    private var dataContext: String = ""

    init {
        viewModelScope.launch {
            val restored = restoreSession()
            if (restored != null && restored.second.isNotEmpty()) {
                dataContext = restored.first
                _state.update { it.copy(messages = restored.second) }
            } else if (engines.isNotEmpty()) {
                runAnalysis()
            }
        }
    }

    /** Fresh analysis — clears the conversation and asks for a summary. */
    fun runAnalysis() {
        if (_state.value.loading || engines.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, messages = emptyList()) }
            dataContext = buildDataContext()
            val request = ChatTurn(
                "user",
                "Сделай полный анализ моих финансов за текущий месяц. " +
                    "Включи: обзор доходов/расходов с суммами, топ категорий расходов с процентами, " +
                    "прогресс по целям, и 2–3 конкретных совета. Оформи красиво по формату из инструкции."
            )
            complete(listOf(systemTurn(), request))?.let { reply ->
                val msgs = listOf(ChatMessage("assistant", reply))
                _state.update { it.copy(loading = false, messages = msgs) }
                persist(msgs)
            }
        }
    }

    /** User asked a follow-up question. */
    fun send(text: String) {
        val question = text.trim()
        if (question.isEmpty() || _state.value.loading || engines.isEmpty()) return
        val withUser = _state.value.messages + ChatMessage("user", question)
        _state.update { it.copy(messages = withUser, loading = true, error = null) }
        persist(withUser)
        viewModelScope.launch {
            if (dataContext.isEmpty()) dataContext = buildDataContext()
            val conversation = buildList {
                add(systemTurn())
                withUser.forEach { add(ChatTurn(it.role, it.content)) }
            }
            complete(conversation)?.let { reply ->
                val msgs = withUser + ChatMessage("assistant", reply)
                _state.update { it.copy(loading = false, messages = msgs) }
                persist(msgs)
            }
        }
    }

    fun clear() {
        viewModelScope.launch {
            settings.clearAiChatSession()
            dataContext = ""
            _state.update { it.copy(messages = emptyList(), error = null) }
            if (engines.isNotEmpty()) runAnalysis()
        }
    }

    /** Tries each configured provider; returns the first success or sets an error. */
    private suspend fun complete(conversation: List<ChatTurn>): String? {
        val errors = mutableListOf<String>()
        for (engine in engines) {
            try {
                val text = engine.chat(conversation)
                _state.update { it.copy(provider = engine.label, error = null) }
                return text.ifBlank { "Пустой ответ. Попробуй ещё раз." }
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
        return null
    }

    private fun systemTurn() = ChatTurn(
        "system",
        """Ты — Алия, дружелюбный AI-ассистент приложения личных финансов Finora.

## Правила ответа
- Язык: русский
- Опирайся ТОЛЬКО на реальные данные пользователя ниже; никогда не выдумывай цифры
- Будь конкретной: называй суммы, проценты, категории
- Тон: дружелюбный, ободряющий, но честный

## Формат вывода (Markdown)
Каждый ответ оформляй красиво и структурированно:

1. **Заголовок секции** — используй строку с эмодзи-иконкой:
   📊 **Обзор за месяц**
2. **Ключевые метрики** — выделяй числа жирным:
   Доход: **45 000 ₽** | Расход: **32 000 ₽** | Баланс: **+13 000 ₽**
3. **Списки** — используй маркированные списки с эмодзи-маркерами:
   - 🍔 Еда — **12 400 ₽** (38%)
   - 🏠 Жильё — **8 000 ₽** (25%)
4. **Прогресс целей** — показывай визуально:
   🎯 Отпуск: ████████░░ **80%** (40 000 / 50 000 ₽)
5. **Советы/выводы** — отдельной секцией с 💡:
   💡 **Совет:** Расходы на кафе выросли на 20% — попробуй установить лимит 5 000 ₽/мес
6. **Разделители** — между секциями ставь пустую строку

Не используй заголовки Markdown (#). Используй **жирный** для акцентов.
Эмодзи используй как иконки-маркеры секций (📊 💰 📈 🎯 💡 ⚠️ ✅ 🔥), не переусердствуй.

=== Данные пользователя (валюта — рубли) ===
$dataContext"""
    )

    private fun persist(messages: List<ChatMessage>) {
        viewModelScope.launch {
            val root = JSONObject().apply {
                put("context", dataContext)
                put("messages", JSONArray().apply {
                    messages.forEach { put(JSONObject().put("role", it.role).put("content", it.content)) }
                })
            }
            settings.setAiChatSession(root.toString())
        }
    }

    private suspend fun restoreSession(): Pair<String, List<ChatMessage>>? {
        val raw = settings.aiChatSession.first()
        if (raw.isBlank()) return null
        return runCatching {
            val root = JSONObject(raw)
            val ctx = root.optString("context")
            val arr = root.optJSONArray("messages") ?: JSONArray()
            val msgs = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ChatMessage(o.optString("role", "assistant"), o.optString("content"))
            }
            ctx to msgs
        }.getOrNull()
    }

    private suspend fun buildDataContext(): String {
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
            .take(8)

        val sb = StringBuilder()
        sb.appendLine("Общий баланс: ${formatMoney(accounts.sumOf { it.balance })}")
        sb.appendLine("Доходы за текущий месяц: ${formatMoney(income)}")
        sb.appendLine("Расходы за текущий месяц: ${formatMoney(expense)}")
        sb.appendLine("Сальдо за месяц: ${formatMoney(income - expense)}")
        sb.appendLine()
        sb.appendLine("Счета:")
        if (accounts.isEmpty()) sb.appendLine("- нет")
        else accounts.forEach { ab ->
            val acc = ab.account
            val savings = if (acc.hasInterest) " (накопительный, ${acc.interestRate}% годовых)" else ""
            sb.appendLine("- ${acc.name}: ${formatMoney(ab.balance)}$savings")
        }
        sb.appendLine()
        sb.appendLine("Топ категорий расходов за месяц:")
        if (topExpenseCats.isEmpty()) sb.appendLine("- нет расходов в этом месяце")
        else topExpenseCats.forEach { (name, total) -> sb.appendLine("- $name: ${formatMoney(total)}") }
        sb.appendLine()
        sb.appendLine("Цели накоплений:")
        if (goals.isEmpty()) sb.appendLine("- нет")
        else goals.forEach { g ->
            val pct = (g.progress * 100).toInt()
            sb.appendLine("- ${g.name}: ${formatMoney(g.savedAmount)} из ${formatMoney(g.targetAmount)} ($pct%)")
        }
        return sb.toString()
    }
}
