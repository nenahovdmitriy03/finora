package com.finora.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val iconKey: String,
    val color: Long,
    val isDefault: Boolean
)

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
