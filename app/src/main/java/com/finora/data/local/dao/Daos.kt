package com.finora.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finora.data.local.entity.AccountEntity
import com.finora.data.local.entity.BudgetEntity
import com.finora.data.local.entity.CategoryEntity
import com.finora.data.local.entity.ChallengeEntity
import com.finora.data.local.entity.GoalContributionEntity
import com.finora.data.local.entity.GoalEntity
import com.finora.data.local.entity.RecurringRuleEntity
import com.finora.data.local.entity.TagEntity
import com.finora.data.local.entity.TemplateEntity
import com.finora.data.local.entity.TransactionEntity
import com.finora.data.local.entity.TransactionTagEntity
import com.finora.data.local.entity.TransferEntity
import kotlinx.coroutines.flow.Flow

/** Lightweight POJO for aggregated balance deltas per account. */
data class BalanceDelta(val accountId: Long, val delta: Double)

/** Aggregated total amount per transaction type (INCOME / EXPENSE). */
data class TypeTotal(val type: String, val total: Double)

// ─── Accounts ────────────────────────────────────────────────────────────────

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts")
    suspend fun getAll(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int
}

// ─── Categories ──────────────────────────────────────────────────────────────

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("SELECT * FROM categories WHERE name = :name AND type = :type LIMIT 1")
    suspend fun findByNameAndType(name: String, type: String): CategoryEntity?

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}

// ─── Transactions ────────────────────────────────────────────────────────────

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions")
    suspend fun getAll(): List<TransactionEntity>

    /** Most recent N transactions — for the home screen preview (avoids loading all rows). */
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    /**
     * Income / expense totals since [since], aggregated in SQL.
     * Used by the home screen instead of summing all rows in memory.
     */
    @Query(
        "SELECT type, COALESCE(SUM(amount), 0) AS total " +
            "FROM transactions WHERE date >= :since GROUP BY type"
    )
    fun observeTotalsSince(since: Long): Flow<List<TypeTotal>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :from AND :to ORDER BY date DESC, id DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity): Long

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: Long)

    @Query(
        "SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END), 0) " +
            "FROM transactions WHERE accountId = :accountId"
    )
    suspend fun balanceDelta(accountId: Long): Double

    /**
     * Aggregated income−expense delta per account, computed in SQL.
     * Much faster than loading every row into memory.
     */
    @Query(
        "SELECT accountId, " +
            "COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END), 0) AS delta " +
            "FROM transactions GROUP BY accountId"
    )
    fun observeBalanceDeltas(): Flow<List<BalanceDelta>>
}

// ─── Goals ───────────────────────────────────────────────────────────────────

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals")
    suspend fun getAll(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: Long): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: GoalEntity): Long

    @Delete
    suspend fun delete(goal: GoalEntity)
}

// ─── Goal Contributions ──────────────────────────────────────────────────────

@Dao
interface GoalContributionDao {
    /** All contributions across all goals, newest first. */
    @Query("SELECT * FROM goal_contributions ORDER BY date DESC")
    fun observeAll(): Flow<List<GoalContributionEntity>>

    @Query("SELECT * FROM goal_contributions")
    suspend fun getAll(): List<GoalContributionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contribution: GoalContributionEntity): Long

    /** Contributions for one goal, newest first. */
    @Query("SELECT * FROM goal_contributions WHERE goalId = :goalId ORDER BY date DESC")
    fun observeByGoal(goalId: Long): Flow<List<GoalContributionEntity>>

    @Insert
    suspend fun insert(contribution: GoalContributionEntity): Long

    @Query("DELETE FROM goal_contributions WHERE goalId = :goalId")
    suspend fun deleteByGoal(goalId: Long)

    @Query("DELETE FROM goal_contributions WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: Long)
}

// ─── Transfers ───────────────────────────────────────────────────────────────

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers")
    suspend fun getAll(): List<TransferEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transfer: TransferEntity): Long

    @Delete
    suspend fun delete(transfer: TransferEntity)

    @Query("DELETE FROM transfers WHERE fromAccountId = :accountId OR toAccountId = :accountId")
    suspend fun deleteByAccount(accountId: Long)

    /**
     * Net transfer effect per account (outgoing = negative, incoming = positive),
     * computed entirely in SQL for efficiency.
     */
    @Query(
        "SELECT fromAccountId AS accountId, -COALESCE(SUM(amount), 0) AS delta " +
            "FROM transfers GROUP BY fromAccountId " +
            "UNION ALL " +
            "SELECT toAccountId AS accountId, COALESCE(SUM(amount), 0) AS delta " +
            "FROM transfers GROUP BY toAccountId"
    )
    fun observeTransferDeltas(): Flow<List<BalanceDelta>>
}

// ─── Recurring Rules ────────────────────────────────────────────────────────

@Dao
interface RecurringRuleDao {
    @Query("SELECT * FROM recurring_rules ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules")
    suspend fun getAll(): List<RecurringRuleEntity>

    @Query("SELECT * FROM recurring_rules WHERE enabled = 1")
    suspend fun getEnabled(): List<RecurringRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: RecurringRuleEntity): Long

    @Delete
    suspend fun delete(rule: RecurringRuleEntity)

    @Query("UPDATE recurring_rules SET lastExecutedAt = :ts WHERE id = :id")
    suspend fun updateLastExecuted(id: Long, ts: Long)
}

// ─── Budgets ─────────────────────────────────────────────────────────────────

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun getAll(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId LIMIT 1")
    suspend fun getByCategoryId(categoryId: Long): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity): Long

    @Delete
    suspend fun delete(budget: BudgetEntity)
}

// ─── Templates ──────────────────────────────────────────────────────────────

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates")
    suspend fun getAll(): List<TemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: TemplateEntity): Long

    @Delete
    suspend fun delete(template: TemplateEntity)
}

// ─── Tags ───────────────────────────────────────────────────────────────────

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags")
    suspend fun getAll(): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tag: TagEntity): Long

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): TagEntity?
}

@Dao
interface TransactionTagDao {
    @Query("SELECT * FROM transaction_tags")
    suspend fun getAll(): List<TransactionTagEntity>

    @Query("SELECT * FROM transaction_tags")
    fun observeAll(): Flow<List<TransactionTagEntity>>

    @Query("SELECT tagId FROM transaction_tags WHERE transactionId = :transactionId")
    fun observeTagIds(transactionId: Long): Flow<List<Long>>

    @Query("SELECT tagId FROM transaction_tags WHERE transactionId = :transactionId")
    suspend fun getTagIds(transactionId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(link: TransactionTagEntity)

    @Query("DELETE FROM transaction_tags WHERE transactionId = :transactionId")
    suspend fun deleteByTransaction(transactionId: Long)

    @Query("DELETE FROM transaction_tags WHERE tagId = :tagId")
    suspend fun deleteByTag(tagId: Long)
}

// ─── Challenges ─────────────────────────────────────────────────────────────

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenges ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges")
    suspend fun getAll(): List<ChallengeEntity>

    @Query("SELECT * FROM challenges WHERE completed = 0 AND endDate >= :now")
    fun observeActive(now: Long): Flow<List<ChallengeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(challenge: ChallengeEntity): Long

    @Delete
    suspend fun delete(challenge: ChallengeEntity)

    @Query("UPDATE challenges SET completed = 1 WHERE id = :id")
    suspend fun markCompleted(id: Long)
}
