package com.finora.data.local

import com.finora.data.local.entity.AccountEntity
import com.finora.data.local.entity.BudgetEntity
import com.finora.data.local.entity.CategoryEntity
import com.finora.data.local.entity.ChallengeEntity
import com.finora.data.local.entity.GoalContributionEntity
import com.finora.data.local.entity.GoalEntity
import com.finora.data.local.entity.TagEntity
import com.finora.data.local.entity.TemplateEntity
import com.finora.data.local.entity.TransactionEntity
import com.finora.data.local.entity.TransferEntity
import com.finora.domain.model.Account
import com.finora.domain.model.AccountType
import com.finora.domain.model.Budget
import com.finora.domain.model.Category
import com.finora.domain.model.Challenge
import com.finora.domain.model.Goal
import com.finora.domain.model.GoalContribution
import com.finora.domain.model.InterestPeriod
import com.finora.domain.model.Tag
import com.finora.domain.model.Template
import com.finora.domain.model.Transaction
import com.finora.domain.model.TransactionType
import com.finora.domain.model.Transfer

private fun parseAccountType(value: String): AccountType =
    AccountType.entries.firstOrNull { it.name == value } ?: AccountType.OTHER

private fun parseInterestPeriod(value: String?): InterestPeriod? =
    value?.let { v -> InterestPeriod.entries.firstOrNull { it.name == v } }

private fun parseTransactionType(value: String): TransactionType =
    TransactionType.entries.firstOrNull { it.name == value } ?: TransactionType.EXPENSE

// ─── Account ─────────────────────────────────────────────────────────────────

fun AccountEntity.toDomain() = Account(
    id = id,
    name = name,
    type = parseAccountType(type),
    initialBalance = initialBalance,
    color = color,
    iconKey = iconKey,
    createdAt = createdAt,
    interestRate = interestRate,
    interestPeriod = parseInterestPeriod(interestPeriod),
    lastInterestAt = lastInterestAt,
    interestPayoutMinute = interestPayoutMinute,
    interestPayoutDay = interestPayoutDay
)

fun Account.toEntity() = AccountEntity(
    id = id,
    name = name,
    type = type.name,
    initialBalance = initialBalance,
    color = color,
    iconKey = iconKey,
    createdAt = createdAt,
    interestRate = interestRate,
    interestPeriod = interestPeriod?.name,
    lastInterestAt = lastInterestAt,
    interestPayoutMinute = interestPayoutMinute,
    interestPayoutDay = interestPayoutDay
)

// ─── Category ────────────────────────────────────────────────────────────────

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

// ─── Transaction ─────────────────────────────────────────────────────────────

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

// ─── Goal ────────────────────────────────────────────────────────────────────

fun GoalEntity.toDomain() = Goal(
    id = id,
    name = name,
    targetAmount = targetAmount,
    savedAmount = savedAmount,
    iconKey = iconKey,
    color = color,
    deadline = deadline,
    createdAt = createdAt,
    planMonths = planMonths,
    plannedMonthlyAmount = plannedMonthlyAmount,
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
    planMonths = planMonths,
    plannedMonthlyAmount = plannedMonthlyAmount,
    linkedAccountId = linkedAccountId
)

// ─── Goal Contribution ──────────────────────────────────────────────────────

fun GoalContributionEntity.toDomain() = GoalContribution(
    id = id,
    goalId = goalId,
    accountId = accountId,
    amount = amount,
    date = date
)

fun GoalContribution.toEntity() = GoalContributionEntity(
    id = id,
    goalId = goalId,
    accountId = accountId,
    amount = amount,
    date = date
)

// ─── Transfer ────────────────────────────────────────────────────────────────

fun TransferEntity.toDomain() = Transfer(
    id = id,
    fromAccountId = fromAccountId,
    toAccountId = toAccountId,
    amount = amount,
    note = note,
    date = date,
    createdAt = createdAt
)

fun Transfer.toEntity() = TransferEntity(
    id = id,
    fromAccountId = fromAccountId,
    toAccountId = toAccountId,
    amount = amount,
    note = note,
    date = date,
    createdAt = createdAt
)

// ─── Budget ─────────────────────────────────────────────────────────────────

fun BudgetEntity.toDomain() = Budget(
    id = id,
    categoryId = categoryId,
    limitAmount = limitAmount,
    periodDays = periodDays,
    createdAt = createdAt
)

fun Budget.toEntity() = BudgetEntity(
    id = id,
    categoryId = categoryId,
    limitAmount = limitAmount,
    periodDays = periodDays,
    createdAt = createdAt
)

// ─── Template ───────────────────────────────────────────────────────────────

fun TemplateEntity.toDomain() = Template(
    id = id,
    name = name,
    amount = amount,
    type = parseTransactionType(type),
    categoryId = categoryId,
    accountId = accountId,
    note = note,
    createdAt = createdAt
)

fun Template.toEntity() = TemplateEntity(
    id = id,
    name = name,
    amount = amount,
    type = type.name,
    categoryId = categoryId,
    accountId = accountId,
    note = note,
    createdAt = createdAt
)

// ─── Tag ────────────────────────────────────────────────────────────────────

fun TagEntity.toDomain() = Tag(id = id, name = name, color = color)
fun Tag.toEntity() = TagEntity(id = id, name = name, color = color)

// ─── Challenge ──────────────────────────────────────────────────────────────

fun ChallengeEntity.toDomain() = Challenge(
    id = id,
    title = title,
    description = description,
    emoji = emoji,
    targetDays = targetDays,
    targetAmount = targetAmount,
    categoryId = categoryId,
    startDate = startDate,
    endDate = endDate,
    completed = completed,
    createdAt = createdAt
)

fun Challenge.toEntity() = ChallengeEntity(
    id = id,
    title = title,
    description = description,
    emoji = emoji,
    targetDays = targetDays,
    targetAmount = targetAmount,
    categoryId = categoryId,
    startDate = startDate,
    endDate = endDate,
    completed = completed,
    createdAt = createdAt
)
