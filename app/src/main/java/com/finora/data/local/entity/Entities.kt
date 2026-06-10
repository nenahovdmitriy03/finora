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
    val lastInterestAt: Long? = null
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
    /** Account the goal is funded from (last used in a contribution). Nullable for legacy rows. */
    val linkedAccountId: Long? = null
)
