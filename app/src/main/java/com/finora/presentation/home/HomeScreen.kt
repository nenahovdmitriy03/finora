package com.finora.presentation.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
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
import com.finora.presentation.theme.LocalFinoraColors
import com.finora.presentation.guide.GuideStep
import com.finora.presentation.guide.LocalGuideController
import com.finora.presentation.guide.guideTarget
import com.finora.presentation.util.formatMoney

@Composable
fun HomeScreen(
    onAddTransaction: () -> Unit,
    onSeeAllTransactions: () -> Unit,
    onSeeAccounts: () -> Unit,
    onSeeGoals: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val guideController = LocalGuideController.current

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Header() }
        item {
            Box(modifier = Modifier.guideTarget(guideController, GuideStep.BALANCE_HERO)) {
                BalanceHero(state)
            }
        }
        item { AiInsightCard(onOpenAi = onOpenAi) }
        item {
            SectionHeader(
                title = "Счета",
                action = {
                    TextButton(onClick = onSeeAccounts) { Text("Все") }
                }
            )
        }
        item { AccountsStrip(state.accounts, onOpen = onSeeAccounts) }

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
    val finora = LocalFinoraColors.current
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(finora.brandStart, finora.brandEnd)))
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
                // Show goals breakdown when there are earmarked funds
                if (state.inGoals > 0.0) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "В целях",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                formatMoney(state.inGoals),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Свободно",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                formatMoney(state.freeBalance),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
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
private fun AiInsightCard(
    onOpenAi: () -> Unit,
    viewModel: AiInsightViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val ai by viewModel.state.collectAsStateWithLifecycle()
    FinoraCard(modifier = Modifier.clickable(enabled = ai.configured) { onOpenAi() }) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI-аналитика",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    ai.provider?.let { provider ->
                        Text(
                            text = "через $provider",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (ai.configured) {
                    TextButton(onClick = onOpenAi) { Text("Анализировать") }
                }
            }
            Spacer(Modifier.height(6.dp))
            if (ai.configured) {
                Text(
                    text = "Открой умного помощника — он разберёт твои доходы, расходы, счета и цели, " +
                        "подскажет, что улучшить, и ответит на твои вопросы.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Чтобы включить ИИ, добавь в local.properties ключ одного из провайдеров и пересобери:\n" +
                        "• OPENROUTER_API_KEY (openrouter.ai — много бесплатных моделей)\n" +
                        "• GROQ_API_KEY (console.groq.com — быстро и бесплатно)\n" +
                        "• GEMINI_API_KEY (Google AI Studio)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AccountsStrip(accounts: List<AccountBalance>, onOpen: () -> Unit) {
    if (accounts.isEmpty()) {
        FinoraCard(modifier = Modifier.clickable { onOpen() }) {
            Text(
                text = "Добавьте счёт по кнопке «Все», чтобы видеть баланс по каждому банку.",
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
                modifier = Modifier
                    .width(170.dp)
                    .clickable { onOpen() }
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
