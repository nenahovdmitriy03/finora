package com.finora.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.finora.data.local.dao.AccountDao
import com.finora.data.local.dao.CategoryDao
import com.finora.data.local.dao.GoalContributionDao
import com.finora.data.local.dao.GoalDao
import com.finora.data.local.dao.RecurringRuleDao
import com.finora.data.local.dao.TransactionDao
import com.finora.data.local.dao.TransferDao
import com.finora.data.local.entity.AccountEntity
import com.finora.data.local.entity.CategoryEntity
import com.finora.data.local.entity.GoalContributionEntity
import com.finora.data.local.entity.GoalEntity
import com.finora.data.local.entity.RecurringRuleEntity
import com.finora.data.local.entity.TransactionEntity
import com.finora.data.local.entity.TransferEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        GoalEntity::class,
        GoalContributionEntity::class,
        TransferEntity::class,
        RecurringRuleEntity::class
    ],
    version = 6,
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

    companion object {
        const val NAME = "finora.db"

        /** v2: add goals.linkedAccountId so a goal remembers which account funds it. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE goals ADD COLUMN linkedAccountId INTEGER")
            }
        }

        /** v3: interest-bearing (savings) accounts with periodic capitalization. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN interestRate REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE accounts ADD COLUMN interestPeriod TEXT")
                db.execSQL("ALTER TABLE accounts ADD COLUMN lastInterestAt INTEGER")
            }
        }

        /** v4: per-account interest payout time of day (minutes from midnight). */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN interestPayoutMinute INTEGER NOT NULL DEFAULT 540")
            }
        }

        /**
         * v5: goal contributions table, transfers table, monthly interest day-of-month.
         *
         * - [goal_contributions] tracks every deposit/withdrawal per-account
         *   (fixes the single-linkedAccountId limitation).
         * - [transfers] moves money between accounts without affecting income/expense stats.
         * - [accounts.interestPayoutDay] lets the user pick which calendar day monthly
         *   interest is paid.
         *
         * Migration also seeds goal_contributions from existing goals and restores
         * account initialBalance that was previously reduced by contributeToGoal().
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create goal_contributions table
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

                // 2. Create transfers table
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

                // 3. Add interest payout day column
                db.execSQL("ALTER TABLE accounts ADD COLUMN interestPayoutDay INTEGER NOT NULL DEFAULT 1")

                // 4. Migrate existing goal data → contribution records & restore balances.
                //    We attribute the full savedAmount to the linkedAccountId. If a goal
                //    was funded from multiple accounts (that's exactly the bug), the last-used
                //    account gets the full credit. The user may need to adjust manually.
                db.execSQL(
                    """INSERT INTO goal_contributions (goalId, accountId, amount, date)
                       SELECT id, linkedAccountId, savedAmount, createdAt
                       FROM goals
                       WHERE linkedAccountId IS NOT NULL AND savedAmount > 0"""
                )
                // Restore initialBalance on accounts that were debited for goals
                db.execSQL(
                    """UPDATE accounts SET initialBalance = initialBalance + COALESCE(
                        (SELECT SUM(g.savedAmount) FROM goals g
                         WHERE g.linkedAccountId = accounts.id AND g.savedAmount > 0), 0)"""
                )
            }
        }

        /** v6: recurring (auto) transaction rules. */
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
    }
}
