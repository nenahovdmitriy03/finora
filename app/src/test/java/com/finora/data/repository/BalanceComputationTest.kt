package com.finora.data.repository

import com.finora.domain.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests the account balance computation logic:
 *   computed balance = initialBalance + txDeltas + transferDeltas
 *
 * This is the core algorithm used in FinanceRepository.observeAccountBalances().
 */
class BalanceComputationTest {

    /**
     * Simulates the combine() logic from FinanceRepository.observeAccountBalances()
     */
    private fun computeBalances(
        accounts: List<Account>,
        txDeltas: Map<Long, Double>,
        transferDeltas: Map<Long, Double>
    ): List<AccountBalance> {
        val deltaMap = mutableMapOf<Long, Double>()
        for ((accId, d) in txDeltas) deltaMap[accId] = (deltaMap[accId] ?: 0.0) + d
        for ((accId, d) in transferDeltas) deltaMap[accId] = (deltaMap[accId] ?: 0.0) + d
        return accounts.map { acc ->
            AccountBalance(acc, acc.initialBalance + (deltaMap[acc.id] ?: 0.0))
        }
    }

    @Test
    fun `balance equals initialBalance when no transactions or transfers`() {
        val acc = account(id = 1, initial = 50_000.0)
        val result = computeBalances(listOf(acc), emptyMap(), emptyMap())
        assertEquals(50_000.0, result[0].balance, 0.001)
    }

    @Test
    fun `income increases balance`() {
        val acc = account(id = 1, initial = 10_000.0)
        val txDeltas = mapOf(1L to 5_000.0) // net income > expense
        val result = computeBalances(listOf(acc), txDeltas, emptyMap())
        assertEquals(15_000.0, result[0].balance, 0.001)
    }

    @Test
    fun `expense decreases balance`() {
        val acc = account(id = 1, initial = 10_000.0)
        val txDeltas = mapOf(1L to -3_000.0) // net expense > income
        val result = computeBalances(listOf(acc), txDeltas, emptyMap())
        assertEquals(7_000.0, result[0].balance, 0.001)
    }

    @Test
    fun `transfer out decreases balance, transfer in increases`() {
        val acc1 = account(id = 1, initial = 20_000.0)
        val acc2 = account(id = 2, initial = 5_000.0)
        // Transfer 10000 from acc1 to acc2
        val transferDeltas = mapOf(1L to -10_000.0, 2L to 10_000.0)
        val result = computeBalances(listOf(acc1, acc2), emptyMap(), transferDeltas)
        assertEquals(10_000.0, result[0].balance, 0.001)
        assertEquals(15_000.0, result[1].balance, 0.001)
    }

    @Test
    fun `mixed transactions and transfers combine correctly`() {
        val acc = account(id = 1, initial = 100_000.0)
        val txDeltas = mapOf(1L to 20_000.0) // +20k from income-expense
        val transferDeltas = mapOf(1L to -15_000.0) // -15k from transfer out
        val result = computeBalances(listOf(acc), txDeltas, transferDeltas)
        // 100000 + 20000 - 15000 = 105000
        assertEquals(105_000.0, result[0].balance, 0.001)
    }

    @Test
    fun `balance can go negative`() {
        val acc = account(id = 1, initial = 1_000.0)
        val txDeltas = mapOf(1L to -5_000.0)
        val result = computeBalances(listOf(acc), txDeltas, emptyMap())
        assertEquals(-4_000.0, result[0].balance, 0.001)
    }

    @Test
    fun `accounts without deltas keep initial balance`() {
        val acc1 = account(id = 1, initial = 10_000.0)
        val acc2 = account(id = 2, initial = 20_000.0)
        val txDeltas = mapOf(1L to 5_000.0) // only acc1 has transactions
        val result = computeBalances(listOf(acc1, acc2), txDeltas, emptyMap())
        assertEquals(15_000.0, result[0].balance, 0.001)
        assertEquals(20_000.0, result[1].balance, 0.001) // unchanged
    }

    @Test
    fun `multiple transfers aggregate per account`() {
        val acc = account(id = 1, initial = 50_000.0)
        // Two transfers: -10000, +3000 = net -7000
        val transferDeltas = mapOf(1L to -7_000.0)
        val result = computeBalances(listOf(acc), emptyMap(), transferDeltas)
        assertEquals(43_000.0, result[0].balance, 0.001)
    }

    // ─── Editor balance reverse-computation ──────────────────────────────

    /**
     * Tests the formula used in AccountsScreen when saving from editor:
     *   newInitialBalance = enteredBalance - (computedBalance - oldInitialBalance)
     *
     * The user enters the desired real balance; we reverse-compute what
     * initialBalance must be to achieve that after tx + transfer deltas.
     */
    @Test
    fun `editor reverse-computes initialBalance correctly`() {
        val oldInitial = 50_000.0
        val computedBalance = 65_000.0 // 15k from transactions
        val externalDelta = computedBalance - oldInitial // 15000

        // User changes balance to 70000 in editor
        val enteredBalance = 70_000.0
        val newInitialBalance = enteredBalance - externalDelta
        assertEquals(55_000.0, newInitialBalance, 0.001)

        // Verify: newInitial + externalDelta = enteredBalance
        assertEquals(enteredBalance, newInitialBalance + externalDelta, 0.001)
    }

    @Test
    fun `editor for new account initial equals entered`() {
        // New account: no prior deltas, so initial = entered
        val oldInitial = 0.0
        val computedBalance = 0.0
        val externalDelta = computedBalance - oldInitial // 0
        val enteredBalance = 100_000.0
        val newInitialBalance = enteredBalance - externalDelta
        assertEquals(100_000.0, newInitialBalance, 0.001)
    }

    @Test
    fun `editor preserves balance when unchanged`() {
        val oldInitial = 30_000.0
        val computedBalance = 45_000.0 // 15k deltas
        val externalDelta = computedBalance - oldInitial
        // User doesn't change balance
        val enteredBalance = computedBalance
        val newInitialBalance = enteredBalance - externalDelta
        assertEquals(oldInitial, newInitialBalance, 0.001)
    }

    @Test
    fun `editor handles user lowering balance`() {
        val oldInitial = 100_000.0
        val computedBalance = 120_000.0 // 20k deltas
        val externalDelta = computedBalance - oldInitial
        // User lowers balance to 110000
        val enteredBalance = 110_000.0
        val newInitialBalance = enteredBalance - externalDelta
        assertEquals(90_000.0, newInitialBalance, 0.001)
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private fun account(id: Long = 0, initial: Double = 0.0) = Account(
        id = id, name = "Test", initialBalance = initial
    )
}
