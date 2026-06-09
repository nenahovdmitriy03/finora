package com.finora.presentation.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.finora.domain.model.AccountBalance
import com.finora.domain.model.Goal
import com.finora.presentation.AppViewModelProvider
import com.finora.presentation.components.EmptyState
import com.finora.presentation.components.FinoraCard
import com.finora.presentation.components.IconChip
import com.finora.presentation.components.SectionHeader
import com.finora.presentation.components.TransactionRow
import com.finora.presentation.theme.Violet
import com.finora.presentation.theme.VioletDark
import com.finora.presentation.util.formatMoney

@Composable
fun HomeScreen(
    onAddTransaction: () -> Unit,
    onSeeAllTransactions: () -> Unit,
    onSeeAccounts: () -> Unit,
    onSeeGoals: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Header() }
        item { BalanceHero(state) }
        item {
            SectionHeader(
                title = "Счета",
                action = {
                    TextButton(onClick = onSeeAccounts) { Text("Все") }
                }
            )
        }
        item { AccountsStrip(state.accounts) }

        item {
            SectionHeader(
                title = "Последние операции",
                action = { TextButton(onClick = onSeeAllTransactions) { Text("Все") } }
            )
        }
        if (state.recentTransactions.isEmpty()) {
            item {
                FinoraCard {
                    EmptyState(
                        icon = Icons.Rounded.ReceiptLong,
                        title = "Пока пусто",
                        subtitle = "Добавьте первую операцию по кнопке +"
                    )
                }
            }
        } else {
            item {
                FinoraCard(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    state.recentTransactions.forEach { details ->
                        TransactionRow(
                            details = details,
                            onClick = { onOpenTransaction(details.transaction.id) }
                        )
                    }
                }
            }
        }

        if (state.goals.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Цели",
                    action = { TextButton(onClick = onSeeGoals) { Text("Все") } }
                )
            }
            item { GoalsPreview(state.goals) }
        }
    }
}

@Composable
private fun Header() {
    Column {
        Text(
            text = "Привет 👋",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Ваши финансы",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun BalanceHero(state: HomeUiState) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Violet, VioletDark)))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(22.dp)) {
                Text(
                    text = "Общий баланс",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = formatMoney(state.totalBalance),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White
                )
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    FlowStat(
                        modifier = Modifier.weight(1f),
                        label = "Доход за месяц",
                        amount = state.monthIncome,
                        positive = true
                    )
                    Spacer(Modifier.width(12.dp))
                    FlowStat(
                        modifier = Modifier.weight(1f),
                        label = "Расход за месяц",
                        amount = state.monthExpense,
                        positive = false
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowStat(
    label: String,
    amount: Double,
    positive: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (positive) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = formatMoney(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun AccountsStrip(accounts: List<AccountBalance>) {
    if (accounts.isEmpty()) {
        FinoraCard {
            Text(
                text = "Добавьте счёт во вкладке «Счета», чтобы видеть баланс по каждому банку.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(accounts.size) { index ->
            val item = accounts[index]
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(170.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    IconChip(iconKey = item.account.iconKey, color = Color(item.account.color))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = item.account.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = formatMoney(item.balance),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalsPreview(goals: List<Goal>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        goals.forEach { goal ->
            FinoraCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconChip(iconKey = goal.iconKey, color = Color(goal.color))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(8.dp))
                        com.finora.presentation.goals.GoalProgressBar(
                            progress = goal.progress,
                            color = Color(goal.color)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${formatMoney(goal.savedAmount)} из ${formatMoney(goal.targetAmount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
