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

/** Capitalization payout frequency for interest-bearing accounts. */
enum class InterestPeriod(val title: String, val periodsPerYear: Int) {
    DAILY("Каждый день", 365),
    MONTHLY("Раз в месяц", 12)
}

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType = AccountType.CARD,
    val initialBalance: Double = 0.0,
    val color: Long = 0xFF6C5CE7,
    val iconKey: String = "wallet",
    val createdAt: Long = System.currentTimeMillis(),
    /** Annual interest rate in percent (0 = no capitalization). */
    val interestRate: Double = 0.0,
    /** How often interest is paid out, null = disabled. */
    val interestPeriod: InterestPeriod? = null,
    /** Timestamp of the last applied capitalization (null = never). */
    val lastInterestAt: Long? = null,
    /** Time of day (minutes from midnight, 0..1439) when interest is paid out. */
    val interestPayoutMinute: Int = 9 * 60,
    /** Day of month (1..31) for monthly interest payout. Clamped at runtime. */
    val interestPayoutDay: Int = 1
) {
    val hasInterest: Boolean get() = interestPeriod != null && interestRate > 0.0
}

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
    val planMonths: Int? = null,
    val plannedMonthlyAmount: Double? = null,
    /** @deprecated Kept for Room compat; use GoalContribution instead. */
    val linkedAccountId: Long? = null
) {
    val progress: Float
        get() = if (targetAmount <= 0) 0f else (savedAmount / targetAmount).toFloat().coerceIn(0f, 1f)
}

/** One deposit/withdrawal for a goal from a specific account. */
data class GoalContribution(
    val id: Long = 0,
    val goalId: Long,
    val accountId: Long,
    val amount: Double,
    val date: Long = System.currentTimeMillis()
)

/** Money transfer between two accounts (doesn't affect income/expense stats). */
data class Transfer(
    val id: Long = 0,
    val fromAccountId: Long,
    val toAccountId: Long,
    val amount: Double,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

/** A transaction joined with its category + account for display. */
data class TransactionDetails(
    val transaction: Transaction,
    val category: Category?,
    val account: Account?,
    val tags: List<Tag> = emptyList()
)

/** Aggregated spend/earn for one category, used by statistics. */
data class CategoryStat(
    val category: Category,
    val total: Double,
    val share: Float,
    val count: Int = 0
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

/** How much a single account contributed (net) to a goal. */
data class GoalAccountSummary(
    val account: Account,
    val netAmount: Double
)

// ─── Budget ─────────────────────────────────────────────────────────────────

data class Budget(
    val id: Long = 0,
    val categoryId: Long,
    val limitAmount: Double,
    val periodDays: Int = 30,
    val createdAt: Long = System.currentTimeMillis()
)

/** Budget + actual spend for display. */
data class BudgetProgress(
    val budget: Budget,
    val category: Category,
    val spent: Double
) {
    val ratio: Float
        get() = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat().coerceIn(0f, 2f) else 0f
    val overBudget: Boolean get() = spent > budget.limitAmount
}

// ─── Template ───────────────────────────────────────────────────────────────

data class Template(
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Tag ────────────────────────────────────────────────────────────────────

data class Tag(
    val id: Long = 0,
    val name: String,
    val color: Long = 0xFF6C5CE7
)

// ─── Challenge ──────────────────────────────────────────────────────────────

data class Challenge(
    val id: Long = 0,
    val title: String,
    val description: String,
    val emoji: String = "🎯",
    val targetDays: Int,
    val targetAmount: Double? = null,
    val categoryId: Long? = null,
    val startDate: Long,
    val endDate: Long,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isActive: Boolean get() = !completed && endDate >= System.currentTimeMillis()
    val daysTotal: Int get() = ((endDate - startDate) / 86_400_000).toInt().coerceAtLeast(1)
    val daysPassed: Int get() = ((System.currentTimeMillis() - startDate) / 86_400_000).toInt().coerceIn(0, daysTotal)
    val progress: Float get() = daysPassed.toFloat() / daysTotal.toFloat()
}
