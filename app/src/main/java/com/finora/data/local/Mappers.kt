package com.finora.data.local

import com.finora.data.local.entity.AccountEntity
import com.finora.data.local.entity.CategoryEntity
import com.finora.data.local.entity.GoalEntity
import com.finora.data.local.entity.TransactionEntity
import com.finora.domain.model.Account
import com.finora.domain.model.AccountType
import com.finora.domain.model.Category
import com.finora.domain.model.Goal
import com.finora.domain.model.Transaction
import com.finora.domain.model.TransactionType

private fun parseAccountType(value: String): AccountType =
    AccountType.entries.firstOrNull { it.name == value } ?: AccountType.OTHER

private fun parseTransactionType(value: String): TransactionType =
    TransactionType.entries.firstOrNull { it.name == value } ?: TransactionType.EXPENSE

fun AccountEntity.toDomain() = Account(
    id = id,
    name = name,
    type = parseAccountType(type),
    initialBalance = initialBalance,
    color = color,
    iconKey = iconKey,
    createdAt = createdAt
)

fun Account.toEntity() = AccountEntity(
    id = id,
    name = name,
    type = type.name,
    initialBalance = initialBalance,
    color = color,
    iconKey = iconKey,
    createdAt = createdAt
)

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    type = parseTransactionType(type),
    iconKey = iconKey,
    color = color,
    isDefault = isDefault
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    type = type.name,
    iconKey = iconKey,
    color = color,
    isDefault = isDefault
)

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    amount = amount,
    type = parseTransactionType(type),
    accountId = accountId,
    categoryId = categoryId,
    note = note,
    date = date,
    createdAt = createdAt
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    amount = amount,
    type = type.name,
    accountId = accountId,
    categoryId = categoryId,
    note = note,
    date = date,
    createdAt = createdAt
)

fun GoalEntity.toDomain() = Goal(
    id = id,
    name = name,
    targetAmount = targetAmount,
    savedAmount = savedAmount,
    iconKey = iconKey,
    color = color,
    deadline = deadline,
    createdAt = createdAt,
    linkedAccountId = linkedAccountId
)

fun Goal.toEntity() = GoalEntity(
    id = id,
    name = name,
    targetAmount = targetAmount,
    savedAmount = savedAmount,
    iconKey = iconKey,
    color = color,
    deadline = deadline,
    createdAt = createdAt,
    linkedAccountId = linkedAccountId
)
