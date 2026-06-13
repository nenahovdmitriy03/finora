package com.finora.presentation.transactions

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

// Semantic text-only colours — used for labels/numbers, never large backgrounds
private val IncomeGreen = Color(0xFF10B981)
private val ExpenseRose = Color(0xFFF43F5E)

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
        // ─── Title ──────────────────────────────────────────────────────
        item {
            Text(
                text = "Статистика",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // ─── Month picker ───────────────────────────────────────────────
        item {
            MonthSelector(
                label = state.monthLabel,
                canGoNext = state.monthOffset < 0,
                onPrev = viewModel::previousMonth,
                onNext = viewModel::nextMonth
            )
        }

        // ─── Summary cards: Income / Expense / Net ──────────────────────
        item {
            val net = state.monthIncome - state.monthExpense
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryChip(
                    label = "Доход",
                    value = formatMoney(state.monthIncome),
                    icon = Icons.Rounded.ArrowDownward,
                    iconTint = IncomeGreen,
                    valueTint = IncomeGreen,
                    modifier = Modifier.weight(1f)
                )
                SummaryChip(
                    label = "Расход",
                    value = formatMoney(state.monthExpense),
                    icon = Icons.Rounded.ArrowUpward,
                    iconTint = ExpenseRose,
                    valueTint = ExpenseRose,
                    modifier = Modifier.weight(1f)
                )
                SummaryChip(
                    label = "Итого",
                    value = formatMoney(net),
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    iconTint = MaterialTheme.colorScheme.primary,
                    valueTint = if (net >= 0) IncomeGreen else ExpenseRose,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ─── Chart + breakdown card ─────────────────────────────────────
        item {
            Box(modifier = Modifier.guideTarget(guideController, GuideStep.TX_BREAKDOWN)) {
                CategoryBreakdownCard(state = state)
            }
        }

        // ─── Filters ────────────────────────────────────────────────────
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

        // ─── Transactions list ──────────────────────────────────────────
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
                            .padding(top = 12.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = relativeDayLabel(group.dayStart),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val net = group.income - group.expense
                        Text(
                            text = formatMoney(net),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (net >= 0) IncomeGreen else ExpenseRose
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

// ─── Month Selector ─────────────────────────────────────────────────────────

@Composable
private fun MonthSelector(
    label: String,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrev) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = onNext, enabled = canGoNext) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Вперёд",
                    tint = if (canGoNext) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

// ─── Summary chip ───────────────────────────────────────────────────────────

@Composable
private fun SummaryChip(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    valueTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = valueTint,
                maxLines = 1
            )
        }
    }
}

// ─── Category Breakdown (chart + legend) ────────────────────────────────────

@Composable
private fun CategoryBreakdownCard(state: TransactionsUiState) {
    var showExpense by remember { mutableStateOf(true) }
    val stats = if (showExpense) state.expenseStats else state.incomeStats
    val total = if (showExpense) state.monthExpense else state.monthIncome

    FinoraCard {
        // Toggle — neutral theme-coloured segment
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SegmentButton("Расходы", showExpense, Modifier.weight(1f)) { showExpense = true }
            SegmentButton("Доходы", !showExpense, Modifier.weight(1f)) { showExpense = false }
        }
        Spacer(Modifier.height(24.dp))

        if (stats.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.ReceiptLong,
                title = "Нет данных",
                subtitle = "За этот месяц операций нет"
            )
        } else {
            var selectedSlice by remember { mutableStateOf(-1) }
            val selectedStat = if (selectedSlice in stats.indices) stats[selectedSlice] else null

            // Chart
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                DonutChart(
                    slices = stats.mapIndexed { i, s ->
                        DonutSlice(s.total.toFloat(), Color(chartColorAt(i)))
                    },
                    size = 200.dp,
                    strokeWidth = 28.dp,
                    onSliceSelected = { selectedSlice = it },
                    center = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                selectedStat?.category?.name ?: "Всего",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                formatMoney(selectedStat?.total ?: total),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (selectedStat != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        "${(selectedStat.share * 100).roundToInt()}%",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                )
            }
            Spacer(Modifier.height(24.dp))

            // Category legend rows
            stats.forEachIndexed { i, stat ->
                CategoryStatRow(
                    stat = stat,
                    color = Color(chartColorAt(i)),
                    isSelected = selectedSlice == i
                )
                if (i < stats.lastIndex) Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun CategoryStatRow(stat: CategoryStat, color: Color, isSelected: Boolean) {
    val animatedShare by animateFloatAsState(
        targetValue = stat.share.coerceIn(0.02f, 1f),
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "barAnim"
    )

    Column(
        modifier = Modifier.animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconChip(iconKey = stat.category.iconKey, color = color, size = 38.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stat.category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "${stat.count} операц.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatMoney(stat.total),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = color.copy(alpha = 0.12f)
                ) {
                    Text(
                        "${(stat.share * 100).roundToInt()}%",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // Gradient progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedShare)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.5f),
                                color
                            )
                        )
                    )
            )
        }
    }
}

// ─── Toggle button — uses theme primary, no red/green ───────────────────────

@Composable
private fun SegmentButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Filter chip ────────────────────────────────────────────────────────────

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = Color.White
        )
    )
}
