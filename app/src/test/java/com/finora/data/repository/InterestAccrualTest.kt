package com.finora.data.repository

import com.finora.domain.model.InterestPeriod
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

/**
 * Tests the interest accrual / capitalization algorithm from FinanceRepository.
 *
 * The compound interest formula:
 *   for each period: gain = balance * (annualRate / 100 / periodsPerYear)
 *                    balance += gain
 *   total interest = sum of all gains
 *
 * Monthly payout timestamps use Calendar month advancement, clamping
 * payoutDay to the month's actual maximum (e.g., 31 → 28 in Feb).
 */
class InterestAccrualTest {

    // ─── Compound interest ───────────────────────────────────────────────

    /** Mirrors FinanceRepository.compoundInterest() */
    private fun compoundInterest(balance: Double, annualRate: Double, period: InterestPeriod, numPeriods: Int): Double {
        val ratePerPeriod = annualRate / 100.0 / period.periodsPerYear
        var bal = balance
        var total = 0.0
        repeat(numPeriods) {
            val gain = bal * ratePerPeriod
            total += gain
            bal += gain
        }
        return kotlin.math.round(total * 100.0) / 100.0
    }

    @Test
    fun `daily interest for 1 day at 10 percent on 100k`() {
        // 100000 * (10/100/365) = ~27.40
        val interest = compoundInterest(100_000.0, 10.0, InterestPeriod.DAILY, 1)
        assertEquals(27.40, interest, 0.1)
    }

    @Test
    fun `daily interest for 30 days at 10 percent on 100k`() {
        // Compound: slightly more than 30 * 27.40 = 822.0
        val interest = compoundInterest(100_000.0, 10.0, InterestPeriod.DAILY, 30)
        assertTrue(interest > 820.0)
        assertTrue(interest < 830.0)
    }

    @Test
    fun `monthly interest for 1 month at 12 percent on 100k`() {
        // 100000 * (12/100/12) = 1000.0
        val interest = compoundInterest(100_000.0, 12.0, InterestPeriod.MONTHLY, 1)
        assertEquals(1_000.0, interest, 0.01)
    }

    @Test
    fun `monthly interest for 12 months at 12 percent on 100k (compound)`() {
        // Compound: each month earns on accumulated balance
        // Simple: 12000.0, Compound should be slightly more
        val interest = compoundInterest(100_000.0, 12.0, InterestPeriod.MONTHLY, 12)
        assertTrue("Compound should exceed simple interest", interest > 12_000.0)
        // Known compound result: 100000 * (1 + 0.01)^12 - 100000 ≈ 12682.50
        assertEquals(12_682.50, interest, 1.0)
    }

    @Test
    fun `zero rate produces zero interest`() {
        val interest = compoundInterest(100_000.0, 0.0, InterestPeriod.MONTHLY, 12)
        assertEquals(0.0, interest, 0.001)
    }

    @Test
    fun `zero balance produces zero interest`() {
        val interest = compoundInterest(0.0, 12.0, InterestPeriod.DAILY, 365)
        assertEquals(0.0, interest, 0.001)
    }

    @Test
    fun `zero periods produce zero interest`() {
        val interest = compoundInterest(100_000.0, 12.0, InterestPeriod.MONTHLY, 0)
        assertEquals(0.0, interest, 0.001)
    }

    @Test
    fun `small balance with high rate`() {
        // 100 rubles at 20% daily for 365 days
        val interest = compoundInterest(100.0, 20.0, InterestPeriod.DAILY, 365)
        // Should be ~22.13 (compound)
        assertTrue(interest > 20.0)
        assertTrue(interest < 25.0)
    }

    @Test
    fun `large balance monthly interest is precise`() {
        // 1M at 16% monthly for 1 month
        // 1000000 * 0.16/12 = 13333.33
        val interest = compoundInterest(1_000_000.0, 16.0, InterestPeriod.MONTHLY, 1)
        assertEquals(13_333.33, interest, 0.01)
    }

    // ─── Monthly payout timestamps ───────────────────────────────────────

