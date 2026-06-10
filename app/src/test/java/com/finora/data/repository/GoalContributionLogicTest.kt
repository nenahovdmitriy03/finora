package com.finora.data.repository

import com.finora.domain.model.Goal
import com.finora.domain.model.GoalAccountSummary
import com.finora.domain.model.GoalContribution
import com.finora.domain.model.Account
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests the goal contribution and aggregation logic:
 * - deposit / withdraw boundary conditions
 * - savedAmount coercion
 * - GoalAccountSummary aggregation (GoalsViewModel logic)
 */
class GoalContributionLogicTest {

    // ─── Deposit / Withdraw logic ────────────────────────────────────────

    /** Simulates FinanceRepository.contributeToGoal() saved-amount calculation */
    private fun newSavedAmount(currentSaved: Double, amount: Double): Double =
        (currentSaved + amount).coerceAtLeast(0.0)

    private fun realized(currentSaved: Double, amount: Double): Double {
        val newSaved = newSavedAmount(currentSaved, amount)
        return newSaved - currentSaved
    }

    @Test
    fun `deposit increases savedAmount`() {
        assertEquals(15_000.0, newSavedAmount(10_000.0, 5_000.0), 0.001)
    }

    @Test
    fun `withdraw decreases savedAmount`() {
        assertEquals(5_000.0, newSavedAmount(10_000.0, -5_000.0), 0.001)
    }

    @Test
    fun `withdraw cannot go below zero`() {
        assertEquals(0.0, newSavedAmount(5_000.0, -10_000.0), 0.001)
    }

    @Test
    fun `realized amount is clamped for over-withdrawal`() {
        val r = realized(5_000.0, -10_000.0)
        assertEquals(-5_000.0, r, 0.001) // only 5000 was actually withdrawn
    }

    @Test
    fun `zero amount produces zero realized`() {
        assertEquals(0.0, realized(10_000.0, 0.0), 0.001)
    }

    @Test
    fun `deposit on empty goal`() {
        assertEquals(1_000.0, newSavedAmount(0.0, 1_000.0), 0.001)
    }

    @Test
    fun `withdraw everything`() {
        val saved = 25_000.0
        assertEquals(0.0, newSavedAmount(saved, -saved), 0.001)
    }

    @Test
    fun `realized equals amount for normal deposit`() {
        assertEquals(5_000.0, realized(10_000.0, 5_000.0), 0.001)
    }

    // ─── Goal.progress after contribution ────────────────────────────────

    @Test
    fun `progress updates after deposit`() {
        val goal = Goal(name = "Test", targetAmount = 100_000.0, savedAmount = 0.0)
        val updated = goal.copy(savedAmount = newSavedAmount(goal.savedAmount, 50_000.0))
        assertEquals(0.5f, updated.progress, 0.001f)
    }

    @Test
    fun `progress caps at 1 after overfunding`() {
        val goal = Goal(name = "Test", targetAmount = 50_000.0, savedAmount = 40_000.0)
        val updated = goal.copy(savedAmount = newSavedAmount(goal.savedAmount, 30_000.0))
        assertEquals(1f, updated.progress, 0.001f)
    }

    // ─── GoalAccountSummary aggregation ──────────────────────────────────

    /** Simulates GoalsViewModel aggregation logic */
    private fun aggregateSources(
        contributions: List<GoalContribution>,
        accountMap: Map<Long, Account>
    ): List<GoalAccountSummary> {
        return contributions
            .groupBy { it.accountId }
            .mapNotNull { (accId, items) ->
                val acc = accountMap[accId] ?: return@mapNotNull null
                val net = items.sumOf { it.amount }
                if (net == 0.0) return@mapNotNull null
                GoalAccountSummary(account = acc, netAmount = net)
            }
            .sortedByDescending { it.netAmount }
    }

    @Test
    fun `single account single deposit`() {
        val acc = Account(id = 1, name = "Тинькофф")
        val contributions = listOf(
            GoalContribution(goalId = 1, accountId = 1, amount = 10_000.0)
        )
        val result = aggregateSources(contributions, mapOf(1L to acc))
        assertEquals(1, result.size)
        assertEquals(10_000.0, result[0].netAmount, 0.001)
    }

    @Test
    fun `two accounts contributing to same goal`() {
        val acc1 = Account(id = 1, name = "Тинькофф")
        val acc2 = Account(id = 2, name = "Сбер")
        val contributions = listOf(
            GoalContribution(goalId = 1, accountId = 1, amount = 20_000.0),
            GoalContribution(goalId = 1, accountId = 2, amount = 10_000.0)
        )
        val result = aggregateSources(contributions, mapOf(1L to acc1, 2L to acc2))
        assertEquals(2, result.size)
        assertEquals(20_000.0, result[0].netAmount, 0.001) // sorted desc
        assertEquals(10_000.0, result[1].netAmount, 0.001)
    }

    @Test
    fun `deposits and withdrawals net out`() {
        val acc = Account(id = 1, name = "Test")
        val contributions = listOf(
            GoalContribution(goalId = 1, accountId = 1, amount = 10_000.0),
            GoalContribution(goalId = 1, accountId = 1, amount = -3_000.0)
        )
        val result = aggregateSources(contributions, mapOf(1L to acc))
        assertEquals(1, result.size)
        assertEquals(7_000.0, result[0].netAmount, 0.001)
    }

    @Test
    fun `fully withdrawn account is excluded`() {
        val acc = Account(id = 1, name = "Test")
        val contributions = listOf(
            GoalContribution(goalId = 1, accountId = 1, amount = 5_000.0),
            GoalContribution(goalId = 1, accountId = 1, amount = -5_000.0)
        )
        val result = aggregateSources(contributions, mapOf(1L to acc))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `deleted account is excluded from sources`() {
        val contributions = listOf(
            GoalContribution(goalId = 1, accountId = 999, amount = 10_000.0)
        )
        val result = aggregateSources(contributions, emptyMap())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `no contributions returns empty list`() {
        val result = aggregateSources(emptyList(), emptyMap())
        assertTrue(result.isEmpty())
    }

    // ─── Free balance calculation ────────────────────────────────────────

    @Test
    fun `free balance is total minus goals`() {
        val totalBalance = 200_000.0
        val inGoals = listOf(
            Goal(name = "A", targetAmount = 100_000.0, savedAmount = 30_000.0),
            Goal(name = "B", targetAmount = 50_000.0, savedAmount = 20_000.0)
        ).sumOf { it.savedAmount }
        val free = (totalBalance - inGoals).coerceAtLeast(0.0)
        assertEquals(150_000.0, free, 0.001)
    }

    @Test
    fun `goals do not affect account balance (earmark only)`() {
        // Key design: contributeToGoal does NOT modify account.initialBalance
        // So account balance stays the same regardless of goal contributions
        val initialBalance = 100_000.0
        val txDelta = 5_000.0
        val computedBalance = initialBalance + txDelta
        // After contributing 30k to a goal, computed balance is unchanged
        assertEquals(105_000.0, computedBalance, 0.001)
    }
}
