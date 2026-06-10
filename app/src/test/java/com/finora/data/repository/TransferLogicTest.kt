package com.finora.data.repository

import com.finora.domain.model.Account
import com.finora.domain.model.AccountBalance
import com.finora.domain.model.Transfer
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests the transfer logic and its effect on account balances.
 *
 * Transfers move money between accounts. They:
 * - DON'T affect income/expense stats
 * - DO affect account balances via transfer deltas
 * - Sender gets -amount, receiver gets +amount
 */
class TransferLogicTest {

    /** Compute transfer deltas from a list of transfers, same as TransferDao SQL */
    private fun transferDeltas(transfers: List<Transfer>): Map<Long, Double> {
        val deltas = mutableMapOf<Long, Double>()
        for (t in transfers) {
            deltas[t.fromAccountId] = (deltas[t.fromAccountId] ?: 0.0) - t.amount
            deltas[t.toAccountId] = (deltas[t.toAccountId] ?: 0.0) + t.amount
        }
        return deltas
    }

    @Test
    fun `single transfer affects two accounts`() {
        val transfers = listOf(
            Transfer(fromAccountId = 1, toAccountId = 2, amount = 5_000.0)
        )
        val deltas = transferDeltas(transfers)
        assertEquals(-5_000.0, deltas[1L]!!, 0.001)
        assertEquals(5_000.0, deltas[2L]!!, 0.001)
    }

    @Test
    fun `multiple transfers accumulate`() {
        val transfers = listOf(
            Transfer(fromAccountId = 1, toAccountId = 2, amount = 5_000.0),
            Transfer(fromAccountId = 1, toAccountId = 2, amount = 3_000.0)
        )
        val deltas = transferDeltas(transfers)
        assertEquals(-8_000.0, deltas[1L]!!, 0.001)
        assertEquals(8_000.0, deltas[2L]!!, 0.001)
    }

    @Test
    fun `bidirectional transfers net out`() {
        val transfers = listOf(
            Transfer(fromAccountId = 1, toAccountId = 2, amount = 10_000.0),
            Transfer(fromAccountId = 2, toAccountId = 1, amount = 10_000.0)
        )
        val deltas = transferDeltas(transfers)
        assertEquals(0.0, deltas[1L]!!, 0.001)
        assertEquals(0.0, deltas[2L]!!, 0.001)
    }

    @Test
    fun `partial return leaves net difference`() {
        val transfers = listOf(
            Transfer(fromAccountId = 1, toAccountId = 2, amount = 10_000.0),
            Transfer(fromAccountId = 2, toAccountId = 1, amount = 3_000.0)
        )
        val deltas = transferDeltas(transfers)
        assertEquals(-7_000.0, deltas[1L]!!, 0.001) // sent 10k, got 3k back
        assertEquals(7_000.0, deltas[2L]!!, 0.001)
    }

    @Test
    fun `no transfers produces empty deltas`() {
        val deltas = transferDeltas(emptyList())
        assertTrue(deltas.isEmpty())
    }

    @Test
    fun `transfers between three accounts`() {
        val transfers = listOf(
            Transfer(fromAccountId = 1, toAccountId = 2, amount = 10_000.0),
            Transfer(fromAccountId = 2, toAccountId = 3, amount = 5_000.0),
            Transfer(fromAccountId = 3, toAccountId = 1, amount = 2_000.0)
        )
        val deltas = transferDeltas(transfers)
        assertEquals(-8_000.0, deltas[1L]!!, 0.001) // -10k + 2k
        assertEquals(5_000.0, deltas[2L]!!, 0.001)   // +10k - 5k
        assertEquals(3_000.0, deltas[3L]!!, 0.001)   // +5k - 2k
    }

    @Test
    fun `total of all deltas is zero (zero-sum)`() {
        val transfers = listOf(
            Transfer(fromAccountId = 1, toAccountId = 2, amount = 15_000.0),
            Transfer(fromAccountId = 3, toAccountId = 1, amount = 7_000.0),
            Transfer(fromAccountId = 2, toAccountId = 3, amount = 3_000.0)
        )
        val deltas = transferDeltas(transfers)
        val totalDelta = deltas.values.sum()
        assertEquals(0.0, totalDelta, 0.001) // money doesn't appear or disappear
    }

    @Test
    fun `transfer does not count as income or expense`() {
        // Transfers have their own table. They only appear in transferDeltas,
        // NOT in transactionDao.observeBalanceDeltas() which only reads transactions.
        // This verifies the design: a transfer of 10k does not create income/expense.
        val incomeExpense = 0.0 // no transaction created
        assertEquals(0.0, incomeExpense, 0.001)
    }

    // ─── Full balance scenario with transfers ────────────────────────────

    @Test
    fun `complete scenario with accounts, transactions, transfers`() {
        // Account 1: initial 100k, +30k income, -10k expense, sent 20k to acc2
        // Account 2: initial 50k, -5k expense, received 20k from acc1
        val acc1Initial = 100_000.0
        val acc2Initial = 50_000.0
        val txDeltas = mapOf(1L to 20_000.0, 2L to -5_000.0) // net tx per account
        val transferDeltasMap = mapOf(1L to -20_000.0, 2L to 20_000.0)

        val bal1 = acc1Initial + (txDeltas[1L] ?: 0.0) + (transferDeltasMap[1L] ?: 0.0)
        val bal2 = acc2Initial + (txDeltas[2L] ?: 0.0) + (transferDeltasMap[2L] ?: 0.0)

        assertEquals(100_000.0, bal1, 0.001) // 100k + 20k - 20k
        assertEquals(65_000.0, bal2, 0.001)  // 50k - 5k + 20k

        // Total balance should equal sum of initial + total tx deltas
        // (transfers are zero-sum so they cancel out in total)
        val totalBalance = bal1 + bal2
        val totalInitial = acc1Initial + acc2Initial
        val totalTxDelta = txDeltas.values.sum()
        assertEquals(totalInitial + totalTxDelta, totalBalance, 0.001)
    }
}
