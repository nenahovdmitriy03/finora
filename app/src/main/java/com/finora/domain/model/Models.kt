package com.finora.domain.model

/** Income or expense. */
enum class TransactionType { INCOME, EXPENSE }

/** App appearance preference. */
enum class ThemeMode(val title: String) {
    SYSTEM("Системная"),
    LIGHT("Светлая"),
    DARK("Тёмная")
}

/**
 * Accent (primary) color the user can pick in Settings.
 * [seed] — main accent, [seedDark] — pressed/dark-container shade,
 * [container] — soft tint used as primaryContainer in light theme.
 */
enum class AccentColor(
    val title: String,
    val seed: Long,
    val seedDark: Long,
    val container: Long
) {
    VIOLET("Лиловый", 0xFF6C5CE7, 0xFF5A4FD6, 0xFFEDEBFE),
    INDIGO("Индиго", 0xFF4C6EF5, 0xFF3B5BDB, 0xFFE7ECFD),
    BLUE("Голубой", 0xFF0E9CE6, 0xFF0B7FC0, 0xFFE2F2FC),
    TEAL("Бирюзовый", 0xFF12A594, 0xFF0E8576, 0xFFDFF5F1),
    GREEN("Зелёный", 0xFF2FA86A, 0xFF258A57, 0xFFE2F4EA),
    AMBER("Янтарь", 0xFFE8893A, 0xFFCF6F2B, 0xFFFCEDE0),
    PINK("Розовый", 0xFFE8568F, 0xFFCF4078, 0xFFFCE6F0),
    SLATE("Графит", 0xFF5A677A, 0xFF45505F, 0xFFE8EBEF)
}

/** Kind of account / bank where money is stored. */
enum class AccountType(val title: String) {
    CARD("Карта"),
    CASH("Наличные"),
    SAVINGS("Накопительный"),
    DEPOSIT("Вклад"),
    OTHER("Другое")
}

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType = AccountType.CARD,
    val initialBalance: Double = 0.0,
    val color: Long = 0xFF6C5CE7,
    val iconKey: String = "wallet",
    val createdAt: Long = System.currentTimeMillis()
)

data class Category(
    val id: Long = 0,
    val name: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val iconKey: String = "category",
    val color: Long = 0xFF6C5CE7,
    val isDefault: Boolean = false
)

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val accountId: Long,
    val categoryId: Long?,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

data class Goal(
    val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val iconKey: String = "target",
    val color: Long = 0xFF3FB18C,
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** Account the goal's money is currently tied to (last contribution source). */
    val linkedAccountId: Long? = null
) {
    val progress: Float
        get() = if (targetAmount <= 0) 0f else (savedAmount / targetAmount).toFloat().coerceIn(0f, 1f)
}

/** A transaction joined with its category + account for display. */
data class TransactionDetails(
    val transaction: Transaction,
    val category: Category?,
    val account: Account?
)

/** Aggregated spend/earn for one category, used by statistics. */
data class CategoryStat(
    val category: Category,
    val total: Double,
    val share: Float
)

/** One bucket on the time axis of the trend chart. */
data class PeriodPoint(
    val label: String,
    val income: Double,
    val expense: Double
)

/** A bank/account together with its current computed balance. */
data class AccountBalance(
    val account: Account,
    val balance: Double
)
