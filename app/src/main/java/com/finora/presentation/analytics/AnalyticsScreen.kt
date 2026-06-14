package com.finora.presentation.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finora.presentation.AppViewModelProvider
import com.finora.presentation.components.EmptyState
import com.finora.presentation.components.FinoraCard
import com.finora.presentation.components.IconChip
import com.finora.presentation.components.SectionHeader
import com.finora.presentation.theme.IncomeGreen
import com.finora.presentation.theme.LocalFinoraColors
import com.finora.presentation.util.formatMoney
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Аналитика",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        item { MonthSummaryCard(state) }
        item {
            SectionHeader(title = "Расходы по категориям")
        }
        item {
            if (state.topExpenseCategories.isEmpty()) {
                FinoraCard {
                    EmptyState(
                        icon = Icons.Rounded.PieChart,
                        title = "Нет расходов за месяц",
                        subtitle = "Когда появятся операции, здесь будет видно, куда уходят деньги."
                    )
                }
            } else {
                CategoryBreakdownCard(state.topExpenseCategories)
            }
        }
        item {
            SectionHeader(title = "Накопительные счета")
        }
        item {
            SavingsYieldCard(
                yields = state.savingsYields,
                totalMonthlyYield = state.totalSavingsMonthlyYield
            )
        }
        item {
            SectionHeader(title = "Цели")
        }
        item {
            GoalsAnalyticsCard(
                activeGoals = state.activeGoals,
                goalsRemaining = state.goalsRemaining
            )
        }
    }
}

@Composable
private fun MonthSummaryCard(state: AnalyticsUiState) {
    val finora = LocalFinoraColors.current
    FinoraCard(padding = PaddingValues(0.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(finora.brandStart, finora.brandEnd)))
                .padding(18.dp)
        ) {
            Column {
                Text(
                    "Текущий месяц",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.78f)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    formatMoney(state.netFlow),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Чистый поток",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f)
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryPill(
                        label = "Доход",
                        amount = state.monthIncome,
                        positive = true,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryPill(
                        label = "Расход",
                        amount = state.monthExpense,
                        positive = false,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                SummaryLine(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    label = "Прогноз баланса к концу месяца",
                    value = formatMoney(state.projectedEndBalance)
                )
            }
        }
    }
}

@Composable
private fun SummaryPill(
    label: String,
    amount: Double,
    positive: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(Color.White.copy(alpha = 0.14f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (positive) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.78f))
            Text(formatMoney(amount), style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
    }
}

@Composable
private fun SummaryLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Color.White.copy(alpha = 0.12f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(10.dp))
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.78f)
        )
        Text(value, style = MaterialTheme.typography.titleSmall, color = Color.White)
    }
}

@Composable
private fun CategoryBreakdownCard(items: List<CategoryAnalytics>) {
    FinoraCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items.forEach { item ->
                val color = Color(item.category.color)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconChip(item.category.iconKey, color, size = 36.dp)
                        Spacer(Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.category.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            val deltaText = when {
                                item.delta > 0.0 -> "на ${formatMoney(item.delta)} больше прошлого месяца"
                                item.delta < 0.0 -> "на ${formatMoney(abs(item.delta))} меньше прошлого месяца"
                                else -> "как в прошлом месяце"
                            }
                            Text(
                                deltaText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            formatMoney(item.amount),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(item.share.coerceIn(0f, 1f))
                                .height(7.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavingsYieldCard(
    yields: List<SavingsYield>,
    totalMonthlyYield: Double
) {
    if (yields.isEmpty()) {
        FinoraCard {
            EmptyState(
                icon = Icons.Rounded.Savings,
                title = "Нет накопительных счетов",
                subtitle = "Добавьте ставку у накопительного счёта, чтобы видеть ориентировочную доходность."
            )
        }
        return
    }

    FinoraCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(IncomeGreen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Savings, contentDescription = null, tint = IncomeGreen)
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    formatMoney(totalMonthlyYield),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Ориентировочно в месяц",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            yields.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconChip(
                        iconKey = item.account.account.iconKey,
                        color = Color(item.account.account.color),
                        size = 34.dp
                    )
                    Spacer(Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.account.account.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "${item.account.account.interestRate}% годовых от ${formatMoney(item.account.balance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        formatMoney(item.monthlyIncome),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = IncomeGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalsAnalyticsCard(
    activeGoals: List<com.finora.domain.model.Goal>,
    goalsRemaining: Double
) {
    if (activeGoals.isEmpty()) {
        FinoraCard {
            EmptyState(
                icon = Icons.Rounded.TrackChanges,
                title = "Активных целей нет",
                subtitle = "Создайте цель, и здесь появится остаток и прогресс."
            )
        }
        return
    }
    FinoraCard {
        SummaryMetric(
            icon = Icons.Rounded.TrackChanges,
            label = "Осталось до всех целей",
            value = formatMoney(goalsRemaining)
        )
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            activeGoals.forEach { goal ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconChip(goal.iconKey, Color(goal.color), size = 34.dp)
                    Spacer(Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            goal.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "${(goal.progress * 100).roundToInt()}% выполнено",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        formatMoney((goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.size(10.dp))
        Column {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
