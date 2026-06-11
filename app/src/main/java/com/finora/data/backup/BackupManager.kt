package com.finora.data.backup

import android.util.Log
import com.finora.data.local.AppDatabase
import com.finora.data.local.entity.AccountEntity
import com.finora.data.local.entity.CategoryEntity
import com.finora.data.local.entity.GoalContributionEntity
import com.finora.data.local.entity.GoalEntity
import com.finora.data.local.entity.TransactionEntity
import com.finora.data.local.entity.TransferEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Full local-data snapshot serialized to / from JSON.
 *
 * This is a *manual* backup the user controls: they export it to a file
 * (e.g. Google Drive, Telegram, email to themselves) and can restore it on any
 * device. It is independent of the Supabase cloud sync and protects against
 * data loss even if the cloud account is unavailable.
 *
 * IDs are preserved so all relationships (transactions→accounts, contributions
 * →goals, etc.) stay intact after a restore.
 */
@Serializable
data class BackupData(
    /** Backup format version — bump when the schema changes. */
    val version: Int = 1,
    /** App data schema (Room) version this backup was taken at. */
    val dbVersion: Int = 5,
    /** When the backup was created (epoch millis). */
    val exportedAt: Long = 0L,
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val goalContributions: List<GoalContributionEntity> = emptyList(),
    val transfers: List<TransferEntity> = emptyList()
) {
    val totalRecords: Int
        get() = accounts.size + categories.size + transactions.size +
            goals.size + goalContributions.size + transfers.size
}

class BackupManager(private val db: AppDatabase) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Reads the entire local database and returns it as a pretty JSON string. */
    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val data = BackupData(
            version = CURRENT_VERSION,
            dbVersion = DB_VERSION,
            exportedAt = System.currentTimeMillis(),
            accounts = db.accountDao().getAll(),
            categories = db.categoryDao().getAll(),
            transactions = db.transactionDao().getAll(),
            goals = db.goalDao().getAll(),
            goalContributions = db.goalContributionDao().getAll(),
            transfers = db.transferDao().getAll()
        )
        Log.d(TAG, "exportToJson: ${data.totalRecords} records")
        json.encodeToString(BackupData.serializer(), data)
    }

    /** Parses a JSON backup without touching the DB — used for validation/preview. */
    fun parse(jsonText: String): BackupData =
        json.decodeFromString(BackupData.serializer(), jsonText)

    /**
     * Restores from a JSON backup. Wipes the current local data and replaces it
     * with the backup's contents (IDs preserved). Returns the restored snapshot.
     *
     * Throws if the JSON is invalid — the caller should surface the error and the
     * DB is only cleared *after* a successful parse, so a bad file can't wipe data.
     */
    suspend fun importFromJson(jsonText: String): BackupData = withContext(Dispatchers.IO) {
        // Parse first — if this throws, we have NOT touched the database.
        val data = parse(jsonText)
        Log.d(TAG, "importFromJson: restoring ${data.totalRecords} records")

        db.clearAllTables()

        // Insert in dependency order (parents before children).
        data.accounts.forEach { db.accountDao().upsert(it) }
        data.categories.forEach { db.categoryDao().upsert(it) }
        data.goals.forEach { db.goalDao().upsert(it) }
        data.transactions.forEach { db.transactionDao().upsert(it) }
        data.transfers.forEach { db.transferDao().upsert(it) }
        data.goalContributions.forEach { db.goalContributionDao().upsert(it) }

        data
    }

    companion object {
        private const val TAG = "BackupManager"
        const val CURRENT_VERSION = 1
        private const val DB_VERSION = 5
        /** Suggested file name for exported backups. */
        fun suggestedFileName(): String {
            val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US)
                .format(java.util.Date())
            return "finora_backup_$ts.json"
        }
    }
}
