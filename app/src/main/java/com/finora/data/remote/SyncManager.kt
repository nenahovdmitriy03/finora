package com.finora.data.remote

import android.util.Log
import com.finora.data.local.AppDatabase
import com.finora.data.local.DefaultData
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * Handles full upload / download of user data between Room (local) and Supabase (cloud).
 *
 * Strategy (v1 — full sync):
 *   • Upload: serialize all Room data → upsert to Supabase (tagged with user_id)
 *   • Download: fetch all rows for user from Supabase → overwrite Room
 *
 * A [Mutex] prevents concurrent upload/download from corrupting data.
 * [scheduleUpload] provides a debounced trigger for syncing after data changes.
 */
class SyncManager(
    private val client: SupabaseClient,
    private val db: AppDatabase
) {
    sealed interface SyncStatus {
        data object Idle : SyncStatus
        data object Uploading : SyncStatus
        data object Downloading : SyncStatus
        data object PendingNetwork : SyncStatus
        data class Synced(val timestamp: Long) : SyncStatus
        data class Error(val message: String) : SyncStatus
    }

    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    /** Prevents concurrent upload/download operations. */
    private val syncMutex = Mutex()

    /** Scope for debounced background uploads. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingUploadJob: Job? = null

    /**
     * User id that needs a sync once connectivity returns.
     * Set by [scheduleUpload] when offline; consumed by [retryPendingSync].
     */
    @Volatile
    var pendingUserId: String? = null
        private set

    companion object {
        private const val TAG = "SyncManager"
    }

    /**
     * Called by [NetworkMonitor] when connectivity returns.
     * Retries the queued upload if any.
     */
    fun retryPendingSync() {
        val userId = pendingUserId ?: return
        pendingUserId = null
        scheduleUpload(userId)
    }

    // ─── DTOs (Supabase row shapes) ──────────────────────────────────────

    @Serializable
    data class AccountRow(
        val id: String? = null,
        val user_id: String,
        val name: String,
        val type: String,
        val initial_balance: Double,
        val color: Long,
        val icon_key: String,
        val interest_rate: Double = 0.0,
        val interest_period: String? = null,
        val last_interest_at: Long? = null,
        val interest_payout_minute: Int = 540,
        val interest_payout_day: Int = 1,
        val created_at: Long
    )

    @Serializable
    data class CategoryRow(
        val id: String? = null,
        val user_id: String,
        val name: String,
        val type: String,
        val icon_key: String,
        val color: Long,
        val is_default: Boolean
    )

    @Serializable
    data class TransactionRow(
        val id: String? = null,
        val user_id: String,
        val amount: Double,
        val type: String,
        val account_id: String,
        val category_id: String? = null,
        val note: String = "",
        val date: Long,
        val created_at: Long
    )

    @Serializable
    data class GoalRow(
        val id: String? = null,
        val user_id: String,
        val name: String,
        val target_amount: Double,
        val saved_amount: Double = 0.0,
        val icon_key: String,
        val color: Long,
        val deadline: Long? = null,
        val plan_months: Int? = null,
        val planned_monthly_amount: Double? = null,
        val created_at: Long
    )

    @Serializable
    data class GoalContributionRow(
        val id: String? = null,
        val user_id: String,
        val goal_id: String,
        val account_id: String,
        val amount: Double,
        val date: Long
    )

    @Serializable
    data class TransferRow(
        val id: String? = null,
        val user_id: String,
        val from_account_id: String,
        val to_account_id: String,
        val amount: Double,
        val note: String = "",
        val date: Long,
        val created_at: Long
    )

    @Serializable
    data class RecurringRuleRow(
        val id: String? = null,
        val user_id: String,
        val name: String,
        val amount: Double,
        val type: String,
        val category_id: String,
        val account_id: String,
        val period_days: Int,
        val last_executed_at: Long? = null,
        val created_at: Long,
        val enabled: Boolean = true
    )

    @Serializable
    data class BudgetRow(
        val id: String? = null,
        val user_id: String,
        val category_id: String,
        val limit_amount: Double,
        val period_days: Int = 30,
        val created_at: Long
    )

    @Serializable
    data class TemplateRow(
        val id: String? = null,
        val user_id: String,
        val name: String,
        val amount: Double,
        val type: String,
        val category_id: String? = null,
        val account_id: String? = null,
        val note: String = "",
        val created_at: Long
    )

    @Serializable
    data class TagRow(
        val id: String? = null,
        val user_id: String,
        val name: String,
        val color: Long
    )

    @Serializable
    data class TransactionTagRow(
        val user_id: String,
        val transaction_id: String,
        val tag_id: String
    )

    @Serializable
    data class ChallengeRow(
        val id: String? = null,
        val user_id: String,
        val title: String,
        val description: String,
        val emoji: String = "\uD83C\uDFAF",
        val target_days: Int,
        val target_amount: Double? = null,
        val category_id: String? = null,
        val start_date: Long,
        val end_date: Long,
        val completed: Boolean = false,
        val created_at: Long
    )

    // ─── Debounced upload trigger ────────────────────────────────────────

    /**
     * Schedules a debounced upload — waits 3 seconds, then uploads.
     * Subsequent calls within the delay window restart the timer.
     * Used after data mutations to keep cloud in sync without blocking the UI.
     */
    fun scheduleUpload(userId: String) {
        pendingUploadJob?.cancel()
        pendingUploadJob = scope.launch {
            delay(3_000L)
            try {
                uploadAll(userId)
                pendingUserId = null
            } catch (e: Exception) {
                Log.e(TAG, "Debounced upload failed — will retry when online", e)
                pendingUserId = userId
                _status.value = SyncStatus.PendingNetwork
            }
        }
    }

    /**
     * Cancels any pending debounced upload. MUST be called before wiping local
     * data (sign-out / account switch / delete) — otherwise a queued [uploadAll]
     * could fire after the local DB is cleared and destroy the cloud copy.
     */
    fun cancelPendingUpload() {
        pendingUploadJob?.cancel()
        pendingUploadJob = null
    }

    /**
     * Post-login sync, launched on the SyncManager's own app-lifecycle [scope]
     * so it SURVIVES the auth screen being torn down. The instant sign-in
     * succeeds, the nav graph (observing auth sessionStatus) navigates to Home
     * and cancels the AuthViewModel's scope — if the download ran there it would
     * be cancelled mid-request (HTTP 499) and no data would load. Running it here
     * decouples it from the UI lifecycle. Home observes Room, so rows appear as
     * soon as the download writes them.
     */
    fun syncOnLogin(userId: String, isSwitchingAccount: Boolean) {
        scope.launch {
            try {
                if (isSwitchingAccount) clearLocalData()
                val hadRemote = downloadAll(userId)
                if (!hadRemote && !isSwitchingAccount) {
                    // Returning/unclaimed user with an empty cloud → back local up.
                    uploadAll(userId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "syncOnLogin failed", e)
            }
        }
    }

    /** Fire-and-forget upload on the app-lifecycle scope (used after registration). */
    fun uploadInBackground(userId: String) {
        scope.launch {
            try { uploadAll(userId) } catch (e: Exception) { Log.e(TAG, "uploadInBackground failed", e) }
        }
    }

    // ─── Upload (Room → Supabase) ────────────────────────────────────────

    /**
     * Full upload: pushes all local Room data to Supabase for [userId].
     * Deletes existing remote data first to avoid duplicates.
     * Thread-safe — waits for any running download to finish first.
     */
    suspend fun uploadAll(userId: String) = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            _status.value = SyncStatus.Uploading
            Log.d(TAG, "uploadAll START for user=$userId")
            try {
                val pg = client.postgrest

            // 0. Load ALL local data up front so we can guard against data loss.
            val localAccounts = db.accountDao().getAll()
            val localCategories = db.categoryDao().observeAll().first()
            val localTx = db.transactionDao().observeAll().first()
            val localGoals = db.goalDao().observeAll().first()
            val localContribs = db.goalContributionDao().observeAll().first()
            val localTransfers = db.transferDao().observeAll().first()
            val localRecurringRules = db.recurringRuleDao().getAll()
            val localBudgets = db.budgetDao().getAll()
            val localTemplates = db.templateDao().getAll()
            val localTags = db.tagDao().getAll()
            val localTransactionTags = db.transactionTagDao().getAll()
            val localChallenges = db.challengeDao().getAll()

            // SAFETY GUARD: never let an empty local DB wipe a populated cloud.
            // Categories alone are auto-seeded defaults and don't count as real data.
            // Previously a debounced upload firing right after sign-out/clearLocalData
            // would DELETE every remote row → full data loss. Now an empty local simply
            // refuses to push a destructive "delete everything" to the cloud.
            val isEffectivelyEmpty = localAccounts.isEmpty() && localTx.isEmpty() &&
                localGoals.isEmpty() && localContribs.isEmpty() && localTransfers.isEmpty() &&
                localRecurringRules.isEmpty() && localBudgets.isEmpty() &&
                localTemplates.isEmpty() && localTags.isEmpty() &&
                localTransactionTags.isEmpty() && localChallenges.isEmpty()
            if (isEffectivelyEmpty) {
                Log.w(TAG, "uploadAll: local is empty — SKIPPING remote wipe to protect cloud")
                _status.value = SyncStatus.Synced(System.currentTimeMillis())
                return@withContext
            }

            // 1. Delete existing remote data (reverse dependency order)
            pg.from("transaction_tags").delete { filter { eq("user_id", userId) } }
            pg.from("challenges").delete { filter { eq("user_id", userId) } }
            pg.from("templates").delete { filter { eq("user_id", userId) } }
            pg.from("budgets").delete { filter { eq("user_id", userId) } }
            pg.from("recurring_rules").delete { filter { eq("user_id", userId) } }
            pg.from("tags").delete { filter { eq("user_id", userId) } }
            pg.from("goal_contributions").delete { filter { eq("user_id", userId) } }
            pg.from("transfers").delete { filter { eq("user_id", userId) } }
            pg.from("transactions").delete { filter { eq("user_id", userId) } }
            pg.from("goals").delete { filter { eq("user_id", userId) } }
            pg.from("categories").delete { filter { eq("user_id", userId) } }
            pg.from("accounts").delete { filter { eq("user_id", userId) } }
            Log.d(TAG, "uploadAll: deleted old remote data")

            // 2. Upload accounts, build localId → UUID map
            val accountMap = mutableMapOf<Long, String>()

            for (acc in localAccounts) {
                val row = AccountRow(
                    user_id = userId, name = acc.name, type = acc.type,
                    initial_balance = acc.initialBalance, color = acc.color,
                    icon_key = acc.iconKey, interest_rate = acc.interestRate,
                    interest_period = acc.interestPeriod, last_interest_at = acc.lastInterestAt,
                    interest_payout_minute = acc.interestPayoutMinute,
                    interest_payout_day = acc.interestPayoutDay, created_at = acc.createdAt
                )
                val result = pg.from("accounts").insert(row) { select() }.decodeSingle<AccountRow>()
                accountMap[acc.id] = result.id!!
            }
            Log.d(TAG, "uploadAll: uploaded ${localAccounts.size} accounts")

            // 3. Upload categories, build localId → UUID map
            val categoryMap = mutableMapOf<Long, String>()

            for (cat in localCategories) {
                val row = CategoryRow(
                    user_id = userId, name = cat.name, type = cat.type,
                    icon_key = cat.iconKey, color = cat.color, is_default = cat.isDefault
                )
                val result = pg.from("categories").insert(row) { select() }.decodeSingle<CategoryRow>()
                categoryMap[cat.id] = result.id!!
            }
            Log.d(TAG, "uploadAll: uploaded ${localCategories.size} categories")

            // 4. Upload transactions (remap accountId, categoryId)
            val transactionMap = mutableMapOf<Long, String>()
            var txCount = 0
            for (tx in localTx) {
                val remoteAccId = accountMap[tx.accountId] ?: continue
                val remoteCatId = tx.categoryId?.let { categoryMap[it] }
                val row = TransactionRow(
                    user_id = userId, amount = tx.amount, type = tx.type,
                    account_id = remoteAccId, category_id = remoteCatId,
                    note = tx.note, date = tx.date, created_at = tx.createdAt
                )
                val result = pg.from("transactions").insert(row) { select() }.decodeSingle<TransactionRow>()
                transactionMap[tx.id] = result.id!!
                txCount++
            }
            Log.d(TAG, "uploadAll: uploaded $txCount transactions")

            // 5. Upload goals, build localId → UUID map
            val goalMap = mutableMapOf<Long, String>()

            for (goal in localGoals) {
                val row = GoalRow(
                    user_id = userId, name = goal.name,
                    target_amount = goal.targetAmount, saved_amount = goal.savedAmount,
                    icon_key = goal.iconKey, color = goal.color,
                    deadline = goal.deadline,
                    plan_months = goal.planMonths,
                    planned_monthly_amount = goal.plannedMonthlyAmount,
                    created_at = goal.createdAt
                )
                val result = pg.from("goals").insert(row) { select() }.decodeSingle<GoalRow>()
                goalMap[goal.id] = result.id!!
            }
            Log.d(TAG, "uploadAll: uploaded ${localGoals.size} goals")

            // 6. Upload goal contributions (remap goalId, accountId)
            var contribCount = 0
            for (c in localContribs) {
                val remoteGoalId = goalMap[c.goalId] ?: continue
                val remoteAccId = accountMap[c.accountId] ?: continue
                val row = GoalContributionRow(
                    user_id = userId, goal_id = remoteGoalId,
                    account_id = remoteAccId, amount = c.amount, date = c.date
                )
                pg.from("goal_contributions").insert(row)
                contribCount++
            }

            // 7. Upload transfers (remap fromAccountId, toAccountId)
            var transferCount = 0
            for (t in localTransfers) {
                val remoteFromId = accountMap[t.fromAccountId] ?: continue
                val remoteToId = accountMap[t.toAccountId] ?: continue
                val row = TransferRow(
                    user_id = userId, from_account_id = remoteFromId,
                    to_account_id = remoteToId, amount = t.amount,
                    note = t.note, date = t.date, created_at = t.createdAt
                )
                pg.from("transfers").insert(row)
                transferCount++
            }

            // 8. Upload recurring rules (remap categoryId, accountId)
            var recurringCount = 0
            for (rule in localRecurringRules) {
                val remoteCatId = categoryMap[rule.categoryId] ?: continue
                val remoteAccId = accountMap[rule.accountId] ?: continue
                val row = RecurringRuleRow(
                    user_id = userId, name = rule.name, amount = rule.amount,
                    type = rule.type, category_id = remoteCatId, account_id = remoteAccId,
                    period_days = rule.periodDays, last_executed_at = rule.lastExecutedAt,
                    created_at = rule.createdAt, enabled = rule.enabled
                )
                pg.from("recurring_rules").insert(row)
                recurringCount++
            }

            // 9. Upload budgets (remap categoryId)
            var budgetCount = 0
            for (budget in localBudgets) {
                val remoteCatId = categoryMap[budget.categoryId] ?: continue
                val row = BudgetRow(
                    user_id = userId, category_id = remoteCatId,
                    limit_amount = budget.limitAmount, period_days = budget.periodDays,
                    created_at = budget.createdAt
                )
                pg.from("budgets").insert(row)
                budgetCount++
            }

            // 10. Upload templates (remap optional categoryId/accountId)
            var templateCount = 0
            for (template in localTemplates) {
                val remoteCatId = template.categoryId?.let { categoryMap[it] }
                val remoteAccId = template.accountId?.let { accountMap[it] }
                val row = TemplateRow(
                    user_id = userId, name = template.name, amount = template.amount,
                    type = template.type, category_id = remoteCatId,
                    account_id = remoteAccId, note = template.note,
                    created_at = template.createdAt
                )
                pg.from("templates").insert(row)
                templateCount++
            }

            // 11. Upload tags, build localId -> UUID map
            val tagMap = mutableMapOf<Long, String>()
            for (tag in localTags) {
                val row = TagRow(user_id = userId, name = tag.name, color = tag.color)
                val result = pg.from("tags").insert(row) { select() }.decodeSingle<TagRow>()
                tagMap[tag.id] = result.id!!
            }

            // 12. Upload transaction-tag links (remap both sides)
            var transactionTagCount = 0
            for (link in localTransactionTags) {
                val remoteTxId = transactionMap[link.transactionId] ?: continue
                val remoteTagId = tagMap[link.tagId] ?: continue
                val row = TransactionTagRow(
                    user_id = userId,
                    transaction_id = remoteTxId,
                    tag_id = remoteTagId
                )
                pg.from("transaction_tags").insert(row)
                transactionTagCount++
            }

            // 13. Upload challenges (remap optional categoryId)
            var challengeCount = 0
            for (challenge in localChallenges) {
                val remoteCatId = challenge.categoryId?.let { categoryMap[it] }
                val row = ChallengeRow(
                    user_id = userId,
                    title = challenge.title,
                    description = challenge.description,
                    emoji = challenge.emoji,
                    target_days = challenge.targetDays,
                    target_amount = challenge.targetAmount,
                    category_id = remoteCatId,
                    start_date = challenge.startDate,
                    end_date = challenge.endDate,
                    completed = challenge.completed,
                    created_at = challenge.createdAt
                )
                pg.from("challenges").insert(row)
                challengeCount++
            }

            Log.d(TAG, "uploadAll DONE: $txCount tx, ${localGoals.size} goals, " +
                    "$contribCount contribs, $transferCount transfers, " +
                    "$recurringCount recurring, $budgetCount budgets, " +
                    "$templateCount templates, ${localTags.size} tags, " +
                    "$transactionTagCount txTags, $challengeCount challenges")
                _status.value = SyncStatus.Synced(System.currentTimeMillis())
            } catch (e: Exception) {
                _status.value = SyncStatus.Error(e.message ?: "Sync upload failed")
                throw e
            }
        }
    }

    // ─── Download (Supabase → Room) ──────────────────────────────────────

    /**
     * Full download: fetches all user data from Supabase, clears Room, inserts fresh.
     * Used on login when the device has no data (or user chooses "restore from cloud").
     * Thread-safe — waits for any running upload to finish first.
     *
     * @return `true` if remote data was found and written to Room; `false` if remote was empty.
     */
    suspend fun downloadAll(userId: String): Boolean = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            _status.value = SyncStatus.Downloading
            Log.d(TAG, "downloadAll START for user=$userId")
            try {
                val pg = client.postgrest

            // 1. Fetch all remote data
            val remoteAccounts = pg.from("accounts")
                .select { filter { eq("user_id", userId) } }.decodeList<AccountRow>()
            val remoteCategories = pg.from("categories")
                .select { filter { eq("user_id", userId) } }.decodeList<CategoryRow>()
            val remoteTransactions = pg.from("transactions")
                .select { filter { eq("user_id", userId) } }.decodeList<TransactionRow>()
            val remoteGoals = pg.from("goals")
                .select { filter { eq("user_id", userId) } }.decodeList<GoalRow>()
            val remoteContribs = pg.from("goal_contributions")
                .select { filter { eq("user_id", userId) } }.decodeList<GoalContributionRow>()
            val remoteTransfers = pg.from("transfers")
                .select { filter { eq("user_id", userId) } }.decodeList<TransferRow>()
            val remoteRecurringRules = pg.from("recurring_rules")
                .select { filter { eq("user_id", userId) } }.decodeList<RecurringRuleRow>()
            val remoteBudgets = pg.from("budgets")
                .select { filter { eq("user_id", userId) } }.decodeList<BudgetRow>()
            val remoteTemplates = pg.from("templates")
                .select { filter { eq("user_id", userId) } }.decodeList<TemplateRow>()
            val remoteTags = pg.from("tags")
                .select { filter { eq("user_id", userId) } }.decodeList<TagRow>()
            val remoteTransactionTags = pg.from("transaction_tags")
                .select { filter { eq("user_id", userId) } }.decodeList<TransactionTagRow>()
            val remoteChallenges = pg.from("challenges")
                .select { filter { eq("user_id", userId) } }.decodeList<ChallengeRow>()

            Log.d(TAG, "downloadAll: remote has ${remoteAccounts.size} accounts, " +
                    "${remoteCategories.size} categories, ${remoteTransactions.size} tx, " +
                    "${remoteGoals.size} goals, ${remoteContribs.size} contribs, " +
                    "${remoteTransfers.size} transfers, ${remoteRecurringRules.size} recurring, " +
                    "${remoteBudgets.size} budgets, ${remoteTemplates.size} templates, " +
                    "${remoteTags.size} tags, ${remoteChallenges.size} challenges")

            // Treat remote as authoritative ONLY if it has real data (accounts,
            // transactions or goals). Categories alone are auto-seeded defaults and
            // can be left behind by a half-finished/interrupted upload — in that case
            // we must NOT clobber local data with a near-empty cloud.
            val remoteHasRealData = remoteAccounts.isNotEmpty() ||
                remoteTransactions.isNotEmpty() || remoteGoals.isNotEmpty() ||
                remoteContribs.isNotEmpty() || remoteTransfers.isNotEmpty() ||
                remoteRecurringRules.isNotEmpty() || remoteBudgets.isNotEmpty() ||
                remoteTemplates.isNotEmpty() || remoteTags.isNotEmpty() ||
                remoteTransactionTags.isNotEmpty() || remoteChallenges.isNotEmpty()
            if (!remoteHasRealData) {
                Log.w(TAG, "downloadAll: remote has no real data — keeping local data")
                _status.value = SyncStatus.Synced(System.currentTimeMillis())
                return@withContext false
            }

            // 2. Clear local Room
            db.clearAllTables()
            Log.d(TAG, "downloadAll: cleared local tables")

            // 3. Insert accounts — UUID → new local id
            val accountMap = mutableMapOf<String, Long>()
            for (ra in remoteAccounts) {
                val entity = com.finora.data.local.entity.AccountEntity(
                    id = 0, name = ra.name, type = ra.type,
                    initialBalance = ra.initial_balance, color = ra.color,
                    iconKey = ra.icon_key, createdAt = ra.created_at,
                    interestRate = ra.interest_rate, interestPeriod = ra.interest_period,
                    lastInterestAt = ra.last_interest_at,
                    interestPayoutMinute = ra.interest_payout_minute,
                    interestPayoutDay = ra.interest_payout_day
                )
                val localId = db.accountDao().upsert(entity)
                accountMap[ra.id!!] = localId
            }

            // 4. Insert categories
            val categoryMap = mutableMapOf<String, Long>()
            for (rc in remoteCategories) {
                val entity = com.finora.data.local.entity.CategoryEntity(
                    id = 0, name = rc.name, type = rc.type,
                    iconKey = rc.icon_key, color = rc.color, isDefault = rc.is_default
                )
                val localId = db.categoryDao().upsert(entity)
                categoryMap[rc.id!!] = localId
            }

            // 5. Insert transactions (remap FK UUIDs → local ids)
            val transactionMap = mutableMapOf<String, Long>()
            for (rt in remoteTransactions) {
                val localAccId = accountMap[rt.account_id] ?: continue
                val localCatId = rt.category_id?.let { categoryMap[it] }
                val entity = com.finora.data.local.entity.TransactionEntity(
                    id = 0, amount = rt.amount, type = rt.type,
                    accountId = localAccId, categoryId = localCatId,
                    note = rt.note, date = rt.date, createdAt = rt.created_at
                )
                val localId = db.transactionDao().upsert(entity)
                transactionMap[rt.id!!] = localId
            }

            // 6. Insert goals
            val goalMap = mutableMapOf<String, Long>()
            for (rg in remoteGoals) {
                val entity = com.finora.data.local.entity.GoalEntity(
                    id = 0, name = rg.name, targetAmount = rg.target_amount,
                    savedAmount = rg.saved_amount, iconKey = rg.icon_key,
                    color = rg.color, deadline = rg.deadline,
                    createdAt = rg.created_at,
                    planMonths = rg.plan_months,
                    plannedMonthlyAmount = rg.planned_monthly_amount
                )
                val localId = db.goalDao().upsert(entity)
                goalMap[rg.id!!] = localId
            }

            // 7. Insert goal contributions
            for (rc in remoteContribs) {
                val localGoalId = goalMap[rc.goal_id] ?: continue
                val localAccId = accountMap[rc.account_id] ?: continue
                val entity = com.finora.data.local.entity.GoalContributionEntity(
                    id = 0, goalId = localGoalId, accountId = localAccId,
                    amount = rc.amount, date = rc.date
                )
                db.goalContributionDao().insert(entity)
            }

            // 8. Insert transfers
            for (rt in remoteTransfers) {
                val localFromId = accountMap[rt.from_account_id] ?: continue
                val localToId = accountMap[rt.to_account_id] ?: continue
                val entity = com.finora.data.local.entity.TransferEntity(
                    id = 0, fromAccountId = localFromId, toAccountId = localToId,
                    amount = rt.amount, note = rt.note,
                    date = rt.date, createdAt = rt.created_at
                )
                db.transferDao().upsert(entity)
            }

            // 9. Insert recurring rules
            for (rr in remoteRecurringRules) {
                val localCatId = categoryMap[rr.category_id] ?: continue
                val localAccId = accountMap[rr.account_id] ?: continue
                val entity = com.finora.data.local.entity.RecurringRuleEntity(
                    id = 0,
                    name = rr.name,
                    amount = rr.amount,
                    type = rr.type,
                    categoryId = localCatId,
                    accountId = localAccId,
                    periodDays = rr.period_days,
                    lastExecutedAt = rr.last_executed_at,
                    createdAt = rr.created_at,
                    enabled = rr.enabled
                )
                db.recurringRuleDao().upsert(entity)
            }

            // 10. Insert budgets
            for (rb in remoteBudgets) {
                val localCatId = categoryMap[rb.category_id] ?: continue
                val entity = com.finora.data.local.entity.BudgetEntity(
                    id = 0,
                    categoryId = localCatId,
                    limitAmount = rb.limit_amount,
                    periodDays = rb.period_days,
                    createdAt = rb.created_at
                )
                db.budgetDao().upsert(entity)
            }

            // 11. Insert templates
            for (rt in remoteTemplates) {
                val localCatId = rt.category_id?.let { categoryMap[it] }
                val localAccId = rt.account_id?.let { accountMap[it] }
                val entity = com.finora.data.local.entity.TemplateEntity(
                    id = 0,
                    name = rt.name,
                    amount = rt.amount,
                    type = rt.type,
                    categoryId = localCatId,
                    accountId = localAccId,
                    note = rt.note,
                    createdAt = rt.created_at
                )
                db.templateDao().upsert(entity)
            }

            // 12. Insert tags
            val tagMap = mutableMapOf<String, Long>()
            for (rt in remoteTags) {
                val entity = com.finora.data.local.entity.TagEntity(
                    id = 0,
                    name = rt.name,
                    color = rt.color
                )
                val localId = db.tagDao().upsert(entity)
                tagMap[rt.id!!] = localId
            }

            // 13. Insert transaction-tag links
            for (rtl in remoteTransactionTags) {
                val localTxId = transactionMap[rtl.transaction_id] ?: continue
                val localTagId = tagMap[rtl.tag_id] ?: continue
                db.transactionTagDao().insert(
                    com.finora.data.local.entity.TransactionTagEntity(
                        transactionId = localTxId,
                        tagId = localTagId
                    )
                )
            }

            // 14. Insert challenges
            for (rc in remoteChallenges) {
                val localCatId = rc.category_id?.let { categoryMap[it] }
                val entity = com.finora.data.local.entity.ChallengeEntity(
                    id = 0,
                    title = rc.title,
                    description = rc.description,
                    emoji = rc.emoji,
                    targetDays = rc.target_days,
                    targetAmount = rc.target_amount,
                    categoryId = localCatId,
                    startDate = rc.start_date,
                    endDate = rc.end_date,
                    completed = rc.completed,
                    createdAt = rc.created_at
                )
                db.challengeDao().upsert(entity)
            }

            Log.d(TAG, "downloadAll DONE: wrote ${remoteAccounts.size} accounts, " +
                    "${remoteCategories.size} categories, ${remoteTransactions.size} tx, " +
                    "${remoteTemplates.size} templates, ${remoteRecurringRules.size} recurring")
                _status.value = SyncStatus.Synced(System.currentTimeMillis())
            return@withContext true
            } catch (e: Exception) {
                _status.value = SyncStatus.Error(e.message ?: "Sync download failed")
                throw e
            }
        }
    }

    // ─── Clear local only ────────────────────────────────────────────────

    /**
     * Wipes the local Room DB (used when a *different* account logs in, so the
     * previous user's data doesn't leak). Re-seeds default categories so the
     * app remains usable. Does NOT touch any remote data.
     */
    suspend fun clearLocalData() = syncMutex.withLock {
        cancelPendingUpload()
        withContext(Dispatchers.IO) {
            Log.d(TAG, "clearLocalData: wiping local Room")
            db.clearAllTables()
            if (db.categoryDao().count() == 0) {
                db.categoryDao().insertAll(DefaultData.categories())
            }
        }
    }

    // ─── Delete all user data ────────────────────────────────────────────

    /**
     * Removes all remote data for [userId] and clears local Room tables.
     */
    suspend fun deleteAllUserData(userId: String) = syncMutex.withLock {
        cancelPendingUpload()
        withContext(Dispatchers.IO) {
            Log.d(TAG, "deleteAllUserData for user=$userId")
            val pg = client.postgrest
            pg.from("transaction_tags").delete { filter { eq("user_id", userId) } }
            pg.from("challenges").delete { filter { eq("user_id", userId) } }
            pg.from("templates").delete { filter { eq("user_id", userId) } }
            pg.from("budgets").delete { filter { eq("user_id", userId) } }
            pg.from("recurring_rules").delete { filter { eq("user_id", userId) } }
            pg.from("tags").delete { filter { eq("user_id", userId) } }
            pg.from("goal_contributions").delete { filter { eq("user_id", userId) } }
            pg.from("transfers").delete { filter { eq("user_id", userId) } }
            pg.from("transactions").delete { filter { eq("user_id", userId) } }
            pg.from("goals").delete { filter { eq("user_id", userId) } }
            pg.from("categories").delete { filter { eq("user_id", userId) } }
            pg.from("accounts").delete { filter { eq("user_id", userId) } }
            db.clearAllTables()
        }
    }
}
