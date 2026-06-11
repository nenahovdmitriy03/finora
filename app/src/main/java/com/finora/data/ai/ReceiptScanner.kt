package com.finora.data.ai

import android.graphics.Bitmap
import com.finora.domain.model.TransactionType
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One transaction the model read from a photo (before the user confirms it). */
data class ScannedTransaction(
    val type: TransactionType,
    val amount: Double,
    val date: Long,
    val categoryName: String?,
    val note: String
)

/**
 * Reads receipts / bank screenshots with a vision model (Gemini) and returns a
 * list of draft transactions. Bank-agnostic: the model just reads the picture.
 * The caller is responsible for letting the user confirm before saving.
 */
class ReceiptScanner(private val gemini: GeminiClient) {

    /** True when a vision-capable provider is configured. */
    val isAvailable: Boolean get() = gemini.isConfigured

    /**
     * @param categories names of the user's categories so the model can pick the closest one.
     */
    suspend fun scan(bitmap: Bitmap, categories: List<String>): List<ScannedTransaction> {
        val today = dateFormat().format(Date())
        val raw = gemini.describeImage(bitmap, buildPrompt(today, categories))
        return parse(raw)
    }

    private fun buildPrompt(today: String, categories: List<String>): String {
        val catLine = if (categories.isEmpty()) "(нет — придумай короткое название)"
        else categories.joinToString(", ")
        return """
            Ты — парсер чеков и банковских скриншотов. На изображении может быть чек,
            скриншот операции или выписка. Извлеки ВСЕ финансовые операции. Сегодня $today.

            Верни СТРОГО валидный JSON-массив без markdown и без текста вокруг.
            Каждый элемент:
            {"type":"expense|income","amount":число,"date":"YYYY-MM-DD","category":"строка","note":"описание"}

            Правила:
            - amount — положительное число в рублях, точка как десятичный разделитель, без символа валюты.
            - type: "expense" для трат/покупок, "income" для поступлений/зачислений.
            - date: если дата не видна — используй $today.
            - category: выбери ближайшую по смыслу из списка: $catLine.
            - note: магазин или назначение платежа, кратко.
            Если операций нет — верни [].
        """.trimIndent()
    }

    private fun parse(raw: String): List<ScannedTransaction> {
        val text = raw.trim()
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        if (start < 0 || end <= start) return emptyList()
        val arr = runCatching { JSONArray(text.substring(start, end + 1)) }.getOrNull() ?: return emptyList()
        val out = mutableListOf<ScannedTransaction>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val amount = o.optDouble("amount", 0.0)
            if (amount <= 0.0) continue
            val type = if (o.optString("type").equals("income", ignoreCase = true))
                TransactionType.INCOME else TransactionType.EXPENSE
            out += ScannedTransaction(
                type = type,
                amount = amount,
                date = parseDate(o.optString("date")),
                categoryName = o.optString("category").ifBlank { null },
                note = o.optString("note")
            )
        }
        return out
    }

    private fun parseDate(s: String): Long = runCatching {
        dateFormat().parse(s)?.time
    }.getOrNull() ?: System.currentTimeMillis()

    private fun dateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US)
}
