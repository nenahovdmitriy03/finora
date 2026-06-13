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
 *
 * When [enabled] is true, the app checks on startup (and periodically) whether
 * enough time has elapsed since [lastExecutedAt] to create the next occurrence.
 *
 * [periodDays] is how often the transaction should recur (e.g. 30 for monthly,
 * 7 for weekly, 1 for daily).
 */
@Serializable
@Entity(
    tableName = "recurring_rules",
    indices = [Index("categoryId"), Index("accountId")]
)
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Display name for the rule, e.g. "Подписка Netflix" */
    val name: String,
    val amount: Double,
    /** "INCOME" or "EXPENSE" */
    val type: String,
    val categoryId: Long,
    val accountId: Long,
    /** Repeat every N days. 30 ≈ monthly, 7 = weekly, 1 = daily. */
    val periodDays: Int,
    /** Timestamp of the last time a transaction was auto-created by this rule. */
    val lastExecutedAt: Long? = null,
    /** When the rule was first created. */
    val createdAt: Long,
    /** Whether the rule is active. */
    val enabled: Boolean = true
)
