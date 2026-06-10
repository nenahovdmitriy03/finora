package com.finora.presentation.util

import com.finora.domain.model.TransactionType
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class FormatTest {

    // ─── formatMoney ─────────────────────────────────────────────────────

    @Test
    fun `formatMoney formats small amount`() {
        val result = formatMoney(500.0)
        assertEquals("500\u00A0₽", result)
    }

    @Test
    fun `formatMoney groups thousands with non-breaking space`() {
        val result = formatMoney(12500.0)
        assertEquals("12\u00A0500\u00A0₽", result)
    }

    @Test
    fun `formatMoney handles millions`() {
        val result = formatMoney(1_234_567.0)
        assertEquals("1\u00A0234\u00A0567\u00A0₽", result)
    }

    @Test
    fun `formatMoney handles zero`() {
        assertEquals("0\u00A0₽", formatMoney(0.0))
    }

    @Test
    fun `formatMoney handles negative`() {
        val result = formatMoney(-5000.0)
        assertEquals("-5\u00A0000\u00A0₽", result)
    }

    @Test
    fun `formatMoney without symbol`() {
        val result = formatMoney(10_000.0, withSymbol = false)
        assertEquals("10\u00A0000", result)
    }

    @Test
    fun `formatMoney rounds fractional to whole`() {
        val result = formatMoney(1234.56)
        assertEquals("1\u00A0235\u00A0₽", result)
    }

    // ─── formatSigned ────────────────────────────────────────────────────

    @Test
    fun `formatSigned income has plus sign`() {
        val result = formatSigned(5000.0, TransactionType.INCOME)
        assertTrue(result.startsWith("+"))
        assertTrue(result.contains("5\u00A0000"))
    }

    @Test
    fun `formatSigned expense has minus sign`() {
        val result = formatSigned(3200.0, TransactionType.EXPENSE)
        assertTrue(result.startsWith("−"))
        assertTrue(result.contains("3\u00A0200"))
    }

    // ─── startOfDay / endOfDay ───────────────────────────────────────────

    @Test
    fun `startOfDay zeros out hours minutes seconds`() {
        val now = System.currentTimeMillis()
        val start = startOfDay(now)
        val cal = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
        assertEquals(0, cal.get(Calendar.MILLISECOND))
    }

    @Test
    fun `endOfDay sets 23-59-59-999`() {
        val now = System.currentTimeMillis()
        val end = endOfDay(now)
        val cal = Calendar.getInstance().apply { timeInMillis = end }
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal.get(Calendar.MINUTE))
        assertEquals(59, cal.get(Calendar.SECOND))
        assertEquals(999, cal.get(Calendar.MILLISECOND))
    }

    @Test
    fun `startOfDay and endOfDay span full day`() {
        val now = System.currentTimeMillis()
        val dayMs = endOfDay(now) - startOfDay(now)
        // Full day minus 1 ms = 23:59:59.999
        assertEquals(24 * 60 * 60 * 1000L - 1, dayMs)
    }

    // ─── startOfMonth / endOfMonth ───────────────────────────────────────

    @Test
    fun `startOfMonth is day 1 at midnight`() {
        val now = System.currentTimeMillis()
        val start = startOfMonth(now)
        val cal = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `endOfMonth is last day at 23-59-59`() {
        val now = System.currentTimeMillis()
        val end = endOfMonth(now)
        val cal = Calendar.getInstance().apply { timeInMillis = end }
        assertEquals(
            cal.getActualMaximum(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
    }

    // ─── isSameDay ───────────────────────────────────────────────────────

    @Test
    fun `isSameDay returns true for same day`() {
        val a = System.currentTimeMillis()
        val b = a + 1000
        assertTrue(isSameDay(a, b))
    }

    @Test
    fun `isSameDay returns false for different days`() {
        val a = System.currentTimeMillis()
        val b = a + 48 * 60 * 60 * 1000L
        assertFalse(isSameDay(a, b))
    }

    // ─── addMonths ───────────────────────────────────────────────────────

    @Test
    fun `addMonths advances month`() {
        // January 15 + 1 month = February 15
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 15, 12, 0, 0)
        }
        val result = addMonths(cal.timeInMillis, 1)
        val resCal = Calendar.getInstance().apply { timeInMillis = result }
        assertEquals(Calendar.FEBRUARY, resCal.get(Calendar.MONTH))
    }

    @Test
    fun `addMonths negative goes backwards`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 10, 12, 0, 0)
        }
        val result = addMonths(cal.timeInMillis, -1)
        val resCal = Calendar.getInstance().apply { timeInMillis = result }
        assertEquals(Calendar.FEBRUARY, resCal.get(Calendar.MONTH))
    }

    // ─── relativeDayLabel ────────────────────────────────────────────────

    @Test
    fun `relativeDayLabel today returns Сегодня`() {
        assertEquals("Сегодня", relativeDayLabel(System.currentTimeMillis()))
    }

    @Test
    fun `relativeDayLabel yesterday returns Вчера`() {
        val yesterday = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        assertEquals("Вчера", relativeDayLabel(yesterday))
    }
}
