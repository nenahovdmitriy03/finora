package com.finora.presentation.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import com.finora.presentation.guide.GuideStep
import com.finora.presentation.guide.LocalGuideController
import com.finora.presentation.guide.guideTarget
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.finora.presentation.AppViewModelProvider
import com.finora.presentation.components.DonutChart
import com.finora.presentation.components.DonutSlice
import com.finora.presentation.components.EmptyState
import com.finora.presentation.components.FinoraCard
import com.finora.presentation.components.IconChip
import com.finora.presentation.components.TransactionRow
import com.finora.presentation.util.chartColorAt
import com.finora.presentation.util.formatMoney
import com.finora.presentation.util.relativeDayLabel
import kotlin.math.roundToInt

@Composable
fun TransactionsScreen(
    onOpenTransaction: (Long) -> Unit,
    viewModel: TransactionsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val guideController = LocalGuideController.current

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Операции",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Box(modifier = Modifier.guideTarget(guideController, GuideStep.TX_BREAKDOWN)) {
                CategoryBreakdownCard(
                    state = state,
                    onPrev = viewModel::previousMonth,
                    onNext = viewModel::nextMonth
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.guideTarget(guideController, GuideStep.TX_FILTERS)
            ) {
                FilterPill("Все", state.filter == TxFilter.ALL) { viewModel.setFilter(TxFilter.ALL) }
                FilterPill("Доходы", state.filter == TxFilter.INCOME) { viewModel.setFilter(TxFilter.INCOME) }
                FilterPill("Расходы", state.filter == TxFilter.EXPENSE) { viewModel.setFilter(TxFilter.EXPENSE) }
            }
        }

        if (state.isEmpty) {
            item {
                FinoraCard {
                    EmptyState(
                        icon = Icons.Rounded.ReceiptLong,
                        title = "Нет операций",
                        subtitle = "Добавьте доход или расход кнопкой +"
                    )
                }
            }
        } else {
            state.groups.forEach { group ->
                item(key = group.dayStart) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = relativeDayLabel(group.dayStart),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val net = group.income - group.expense
                        Text(
                            text = formatMoney(net),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (net >= 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item(key = "card_${group.dayStart}") {
                    FinoraCard(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                        group.items.forEach { details ->
                            TransactionRow(
                                details = details,
                                onClick = { onOpenTransaction(details.transaction.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    state: TransactionsUiState,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    var showExpense by remember { mutableStateOf(true) }
    val stats = if (showExpense) state.expenseStats else state.incomeStats
    val total = if (showExpense) state.monthExpense else state.monthIncome

    FinoraCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Назад")
            }
            Text(
                state.monthLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = onNext, enabled = state.monthOffset < 0) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Вперёд")
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SegmentButton("Расходы", showExpense, Modifier.weight(1f)) { showExpense = true }
            SegmentButton("Доходы", !showExpense, Modifier.weight(1f)) { showExpense = false }
        }
        Spacer(Modifier.height(20.dp))

        if (stats.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.ReceiptLong,
                title = "Нет данных",
                subtitle = "За этот месяц операций нет"
            )
        } else {
            var selectedSlice by remember { mutableStateOf(-1) }
            val selectedStat = if (selectedSlice in stats.indices) stats[selectedSlice] else null

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                DonutChart(
                    slices = stats.mapIndexed { i, s -> DonutSlice(s.total.toFloat(), Color(chartColorAt(i))) },
                    onSliceSelected = { selectedSlice = it },
                    center = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                selectedStat?.category?.name
                                    ?: if (showExpense) "Расходы" else "Доходы",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                formatMoney(selectedStat?.total ?: total),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (selectedStat != null) {
                                Text(
                                    "${(selectedStat.share * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
            }
            Spacer(Modifier.height(20.dp))
            stats.forEachIndexed { i, stat ->
                CategoryStatRow(stat, Color(chartColorAt(i)))
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CategoryStatRow(stat: CategoryStat, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconChip(iconKey = stat.category.iconKey, color = color, size = 34.dp)
            Spacer(Modifier.width(10.dp))
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
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = MaterialTheme.shapes.large,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = Color.White
        )
    )
}
