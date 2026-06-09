package com.finora.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
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
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun goalDao(): GoalDao

    companion object {
        const val NAME = "finora.db"
    }
}
