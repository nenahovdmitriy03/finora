package com.finora.data.remote

import com.finora.data.local.AppDatabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * Handles full upload / download of user data between Room (local) and Supabase (cloud).
 *
 * Strategy (v1 — full sync):
 *   • Upload: serialize all Room data → upsert to Supabase (tagged with user_id)
 *   • Download: fetch all rows for user from Supabase → overwrite Room
 *
 * Foreign keys in Supabase use UUIDs. We maintain a mapping
 * (Room Long id ↔ Supabase UUID) during upload/download.
 */
class SyncManager(
    private val client: SupabaseClient,
    private val db: AppDatabase
) {

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

    // ─── Upload (Room → Supabase) ────────────────────────────────────────

    /**
     * Full upload: pushes all local Room data to Supabase for [userId].
     * Deletes existing remote data first to avoid duplicates.
     */
    suspend fun uploadAll(userId: String) = withContext(Dispatchers.IO) {
        val pg = client.postgrest

        // 1. Delete existing remote data (reverse dependency order)
        pg.from("goal_contributions").delete { filter { eq("user_id", userId) } }
        pg.from("transfers").delete { filter { eq("user_id", userId) } }
        pg.from("transactions").delete { filter { eq("user_id", userId) } }
        pg.from("goals").delete { filter { eq("user_id", userId) } }
        pg.from("categories").delete { filter { eq("user_id", userId) } }
        pg.from("accounts").delete { filter { eq("user_id", userId) } }

        // 2. Upload accounts, build localId → UUID map
        val localAccounts = db.accountDao().getAll()
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

        // 3. Upload categories, build localId → UUID map
        val localCategories = db.categoryDao().observeAll().first()
        val categoryMap = mutableMapOf<Long, String>()

        for (cat in localCategories) {
            val row = CategoryRow(
                user_id = userId, name = cat.name, type = cat.type,
                icon_key = cat.iconKey, color = cat.color, is_default = cat.isDefault
            )
            val result = pg.from("categories").insert(row) { select() }.decodeSingle<CategoryRow>()
            categoryMap[cat.id] = result.id!!
        }

        // 4. Upload transactions (remap accountId, categoryId)
        val localTx = db.transactionDao().observeAll().first()
        for (tx in localTx) {
            val remoteAccId = accountMap[tx.accountId] ?: continue
            val remoteCatId = tx.categoryId?.let { categoryMap[it] }
            val row = TransactionRow(
                user_id = userId, amount = tx.amount, type = tx.type,
                account_id = remoteAccId, category_id = remoteCatId,
                note = tx.note, date = tx.date, created_at = tx.createdAt
            )
            pg.from("transactions").insert(row)
        }

        // 5. Upload goals, build localId → UUID map
        val localGoals = db.goalDao().observeAll().first()
        val goalMap = mutableMapOf<Long, String>()

        for (goal in localGoals) {
            val row = GoalRow(
                user_id = userId, name = goal.name,
                target_amount = goal.targetAmount, saved_amount = goal.savedAmount,
                icon_key = goal.iconKey, color = goal.color,
                deadline = goal.deadline, created_at = goal.createdAt
            )
            val result = pg.from("goals").insert(row) { select() }.decodeSingle<GoalRow>()
            goalMap[goal.id] = result.id!!
        }

        // 6. Upload goal contributions (remap goalId, accountId)
        val localContribs = db.goalContributionDao().observeAll().first()
        for (c in localContribs) {
            val remoteGoalId = goalMap[c.goalId] ?: continue
            val remoteAccId = accountMap[c.accountId] ?: continue
            val row = GoalContributionRow(
                user_id = userId, goal_id = remoteGoalId,
                account_id = remoteAccId, amount = c.amount, date = c.date
            )
            pg.from("goal_contributions").insert(row)
        }

        // 7. Upload transfers (remap fromAccountId, toAccountId)
        val localTransfers = db.transferDao().observeAll().first()
        for (t in localTransfers) {
            val remoteFromId = accountMap[t.fromAccountId] ?: continue
            val remoteToId = accountMap[t.toAccountId] ?: continue
            val row = TransferRow(
                user_id = userId, from_account_id = remoteFromId,
                to_account_id = remoteToId, amount = t.amount,
                note = t.note, date = t.date, created_at = t.createdAt
            )
            pg.from("transfers").insert(row)
        }
    }

    // ─── Download (Supabase → Room) ──────────────────────────────────────

    /**
     * Full download: fetches all user data from Supabase, clears Room, inserts fresh.
     * Used on login when the device has no data (or user chooses "restore from cloud").
     */
    suspend fun downloadAll(userId: String) = withContext(Dispatchers.IO) {
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

        // If remote is completely empty, don't wipe local data
        if (remoteAccounts.isEmpty() && remoteCategories.isEmpty()) return@withContext

        // 2. Clear local Room
        db.clearAllTables()

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
        for (rt in remoteTransactions) {
            val localAccId = accountMap[rt.account_id] ?: continue
            val localCatId = rt.category_id?.let { categoryMap[it] }
            val entity = com.finora.data.local.entity.TransactionEntity(
                id = 0, amount = rt.amount, type = rt.type,
                accountId = localAccId, categoryId = localCatId,
                note = rt.note, date = rt.date, createdAt = rt.created_at
            )
            db.transactionDao().upsert(entity)
        }

        // 6. Insert goals
        val goalMap = mutableMapOf<String, Long>()
        for (rg in remoteGoals) {
            val entity = com.finora.data.local.entity.GoalEntity(
                id = 0, name = rg.name, targetAmount = rg.target_amount,
                savedAmount = rg.saved_amount, iconKey = rg.icon_key,
                color = rg.color, deadline = rg.deadline,
                createdAt = rg.created_at
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
    }
}
