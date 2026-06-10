package com.finora.domain.model

import org.junit.Assert.*
import org.junit.Test

class ModelsTest {

    // ─── Goal.progress ───────────────────────────────────────────────────

    @Test
    fun `progress is zero when no money saved`() {
        val goal = goal(target = 100_000.0, saved = 0.0)
        assertEquals(0f, goal.progress, 0.001f)
    }

    @Test
    fun `progress is 50 percent when half saved`() {
        val goal = goal(target = 10_000.0, saved = 5_000.0)
        assertEquals(0.5f, goal.progress, 0.001f)
    }

    @Test
    fun `progress is clamped to 1 when overfunded`() {
        val goal = goal(target = 10_000.0, saved = 15_000.0)
        assertEquals(1f, goal.progress, 0.001f)
    }

    @Test
    fun `progress is zero when target is zero`() {
        val goal = goal(target = 0.0, saved = 500.0)
        assertEquals(0f, goal.progress, 0.001f)
    }

    @Test
    fun `progress is zero when target is negative`() {
        val goal = goal(target = -100.0, saved = 50.0)
        assertEquals(0f, goal.progress, 0.001f)
    }

    @Test
    fun `progress is 100 percent when goal fully reached`() {
        val goal = goal(target = 50_000.0, saved = 50_000.0)
        assertEquals(1f, goal.progress, 0.001f)
    }

    // ─── Account.hasInterest ─────────────────────────────────────────────

    @Test
    fun `hasInterest true when rate and period set`() {
        val acc = account(rate = 12.0, period = InterestPeriod.MONTHLY)
        assertTrue(acc.hasInterest)
    }

    @Test
    fun `hasInterest false when rate is zero`() {
        val acc = account(rate = 0.0, period = InterestPeriod.DAILY)
        assertFalse(acc.hasInterest)
    }

    @Test
    fun `hasInterest false when period is null`() {
        val acc = account(rate = 5.0, period = null)
        assertFalse(acc.hasInterest)
    }

    @Test
    fun `hasInterest false when both absent`() {
        val acc = account(rate = 0.0, period = null)
        assertFalse(acc.hasInterest)
    }

    // ─── AccountBalance ──────────────────────────────────────────────────

    @Test
    fun `AccountBalance holds account and balance together`() {
        val acc = account(initialBalance = 1000.0)
        val ab = AccountBalance(acc, 1500.0)
        assertEquals(acc, ab.account)
        assertEquals(1500.0, ab.balance, 0.001)
    }

    // ─── InterestPeriod ──────────────────────────────────────────────────

    @Test
    fun `daily period has 365 periods per year`() {
        assertEquals(365, InterestPeriod.DAILY.periodsPerYear)
    }

    @Test
    fun `monthly period has 12 periods per year`() {
        assertEquals(12, InterestPeriod.MONTHLY.periodsPerYear)
    }

    // ─── GoalContribution ────────────────────────────────────────────────

    @Test
    fun `GoalContribution stores deposit correctly`() {
        val c = GoalContribution(goalId = 1, accountId = 2, amount = 5000.0, date = 100L)
        assertEquals(1L, c.goalId)
        assertEquals(2L, c.accountId)
        assertEquals(5000.0, c.amount, 0.001)
    }

    @Test
    fun `GoalContribution negative amount represents withdrawal`() {
        val c = GoalContribution(goalId = 1, accountId = 2, amount = -3000.0)
        assertTrue(c.amount < 0)
    }

    // ─── Transfer ────────────────────────────────────────────────────────

    @Test
    fun `Transfer stores from-to correctly`() {
        val t = Transfer(fromAccountId = 1, toAccountId = 2, amount = 10_000.0)
        assertEquals(1L, t.fromAccountId)
        assertEquals(2L, t.toAccountId)
        assertEquals(10_000.0, t.amount, 0.001)
    }

    // ─── AccountType ─────────────────────────────────────────────────────

    @Test
    fun `AccountType entries have Russian titles`() {
        assertEquals("Карта", AccountType.CARD.title)
        assertEquals("Наличные", AccountType.CASH.title)
        assertEquals("Накопительный", AccountType.SAVINGS.title)
        assertEquals("Вклад", AccountType.DEPOSIT.title)
        assertEquals("Другое", AccountType.OTHER.title)
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private fun goal(target: Double, saved: Double) = Goal(
        name = "Test", targetAmount = target, savedAmount = saved
    )

    private fun account(
        initialBalance: Double = 0.0,
        rate: Double = 0.0,
        period: InterestPeriod? = null
    ) = Account(
        name = "Test",
        initialBalance = initialBalance,
        interestRate = rate,
        interestPeriod = period
    )
}
