package com.finora.data.repository

import com.finora.data.local.AppDatabase
import com.finora.data.local.DefaultData
import com.finora.data.local.toDomain
import com.finora.data.local.toEntity
import com.finora.domain.model.Account
import com.finora.domain.model.AccountBalance
import com.finora.domain.model.Category
import com.finora.domain.model.Goal
import com.finora.domain.model.GoalContribution
import com.finora.domain.model.InterestPeriod
import com.finora.domain.model.Transaction
import com.finora.domain.model.TransactionDetails
import com.finora.domain.model.TransactionType
import com.finora.domain.model.Transfer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar

/**
 * Single source of truth for all finance data. Reads return [Flow]s of domain
 * models, writes are suspend functions.
 */
class FinanceRepository(private val db: AppDatabase) {

    private val accountDao = db.accountDao()
    private val categoryDao = db.categoryDao()
    private val transactionDao = db.transactionDao()
    private val goalDao = db.goalDao()
    private val goalContributionDao = db.goalContributionDao()
    private val transferDao = db.transferDao()

    private companion object {
        const val DAY_MS = 86_400_000L
        const val MAX_PERIODS = 400 // safety cap against huge backfills
    }

    // ─── Accounts ────────────────────────────────────────────────────────────

    fun observeAccounts(): Flow<List<Account>> =
        accountDao.observeAll().map { list -> list.map { it.toDomain() } }

    /**
     * Efficient account balances: SQL-aggregated transaction deltas + transfer
     * deltas, combined with initial balances.
     * Previous implementation loaded every transaction row into memory.
     */
    fun observeAccountBalances(): Flow<List<AccountBalance>> =
        combine(
            accountDao.observeAll(),
            transactionDao.observeBalanceDeltas(),
            transferDao.observeTransferDeltas()
        ) { accounts, txDeltas, transferDeltas ->
            // Merge all deltas into a single map keyed by accountId
            val deltaMap = mutableMapOf<Long, Double>()
            for (d in txDeltas) deltaMap[d.accountId] = (deltaMap[d.accountId] ?: 0.0) + d.delta
            for (d in transferDeltas) deltaMap[d.accountId] = (deltaMap[d.accountId] ?: 0.0) + d.delta
            accounts.map { acc ->
                AccountBalance(acc.toDomain(), acc.initialBalance + (deltaMap[acc.id] ?: 0.0))
            }
        }

    fun observeTotalBalance(): Flow<Double> =
        observeAccountBalances().map { list -> list.sumOf { it.balance } }

    suspend fun addAccount(account: Account): Long = accountDao.upsert(account.toEntity())
    suspend fun updateAccount(account: Account) = accountDao.update(account.toEntity())
    suspend fun deleteAccount(account: Account) {
        transactionDao.deleteByAccount(account.id)
        transferDao.deleteByAccount(account.id)
        goalContributionDao.deleteByAccount(account.id)
        accountDao.delete(account.toEntity())
    }

    // ─── Categories ──────────────────────────────────────────────────────────

    fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeCategories(type: TransactionType): Flow<List<Category>> =
        observeCategories().map { list -> list.filter { it.type == type } }

    suspend fun addCategory(category: Category): Long = categoryDao.upsert(category.toEntity())
    suspend fun deleteCategory(category: Category) = categoryDao.delete(category.toEntity())

    // ─── Transactions ────────────────────────────────────────────────────────

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

    // ─── Transfers ───────────────────────────────────────────────────────────

    fun observeTransfers(): Flow<List<Transfer>> =
        transferDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun addTransfer(transfer: Transfer): Long =
        transferDao.upsert(transfer.toEntity())

    suspend fun deleteTransfer(transfer: Transfer) =
        transferDao.delete(transfer.toEntity())

    // ─── Goals ───────────────────────────────────────────────────────────────

    fun observeGoals(): Flow<List<Goal>> =
        goalDao.observeAll().map { list -> list.map { it.toDomain() } }

    /** All contributions for all goals (for aggregation in the UI layer). */
    fun observeGoalContributions(): Flow<List<GoalContribution>> =
        goalContributionDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun addGoal(goal: Goal): Long = goalDao.upsert(goal.toEntity())
    suspend fun updateGoal(goal: Goal) { goalDao.upsert(goal.toEntity()) }
    suspend fun deleteGoal(goal: Goal) {
        goalContributionDao.deleteByGoal(goal.id)
        goalDao.delete(goal.toEntity())
    }

    /**
     * Moves [amount] between a goal and an account.
     *  - amount > 0 → deposit into goal, sourced from [accountId].
     *  - amount < 0 → withdraw from goal, returned to [accountId].
     *
     * **Key change vs. previous version**: account.initialBalance is NOT modified.
     * The money is still physically on the account; the goal only represents an
     * "earmark". The full balance is always shown on the account card.
     *
     * Every deposit/withdrawal is recorded in [goal_contributions] so we know
     * exactly which accounts funded the goal and by how much.
     */
    suspend fun contributeToGoal(goalId: Long, accountId: Long, amount: Double) {
        val goal = goalDao.getById(goalId)?.toDomain() ?: return
        accountDao.getById(accountId) ?: return // validate account exists
        val newSaved = (goal.savedAmount + amount).coerceAtLeast(0.0)
        val realized = newSaved - goal.savedAmount
        if (realized == 0.0) return

        // 1. Update the goal's saved amount (keep linkedAccountId for compat)
        goalDao.upsert(goal.copy(savedAmount = newSaved, linkedAccountId = accountId).toEntity())

        // 2. Record the contribution so the UI can show all contributing accounts
        goalContributionDao.insert(
            GoalContribution(
                goalId = goalId,
                accountId = accountId,
                amount = realized,
                date = System.currentTimeMillis()
            ).toEntity()
        )
        // Note: account.initialBalance is intentionally NOT changed.
    }

