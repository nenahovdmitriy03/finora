package com.finora.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.finora.data.local.dao.AccountDao
import com.finora.data.local.dao.CategoryDao
import com.finora.data.local.dao.GoalDao
import com.finora.data.local.dao.TransactionDao
import com.finora.data.local.entity.AccountEntity
import com.finora.data.local.entity.CategoryEntity
import com.finora.data.local.entity.GoalEntity
import com.finora.data.local.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        GoalEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun goalDao(): GoalDao

    companion object {
        const val NAME = "finora.db"

        /** v2: add goals.linkedAccountId so a goal remembers which account funds it. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE goals ADD COLUMN linkedAccountId INTEGER")
            }
        }
    }
}
