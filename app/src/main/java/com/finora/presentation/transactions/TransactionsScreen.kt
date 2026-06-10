package com.finora.presentation.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finora.presentation.AppViewModelProvider
import com.finora.presentation.components.EmptyState
import com.finora.presentation.components.FinoraCard
import com.finora.presentation.components.TransactionRow
import com.finora.presentation.util.formatMoney
import com.finora.presentation.util.relativeDayLabel

@Composable
fun TransactionsScreen(
    onOpenTransaction: (Long) -> Unit,
    viewModel: TransactionsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = MaterialTheme.shapes.large,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = androidx.compose.ui.graphics.Color.White
        )
    )
}
