package com.finora.presentation.util

import com.finora.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private val RU = Locale("ru", "RU")

/** "12 500 ₽" — grouped with non-breaking spaces, no decimals when whole. */
fun formatMoney(amount: Double, withSymbol: Boolean = true): String {
    val rounded = amount.roundToLong()
    val grouped = groupThousands(abs(rounded))
    val sign = if (rounded < 0) "-" else ""
    return if (withSymbol) "$sign$grouped\u00A0₽" else "$sign$grouped"
}

/** "+12 500 ₽" / "−3 200 ₽" depending on transaction type. */
fun formatSigned(amount: Double, type: TransactionType): String {
    val sign = if (type == TransactionType.INCOME) "+" else "−"
    return "$sign${groupThousands(abs(amount.roundToLong()))}\u00A0₽"
}

private fun groupThousands(value: Long): String {
    val s = value.toString()
    val sb = StringBuilder()
    var count = 0
    for (i in s.length - 1 downTo 0) {
        sb.append(s[i])
        count++
        if (count % 3 == 0 && i != 0) sb.append('\u00A0')
    }
    return sb.reverse().toString()
}

private fun cal(millis: Long): Calendar = Calendar.getInstance(RU).apply { timeInMillis = millis }

fun startOfDay(millis: Long): Long = cal(millis).apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun endOfDay(millis: Long): Long = cal(millis).apply {
    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
}.timeInMillis

fun startOfMonth(millis: Long): Long = cal(millis).apply {
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun endOfMonth(millis: Long): Long = cal(millis).apply {
    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
}.timeInMillis

fun addMonths(millis: Long, months: Int): Long = cal(millis).apply {
    add(Calendar.MONTH, months)
}.timeInMillis

private val dayMonth = SimpleDateFormat("d MMM", RU)
private val fullDate = SimpleDateFormat("d MMMM yyyy", RU)
private val monthYear = SimpleDateFormat("LLLL yyyy", RU)
private val shortMonth = SimpleDateFormat("LLL", RU)

fun formatDayMonth(millis: Long): String = dayMonth.format(Date(millis))
fun formatFullDate(millis: Long): String = fullDate.format(Date(millis)).replaceFirstChar { it.titlecase(RU) }
fun formatMonthYear(millis: Long): String = monthYear.format(Date(millis)).replaceFirstChar { it.titlecase(RU) }
fun formatShortMonth(millis: Long): String = shortMonth.format(Date(millis)).replaceFirstChar { it.titlecase(RU) }

/** "Сегодня" / "Вчера" / "5 июня". */
fun relativeDayLabel(millis: Long): String {
    val today = startOfDay(System.currentTimeMillis())
    val day = startOfDay(millis)
    val oneDay = 24L * 60 * 60 * 1000
    return when (day) {
        today -> "Сегодня"
        today - oneDay -> "Вчера"
        else -> formatFullDate(millis).removeSuffix(" ${cal(millis).get(Calendar.YEAR)}")
    }
}

fun isSameDay(a: Long, b: Long): Boolean = startOfDay(a) == startOfDay(b)
