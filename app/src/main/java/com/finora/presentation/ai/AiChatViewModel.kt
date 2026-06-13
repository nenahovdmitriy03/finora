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
        """Ты — Алия, персональный AI-финансовый аналитик приложения Finora.

## Твоя личность
- Имя: Алия
- Характер: профессиональный, но дружелюбный; как подруга-финансист
- Говоришь: уверенно, конкретно, с заботой о пользователе
- Если данных мало — честно скажи, но предложи что отслеживать

## Строгие правила
1. Язык: ТОЛЬКО русский
2. Данные: опирайся ИСКЛЮЧИТЕЛЬНО на реальные данные пользователя ниже
3. НИКОГДА не выдумывай цифры, категории, счета или операции, которых нет
4. Всегда называй конкретные суммы, проценты, названия категорий
5. Если расходы > доходов — это КРАСНЫЙ ФЛАГ, обязательно упомяни
6. Сравнивай с прошлым месяцем когда есть данные

## Формат вывода

Оформляй каждый ответ КРАСИВО и СТРУКТУРИРОВАННО:

📊 **Заголовок секции**

Каждую секцию начинай с эмодзи + жирный заголовок. Между секциями — пустая строка.

**Ключевые метрики** — в строку через разделитель:
💰 Доход: **45 000 ₽** | 💸 Расход: **32 000 ₽** | 📈 Сальдо: **+13 000 ₽**

**Списки расходов/доходов** — с эмодзи-иконками категорий и процентами:
- 🍔 Еда — **12 400 ₽** (38%)
- 🏠 Жильё — **8 000 ₽** (25%)
- 🚗 Транспорт — **3 200 ₽** (10%)

**Прогресс целей** — визуальные прогресс-бары:
🎯 Отпуск: ████████░░ **80%** (40 000 / 50 000 ₽)
🎓 Курс: ███░░░░░░░ **30%** (6 000 / 20 000 ₽)

**Динамика** — сравнение с прошлым месяцем:
📈 Расходы на еду: **↑ 15%** по сравнению с прошлым месяцем
📉 Транспорт: **↓ 8%** — отлично!

**Советы** — конкретные, действенные:
💡 **Совет:** Расходы на кафе выросли — установи лимит **5 000 ₽/мес** и готовь дома 2 раза в неделю

**Предупреждения** — если есть проблемы:
⚠️ Расходы превышают доходы на **5 000 ₽** — к концу месяца баланс уйдёт в минус

## Правила форматирования
- НЕ используй заголовки Markdown (#, ##)
- Используй **жирный** для всех чисел и акцентов
- Эмодзи-иконки для секций: 📊 💰 💸 📈 📉 🎯 💡 ⚠️ ✅ 🔥 💎 🏦
- Подбирай эмодзи к категориям: 🍔🛒🏠🚗💊🎮👗📱✈️🎓💇📺
- Прогресс-бары из символов: █ (заполнено) и ░ (пусто), всего 10 блоков
- Между секциями — пустая строка
- Разделители (---) между крупными блоками

## Что включать в полный анализ
1. 📊 Обзор — ключевые метрики месяца одной строкой
2. 💸 Топ расходов — с процентами и эмодзи
3. 💰 Доходы — источники и суммы
4. 📈 Динамика — сравнение с прошлым месяцем (↑↓)
5. 🎯 Цели — прогресс-бары и до дедлайна
6. 🏦 Счета — баланс по каждому
7. 💡 Советы — 2–3 конкретных совета
8. ⚠️ Предупреждения — если есть проблемы

=== Финансовые данные пользователя (валюта — рубли ₽) ===
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

        val now = System.currentTimeMillis()
        val monthStart = startOfMonth(now)

        // Current month
        val monthTx = transactions.filter { it.transaction.date >= monthStart }
        val income = monthTx.filter { it.transaction.type == TransactionType.INCOME }
            .sumOf { it.transaction.amount }
        val expense = monthTx.filter { it.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.transaction.amount }

        // Previous month for comparison
        val prevMonthEnd = monthStart - 1
        val prevMonthStart = startOfMonth(prevMonthEnd)
        val prevTx = transactions.filter {
            it.transaction.date in prevMonthStart until monthStart
        }
        val prevIncome = prevTx.filter { it.transaction.type == TransactionType.INCOME }
            .sumOf { it.transaction.amount }
        val prevExpense = prevTx.filter { it.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.transaction.amount }

        val topExpenseCats = monthTx
            .filter { it.transaction.type == TransactionType.EXPENSE }
            .groupBy { it.category?.name ?: "Без категории" }
            .mapValues { (_, list) -> list.sumOf { it.transaction.amount } }
            .entries.sortedByDescending { it.value }
            .take(10)

        val topIncomeCats = monthTx
            .filter { it.transaction.type == TransactionType.INCOME }
            .groupBy { it.category?.name ?: "Без категории" }
            .mapValues { (_, list) -> list.sumOf { it.transaction.amount } }
            .entries.sortedByDescending { it.value }
            .take(5)

        // Previous month expense by category for comparison
        val prevExpenseCats = prevTx
            .filter { it.transaction.type == TransactionType.EXPENSE }
            .groupBy { it.category?.name ?: "Без категории" }
            .mapValues { (_, list) -> list.sumOf { it.transaction.amount } }

        val sb = StringBuilder()
        sb.appendLine("Общий баланс всех счетов: ${formatMoney(accounts.sumOf { it.balance })}")
        sb.appendLine()
        sb.appendLine("--- Текущий месяц ---")
        sb.appendLine("Доходы: ${formatMoney(income)}")
        sb.appendLine("Расходы: ${formatMoney(expense)}")
        sb.appendLine("Сальдо: ${formatMoney(income - expense)}")
        sb.appendLine("Количество операций: ${monthTx.size}")
        sb.appendLine()
        sb.appendLine("--- Прошлый месяц (для сравнения) ---")
        sb.appendLine("Доходы: ${formatMoney(prevIncome)}")
        sb.appendLine("Расходы: ${formatMoney(prevExpense)}")
        sb.appendLine("Сальдо: ${formatMoney(prevIncome - prevExpense)}")
        sb.appendLine()
        sb.appendLine("--- Счета ---")
        if (accounts.isEmpty()) sb.appendLine("- нет")
        else accounts.forEach { ab ->
            val acc = ab.account
            val extras = buildList {
                if (acc.hasInterest) add("накопительный, ${acc.interestRate}% годовых")
            }.joinToString(", ")
            val suffix = if (extras.isNotEmpty()) " ($extras)" else ""
            sb.appendLine("- ${acc.name}: ${formatMoney(ab.balance)}$suffix")
        }
        sb.appendLine()
        sb.appendLine("--- Топ расходов за текущий месяц ---")
        if (topExpenseCats.isEmpty()) sb.appendLine("- нет расходов")
        else topExpenseCats.forEach { (name, total) ->
            val pct = if (expense > 0) ((total / expense) * 100).toInt() else 0
            val prev = prevExpenseCats[name]
            val delta = if (prev != null && prev > 0) {
                val change = ((total - prev) / prev * 100).toInt()
                if (change > 0) " (↑${change}% vs прошлый мес.)"
                else if (change < 0) " (↓${-change}% vs прошлый мес.)"
                else " (без изменений)"
            } else ""
            sb.appendLine("- $name: ${formatMoney(total)} ($pct%)$delta")
        }
        sb.appendLine()
        sb.appendLine("--- Источники доходов ---")
        if (topIncomeCats.isEmpty()) sb.appendLine("- нет доходов")
        else topIncomeCats.forEach { (name, total) -> sb.appendLine("- $name: ${formatMoney(total)}") }
        sb.appendLine()
        sb.appendLine("--- Цели накоплений ---")
        if (goals.isEmpty()) sb.appendLine("- нет целей")
        else goals.forEach { g ->
            val pct = (g.progress * 100).toInt()
            val deadlineStr = g.deadline?.let { dl ->
                val daysLeft = ((dl - now) / 86_400_000).toInt()
                if (daysLeft > 0) ", осталось $daysLeft дней" else ", дедлайн прошёл"
            } ?: ""
            val remaining = g.targetAmount - g.savedAmount
            sb.appendLine("- ${g.name}: ${formatMoney(g.savedAmount)} / ${formatMoney(g.targetAmount)} ($pct%, осталось ${formatMoney(remaining)})$deadlineStr")
        }
        return sb.toString()
    }
}
