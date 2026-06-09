package com.finora.presentation.util

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

private const val GROUP_SEP = '\u00A0' // non-breaking space, RU thousands separator
private const val DECIMAL_SEP = ','

/**
 * Keeps only digits and a single decimal separator (max 2 decimals).
 * The stored value always uses '.' as the decimal separator.
 */
fun sanitizeMoneyInput(input: String): String {
    val sb = StringBuilder()
    var hasDot = false
    var decimals = 0
    for (ch in input) {
        when {
            ch.isDigit() -> {
                if (hasDot) {
                    if (decimals < 2) {
                        sb.append(ch)
                        decimals++
                    }
                } else {
                    sb.append(ch)
                }
            }
            (ch == '.' || ch == ',') && !hasDot && sb.isNotEmpty() -> {
                hasDot = true
                sb.append('.')
            }
        }
    }
    return sb.toString()
}

/** Parses a sanitized money string back into a Double. */
fun parseMoney(raw: String): Double = raw.replace(',', '.').toDoubleOrNull() ?: 0.0

/**
 * Visually groups the integer part of a numeric string with thousands separators
 * ("1234567" -> "1 234 567") while keeping the raw digits in state. Handles the
 * cursor offset mapping so editing stays natural.
 */
class ThousandsVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val dotIndex = raw.indexOf('.')
        val intPart = if (dotIndex >= 0) raw.substring(0, dotIndex) else raw
        val decPart = if (dotIndex >= 0) raw.substring(dotIndex + 1) else null
        val n = intPart.length

        // Number of group separators inserted before the given integer offset.
        fun sepsBefore(intOffset: Int): Int {
            var count = 0
            var b = 1
            while (b < n && b <= intOffset) {
                if ((n - b) % 3 == 0) count++
                b++
            }
            return count
        }
        val totalSeps = sepsBefore(n)

        val out = StringBuilder()
        for (i in 0 until n) {
            if (i > 0 && (n - i) % 3 == 0) out.append(GROUP_SEP)
            out.append(intPart[i])
        }
        if (decPart != null) {
            out.append(DECIMAL_SEP)
            out.append(decPart)
        }
        val outText = out.toString()

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val o = offset.coerceIn(0, raw.length)
                return if (o <= n) o + sepsBefore(o) else o + totalSeps
            }

            override fun transformedToOriginal(offset: Int): Int {
                val t = offset.coerceIn(0, outText.length)
                var o = 0
                while (o < raw.length && originalToTransformed(o + 1) <= t) o++
                return o
            }
        }
        return TransformedText(AnnotatedString(outText), mapping)
    }
}

/** OutlinedTextField preconfigured for money entry with live thousands grouping. */
@Composable
fun MoneyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val transformation = remember { ThousandsVisualTransformation() }
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(sanitizeMoneyInput(it)) },
        label = { Text(label) },
        suffix = { Text("₽") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = transformation,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    )
}
