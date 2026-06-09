package com.finora.di

import android.content.Context
import androidx.room.Room
import com.finora.data.local.AppDatabase
import com.finora.data.repository.FinanceRepository

/** Manual dependency container — created once in [com.finora.FinoraApp]. */
class AppContainer(context: Context) {

    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        AppDatabase.NAME
    ).build()

    val repository: FinanceRepository = FinanceRepository(database)
}
