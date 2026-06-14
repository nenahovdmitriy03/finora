package com.finora.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.finora.data.local.dao.AccountDao
import com.finora.data.local.dao.BudgetDao
import com.finora.data.local.dao.CategoryDao
import com.finora.data.local.dao.ChallengeDao
import com.finora.data.local.dao.GoalContributionDao
import com.finora.data.local.dao.GoalDao
import com.finora.data.local.dao.RecurringRuleDao
import com.finora.data.local.dao.TagDao
import com.finora.data.local.dao.TemplateDao
import com.finora.data.local.dao.TransactionDao
import com.finora.data.local.dao.TransactionTagDao
import com.finora.data.local.dao.TransferDao
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

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        GoalEntity::class,
        GoalContributionEntity::class,
        TransferEntity::class,
        RecurringRuleEntity::class,
        BudgetEntity::class,
        TemplateEntity::class,
        TagEntity::class,
        TransactionTagEntity::class,
        ChallengeEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun goalDao(): GoalDao
    abstract fun goalContributionDao(): GoalContributionDao
    abstract fun transferDao(): TransferDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun budgetDao(): BudgetDao
    abstract fun templateDao(): TemplateDao
    abstract fun tagDao(): TagDao
    abstract fun transactionTagDao(): TransactionTagDao
    abstract fun challengeDao(): ChallengeDao

    companion object {
        const val NAME = "finora.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE goals ADD COLUMN linkedAccountId INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN interestRate REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE accounts ADD COLUMN interestPeriod TEXT")
                db.execSQL("ALTER TABLE accounts ADD COLUMN lastInterestAt INTEGER")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN interestPayoutMinute INTEGER NOT NULL DEFAULT 540")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS goal_contributions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        goalId INTEGER NOT NULL,
                        accountId INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        date INTEGER NOT NULL
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_goal_contributions_goalId ON goal_contributions(goalId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_goal_contributions_accountId ON goal_contributions(accountId)")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS transfers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        fromAccountId INTEGER NOT NULL,
                        toAccountId INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        note TEXT NOT NULL DEFAULT '',
                        date INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transfers_fromAccountId ON transfers(fromAccountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transfers_toAccountId ON transfers(toAccountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transfers_date ON transfers(date)")

                db.execSQL("ALTER TABLE accounts ADD COLUMN interestPayoutDay INTEGER NOT NULL DEFAULT 1")

                db.execSQL(
                    """INSERT INTO goal_contributions (goalId, accountId, amount, date)
                       SELECT id, linkedAccountId, savedAmount, createdAt
                       FROM goals
                       WHERE linkedAccountId IS NOT NULL AND savedAmount > 0"""
                )
                db.execSQL(
                    """UPDATE accounts SET initialBalance = initialBalance + COALESCE(
                        (SELECT SUM(g.savedAmount) FROM goals g
                         WHERE g.linkedAccountId = accounts.id AND g.savedAmount > 0), 0)"""
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS recurring_rules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        amount REAL NOT NULL,
                        type TEXT NOT NULL,
                        categoryId INTEGER NOT NULL,
                        accountId INTEGER NOT NULL,
                        periodDays INTEGER NOT NULL,
                        lastExecutedAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        enabled INTEGER NOT NULL DEFAULT 1
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_rules_categoryId ON recurring_rules(categoryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_rules_accountId ON recurring_rules(accountId)")
            }
        }

        /**
         * v7: budgets, templates, tags, transaction_tags, challenges.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Budgets
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS budgets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        categoryId INTEGER NOT NULL,
                        limitAmount REAL NOT NULL,
                        periodDays INTEGER NOT NULL DEFAULT 30,
                        createdAt INTEGER NOT NULL
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_categoryId ON budgets(categoryId)")

                // Templates
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        amount REAL NOT NULL,
                        type TEXT NOT NULL,
                        categoryId INTEGER,
                        accountId INTEGER,
                        note TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )"""
                )

                // Tags
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS tags (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        color INTEGER NOT NULL
                    )"""
                )

                // Transaction ↔ Tag junction
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS transaction_tags (
                        transactionId INTEGER NOT NULL,
                        tagId INTEGER NOT NULL,
                        PRIMARY KEY (transactionId, tagId)
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transaction_tags_transactionId ON transaction_tags(transactionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transaction_tags_tagId ON transaction_tags(tagId)")

                // Challenges
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS challenges (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        emoji TEXT NOT NULL DEFAULT '🎯',
                        targetDays INTEGER NOT NULL,
                        targetAmount REAL,
                        categoryId INTEGER,
                        startDate INTEGER NOT NULL,
                        endDate INTEGER NOT NULL,
                        completed INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )"""
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE goals ADD COLUMN planMonths INTEGER")
                db.execSQL("ALTER TABLE goals ADD COLUMN plannedMonthlyAmount REAL")
            }
        }
    }
}
