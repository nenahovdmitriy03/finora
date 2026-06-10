package com.finora.presentation.util

import org.junit.Assert.*
import org.junit.Test

class MoneyInputTest {

    // ─── sanitizeMoneyInput ──────────────────────────────────────────────

    @Test
    fun `digits pass through unchanged`() {
        assertEquals("12345", sanitizeMoneyInput("12345"))
    }

    @Test
    fun `comma converted to dot`() {
        assertEquals("123.45", sanitizeMoneyInput("123,45"))
    }

    @Test
    fun `dot preserved as decimal separator`() {
        assertEquals("123.45", sanitizeMoneyInput("123.45"))
    }

    @Test
    fun `only two decimal places allowed`() {
        assertEquals("100.99", sanitizeMoneyInput("100.999"))
    }

    @Test
    fun `second decimal separator ignored`() {
        assertEquals("100.50", sanitizeMoneyInput("100.50.30"))
    }

    @Test
    fun `leading dot removed (dot before any digit is ignored)`() {
        assertEquals("", sanitizeMoneyInput(".99"))
    }

    @Test
    fun `non-numeric characters stripped`() {
        assertEquals("5000", sanitizeMoneyInput("5 000₽"))
    }

    @Test
    fun `empty input returns empty`() {
        assertEquals("", sanitizeMoneyInput(""))
    }

    @Test
    fun `only dots returns empty`() {
        assertEquals("", sanitizeMoneyInput("..."))
    }

    @Test
    fun `mixed comma-dot only first separator used`() {
        assertEquals("1.23", sanitizeMoneyInput("1,23.45"))
    }

    // ─── parseMoney ──────────────────────────────────────────────────────

    @Test
    fun `parseMoney parses integer`() {
        assertEquals(5000.0, parseMoney("5000"), 0.001)
    }

    @Test
    fun `parseMoney parses decimal with dot`() {
        assertEquals(123.45, parseMoney("123.45"), 0.001)
    }

    @Test
    fun `parseMoney converts comma to dot`() {
        assertEquals(99.99, parseMoney("99,99"), 0.001)
    }

    @Test
    fun `parseMoney returns zero for empty`() {
        assertEquals(0.0, parseMoney(""), 0.001)
    }

    @Test
    fun `parseMoney returns zero for garbage`() {
        assertEquals(0.0, parseMoney("abc"), 0.001)
    }

    @Test
    fun `parseMoney handles large numbers`() {
        assertEquals(1_000_000.0, parseMoney("1000000"), 0.001)
    }

    // ─── ThousandsVisualTransformation ───────────────────────────────────

    @Test
    fun `visual transformation groups thousands`() {
        val vt = ThousandsVisualTransformation()
        val result = vt.filter(androidx.compose.ui.text.AnnotatedString("1234567"))
        assertEquals("1\u00A0234\u00A0567", result.text.text)
    }

    @Test
    fun `visual transformation no grouping for short numbers`() {
        val vt = ThousandsVisualTransformation()
        val result = vt.filter(androidx.compose.ui.text.AnnotatedString("999"))
        assertEquals("999", result.text.text)
    }

    @Test
    fun `visual transformation preserves decimal part with comma`() {
        val vt = ThousandsVisualTransformation()
        val result = vt.filter(androidx.compose.ui.text.AnnotatedString("12345.67"))
        assertEquals("12\u00A0345,67", result.text.text)
    }

    @Test
    fun `visual transformation handles empty string`() {
        val vt = ThousandsVisualTransformation()
        val result = vt.filter(androidx.compose.ui.text.AnnotatedString(""))
        assertEquals("", result.text.text)
    }

    @Test
    fun `visual transformation cursor offset mapping is consistent`() {
        val vt = ThousandsVisualTransformation()
        val result = vt.filter(androidx.compose.ui.text.AnnotatedString("1234567"))
        val mapping = result.offsetMapping

        // Original offset 0 ("1") → transformed offset 0 ("1")
        assertEquals(0, mapping.originalToTransformed(0))
        // Original offset 7 (end) → transformed offset 9 (end of "1 234 567")
        assertEquals(9, mapping.originalToTransformed(7))
        // Round-trip: transformed end → original end
        assertEquals(7, mapping.transformedToOriginal(9))
    }
}
