package com.finora.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val initialBalance: Double,
    val color: Long,
    val iconKey: String,
    val createdAt: Long,
    /** Annual interest rate in percent. 0 for normal accounts. */
    val interestRate: Double = 0.0,
    /** "DAILY"/"MONTHLY" or null if no capitalization. */
    val interestPeriod: String? = null,
    /** Last capitalization timestamp; null = never applied. */
    val lastInterestAt: Long? = null,
    /** Payout time of day in minutes from midnight (0..1439). */
    val interestPayoutMinute: Int = 9 * 60,
    /** Day of month for monthly interest payout (1..31). Clamped to actual month length. */
    val interestPayoutDay: Int = 1
)

@Serializable
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val iconKey: String,
    val color: Long,
    val isDefault: Boolean
)

@Serializable
@Entity(
    tableName = "transactions",
    indices = [Index("accountId"), Index("categoryId"), Index("date")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String,
    val accountId: Long,
    val categoryId: Long?,
    val note: String,
    val date: Long,
    val createdAt: Long
)

@Serializable
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val iconKey: String,
    val color: Long,
    val deadline: Long?,
    val createdAt: Long,
    val planMonths: Int? = null,
    val plannedMonthlyAmount: Double? = null,
    /** @deprecated Kept for backward compat. Use goal_contributions table instead. */
    val linkedAccountId: Long? = null
)

/** Tracks every deposit/withdrawal to a goal from a specific account. */
@Serializable
@Entity(
    tableName = "goal_contributions",
    indices = [Index("goalId"), Index("accountId")]
)
data class GoalContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val accountId: Long,
    /** Positive = deposit into goal, negative = withdrawal. */
    val amount: Double,
    val date: Long
)

/** Money moved between two accounts (not income/expense). */
@Serializable
@Entity(
    tableName = "transfers",
    indices = [Index("fromAccountId"), Index("toAccountId"), Index("date")]
)
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromAccountId: Long,
    val toAccountId: Long,
    val amount: Double,
    val note: String = "",
    val date: Long,
    val createdAt: Long
)

/**
 * A rule describing a recurring (automatic) transaction.
 */
@Serializable
@Entity(
    tableName = "recurring_rules",
    indices = [Index("categoryId"), Index("accountId")]
)
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    /** "INCOME" or "EXPENSE" */
    val type: String,
    val categoryId: Long,
    val accountId: Long,
    val periodDays: Int,
    val lastExecutedAt: Long? = null,
    val createdAt: Long,
    val enabled: Boolean = true
)

// ─── Budgets ─────────────────────────────────────────────────────────────────

/**
 * Monthly (or custom-period) spending limit for a category.
 * Progress is computed dynamically from actual transactions.
 */
@Serializable
@Entity(
    tableName = "budgets",
    indices = [Index("categoryId")]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    /** Spending limit in rubles. */
    val limitAmount: Double,
    /** Period length in days (30 = monthly, 7 = weekly). */
    val periodDays: Int = 30,
    val createdAt: Long
)

// ─── Transaction templates ──────────────────────────────────────────────────

/**
 * Quick-fill template — user taps to pre-populate the add-transaction form.
 */
@Serializable
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    /** "INCOME" or "EXPENSE" */
    val type: String,
    val categoryId: Long?,
    val accountId: Long?,
    val note: String = "",
    val createdAt: Long
)

// ─── Tags ───────────────────────────────────────────────────────────────────

@Serializable
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Long
)

/** Many-to-many junction between transactions and tags. */
@Serializable
@Entity(
    tableName = "transaction_tags",
    primaryKeys = ["transactionId", "tagId"],
    indices = [Index("transactionId"), Index("tagId")]
)
data class TransactionTagEntity(
    val transactionId: Long,
    val tagId: Long
)

// ─── Challenges & achievements ──────────────────────────────────────────────

/**
 * A time-bound personal finance challenge.
 * [targetDays] — streak or duration required.
 * [targetAmount] — optional spending cap (for "spend less than X" challenges).
 * [categoryId] — optional: constraint to a specific category.
 */
@Serializable
@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val emoji: String = "🎯",
    /** Number of days the challenge runs. */
    val targetDays: Int,
    /** Optional max spend for the period (null = not a spending challenge). */
    val targetAmount: Double? = null,
    /** Optional category constraint. */
    val categoryId: Long? = null,
    val startDate: Long,
    val endDate: Long,
    /** Whether the user completed it. */
    val completed: Boolean = false,
    val createdAt: Long
)
