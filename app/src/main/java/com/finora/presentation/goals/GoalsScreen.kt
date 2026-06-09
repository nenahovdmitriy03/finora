package com.finora.presentation.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finora.domain.model.Goal
import com.finora.presentation.AppViewModelProvider
import com.finora.presentation.components.ColorPickerRow
import com.finora.presentation.components.EmptyState
import com.finora.presentation.components.FinoraCard
import com.finora.presentation.components.IconChip
import com.finora.presentation.components.IconPickerRow
import com.finora.presentation.components.SectionHeader
import com.finora.presentation.util.MoneyTextField
import com.finora.presentation.util.finoraPalette
import com.finora.presentation.util.formatMoney
import kotlin.math.roundToInt

@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    var editorGoal by remember { mutableStateOf<Goal?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var contributeGoal by remember { mutableStateOf<Goal?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Цели",
                action = {
                    FilledTonalButton(onClick = { editorGoal = null; showEditor = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Новая")
                    }
                }
            )
        }

        if (goals.isEmpty()) {
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
            items(goals.size) { index ->
                val goal = goals[index]
                GoalCard(
                    goal = goal,
                    onEdit = { editorGoal = goal; showEditor = true },
                    onContribute = { contributeGoal = goal }
                )
            }
        }
    }

    if (showEditor) {
        GoalEditorDialog(
            initial = editorGoal,
            onDismiss = { showEditor = false },
            onConfirm = { name, target, icon, color ->
                viewModel.saveGoal(
                    id = editorGoal?.id ?: 0L,
                    name = name,
                    target = target,
                    iconKey = icon,
                    color = color,
                    deadline = editorGoal?.deadline,
                    saved = editorGoal?.savedAmount ?: 0.0
                )
                showEditor = false
            },
            onDelete = editorGoal?.let { g -> { viewModel.delete(g); showEditor = false } }
        )
    }

    contributeGoal?.let { goal ->
        ContributeDialog(
            goal = goal,
            onDismiss = { contributeGoal = null },
            onConfirm = { delta ->
                viewModel.contribute(goal.id, delta)
                contributeGoal = null
            }
        )
    }
}

@Composable
private fun GoalCard(goal: Goal, onEdit: () -> Unit, onContribute: () -> Unit) {
    val color = Color(goal.color)
    FinoraCard(modifier = Modifier.clickable { onEdit() }) {
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
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(onClick = onContribute, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Пополнить")
        }
    }
}

@Composable
private fun GoalEditorDialog(
    initial: Goal?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, target: Double, icon: String, color: Long) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var target by remember { mutableStateOf(initial?.targetAmount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var icon by remember { mutableStateOf(initial?.iconKey ?: "target") }
    var color by remember { mutableStateOf(initial?.color ?: finoraPalette[1]) }
    val iconKeys = listOf("target", "savings", "home", "car", "flight", "school", "phone", "gift", "games")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Новая цель" else "Цель") },
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

@Composable
private fun ContributeDialog(
    goal: Goal,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Пополнить «${goal.name}»") },
        text = {
            Column {
                Text(
                    "Накоплено ${formatMoney(goal.savedAmount)} из ${formatMoney(goal.targetAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                MoneyTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Сумма",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(amount.replace(',', '.').toDoubleOrNull() ?: 0.0) },
                enabled = (amount.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0.0
            ) { Text("Добавить") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onConfirm(-(amount.replace(',', '.').toDoubleOrNull() ?: 0.0)) }) {
                    Text("Снять")
                }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        }
    )
}