    /** Mirrors FinanceRepository.monthlyPayoutTimestamps() */
    private fun monthlyPayoutTimestamps(
        base: Long, now: Long, payoutDay: Int, payoutMinute: Int = 540
    ): List<Long> {
        val result = mutableListOf<Long>()
        val cal = Calendar.getInstance().apply { timeInMillis = base }
        cal.add(Calendar.MONTH, 1)
        for (i in 0 until 400) {
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, payoutDay.coerceAtMost(maxDay))
            cal.set(Calendar.HOUR_OF_DAY, payoutMinute / 60)
            cal.set(Calendar.MINUTE, payoutMinute % 60)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val ts = cal.timeInMillis
            if (ts > now) break
            if (ts > base) result += ts
            cal.add(Calendar.MONTH, 1)
        }
        return result
    }

    @Test
    fun `no payouts when now is before next month`() {
        val base = calMillis(2026, Calendar.JUNE, 1)
        val now = calMillis(2026, Calendar.JUNE, 15)
        val payouts = monthlyPayoutTimestamps(base, now, payoutDay = 1)
        assertTrue(payouts.isEmpty())
    }

    @Test
    fun `one payout after one month`() {
        val base = calMillis(2026, Calendar.JANUARY, 1)
        val now = calMillis(2026, Calendar.FEBRUARY, 2)
        val payouts = monthlyPayoutTimestamps(base, now, payoutDay = 1)
        assertEquals(1, payouts.size)
    }

    @Test
    fun `three payouts after three months`() {
        val base = calMillis(2026, Calendar.JANUARY, 1)
        val now = calMillis(2026, Calendar.APRIL, 2)
        val payouts = monthlyPayoutTimestamps(base, now, payoutDay = 1)
        assertEquals(3, payouts.size)
    }

    @Test
    fun `payoutDay 31 clamped to 28 in February`() {
        val base = calMillis(2026, Calendar.JANUARY, 15)
        val now = calMillis(2026, Calendar.MARCH, 1)
        val payouts = monthlyPayoutTimestamps(base, now, payoutDay = 31)
        assertEquals(1, payouts.size) // Feb payout
        val cal = Calendar.getInstance().apply { timeInMillis = payouts[0] }
        assertEquals(28, cal.get(Calendar.DAY_OF_MONTH)) // Feb 28 (non-leap 2026)
    }

    @Test
    fun `payoutDay 31 stays 31 in January`() {
        val base = calMillis(2025, Calendar.DECEMBER, 1)
        val now = calMillis(2026, Calendar.FEBRUARY, 1)
        val payouts = monthlyPayoutTimestamps(base, now, payoutDay = 31)
        assertEquals(1, payouts.size) // Jan 31 payout
        val cal = Calendar.getInstance().apply { timeInMillis = payouts[0] }
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `twelve payouts in a year`() {
        val base = calMillis(2025, Calendar.JANUARY, 1)
        val now = calMillis(2026, Calendar.JANUARY, 2)
        val payouts = monthlyPayoutTimestamps(base, now, payoutDay = 1)
        assertEquals(12, payouts.size)
    }

    @Test
    fun `payoutMinute is respected`() {
        val base = calMillis(2026, Calendar.JANUARY, 1)
        val now = calMillis(2026, Calendar.FEBRUARY, 2)
        // Payout at 14:30 (870 minutes)
        val payouts = monthlyPayoutTimestamps(base, now, payoutDay = 1, payoutMinute = 870)
        assertEquals(1, payouts.size)
        val cal = Calendar.getInstance().apply { timeInMillis = payouts[0] }
        assertEquals(14, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
    }

    // ─── Daily period counting ───────────────────────────────────────────

    @Test
    fun `daily periods count correctly`() {
        val dayMs = 86_400_000L
        val base = calMillis(2026, Calendar.JUNE, 1)
        val now = base + 10 * dayMs
        val periods = ((now - base) / dayMs).toInt()
        assertEquals(10, periods)
    }

    @Test
    fun `partial day does not count as period`() {
        val dayMs = 86_400_000L
        val base = calMillis(2026, Calendar.JUNE, 1)
        val now = base + 2 * dayMs + dayMs / 2 // 2.5 days
        val periods = ((now - base) / dayMs).toInt()
        assertEquals(2, periods)
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private fun calMillis(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            set(year, month, day, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
