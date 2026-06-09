package com.finora.data.repository

import com.finora.data.local.AppDatabase
import com.finora.data.local.DefaultData
import com.finora.data.local.toDomain
import com.finora.data.local.toEntity
import com.finora.domain.model.Account
import com.finora.domain.model.AccountBalance
import com.finora.domain.model.Category
import com.finora.domain.model.Goal
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

    suspend fun contributeToGoal(goalId: Long, amount: Double) {
        val goal = goalDao.getById(goalId)?.toDomain() ?: return
        val updated = goal.copy(savedAmount = (goal.savedAmount + amount).coerceAtLeast(0.0))
        goalDao.upsert(updated.toEntity())
    }

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
                    color = 0xFF00B894,
                    iconKey = "cash"
                ).toEntity()
            )
        }
    }
}
