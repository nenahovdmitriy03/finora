package com.finora.presentation.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finora.domain.model.AccountBalance
import com.finora.domain.model.Goal
import com.finora.domain.model.GoalAccountSummary
import com.finora.presentation.AppViewModelProvider
import com.finora.presentation.components.ColorPickerRow
import com.finora.presentation.guide.GuideStep
import com.finora.presentation.guide.LocalGuideController
import com.finora.presentation.guide.guideTarget
import com.finora.presentation.components.EmptyState
import com.finora.presentation.components.FinoraCard
import com.finora.presentation.components.IconChip
import com.finora.presentation.components.IconPickerRow
import com.finora.presentation.components.SectionHeader
import com.finora.presentation.util.MoneyTextField
import com.finora.presentation.util.finoraPalette
import com.finora.presentation.util.formatMoney
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val goalsWithSources by viewModel.goalsWithSources.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    var showNewGoalDialog by remember { mutableStateOf(false) }
    var detailGoalId by remember { mutableStateOf<Long?>(null) }
    var planMonths by remember { mutableStateOf(12) }
    val guideController = LocalGuideController.current

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Цели",
                modifier = Modifier.guideTarget(guideController, GuideStep.GOALS_CREATE),
                action = {
                    FilledTonalButton(onClick = { showNewGoalDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Новая")
                    }
                }
            )
        }

        if (goalsWithSources.isEmpty()) {
            item {
                FinoraCard {
                    EmptyState(
                        icon = Icons.Rounded.TrackChanges,
                        title = "Целей пока нет",
                        subtitle = "Создайте цель — например, на отпуск или новый телефон"
                    )
                }
            }
        } else {
            items(goalsWithSources.size) { index ->
                val gws = goalsWithSources[index]
                GoalCard(
                    goal = gws.goal,
                    sources = gws.sources,
                    monthlyPace = gws.monthlyPace,
                    planMonths = planMonths,
                    onPlanMonthsChange = { planMonths = it },
                    onClick = { detailGoalId = gws.goal.id }
                )
            }
        }
    }

    // ─── New goal creation dialog ────────────────────────────────────────
    if (showNewGoalDialog) {
        GoalEditorDialog(
            initial = null,
            onDismiss = { showNewGoalDialog = false },
            onConfirm = { name, target, icon, color ->
                viewModel.saveGoal(
                    id = 0L,
                    name = name,
                    target = target,
                    iconKey = icon,
                    color = color,
                    deadline = null,
                    saved = 0.0,
                    linkedAccountId = null
                )
                showNewGoalDialog = false
            },
            onDelete = null
        )
    }

    // ─── Goal detail/edit full-screen ────────────────────────────────────
    detailGoalId?.let { goalId ->
        val gws = goalsWithSources.find { it.goal.id == goalId }
        if (gws != null) {
            GoalDetailScreen(
                goal = gws.goal,
                sources = gws.sources,
                monthlyPace = gws.monthlyPace,
                planMonths = planMonths,
                onPlanMonthsChange = { planMonths = it },
                accounts = accounts,
                onDismiss = { detailGoalId = null },
                onContribute = { accountId, amount ->
                    viewModel.contribute(goalId, accountId, amount)
                },
                onSave = { name, target, icon, color ->
                    viewModel.saveGoal(
                        id = goalId,
                        name = name,
                        target = target,
                        iconKey = icon,
                        color = color,
                        deadline = gws.goal.deadline,
                        saved = gws.goal.savedAmount,
                        linkedAccountId = gws.goal.linkedAccountId
                    )
                },
                onDelete = {
                    viewModel.delete(gws.goal)
                    detailGoalId = null
                }
            )
        }
    }
}

// ─── Goal Card (list item) ───────────────────────────────────────────────────

