package com.finora.presentation.recurring

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finora.data.local.entity.RecurringRuleEntity
import com.finora.domain.model.TransactionType
import com.finora.presentation.AppViewModelProvider
import com.finora.presentation.components.EmptyState
import com.finora.presentation.components.FinoraCard
import com.finora.presentation.components.IconChip
import com.finora.presentation.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecurringRulesScreen(
    onBack: () -> Unit,
    viewModel: RecurringRulesViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Автооперации") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSheet = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Добавить правило", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.rules.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Repeat,
                        title = "Нет автоопераций",
                        subtitle = "Создайте правило, и операции будут добавляться автоматически"
                    )
                }
            }
            items(state.rules.size) { index ->
                val rule = state.rules[index]
                val category = state.categories.firstOrNull { it.id == rule.categoryId }
                val account = state.accounts.firstOrNull { it.id == rule.accountId }

                RuleCard(
                    rule = rule,
                    categoryName = category?.name ?: "—",
                    categoryIcon = category?.iconKey ?: "receipt",
                    categoryColor = category?.color ?: 0xFF6C5CE7,
                    accountName = account?.name ?: "—",
                    onToggle = { viewModel.toggleRule(rule) },
                    onDelete = { viewModel.deleteRule(rule) }
                )
            }
        }
    }

    if (showSheet) {
        AddRuleSheet(
            categories = state.categories,
            accounts = state.accounts,
            onDismiss = { showSheet = false },
            onCreate = { name, amount, type, catId, accId, period ->
                viewModel.createRule(name, amount, type, catId, accId, period)
                showSheet = false
            }
        )
    }
}

@Composable
private fun RuleCard(
    rule: RecurringRuleEntity,
    categoryName: String,
    categoryIcon: String,
    categoryColor: Long,
    accountName: String,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isExpense = rule.type == "EXPENSE"
    FinoraCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip(
                iconKey = categoryIcon,
                color = Color(categoryColor),
                size = 44.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${if (isExpense) "−" else "+"}${formatMoney(rule.amount)} • $categoryName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Каждые ${periodLabel(rule.periodDays)} • $accountName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = rule.enabled, onCheckedChange = { onToggle() })
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun periodLabel(days: Int): String = when (days) {
    1 -> "день"
    7 -> "неделю"
    14 -> "2 недели"
    30 -> "месяц"
    90 -> "квартал"
    365 -> "год"
    else -> "$days дн."
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddRuleSheet(
    categories: List<com.finora.data.local.entity.CategoryEntity>,
    accounts: List<com.finora.data.local.entity.AccountEntity>,
    onDismiss: () -> Unit,
    onCreate: (String, Double, String, Long, Long, Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var selectedPeriod by remember { mutableStateOf(30) }

    val typedCategories = categories.filter {
        it.type == if (isExpense) TransactionType.EXPENSE.name else TransactionType.INCOME.name
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Новая автооперация",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название (например, Аренда)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Сумма ₽") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(Modifier.height(16.dp))

            // Type toggle
            Text("Тип", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeChip("Расход", isExpense) { isExpense = true; selectedCategoryId = null }
                TypeChip("Доход", !isExpense) { isExpense = false; selectedCategoryId = null }
            }
            Spacer(Modifier.height(16.dp))

            // Category
            Text("Категория", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                typedCategories.forEach { cat ->
                    val sel = selectedCategoryId == cat.id
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (sel) Color(cat.color).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { selectedCategoryId = cat.id }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconChip(iconKey = cat.iconKey, color = Color(cat.color), size = 28.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(cat.name, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Account
            if (accounts.isNotEmpty()) {
                Text("Счёт", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.forEach { acc ->
                        val sel = selectedAccountId == acc.id
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (sel) Color(acc.color).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedAccountId = acc.id }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconChip(iconKey = acc.iconKey, color = Color(acc.color), size = 28.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(acc.name, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Period
            Text("Период", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "День", 7 to "Неделя", 30 to "Месяц", 90 to "Квартал", 365 to "Год").forEach { (days, label) ->
                    val sel = selectedPeriod == days
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { selectedPeriod = days }
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            val amount = amountText.toDoubleOrNull() ?: 0.0
            val canCreate = name.isNotBlank() && amount > 0 && selectedCategoryId != null && selectedAccountId != null

            Button(
                onClick = {
                    onCreate(
                        name.trim(),
                        amount,
                        if (isExpense) "EXPENSE" else "INCOME",
                        selectedCategoryId!!,
                        selectedAccountId!!,
                        selectedPeriod
                    )
                },
                enabled = canCreate,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Создать", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
