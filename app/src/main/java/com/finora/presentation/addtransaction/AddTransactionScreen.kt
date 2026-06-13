package com.finora.presentation.addtransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finora.domain.model.TransactionType
import com.finora.presentation.AppViewModelProvider
import com.finora.presentation.components.IconChip
import com.finora.presentation.util.ThousandsVisualTransformation
import com.finora.presentation.util.expenseIconKeys
import com.finora.presentation.util.formatFullDate
import com.finora.presentation.util.incomeIconKeys

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    transactionId: Long,
    onDone: () -> Unit,
    viewModel: AddTransactionViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()

    LaunchedEffect(transactionId) { viewModel.load(transactionId) }
    LaunchedEffect(accounts) { viewModel.ensureDefaultAccount() }

    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    val isTransfer = viewModel.mode == EntryMode.TRANSFER
    val visibleCategories = categories.filter { it.type == viewModel.type }
    val selectedCategory = visibleCategories.firstOrNull { it.id == viewModel.categoryId }
    val accentColor = MaterialTheme.colorScheme.primary
    val isEditing = viewModel.editingId != null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            isEditing -> "Редактировать"
                            isTransfer -> "Перевод"
                            else -> "Новая операция"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { viewModel.delete(onDone) }) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Type toggle — now 3-way: Расход / Доход / Перевод
            ModeToggle(
                mode = viewModel.mode,
                onChange = viewModel::updateMode
            )

            // ─── Template quick-fill row ──────────────────────────────
            if (templates.isNotEmpty() && !isEditing) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Шаблоны",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(templates, key = { it.id }) { tpl ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable { viewModel.applyTemplate(tpl) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    tpl.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1
                                )
                                Text(
                                    "${tpl.amount.toLong()} ₽",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            // Amount
            AmountField(
                value = viewModel.amountText,
                onValueChange = viewModel::setAmount,
                accent = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))

            if (isTransfer) {
                // ─── Transfer mode: from / to account pickers ────────────
                Text(
                    "Со счёта",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                if (accounts.isEmpty()) {
                    Text(
                        "Сначала создайте счёт: Настройки → Мои счета.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        accounts.forEach { account ->
                            CategoryChip(
                                name = account.name,
                                iconKey = account.iconKey,
                                color = Color(account.color),
                                selected = viewModel.accountId == account.id,
                                onClick = { viewModel.setAccount(account.id) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                Text(
                    "На счёт",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                if (accounts.size < 2) {
                    Text(
                        "Для перевода нужно минимум 2 счёта.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        accounts.filter { it.id != viewModel.accountId }.forEach { account ->
                            CategoryChip(
                                name = account.name,
                                iconKey = account.iconKey,
                                color = Color(account.color),
                                selected = viewModel.toAccountId == account.id,
                                onClick = { viewModel.setToAccount(account.id) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

            } else {
                // ─── Income/Expense mode: category + single account ──────

                // Category — opens a dedicated full-screen picker
                Text(
                    "Категория",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { showCategoryPicker = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedCategory != null) {
                        IconChip(iconKey = selectedCategory.iconKey, color = Color(selectedCategory.color), size = 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            selectedCategory.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    } else {
                        Text(
                            "Выберите категорию",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "Изменить",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(24.dp))

                // Account
                Text(
                    "Счёт",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                if (accounts.isEmpty()) {
                    Text(
                        "Сначала создайте счёт: Настройки → Мои счета.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        accounts.forEach { account ->
                            CategoryChip(
                                name = account.name,
                                iconKey = account.iconKey,
                                color = Color(account.color),
                                selected = viewModel.accountId == account.id,
                                onClick = { viewModel.setAccount(account.id) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Date
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { showDatePicker = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text("Дата", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground)
                Text(
                    formatFullDate(viewModel.dateMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))

            // ─── Tags ─────────────────────────────────────────────────
            if (!isTransfer) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Теги",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                var newTagName by remember { mutableStateOf("") }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allTags.forEach { tag ->
                        val sel = tag.id in viewModel.selectedTagIds
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (sel) Color(tag.color).copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (sel) 1.5.dp else 0.dp,
                                    color = if (sel) Color(tag.color) else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { viewModel.toggleTag(tag.id) }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                "#${tag.name}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (sel) Color(tag.color) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                // Quick-add new tag
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        placeholder = { Text("Новый тег") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            if (newTagName.isNotBlank()) {
                                viewModel.createTag(newTagName, 0xFF6C5CE7)
                                newTagName = ""
                            }
                        },
                        enabled = newTagName.isNotBlank()
                    ) { Text("Добавить") }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Note
            OutlinedTextField(
                value = viewModel.note,
                onValueChange = viewModel::updateNote,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Комментарий (необязательно)") },
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )
            Spacer(Modifier.height(28.dp))

            Button(
                onClick = { viewModel.save(onDone) },
                enabled = viewModel.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        isEditing -> "Сохранить"
                        isTransfer -> "Перевести"
                        else -> "Добавить"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = viewModel.dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { viewModel.setDate(it) }
                    showDatePicker = false
                }) { Text("Готово") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showCategoryPicker && !isTransfer) {
        CategoryPickerDialog(
            categories = visibleCategories,
            selectedId = viewModel.categoryId,
            iconKeys = if (viewModel.type == TransactionType.INCOME) incomeIconKeys else expenseIconKeys,
            accent = accentColor,
            onSelect = { id -> viewModel.setCategory(id) },
            onCreate = { name, icon, color -> viewModel.createCategory(name, icon, color) },
            onDismiss = { showCategoryPicker = false }
        )
    }
}

// ─── 3-way Mode Toggle ──────────────────────────────────────────────────────

@Composable
private fun ModeToggle(
    mode: EntryMode,
    onChange: (EntryMode) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp)
    ) {
        ToggleSegment(
            label = "Расход",
            selected = mode == EntryMode.EXPENSE,
            selectedColor = accent,
            modifier = Modifier.weight(1f)
        ) { onChange(EntryMode.EXPENSE) }
        ToggleSegment(
            label = "Доход",
            selected = mode == EntryMode.INCOME,
            selectedColor = accent,
            modifier = Modifier.weight(1f)
        ) { onChange(EntryMode.INCOME) }
        ToggleSegment(
            label = "Перевод",
            selected = mode == EntryMode.TRANSFER,
            selectedColor = accent,
            modifier = Modifier.weight(1f)
        ) { onChange(EntryMode.TRANSFER) }
    }
}

@Composable
private fun ToggleSegment(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) selectedColor else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Shared composables ──────────────────────────────────────────────────────

@Composable
private fun AmountField(value: String, onValueChange: (String) -> Unit, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    "0",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp)
                )
            },
            textStyle = MaterialTheme.typography.displaySmall.copy(
                fontSize = 40.sp,
                textAlign = TextAlign.Center,
                color = accent
            ),
            suffix = { Text("  ₽", style = MaterialTheme.typography.titleLarge) },
            visualTransformation = remember { ThousandsVisualTransformation() },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        )
    }
}

@Composable
private fun CategoryChip(
    name: String,
    iconKey: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .background(if (selected) color.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) color else MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.large
            )
            .clickable { onClick() }
            .padding(start = 8.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconChip(iconKey = iconKey, color = color, size = 30.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