@Composable
private fun GoalCard(
    goal: Goal,
    sources: List<GoalAccountSummary>,
    monthlyPace: Double,
    planMonths: Int,
    onPlanMonthsChange: (Int) -> Unit,
    onClick: () -> Unit
) {
    val color = Color(goal.color)
    FinoraCard(modifier = Modifier.clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip(iconKey = goal.iconKey, color = color, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    goal.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "${(goal.progress * 100).roundToInt()}% выполнено",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        GoalProgressBar(progress = goal.progress, color = color)
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                formatMoney(goal.savedAmount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Text(
                "из ${formatMoney(goal.targetAmount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        GoalForecastBlock(
            goal = goal,
            monthlyPace = monthlyPace,
            planMonths = planMonths,
            onPlanMonthsChange = onPlanMonthsChange,
            compact = true
        )

        if (sources.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                sources.forEach { summary ->
                    SourceAccountRow(
                        accountName = summary.account.name,
                        accountIconKey = summary.account.iconKey,
                        accountColor = Color(summary.account.color),
                        amount = summary.netAmount
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalForecastBlock(
    goal: Goal,
    monthlyPace: Double,
    planMonths: Int,
    onPlanMonthsChange: (Int) -> Unit,
    compact: Boolean
) {
    val remaining = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)
    if (remaining <= 0.0) {
        ForecastRow(
            title = "Цель закрыта",
            subtitle = "Можно перевести её в завершённые или поставить новую планку.",
            selectedMonths = planMonths,
            onPlanMonthsChange = onPlanMonthsChange
        )
        return
    }

    val selectedMonths = planMonths.coerceAtLeast(1)
    val plannedMonthly = remaining / selectedMonths.toDouble()
    val paceText = if (monthlyPace > 0.0) {
        val months = ceil(remaining / monthlyPace).toInt().coerceAtLeast(1)
        "При текущем темпе: примерно $months мес."
    } else {
        "Пополните цель, чтобы увидеть прогноз по темпу."
    }
    ForecastRow(
        title = "План накопления",
        subtitle = "За $selectedMonths мес.: ${formatMoney(plannedMonthly)}/мес.",
        extra = if (compact) null else paceText,
        selectedMonths = selectedMonths,
        onPlanMonthsChange = onPlanMonthsChange
    )
}

@Composable
private fun ForecastRow(
    title: String,
    subtitle: String,
    extra: String? = null,
    selectedMonths: Int,
    onPlanMonthsChange: (Int) -> Unit
) {
    Spacer(Modifier.height(12.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (extra != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                extra,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(6, 12, 24).forEach { months ->
                FilterChip(
                    selected = selectedMonths == months,
                    onClick = { onPlanMonthsChange(months) },
                    label = { Text("$months мес.") }
                )
            }
        }
    }
}

@Composable
private fun SourceAccountRow(
    accountName: String,
    accountIconKey: String,
    accountColor: Color,
    amount: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconChip(iconKey = accountIconKey, color = accountColor, size = 30.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                accountName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Вложено со счёта",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            formatMoney(amount),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// ─── Full-screen Goal Detail / Edit screen ───────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalDetailScreen(
    goal: Goal,
    sources: List<GoalAccountSummary>,
    monthlyPace: Double,
    planMonths: Int,
    onPlanMonthsChange: (Int) -> Unit,
    accounts: List<AccountBalance>,
    onDismiss: () -> Unit,
    onContribute: (accountId: Long, amount: Double) -> Unit,
    onSave: (name: String, target: Double, icon: String, color: Long) -> Unit,
    onDelete: () -> Unit
) {
    val goalColor = Color(goal.color)
    var showEditDialog by remember { mutableStateOf(false) }
    var showDepositSheet by remember { mutableStateOf(false) }
    var showWithdrawSheet by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        goal.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Редактировать")
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // ─── Goal header ─────────────────────────────────
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconChip(iconKey = goal.iconKey, color = goalColor, size = 64.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "${(goal.progress * 100).roundToInt()}%",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = goalColor
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "выполнено",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    GoalProgressBar(progress = goal.progress, color = goalColor)
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Накоплено",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                formatMoney(goal.savedAmount),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = goalColor
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Цель",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                formatMoney(goal.targetAmount),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    val remaining = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)
                    if (remaining > 0.0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Осталось: ${formatMoney(remaining)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    GoalForecastBlock(
                        goal = goal,
                        monthlyPace = monthlyPace,
                        planMonths = planMonths,
                        onPlanMonthsChange = onPlanMonthsChange,
                        compact = false
                    )

                    // ─── Action buttons: deposit / withdraw ──────────
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showDepositSheet = true },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = MaterialTheme.shapes.large,
                            colors = ButtonDefaults.buttonColors(containerColor = goalColor)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Пополнить", style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedButton(
                            onClick = { showWithdrawSheet = true },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = MaterialTheme.shapes.large,
                            enabled = goal.savedAmount > 0.0
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Снять", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    // ─── Contributing accounts ───────────────────────
                    if (sources.isNotEmpty()) {
                        Spacer(Modifier.height(28.dp))
                        Text(
                            "Откуда вложено",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            sources.forEach { summary ->
                                SourceAccountRow(
                                    accountName = summary.account.name,
                                    accountIconKey = summary.account.iconKey,
                                    accountColor = Color(summary.account.color),
                                    amount = summary.netAmount
                                )
                            }
                        }
                    }

                    // ─── Delete ──────────────────────────────────────
                    Spacer(Modifier.height(32.dp))
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Удалить цель", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    // ─── Edit dialog (name, target, icon, color) ─────────────────────────
    if (showEditDialog) {
        GoalEditorDialog(
            initial = goal,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, target, icon, color ->
                onSave(name, target, icon, color)
                showEditDialog = false
            },
            onDelete = null // delete is on the detail screen itself
        )
    }

    // ─── Deposit dialog ──────────────────────────────────────────────────
    if (showDepositSheet) {
        ContributeDialog(
            title = "Пополнить «${goal.name}»",
            goal = goal,
            accounts = accounts,
            confirmLabel = "Пополнить",
            isWithdraw = false,
            onDismiss = { showDepositSheet = false },
            onConfirm = { accountId, amount ->
                onContribute(accountId, amount)
                showDepositSheet = false
            }
        )
    }

    // ─── Withdraw dialog ─────────────────────────────────────────────────
    if (showWithdrawSheet) {
        ContributeDialog(
            title = "Снять из «${goal.name}»",
            goal = goal,
            accounts = accounts,
            confirmLabel = "Снять",
            isWithdraw = true,
            onDismiss = { showWithdrawSheet = false },
            onConfirm = { accountId, amount ->
                onContribute(accountId, -amount) // negative = withdraw
                showWithdrawSheet = false
            }
        )
    }
}

// ─── Goal Editor Dialog (name / target / icon / color) ───────────────────────

@Composable
private fun GoalEditorDialog(
    initial: Goal?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, target: Double, icon: String, color: Long) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var target by remember {
        mutableStateOf(
            initial?.targetAmount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var icon by remember { mutableStateOf(initial?.iconKey ?: "target") }
    var color by remember { mutableStateOf(initial?.color ?: finoraPalette[1]) }
    val iconKeys = listOf("target", "savings", "home", "car", "flight", "school", "phone", "gift", "games")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Новая цель" else "Редактировать цель") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                MoneyTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = "Сумма цели",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Text("Иконка", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                IconPickerRow(keys = iconKeys, selected = icon, color = Color(color), onSelect = { icon = it })
                Spacer(Modifier.height(14.dp))
                Text("Цвет", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                ColorPickerRow(colors = finoraPalette, selected = color, onSelect = { color = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = target.replace(',', '.').toDoubleOrNull() ?: 0.0
                    onConfirm(name, amount, icon, color)
                },
                enabled = name.isNotBlank() && (target.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0.0
            ) { Text("Сохранить") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Удалить", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        }
    )
}

// ─── Contribute / Withdraw Dialog ────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContributeDialog(
    title: String,
    goal: Goal,
    accounts: List<AccountBalance>,
    confirmLabel: String,
    isWithdraw: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (accountId: Long, amount: Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var selectedAccountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.account?.id) }
    val parsed = amount.replace(',', '.').toDoubleOrNull() ?: 0.0
    val maxWithdraw = if (isWithdraw) goal.savedAmount else Double.MAX_VALUE
    val ready = parsed > 0.0 && parsed <= maxWithdraw && selectedAccountId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (isWithdraw) {
                    Text(
                        "Доступно для снятия: ${formatMoney(goal.savedAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "Накоплено ${formatMoney(goal.savedAmount)} из ${formatMoney(goal.targetAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
                MoneyTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Сумма",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                if (accounts.isEmpty()) {
                    Text(
                        "Сначала создайте счёт: Настройки → Мои счета.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        if (isWithdraw) "На счёт" else "Со счёта",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accounts.forEach { ab ->
                            AccountPill(
                                title = ab.account.name,
                                subtitle = formatMoney(ab.balance),
                                color = Color(ab.account.color),
                                selected = selectedAccountId == ab.account.id,
                                onClick = { selectedAccountId = ab.account.id }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedAccountId?.let { onConfirm(it, parsed) } },
                enabled = ready
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
private fun AccountPill(
    title: String,
    subtitle: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) color.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = if (selected) color else Color.Transparent,
                shape = MaterialTheme.shapes.medium
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
