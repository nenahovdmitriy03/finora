package com.finora.presentation.tips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class ProductRecommendation(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val detail: String,
    val url: String,
    val source: String // "banki.ru" | "sravni.ru"
)

data class CashbackRecommendation(
    val cardName: String,
    val bank: String,
    val matchedCategory: String,
    val cashbackPercent: String,
    val monthlySaving: String,
    val url: String
)

data class RecommendationsUiState(
    val totalBalance: Double = 0.0,
    val idleBalance: Double = 0.0,
    val topSpendingCategory: String? = null,
    val topSpendingAmount: Double = 0.0,
    val deposits: List<ProductRecommendation> = emptyList(),
    val cashbackCards: List<CashbackRecommendation> = emptyList(),
    val savingsAccounts: List<ProductRecommendation> = emptyList(),
    val loading: Boolean = true
)

/**
 * Builds personalized financial product recommendations based on user data,
 * with deep links to Банки.ру and Сравни.ру comparison pages.
 */
class RecommendationsViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecommendationsUiState())
    val state: StateFlow<RecommendationsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }

            val accounts = repository.observeAccountBalances().first()
            val transactions = repository.observeTransactionDetails().first()

            val totalBalance = accounts.sumOf { it.balance }

            // Identify "idle" money (non-savings accounts with positive balance)
            val idleBalance = accounts
                .filter { !it.account.hasInterest && it.balance > 0 }
                .sumOf { it.balance }

            // Top spending category this month
            val now = System.currentTimeMillis()
            val monthStart = startOfMonth(now)
            val monthTx = transactions.filter {
                it.transaction.type == TransactionType.EXPENSE &&
                    it.transaction.date >= monthStart
            }
            val topCat = monthTx
                .groupBy { it.category?.name ?: "Прочее" }
                .mapValues { (_, list) -> list.sumOf { it.transaction.amount } }
                .maxByOrNull { it.value }

            // Build recommendations
            val deposits = buildDepositRecommendations(idleBalance)
            val savings = buildSavingsRecommendations(idleBalance)
            val cashback = buildCashbackRecommendations(
                topCat?.key, topCat?.value ?: 0.0, monthTx
                    .groupBy { it.category?.name ?: "Прочее" }
                    .mapValues { (_, list) -> list.sumOf { it.transaction.amount } }
            )

            _state.update {
                it.copy(
                    totalBalance = totalBalance,
                    idleBalance = idleBalance,
                    topSpendingCategory = topCat?.key,
                    topSpendingAmount = topCat?.value ?: 0.0,
                    deposits = deposits,
                    cashbackCards = cashback,
                    savingsAccounts = savings,
                    loading = false
                )
            }
        }
    }

    private fun buildDepositRecommendations(amount: Double): List<ProductRecommendation> {
        val amountParam = when {
            amount >= 1_000_000 -> "1000000"
            amount >= 500_000 -> "500000"
            amount >= 100_000 -> "100000"
            amount >= 50_000 -> "50000"
            else -> "10000"
        }
        return listOf(
            ProductRecommendation(
                emoji = "🏦",
                title = "Вклады с высокой ставкой",
                subtitle = "До 23% годовых",
                detail = if (amount > 0) "При вложении ${formatMoney(amount)} доход ~${formatMoney(amount * 0.21 / 12)}/мес"
                else "Сравни ставки 50+ банков",
                url = "https://www.banki.ru/products/deposits/?amount=$amountParam&period=365",
                source = "banki.ru"
            ),
            ProductRecommendation(
                emoji = "📈",
                title = "Накопительные счета",
                subtitle = "До 22% без фиксации срока",
                detail = "Деньги всегда доступны, проценты каждый день",
                url = "https://www.sravni.ru/vklady/nakopitelnye-scheta/",
                source = "sravni.ru"
            ),
            ProductRecommendation(
                emoji = "🔒",
                title = "Надёжные вклады (топ-10 банков)",
                subtitle = "Системно значимые банки",
                detail = "Сбер, ВТБ, Альфа, Тинькофф — до 21%",
                url = "https://www.banki.ru/products/deposits/?bank_id=322,2474,1490,2673",
                source = "banki.ru"
            )
        )
    }

    private fun buildSavingsRecommendations(amount: Double): List<ProductRecommendation> {
        return listOf(
            ProductRecommendation(
                emoji = "💳",
                title = "Дебетовые карты с процентом",
                subtitle = "До 20% на остаток",
                detail = "Процент на остаток + кэшбэк на покупки",
                url = "https://www.sravni.ru/debetovye-karty/s-procentom-na-ostatok/",
                source = "sravni.ru"
            ),
            ProductRecommendation(
                emoji = "🏗️",
                title = "ИИС — инвестиционный вычет",
                subtitle = "Возврат до 52 000 ₽/год",
                detail = "Пополни ИИС на 400 000 ₽ и получи 13% от государства",
                url = "https://www.banki.ru/investment/iis/",
                source = "banki.ru"
            )
        )
    }

    private fun buildCashbackRecommendations(
        topCategory: String?,
        topAmount: Double,
        allCategories: Map<String, Double>
    ): List<CashbackRecommendation> {
        if (topCategory == null) return emptyList()

        // Map spending categories to best known cashback cards
        data class CardInfo(
            val name: String, val bank: String, val percent: String,
            val categories: List<String>, val url: String
        )

        val cards = listOf(
            CardInfo("Tinkoff Black", "Тинькофф", "1–15%",
                listOf("Продукты", "Кафе и рестораны", "Развлечения", "Покупки", "Транспорт"),
                "https://www.sravni.ru/debetovye-karty/tinkoff-blek/"
            ),
            CardInfo("Альфа-Карта", "Альфа-Банк", "2–33%",
                listOf("Продукты", "Кафе и рестораны", "Транспорт", "Развлечения"),
                "https://www.sravni.ru/debetovye-karty/alfa-karta/"
            ),
            CardInfo("СберКарта", "Сбер", "0.5–5%",
                listOf("Продукты", "Кафе и рестораны", "Покупки", "Транспорт"),
                "https://www.sravni.ru/debetovye-karty/sber-karta/"
            ),
            CardInfo("Газпромбанк", "Газпромбанк", "1–10%",
                listOf("Продукты", "Покупки", "Здоровье", "Путешествия"),
                "https://www.sravni.ru/debetovye-karty/gazprombank/"
            ),
            CardInfo("Яндекс Pay", "Яндекс", "1–10%",
                listOf("Продукты", "Кафе и рестораны", "Покупки", "Подписки"),
                "https://www.sravni.ru/debetovye-karty/yandex-pay/"
            )
        )

        return cards
            .filter { card -> card.categories.any { cat -> allCategories.containsKey(cat) } }
            .take(4)
            .map { card ->
                val bestMatch = card.categories
                    .filter { allCategories.containsKey(it) }
                    .maxByOrNull { allCategories[it] ?: 0.0 }
                    ?: topCategory
                val spending = allCategories[bestMatch] ?: topAmount
                val estSaving = spending * 0.05 // ~5% average cashback estimate

                CashbackRecommendation(
                    cardName = card.name,
                    bank = card.bank,
                    matchedCategory = bestMatch,
                    cashbackPercent = card.percent,
                    monthlySaving = "~${formatMoney(estSaving)}/мес",
                    url = card.url
                )
            }
    }
}