    // ─── Interest / capitalization ───────────────────────────────────────────

    /**
     * Accrues interest for every savings account whose payout period(s) elapsed
     * since [lastInterestAt] (or createdAt). Interest is booked as INCOME
     * transactions in the "Капитализация" category.
     *
     * For **daily** interest: uses 24 h (DAY_MS) periods.
     * For **monthly** interest: advances calendar month-by-month, landing on
     * [Account.interestPayoutDay] (clamped to the month's actual max day).
     *
     * Idempotent — safe to call on every launch.
     */
    suspend fun applyInterestAccruals(now: Long = System.currentTimeMillis()) {
        val savings = accountDao.getAll().map { it.toDomain() }.filter { it.hasInterest }
        if (savings.isEmpty()) return
        var capCategoryId: Long? = null

        for (acc in savings) {
            val period = acc.interestPeriod ?: continue
            val base = acc.lastInterestAt ?: acc.createdAt
            if (now <= base) continue

            val ratePerPeriod = acc.interestRate / 100.0 / period.periodsPerYear
            if (ratePerPeriod <= 0.0) {
                // Zero rate — just advance the clock
                accountDao.update(acc.copy(lastInterestAt = now).toEntity())
                continue
            }

            when (period) {
                InterestPeriod.DAILY -> {
                    val periodMs = DAY_MS
                    val periods = ((now - base) / periodMs).toInt().coerceIn(0, MAX_PERIODS)
                    if (periods <= 0) continue
                    val advanceTo = base + periods.toLong() * periodMs
                    val interest = compoundInterest(acc, ratePerPeriod, periods)
                    if (interest > 0.0) {
                        if (capCategoryId == null) capCategoryId = ensureCapitalizationCategory()
                        bookInterest(acc, interest, capCategoryId, now)
                    }
                    accountDao.update(acc.copy(lastInterestAt = advanceTo).toEntity())
                }

                InterestPeriod.MONTHLY -> {
                    // Advance month-by-month using the chosen payoutDay
                    val payouts = monthlyPayoutTimestamps(base, now, acc.interestPayoutDay, acc.interestPayoutMinute)
                    if (payouts.isEmpty()) continue
                    val periods = payouts.size.coerceAtMost(MAX_PERIODS)
                    val interest = compoundInterest(acc, ratePerPeriod, periods)
                    if (interest > 0.0) {
                        if (capCategoryId == null) capCategoryId = ensureCapitalizationCategory()
                        bookInterest(acc, interest, capCategoryId, now)
                    }
                    accountDao.update(acc.copy(lastInterestAt = payouts.last()).toEntity())
                }
            }
        }
    }

    /**
     * Returns a list of payout timestamps falling between (base, now] using
     * the given day-of-month and minute-of-day.
     */
    private fun monthlyPayoutTimestamps(
        base: Long,
        now: Long,
        payoutDay: Int,
        payoutMinute: Int
    ): List<Long> {
        val result = mutableListOf<Long>()
        val cal = Calendar.getInstance().apply { timeInMillis = base }
        // Start from the month after base and move forward
        cal.add(Calendar.MONTH, 1)
        for (i in 0 until MAX_PERIODS) {
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, payoutDay.coerceAtMost(maxDay))
            cal.set(Calendar.HOUR_OF_DAY, payoutMinute / 60)
            cal.set(Calendar.MINUTE, payoutMinute % 60)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val ts = cal.timeInMillis
            if (ts > now) break
            if (ts > base) result += ts
            cal.add(Calendar.MONTH, 1)
        }
        return result
    }

    /** Compound interest across [periods] at [ratePerPeriod] on [acc]'s current balance. */
    private suspend fun compoundInterest(acc: Account, ratePerPeriod: Double, periods: Int): Double {
        var balance = acc.initialBalance + transactionDao.balanceDelta(acc.id)
        var total = 0.0
        repeat(periods) {
            val gain = balance * ratePerPeriod
            total += gain
            balance += gain
        }
        return round2(total)
    }

    /** Book an interest income transaction. */
    private suspend fun bookInterest(acc: Account, amount: Double, categoryId: Long, date: Long) {
        transactionDao.upsert(
            Transaction(
                amount = amount,
                type = TransactionType.INCOME,
                accountId = acc.id,
                categoryId = categoryId,
                note = "Проценты по счёту «${acc.name}»",
                date = date
            ).toEntity()
        )
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

    // ─── Seeding ─────────────────────────────────────────────────────────────

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
