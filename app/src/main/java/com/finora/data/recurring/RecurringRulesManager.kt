package com.finora.data.recurring

import android.util.Log
import com.finora.data.local.AppDatabase
import com.finora.data.local.entity.TransactionEntity
import java.util.concurrent.TimeUnit

/**
 * Checks all enabled recurring rules and creates any overdue transactions.
 *
 * Call [executePending] once on app launch (or when the user opens the Transactions
 * or Home screen). It is safe to call multiple times — a rule will only fire once
 * per period.
 *
 * The logic is simple on purpose:
 * - For each enabled rule, compare [now] against [lastExecutedAt].
 * - If at least [periodDays] have elapsed (or the rule has never run), insert a
 *   transaction and bump [lastExecutedAt].
 * - For rules that were dormant for multiple periods (e.g. the app wasn't opened
 *   for 2 months and the rule fires monthly), it creates ONE transaction per
 *   missed period — so the books stay accurate.
 */
class RecurringRulesManager(private val db: AppDatabase) {

    companion object {
        private const val TAG = "RecurringRules"
    }

    /**
     * Runs all enabled rules and creates transactions for each overdue period.
     * Returns the number of transactions inserted.
     */
    suspend fun executePending(): Int {
        val rules = db.recurringRuleDao().getEnabled()
        var created = 0
        val now = System.currentTimeMillis()

        for (rule in rules) {
            val periodMs = TimeUnit.DAYS.toMillis(rule.periodDays.toLong())
            var anchor = rule.lastExecutedAt ?: (rule.createdAt - periodMs) // fire immediately on first run

            while (anchor + periodMs <= now) {
                val txDate = anchor + periodMs
                db.transactionDao().upsert(
                    TransactionEntity(
                        amount = rule.amount,
                        type = rule.type,
                        accountId = rule.accountId,
                        categoryId = rule.categoryId,
                        note = "⟳ ${rule.name}",
                        date = txDate,
                        createdAt = now
                    )
                )
                db.recurringRuleDao().updateLastExecuted(rule.id, txDate)
                anchor = txDate
                created++
                Log.d(TAG, "Created recurring tx for rule '${rule.name}' at $txDate")
            }
        }
        if (created > 0) Log.i(TAG, "Created $created recurring transaction(s)")
        return created
    }
}
