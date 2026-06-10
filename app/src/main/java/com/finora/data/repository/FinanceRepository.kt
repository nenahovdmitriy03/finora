package com.finora.data.repository

import com.finora.data.local.AppDatabase
import com.finora.data.local.DefaultData
import com.finora.data.local.toDomain
import com.finora.data.local.toEntity
import com.finora.domain.model.Account
import com.finora.domain.model.AccountBalance
import com.finora.domain.model.Category
import com.finora.domain.model.Goal
import com.finora.domain.model.InterestPeriod
import com.finora.domain.model.Transaction
import com.finora.domain.model.TransactionDetails
import com.finora.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Single source of truth for all finance data. Reads return [Flow]s of domain
 * models, writes are suspend functions.
 */
class FinanceRepository(private val db: AppDatabase) {

    private val accountDao = db.accountDao()
    private val categoryDao = db.categoryDao()
    private val transactionDao = db.transactionDao()
    private val goalDao = db.goalDao()

    private companion object {
        const val DAY_MS = 86_400_000L
        const val MAX_PERIODS = 400 // safety cap against huge backfills
    }

    // ---- Accounts ----
    fun observeAccounts(): Flow<List<Account>> =
        accountDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeAccountBalances(): Flow<List<AccountBalance>> =
        combine(accountDao.observeAll(), transactionDao.observeAll()) { accounts, txs ->
            accounts.map { acc ->
                val delta = txs.filter { it.accountId == acc.id }.sumOf {
                    if (it.type == TransactionType.INCOME.name) it.amount else -it.amount
                }
                AccountBalance(acc.toDomain(), acc.initialBalance + delta)
            }
        }

    fun observeTotalBalance(): Flow<Double> =
        observeAccountBalances().map { list -> list.sumOf { it.balance } }

    suspend fun addAccount(account: Account): Long = accountDao.upsert(account.toEntity())
    suspend fun updateAccount(account: Account) = accountDao.update(account.toEntity())
    suspend fun deleteAccount(account: Account) {
        transactionDao.deleteByAccount(account.id)
        accountDao.delete(account.toEntity())
    }

    // ---- Categories ----
    fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeCategories(type: TransactionType): Flow<List<Category>> =
        observeCategories().map { list -> list.filter { it.type == type } }

    suspend fun addCategory(category: Category): Long = categoryDao.upsert(category.toEntity())
    suspend fun deleteCategory(category: Category) = categoryDao.delete(category.toEntity())

    // ---- Transactions ----
    fun observeTransactionDetails(): Flow<List<TransactionDetails>> =
        combine(
            transactionDao.observeAll(),
            categoryDao.observeAll(),
            accountDao.observeAll()
        ) { txs, cats, accs ->
            val catMap = cats.associateBy { it.id }
            val accMap = accs.associateBy { it.id }
            txs.map { tx ->
                TransactionDetails(
                    transaction = tx.toDomain(),
                    category = tx.categoryId?.let { catMap[it]?.toDomain() },
                    account = accMap[tx.accountId]?.toDomain()
                )
            }
        }

    fun observeTransactionsBetween(from: Long, to: Long): Flow<List<Transaction>> =
        transactionDao.observeBetween(from, to).map { list -> list.map { it.toDomain() } }

    suspend fun addTransaction(transaction: Transaction): Long =
        transactionDao.upsert(transaction.toEntity())

    suspend fun deleteTransaction(transaction: Transaction) =
        transactionDao.delete(transaction.toEntity())

    suspend fun getTransaction(id: Long): Transaction? =
        transactionDao.getById(id)?.toDomain()

    // ---- Goals ----
    fun observeGoals(): Flow<List<Goal>> =
        goalDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun addGoal(goal: Goal): Long = goalDao.upsert(goal.toEntity())
    suspend fun updateGoal(goal: Goal) { goalDao.upsert(goal.toEntity()) }
    suspend fun deleteGoal(goal: Goal) = goalDao.delete(goal.toEntity())

