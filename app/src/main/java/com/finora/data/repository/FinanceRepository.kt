package com.finora.data.repository

import com.finora.data.local.AppDatabase
import com.finora.data.local.DefaultData
import com.finora.data.local.toDomain
import com.finora.data.local.toEntity
import com.finora.data.local.entity.TransactionTagEntity
import com.finora.data.local.entity.RecurringRuleEntity
import com.finora.data.remote.AuthRepository
import com.finora.data.remote.SyncManager
import com.finora.domain.model.Account
import com.finora.domain.model.AccountBalance
import com.finora.domain.model.Budget
import com.finora.domain.model.BudgetProgress
import com.finora.domain.model.Category
import com.finora.domain.model.Challenge
import com.finora.domain.model.Goal
import com.finora.domain.model.GoalContribution
import com.finora.domain.model.InterestPeriod
import com.finora.domain.model.Tag
import com.finora.domain.model.Template
import com.finora.domain.model.Transaction
import com.finora.domain.model.TransactionDetails
import com.finora.domain.model.TransactionType
import com.finora.domain.model.Transfer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar

/** Aggregated income / expense totals for a period (e.g. the current month). */
data class MonthTotals(val income: Double, val expense: Double)

/**
 * Single source of truth for all finance data. Reads return [Flow]s of domain
 * models, writes are suspend functions.
 */
