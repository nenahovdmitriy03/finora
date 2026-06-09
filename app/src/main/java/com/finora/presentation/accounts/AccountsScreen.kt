package com.finora.presentation.accounts

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finora.domain.model.Account
import com.finora.domain.model.AccountBalance
import com.finora.domain.model.AccountType
import com.finora.presentation.AppViewModelProvider
import com.finora.presentation.components.ColorPickerRow
import com.finora.presentation.components.FinoraCard
import com.finora.presentation.components.IconChip
import com.finora.presentation.components.IconPickerRow
import com.finora.presentation.components.SectionHeader
import com.finora.presentation.theme.Violet
import com.finora.presentation.theme.VioletDark
import com.finora.presentation.util.accountIconKeys
import com.finora.presentation.util.finoraPalette
import com.finora.presentation.util.formatMoney

@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editorAccount by remember { mutableStateOf<Account?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "Счета",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        item {
            Surface(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Violet, VioletDark)))
                        .padding(22.dp)
                ) {
                    Text(
                        "Всего на счетах",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        formatMoney(state.total),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White
                    )
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
        AccountEditorDialog(
            initial = editorAccount,
            onDismiss = { showEditor = false },
            onConfirm = { name, type, balance, icon, color ->
                viewModel.saveAccount(
                    id = editorAccount?.id ?: 0L,
                    name = name,
                    type = type,
                    initialBalance = balance,
                    iconKey = icon,
                    color = color
                )
                showEditor = false
            },
            onDelete = editorAccount?.let { acc -> { viewModel.delete(acc); showEditor = false } }
        )
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

@Composable
private fun AccountEditorDialog(
    initial: Account?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: AccountType, balance: Double, icon: String, color: Long) -> Unit,
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Новый счёт" else "Счёт") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название (напр. Тинькофф)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = balance,
                    onValueChange = { v -> balance = v.filter { it.isDigit() || it == '.' || it == ',' || it == '-' } },
                    label = { Text("Текущий баланс, ₽") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Text("Тип", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AccountType.entries.chunked(3).forEach { rowTypes ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowTypes.forEach { t ->
                                TypePill(
                                    label = t.title,
                                    selected = type == t,
                                    onClick = { type = t }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Иконка", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                IconPickerRow(keys = accountIconKeys, selected = icon, color = Color(color), onSelect = { icon = it })
                Spacer(Modifier.height(14.dp))
                Text("Цвет", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                ColorPickerRow(colors = finoraPalette, selected = color, onSelect = { color = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = balance.replace(',', '.').toDoubleOrNull() ?: 0.0
                    onConfirm(name, type, value, icon, color)
                },
                enabled = name.isNotBlank()
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