    /**
     * Moves [amount] between a goal and an account.
     *  - amount > 0 → deposit into goal, debited from [accountId].
     *  - amount < 0 → withdraw from goal, credited back to [accountId].
     * The goal's saved amount never goes below 0, and the account balance is
     * adjusted by exactly the realized delta (so money is moved, not created).
     */
    suspend fun contributeToGoal(goalId: Long, accountId: Long, amount: Double) {
        val goal = goalDao.getById(goalId)?.toDomain() ?: return
        val account = accountDao.getById(accountId)?.toDomain() ?: return
        val newSaved = (goal.savedAmount + amount).coerceAtLeast(0.0)
        val realized = newSaved - goal.savedAmount
        if (realized == 0.0) return
        // Remember which account this goal's money is tied to (for display).
        goalDao.upsert(goal.copy(savedAmount = newSaved, linkedAccountId = accountId).toEntity())
        accountDao.upsert(account.copy(initialBalance = account.initialBalance - realized).toEntity())
    }

    // ---- Interest / capitalization ----
    /**
     * Accrues interest for every savings account whose payout period(s) elapsed
     * since [lastInterestAt] (or createdAt). Interest is booked as INCOME
     * transactions in the "Капитализация" category, so it shows up everywhere
     * balances are derived from transactions. Idempotent — safe to call on launch.
     */
    suspend fun applyInterestAccruals(now: Long = System.currentTimeMillis()) {
        val savings = accountDao.getAll().map { it.toDomain() }.filter { it.hasInterest }
        if (savings.isEmpty()) return
        var capCategoryId: Long? = null
        for (acc in savings) {
            val period = acc.interestPeriod ?: continue
            val periodMs = when (period) {
                InterestPeriod.DAILY -> DAY_MS
                InterestPeriod.MONTHLY -> 30L * DAY_MS
            }
            val base = acc.lastInterestAt ?: acc.createdAt
            if (now <= base) continue
            val periods = ((now - base) / periodMs).toInt().coerceIn(0, MAX_PERIODS)
            if (periods <= 0) continue
            val ratePerPeriod = acc.interestRate / 100.0 / period.periodsPerYear
            val advanceTo = base + periods.toLong() * periodMs
            if (ratePerPeriod <= 0.0) {
                accountDao.update(acc.copy(lastInterestAt = advanceTo).toEntity())
                continue
            }
            var balance = acc.initialBalance + transactionDao.balanceDelta(acc.id)
            var interestTotal = 0.0
            repeat(periods) {
                val gain = balance * ratePerPeriod
                interestTotal += gain
                balance += gain
            }
            val rounded = round2(interestTotal)
            if (rounded > 0.0) {
                if (capCategoryId == null) capCategoryId = ensureCapitalizationCategory()
                transactionDao.upsert(
                    Transaction(
                        amount = rounded,
                        type = TransactionType.INCOME,
                        accountId = acc.id,
                        categoryId = capCategoryId,
                        note = "Проценты по счёту «${acc.name}»",
                        date = now
                    ).toEntity()
                )
            }
            accountDao.update(acc.copy(lastInterestAt = advanceTo).toEntity())
        }
    }

    private suspend fun ensureCapitalizationCategory(): Long {
        categoryDao.findByNameAndType(
            DefaultData.CAPITALIZATION_CATEGORY,
            TransactionType.INCOME.name
        )?.let { return it.id }
        return categoryDao.upsert(
            Category(
                name = DefaultData.CAPITALIZATION_CATEGORY,
                type = TransactionType.INCOME,
                iconKey = "percent",
                color = 0xFF55EFC4,
                isDefault = true
            ).toEntity()
        )
    }

    private fun round2(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0

    // ---- Seeding ----
    suspend fun ensureSeeded() {
        if (categoryDao.count() == 0) {
            categoryDao.insertAll(DefaultData.categories())
        }
        if (accountDao.count() == 0) {
            accountDao.upsert(
                Account(
                    name = "Наличные",
                    type = com.finora.domain.model.AccountType.CASH,
                    initialBalance = 0.0,
                    color = 0xFF3FB18C,
                    iconKey = "cash"
                ).toEntity()
            )
        }
    }
}