class FinanceRepository(
    private val db: AppDatabase,
    private val syncManager: SyncManager? = null,
    private val authRepository: AuthRepository? = null
) {

    private val accountDao = db.accountDao()
    private val categoryDao = db.categoryDao()
    private val transactionDao = db.transactionDao()
    private val goalDao = db.goalDao()
    private val goalContributionDao = db.goalContributionDao()
    private val transferDao = db.transferDao()
    private val budgetDao = db.budgetDao()
    private val templateDao = db.templateDao()
    private val tagDao = db.tagDao()
    private val transactionTagDao = db.transactionTagDao()
    private val challengeDao = db.challengeDao()
    private val recurringRuleDao = db.recurringRuleDao()

    private companion object {
        const val DAY_MS = 86_400_000L
        const val MAX_PERIODS = 400 // safety cap against huge backfills
    }

    /** Triggers a debounced cloud upload after data mutations. */
    private fun triggerCloudSync() {
        val userId = authRepository?.currentUserId() ?: return
        syncManager?.scheduleUpload(userId)
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

    suspend fun addAccount(account: Account): Long =
        accountDao.upsert(account.toEntity()).also { triggerCloudSync() }
    suspend fun updateAccount(account: Account) {
        accountDao.update(account.toEntity()); triggerCloudSync()
    }
    suspend fun deleteAccount(account: Account) {
        transactionDao.deleteByAccount(account.id)
        transferDao.deleteByAccount(account.id)
        goalContributionDao.deleteByAccount(account.id)
        accountDao.delete(account.toEntity())
        triggerCloudSync()
    }

    // ─── Categories ──────────────────────────────────────────────────────────

    fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeCategories(type: TransactionType): Flow<List<Category>> =
        observeCategories().map { list -> list.filter { it.type == type } }

    suspend fun addCategory(category: Category): Long =
        categoryDao.upsert(category.toEntity()).also { triggerCloudSync() }
    suspend fun deleteCategory(category: Category) {
        categoryDao.delete(category.toEntity()); triggerCloudSync()
    }

    // ─── Transactions ────────────────────────────────────────────────────────

    fun observeTransactionDetails(): Flow<List<TransactionDetails>> =
        combine(
            transactionDao.observeAll(),
            categoryDao.observeAll(),
            accountDao.observeAll(),
            tagDao.observeAll(),
            transactionTagDao.observeAll()
        ) { txs, cats, accs, tags, txTags ->
            val catMap = cats.associateBy { it.id }
            val accMap = accs.associateBy { it.id }
            val tagMap = tags.associateBy { it.id }
            val tagsByTx = txTags.groupBy({ it.transactionId }, { it.tagId })
            txs.map { tx ->
                TransactionDetails(
                    transaction = tx.toDomain(),
                    category = tx.categoryId?.let { catMap[it]?.toDomain() },
                    account = accMap[tx.accountId]?.toDomain(),
                    tags = tagsByTx[tx.id]?.mapNotNull { tagMap[it]?.toDomain() } ?: emptyList()
                )
            }
        }

    /**
     * Recent transaction details (with category + account), limited to [limit] rows.
     * Efficient alternative to [observeTransactionDetails] for the home preview —
     * only loads the N newest rows instead of the entire table.
     */
    fun observeRecentTransactionDetails(limit: Int): Flow<List<TransactionDetails>> =
        combine(
            transactionDao.observeRecent(limit),
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

    /** Income / expense totals since [since], aggregated in SQL (no in-memory summation). */
    fun observeMonthTotals(since: Long): Flow<MonthTotals> =
        transactionDao.observeTotalsSince(since).map { rows ->
            var income = 0.0
            var expense = 0.0
            for (r in rows) {
                when (r.type) {
                    TransactionType.INCOME.name -> income += r.total
                    TransactionType.EXPENSE.name -> expense += r.total
                }
            }
            MonthTotals(income, expense)
        }

    fun observeTransactionsBetween(from: Long, to: Long): Flow<List<Transaction>> =
        transactionDao.observeBetween(from, to).map { list -> list.map { it.toDomain() } }

    suspend fun addTransaction(transaction: Transaction): Long =
        transactionDao.upsert(transaction.toEntity()).also { triggerCloudSync() }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.delete(transaction.toEntity()); triggerCloudSync()
    }

    suspend fun getTransaction(id: Long): Transaction? =
        transactionDao.getById(id)?.toDomain()

    // ─── Transfers ───────────────────────────────────────────────────────────

    fun observeTransfers(): Flow<List<Transfer>> =
        transferDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun addTransfer(transfer: Transfer): Long =
        transferDao.upsert(transfer.toEntity()).also { triggerCloudSync() }

    suspend fun deleteTransfer(transfer: Transfer) {
        transferDao.delete(transfer.toEntity()); triggerCloudSync()
    }

    // ─── Goals ───────────────────────────────────────────────────────────────

    fun observeGoals(): Flow<List<Goal>> =
        goalDao.observeAll().map { list -> list.map { it.toDomain() } }

    /** All contributions for all goals (for aggregation in the UI layer). */
    fun observeGoalContributions(): Flow<List<GoalContribution>> =
        goalContributionDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun addGoal(goal: Goal): Long =
        goalDao.upsert(goal.toEntity()).also { triggerCloudSync() }
    suspend fun updateGoal(goal: Goal) {
        goalDao.upsert(goal.toEntity()); triggerCloudSync()
    }
    suspend fun deleteGoal(goal: Goal) {
        goalContributionDao.deleteByGoal(goal.id)
        goalDao.delete(goal.toEntity())
        triggerCloudSync()
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
        triggerCloudSync()
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

    // ─── Budgets ──────────────────────────────────────────────────────────────

    fun observeBudgets(): Flow<List<Budget>> =
        budgetDao.observeAll().map { list -> list.map { it.toDomain() } }

    /**
     * Returns budget progress by combining budget limits with actual expenses
     * in the current period window (last [budget.periodDays] days).
     */
    fun observeBudgetProgress(): Flow<List<BudgetProgress>> =
        combine(
            budgetDao.observeAll(),
            categoryDao.observeAll(),
            transactionDao.observeAll()
        ) { budgets, cats, txs ->
            val catMap = cats.associateBy { it.id }
            val now = System.currentTimeMillis()
            budgets.mapNotNull { be ->
                val cat = catMap[be.categoryId]?.toDomain() ?: return@mapNotNull null
                val windowStart = now - be.periodDays.toLong() * DAY_MS
                val spent = txs
                    .filter { it.type == TransactionType.EXPENSE.name && it.categoryId == be.categoryId && it.date >= windowStart }
                    .sumOf { it.amount }
                BudgetProgress(be.toDomain(), cat, spent)
            }
        }

    suspend fun addBudget(budget: Budget): Long =
        budgetDao.upsert(budget.toEntity()).also { triggerCloudSync() }

    suspend fun deleteBudget(budget: Budget) {
        budgetDao.delete(budget.toEntity())
        triggerCloudSync()
    }

    // ─── Templates ──────────────────────────────────────────────────────────

    fun observeTemplates(): Flow<List<Template>> =
        templateDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun addTemplate(template: Template): Long =
        templateDao.upsert(template.toEntity()).also { triggerCloudSync() }

    suspend fun deleteTemplate(template: Template) {
        templateDao.delete(template.toEntity())
        triggerCloudSync()
    }

    // ─── Tags ───────────────────────────────────────────────────────────────

    fun observeTags(): Flow<List<Tag>> =
        tagDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun addTag(tag: Tag): Long =
        tagDao.upsert(tag.toEntity()).also { triggerCloudSync() }

    suspend fun deleteTag(tag: Tag) {
        transactionTagDao.deleteByTag(tag.id)
        tagDao.delete(tag.toEntity())
        triggerCloudSync()
    }

    /** Replace all tag links for a transaction with the given set. */
    suspend fun setTransactionTags(transactionId: Long, tagIds: Set<Long>) {
        transactionTagDao.deleteByTransaction(transactionId)
        tagIds.forEach { tagId ->
            transactionTagDao.insert(TransactionTagEntity(transactionId, tagId))
        }
        triggerCloudSync()
    }

    fun observeTransactionTagIds(transactionId: Long): Flow<List<Long>> =
        transactionTagDao.observeTagIds(transactionId)

    suspend fun getTransactionTagIds(transactionId: Long): List<Long> =
        transactionTagDao.getTagIds(transactionId)

    // ─── Challenges ─────────────────────────────────────────────────────────

    fun observeChallenges(): Flow<List<Challenge>> =
        challengeDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActiveChallenges(): Flow<List<Challenge>> =
        challengeDao.observeActive(System.currentTimeMillis())
            .map { list -> list.map { it.toDomain() } }

    suspend fun addChallenge(challenge: Challenge): Long =
        challengeDao.upsert(challenge.toEntity()).also { triggerCloudSync() }

    suspend fun deleteChallenge(challenge: Challenge) {
        challengeDao.delete(challenge.toEntity())
        triggerCloudSync()
    }

    suspend fun completeChallenge(id: Long) {
        challengeDao.markCompleted(id)
        triggerCloudSync()
    }

    suspend fun addRecurringRule(rule: RecurringRuleEntity): Long =
        recurringRuleDao.upsert(rule).also { triggerCloudSync() }

    suspend fun updateRecurringRule(rule: RecurringRuleEntity) {
        recurringRuleDao.upsert(rule)
        triggerCloudSync()
    }

    suspend fun deleteRecurringRule(rule: RecurringRuleEntity) {
        recurringRuleDao.delete(rule)
        triggerCloudSync()
    }

    /**
     * Total spending in a category between two timestamps.
     * Used by the challenges system to check spending caps.
     */
    suspend fun expenseInCategoryBetween(categoryId: Long?, from: Long, to: Long): Double {
        // Fetch all transactions, filter in-memory (small dataset)
        return transactionDao.getAll()
            .filter {
                it.type == TransactionType.EXPENSE.name &&
                    it.date in from..to &&
                    (categoryId == null || it.categoryId == categoryId)
            }
            .sumOf { it.amount }
    }

    // ─── Seeding ─────────────────────────────────────────────────────────────

    /**
     * Seeds only the default categories on first launch.
     *
     * NOTE: No demo accounts / goals / transactions are seeded — new users
     * start with a clean slate and add their own data. (The previous version
     * generated fake "Тинькофф Блэк", "Сбербанк Премьер" etc. for store
     * screenshots; that has been removed.)
     */
    suspend fun ensureSeeded() {
        if (categoryDao.count() == 0) {
            categoryDao.insertAll(DefaultData.categories())
        }
    }
}
