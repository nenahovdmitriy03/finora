package com.finora.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finora.domain.model.CategoryStat
import com.finora.domain.model.TransactionType
import com.finora.presentation.AppViewModelProvider
import com.finora.presentation.components.DonutChart
import com.finora.presentation.components.DonutSlice
import com.finora.presentation.components.EmptyState
import com.finora.presentation.components.FinoraCard
import com.finora.presentation.components.NetBar
import com.finora.presentation.components.NetTrendChart
import com.finora.presentation.util.formatMoney
import kotlin.math.roundToInt

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showExpense by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Статистика",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = viewModel::previousMonth) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Назад")
                }
                Text(
                    state.monthLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(
                    onClick = viewModel::nextMonth,
                    enabled = state.monthOffset < 0
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Вперёд")
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryTile(
                    modifier = Modifier.weight(1f),
                    label = "Доходы",
                    amount = state.income,
                    color = MaterialTheme.colorScheme.primary
                )
                SummaryTile(
                    modifier = Modifier.weight(1f),
                    label = "Расходы",
                    amount = state.expense,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        item {
            FinoraCard {
                Text(
                    "Чистый поток за 6 месяцев",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Доходы минус расходы по месяцам",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                NetTrendChart(
                    bars = state.trend.map { NetBar(it.label, (it.income - it.expense).toFloat()) },
                    positiveColor = MaterialTheme.colorScheme.primary,
                    negativeColor = MaterialTheme.colorScheme.outline,
                    baselineColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    state.trend.forEach { point ->
                        Text(
                            point.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendDot(MaterialTheme.colorScheme.primary, "Плюс")
                    LegendDot(MaterialTheme.colorScheme.outline, "Минус")
                }
            }
        }

        val stats = if (showExpense) state.expenseStats else state.incomeStats
        val total = if (showExpense) state.expense else state.income

        item {
            FinoraCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SegmentButton("Расходы", showExpense, Modifier.weight(1f)) { showExpense = true }
                    SegmentButton("Доходы", !showExpense, Modifier.weight(1f)) { showExpense = false }
                }
                Spacer(Modifier.height(20.dp))
                if (stats.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.PieChart,
                        title = "Нет данных",
                        subtitle = "За этот месяц операций нет"
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        DonutChart(
                            slices = stats.map { DonutSlice(it.total.toFloat(), Color(it.category.color)) },
                            center = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        if (showExpense) "Расходы" else "Доходы",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        formatMoney(total),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    stats.forEach { stat ->
                        CategoryStatRow(stat)
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryTile(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    FinoraCard(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(
            formatMoney(amount),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun CategoryStatRow(stat: CategoryStat) {
    val color = Color(stat.category.color)
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(10.dp).clip(CircleShape).background(color)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stat.category.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "${(stat.share * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(10.dp))
            Text(
                formatMoney(stat.total),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(stat.share.coerceIn(0.02f, 1f))
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun SegmentButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
