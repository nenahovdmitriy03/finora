package com.finora.data.ai

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.finora.FinoraApp
import com.finora.MainActivity
import com.finora.R
import com.finora.domain.model.TransactionType
import com.finora.presentation.util.formatMoney
import com.finora.presentation.util.startOfMonth
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Background worker that runs once a day, asks the AI to analyse
 * the user's finances and caches the result in DataStore.
 * A notification is shown so the user can open the full chat.
 */
class DailyInsightWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val WORK_NAME = "daily_insight"
        const val CHANNEL_ID = "daily_insight_channel"
        private const val NOTIFICATION_ID = 7001

        /** Call once from Application.onCreate(). */
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Ежедневный анализ от Алии",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Алия анализирует ваши финансы каждый день"
                }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(channel)
            }
        }
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as? FinoraApp ?: return Result.failure()
        val container = app.container
        val engines = AiProviders.configured()
        if (engines.isEmpty()) return Result.success() // no API key

        val dataContext = buildDataContext(container)
        val prompt = buildPrompt(dataContext)

        // Try each engine
        var insight: String? = null
        for (engine in engines) {
            try {
                insight = engine.generate(prompt)
                if (!insight.isNullOrBlank()) break
            } catch (_: Exception) { /* try next */ }
        }

        if (insight.isNullOrBlank()) return Result.retry()

        // Persist
        val dateStr = SimpleDateFormat("d MMMM", Locale("ru")).format(Date())
        container.settings.setDailyInsight(insight, dateStr)

        // Show notification
        showNotification(insight)

        return Result.success()
    }

    private suspend fun buildDataContext(
        container: com.finora.di.AppContainer
    ): String {
        val repo = container.repository
        val accounts = repo.observeAccountBalances().first()
        val transactions = repo.observeTransactionDetails().first()
        val goals = repo.observeGoals().first()

        val now = System.currentTimeMillis()
        val monthStart = startOfMonth(now)

        // Current month
        val monthTx = transactions.filter { it.transaction.date >= monthStart }
        val income = monthTx.filter { it.transaction.type == TransactionType.INCOME }
            .sumOf { it.transaction.amount }
        val expense = monthTx.filter { it.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.transaction.amount }

        // Previous month for comparison
        val prevMonthEnd = monthStart - 1
        val prevMonthStart = startOfMonth(prevMonthEnd)
        val prevTx = transactions.filter {
            it.transaction.date in prevMonthStart until monthStart
        }
        val prevIncome = prevTx.filter { it.transaction.type == TransactionType.INCOME }
            .sumOf { it.transaction.amount }
        val prevExpense = prevTx.filter { it.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.transaction.amount }

        val topExpenseCats = monthTx
            .filter { it.transaction.type == TransactionType.EXPENSE }
            .groupBy { it.category?.name ?: "Без категории" }
            .mapValues { (_, list) -> list.sumOf { it.transaction.amount } }
            .entries.sortedByDescending { it.value }
            .take(8)

        val topIncomeCats = monthTx
            .filter { it.transaction.type == TransactionType.INCOME }
            .groupBy { it.category?.name ?: "Без категории" }
            .mapValues { (_, list) -> list.sumOf { it.transaction.amount } }
            .entries.sortedByDescending { it.value }
            .take(5)

        val sb = StringBuilder()
        sb.appendLine("Общий баланс: ${formatMoney(accounts.sumOf { it.balance })}")
        sb.appendLine()
        sb.appendLine("Текущий месяц:")
        sb.appendLine("  Доходы: ${formatMoney(income)}")
        sb.appendLine("  Расходы: ${formatMoney(expense)}")
        sb.appendLine("  Сальдо: ${formatMoney(income - expense)}")
        sb.appendLine()
        sb.appendLine("Прошлый месяц (для сравнения):")
        sb.appendLine("  Доходы: ${formatMoney(prevIncome)}")
        sb.appendLine("  Расходы: ${formatMoney(prevExpense)}")
        sb.appendLine("  Сальдо: ${formatMoney(prevIncome - prevExpense)}")
        sb.appendLine()
        sb.appendLine("Счета:")
        if (accounts.isEmpty()) sb.appendLine("  - нет")
        else accounts.forEach { ab ->
            val acc = ab.account
            val extra = if (acc.hasInterest) " (накопительный, ${acc.interestRate}% годовых)" else ""
            sb.appendLine("  - ${acc.name}: ${formatMoney(ab.balance)}$extra")
        }
        sb.appendLine()
        sb.appendLine("Топ категорий расходов:")
        if (topExpenseCats.isEmpty()) sb.appendLine("  - нет")
        else topExpenseCats.forEach { (n, t) ->
            val pct = if (expense > 0) ((t / expense) * 100).toInt() else 0
            sb.appendLine("  - $n: ${formatMoney(t)} ($pct%)")
        }
        sb.appendLine()
        sb.appendLine("Топ категорий доходов:")
        if (topIncomeCats.isEmpty()) sb.appendLine("  - нет")
        else topIncomeCats.forEach { (n, t) -> sb.appendLine("  - $n: ${formatMoney(t)}") }
        sb.appendLine()
        sb.appendLine("Цели:")
        if (goals.isEmpty()) sb.appendLine("  - нет")
        else goals.forEach { g ->
            val pct = (g.progress * 100).toInt()
            val deadline = g.deadline?.let {
                val df = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                " (дедлайн ${df.format(Date(it))})"
            } ?: ""
            sb.appendLine("  - ${g.name}: ${formatMoney(g.savedAmount)} / ${formatMoney(g.targetAmount)} ($pct%)$deadline")
        }
        sb.appendLine()
        sb.appendLine("Количество операций за месяц: ${monthTx.size}")
        return sb.toString()
    }

    private fun buildPrompt(dataContext: String): String = """
Ты — Алия, дружелюбный AI-финансовый ассистент приложения Finora.
Сделай краткий ежедневный обзор финансов пользователя на русском языке.

Правила:
- Опирайся ТОЛЬКО на данные ниже, не выдумывай
- Будь краткой: 3–5 пунктов с эмодзи
- Сравни с прошлым месяцем если есть данные
- Дай 1 конкретный совет
- Никаких вступлений — сразу суть

=== Данные (рубли) ===
$dataContext
""".trimIndent()

    private fun showNotification(insight: String) {
        // Check permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("start_route", "ai_chat")
        }
        val pending = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Take first 2 lines for the collapsed notification
        val shortText = insight.lines()
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("\n")

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📊 Алия — ежедневный анализ")
            .setContentText(shortText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(insight))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }
}
