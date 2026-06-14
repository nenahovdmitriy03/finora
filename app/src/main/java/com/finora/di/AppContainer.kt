package com.finora.di

import android.content.Context
import androidx.room.Room


import com.finora.data.backup.BackupManager
import com.finora.data.local.AppDatabase
import com.finora.data.preferences.SettingsRepository
import com.finora.data.remote.AuthRepository
import com.finora.data.remote.NetworkMonitor
import com.finora.data.remote.SupabaseModule
import com.finora.data.remote.SyncManager
import com.finora.data.repository.FinanceRepository

/** Manual dependency container — created once in [com.finora.FinoraApp]. */
class AppContainer(context: Context) {

    val db: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        AppDatabase.NAME
    )
        .addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8
        )
        .build()

    val settings: SettingsRepository = SettingsRepository(context.applicationContext)

    // ─── Network ──────────────────────────────────────────────────────────

    val networkMonitor: NetworkMonitor = NetworkMonitor(context.applicationContext)

    // ─── Supabase ────────────────────────────────────────────────────────

    private val supabaseClient = SupabaseModule.client

    val authRepository: AuthRepository = AuthRepository(supabaseClient)

    val syncManager: SyncManager = SyncManager(supabaseClient, db)

    /** Manual JSON backup/restore of all local data. */
    val backupManager: BackupManager = BackupManager(db)

    // FinanceRepository gets sync dependencies for auto-sync after data changes
    val repository: FinanceRepository = FinanceRepository(db, syncManager, authRepository)
}
