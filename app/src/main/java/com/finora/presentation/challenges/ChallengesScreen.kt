package com.finora.presentation.challenges

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finora.domain.model.Category
import com.finora.presentation.AppViewModelProvider
import com.finora.presentation.components.EmptyState
import com.finora.presentation.components.FinoraCard
import com.finora.presentation.components.IconChip
import com.finora.presentation.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen(
    onBack: () -> Unit,
    viewModel: ChallengesViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Челленджи") },
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
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Новый челлендж")
            }
        }
    ) { padding ->
        val hasContent = state.active.isNotEmpty() || state.completed.isNotEmpty()
        if (!hasContent) {
            Box(modifier = Modifier.padding(padding)) {
                EmptyState(
                    icon = Icons.Rounded.EmojiEvents,
                    title = "Нет челленджей",
                    subtitle = "Бросьте себе вызов и улучшите финансовые привычки!"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.active.isNotEmpty()) {
                    item {
                        Text(
                            "Активные",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    items(state.active, key = { it.challenge.id }) { cw ->
                        ChallengeCard(
                            cw = cw,
                            onComplete = { viewModel.completeChallenge(cw.challenge.id) },
                            onDelete = { viewModel.deleteChallenge(cw.challenge) }
                        )
                    }
                }
                if (state.completed.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Завершённые",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(state.completed, key = { it.challenge.id }) { cw ->
                        ChallengeCard(
                            cw = cw,
                            onComplete = null,
                            onDelete = { viewModel.deleteChallenge(cw.challenge) }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddChallengeDialog(
            categories = state.expenseCategories,
            onAdd = { title, desc, emoji, days, amount, catId ->
                viewModel.addChallenge(title, desc, emoji, days, amount, catId)
            },
            onDismiss = { showAdd = false }
        )
    }
}

@Composable
private fun ChallengeCard(
    cw: ChallengeWithSpend,
    onComplete: (() -> Unit)?,
    onDelete: () -> Unit
) {
    val ch = cw.challenge
    val progressAnim by animateFloatAsState(
        targetValue = ch.progress.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "challengeProgress"
    )
    val isCompleted = ch.completed
    val barColor = when {
        isCompleted -> Color(0xFF3FB18C)
        cw.isOnTrack -> MaterialTheme.colorScheme.primary
        else -> Color(0xFFE07685)
    }

    FinoraCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ch.emoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    ch.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (ch.description.isNotBlank()) {
                    Text(
                        ch.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isCompleted) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Завершено",
                    tint = Color(0xFF3FB18C),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressAnim)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
        Spacer(Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "День ${ch.daysPassed} / ${ch.daysTotal}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (ch.targetAmount != null && cw.spent != null) {
                    Text(
                        "Потрачено: ${formatMoney(cw.spent)} / ${formatMoney(ch.targetAmount)} ₽",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (cw.isOnTrack) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFE07685)
                    )
                }
            }
            if (onComplete != null && !isCompleted) {
                TextButton(onClick = onComplete) {
                    Text("Завершить", color = Color(0xFF3FB18C))
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Preset challenges the user can pick from. */
private data class ChallengePreset(
    val title: String,
    val description: String,
    val emoji: String,
    val days: Int,
    val targetAmount: Double? = null,
    val needsCategory: Boolean = false
)

private val presets = listOf(
    ChallengePreset("Неделя без кафе", "Не тратить в кафе и ресторанах 7 дней", "☕", 7, needsCategory = true),
    ChallengePreset("30 дней экономии", "Сократить общие расходы за месяц", "💪", 30, targetAmount = 30000.0),
    ChallengePreset("Без импульсивных покупок", "14 дней без незапланированных трат в \"Покупки\"", "🛍️", 14, needsCategory = true),
    ChallengePreset("Марафон накоплений", "Откладывать каждый день 21 день подряд", "🏃", 21)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddChallengeDialog(
    categories: List<Category>,
    onAdd: (title: String, desc: String, emoji: String, days: Int, amount: Double?, catId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🎯") }
    var daysText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var catId by remember { mutableStateOf<Long?>(null) }
    var showCustom by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый челлендж") },
        text = {
            Column {
                if (!showCustom) {
                    Text(
                        "Готовые варианты",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    presets.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    title = p.title
                                    desc = p.description
                                    emoji = p.emoji
                                    daysText = p.days.toString()
                                    amountText = p.targetAmount?.toLong()?.toString() ?: ""
                                    showCustom = true
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(p.emoji, fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(p.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(p.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showCustom = true }) {
                        Text("Создать свой")
                    }
                } else {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Описание") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Row {
                        OutlinedTextField(value = emoji, onValueChange = { if (it.length <= 4) emoji = it }, label = { Text("Эмодзи") }, singleLine = true, modifier = Modifier.width(80.dp))
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = daysText,
                            onValueChange = { daysText = it.filter { c -> c.isDigit() } },
                            label = { Text("Дней") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Лимит расходов (₽, необязательно)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Категория (необязательно)", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val sel = catId == cat.id
                            Row(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.large)
                                    .background(
                                        if (sel) Color(cat.color).copy(alpha = 0.16f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { catId = if (sel) null else cat.id }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconChip(iconKey = cat.iconKey, color = Color(cat.color), size = 20.dp)
                                Spacer(Modifier.width(4.dp))
                                Text(cat.name, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showCustom) {
                TextButton(
                    onClick = {
                        val days = daysText.toIntOrNull() ?: return@TextButton
                        val amt = amountText.toDoubleOrNull()
                        onAdd(title, desc, emoji, days, amt, catId)
                        onDismiss()
                    },
                    enabled = title.isNotBlank() && (daysText.toIntOrNull() ?: 0) > 0
                ) { Text("Начать") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
