package com.finora.presentation.accounts

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finora.domain.model.Account
import com.finora.domain.model.AccountBalance
import com.finora.domain.model.AccountType
import com.finora.domain.model.InterestPeriod
import com.finora.presentation.AppViewModelProvider
import com.finora.presentation.components.ColorPickerRow
import com.finora.presentation.components.FinoraCard
import com.finora.presentation.components.IconChip
import com.finora.presentation.components.IconPickerRow
import com.finora.presentation.components.SectionHeader
import com.finora.presentation.util.MoneyTextField
import com.finora.presentation.util.accountIconKeys
import com.finora.presentation.util.finoraPalette
import com.finora.presentation.util.formatMoney
import com.finora.presentation.util.parseMoney

@Composable
fun AccountsScreen(
    onBack: () -> Unit,
    viewModel: AccountsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editorAccount by remember { mutableStateOf<Account?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    "Счета",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        item {
            FinoraCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Всего на счетах",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formatMoney(state.total),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "${state.accounts.size} ${accountWord(state.accounts.size)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        item {
            SectionHeader(
                title = "Мои счета",
                action = {
                    FilledTonalButton(onClick = { editorAccount = null; showEditor = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Добавить")
                    }
                }
            )
        }

        if (state.accounts.isEmpty()) {
            item {
                FinoraCard {
                    Text(
                        "Добавьте банк или кошелёк, где хранятся ваши деньги.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(state.accounts.size) { index ->
                AccountCard(
                    item = state.accounts[index],
                    onClick = { editorAccount = state.accounts[index].account; showEditor = true }
                )
            }
        }
    }

    if (showEditor) {
        AccountEditorScreen(
            initial = editorAccount,
            onDismiss = { showEditor = false },
            onConfirm = { name, type, balance, icon, color, rate, period, payoutMinute, payoutDay ->
                viewModel.saveAccount(
                    id = editorAccount?.id ?: 0L,
                    name = name,
                    type = type,
                    initialBalance = balance,
                    iconKey = icon,
                    color = color,
                    interestRate = rate,
                    interestPeriod = period,
                    interestPayoutMinute = payoutMinute,
                    interestPayoutDay = payoutDay,
                    previousLastInterestAt = editorAccount?.lastInterestAt,
                    previouslyHadInterest = editorAccount?.hasInterest == true
                )
                showEditor = false
            },
            onDelete = editorAccount?.let { acc -> { viewModel.delete(acc); showEditor = false } }
        )
    }
}

private fun accountWord(count: Int): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> "счетов"
        mod10 == 1 -> "счёт"
        mod10 in 2..4 -> "счёта"
        else -> "счетов"
    }
}

@Composable
private fun AccountCard(item: AccountBalance, onClick: () -> Unit) {
    FinoraCard(modifier = Modifier.clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip(iconKey = item.account.iconKey, color = Color(item.account.color), size = 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.account.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    item.account.type.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                formatMoney(item.balance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AccountEditorScreen(
    initial: Account?,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        type: AccountType,
        balance: Double,
        icon: String,
        color: Long,
        interestRate: Double,
        interestPeriod: InterestPeriod?,
        interestPayoutMinute: Int,
        interestPayoutDay: Int
    ) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var balance by remember {
        mutableStateOf(
            initial?.initialBalance?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""
        )
    }
    var icon by remember { mutableStateOf(initial?.iconKey ?: "card") }
    var color by remember { mutableStateOf(initial?.color ?: finoraPalette[0]) }
    var type by remember { mutableStateOf(initial?.type ?: AccountType.CARD) }
    var interestOn by remember { mutableStateOf(initial?.hasInterest ?: false) }
    var rateText by remember {
        mutableStateOf(
            initial?.interestRate?.takeIf { it > 0.0 }
                ?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""
        )
    }
    var period by remember { mutableStateOf(initial?.interestPeriod ?: InterestPeriod.MONTHLY) }
    var payoutMinute by remember { mutableIntStateOf(initial?.interestPayoutMinute ?: 9 * 60) }
    var payoutDay by remember { mutableIntStateOf(initial?.interestPayoutDay ?: 1) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }

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
                        Icon(Icons.Rounded.Close, contentDescription = "Закрыть")
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (initial == null) "Новый счёт" else "Счёт",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            val rate = if (interestOn) parseMoney(rateText) else 0.0
                            val selectedPeriod = if (interestOn) period else null
                            onConfirm(name, type, parseMoney(balance), icon, color, rate, selectedPeriod, payoutMinute, payoutDay)
                        },
                        enabled = name.isNotBlank()
                    ) { Text("Сохранить", style = MaterialTheme.typography.titleMedium) }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Название (напр. Тинькофф)") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    MoneyTextField(
                        value = balance,
                        onValueChange = { balance = it },
                        label = "Текущий баланс",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(18.dp))
                    Text("Тип", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AccountType.entries.forEach { t ->
                            TypePill(label = t.title, selected = type == t, onClick = { type = t })
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Иконка", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    IconPickerRow(keys = accountIconKeys, selected = icon, color = Color(color), onSelect = { icon = it })
                    Spacer(Modifier.height(18.dp))
                    Text("Цвет", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    ColorPickerRow(colors = finoraPalette, selected = color, onSelect = { color = it })

                    Spacer(Modifier.height(22.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Накопительный счёт",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                "Начислять проценты в «Капитализацию»",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = interestOn, onCheckedChange = { interestOn = it })
                    }

                    if (interestOn) {
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = rateText,
                            onValueChange = { input ->
                                rateText = input.filter { it.isDigit() || it == '.' || it == ',' }
                            },
                            label = { Text("Годовая ставка") },
                            suffix = { Text("% годовых") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(14.dp))
                        Text("Как часто платить", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InterestPeriod.entries.forEach { p ->
                                TypePill(label = p.title, selected = period == p, onClick = { period = p })
                            }
                        }

                        // ─── Day of month picker (only for MONTHLY) ─────────
                        if (period == InterestPeriod.MONTHLY) {
                            Spacer(Modifier.height(14.dp))
                            Text("Дата выплаты", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { showDayPicker = true }
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Проценты начисляются",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    "$payoutDay-го числа",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Text("Время выплаты", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { showTimePicker = true }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Проценты начисляются в",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                formatMinuteOfDay(payoutMinute),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (onDelete != null) {
                        Spacer(Modifier.height(28.dp))
                        TextButton(
                            onClick = onDelete,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Удалить счёт", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    // ─── Time picker dialog ──────────────────────────────────────────────────
    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = payoutMinute / 60,
            initialMinute = payoutMinute % 60,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Время выплаты процентов") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    payoutMinute = timeState.hour * 60 + timeState.minute
                    showTimePicker = false
                }) { Text("Готово") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Отмена") }
            }
        )
    }

    // ─── Day of month picker dialog ──────────────────────────────────────────
    if (showDayPicker) {
        DayOfMonthPickerDialog(
            selected = payoutDay,
            onConfirm = { payoutDay = it; showDayPicker = false },
            onDismiss = { showDayPicker = false }
        )
    }
}

/** Grid dialog to pick a day of month (1..31). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayOfMonthPickerDialog(
    selected: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Число месяца") },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (day in 1..31) {
                    val isSelected = day == selected
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onConfirm(day) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

private fun formatMinuteOfDay(minute: Int): String {
    val h = (minute / 60).coerceIn(0, 23)
    val m = (minute % 60).coerceIn(0, 59)
    return "%02d:%02d".format(h, m)
}

@Composable
private fun TypePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
