package com.finora.presentation.scan

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finora.domain.model.TransactionType
import com.finora.presentation.AppViewModelProvider
import com.finora.presentation.components.FinoraCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReceiptScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: ScanReceiptViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val bitmap = runCatching {
            context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
        if (bitmap != null) viewModel.scan(bitmap)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Сканировать", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
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
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!state.available) {
                NotConfigured()
                return@Column
            }

            state.error?.let { err ->
                FinoraCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }

            when (state.phase) {
                ScanPhase.SCANNING -> Centered { Loading() }
                ScanPhase.REVIEW -> ReviewContent(state, viewModel, onDone)
                ScanPhase.SAVING -> Centered { Loading("Сохраняю…") }
                else -> IdleContent { launcher.launch("image/*") }
            }
        }
    }
}

@Composable
private fun IdleContent(onPick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.PhotoCamera,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Сфотографируй чек или скриншот операции — ИИ распознает суммы и категории.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onPick) {
            Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Выбрать фото или чек")
        }
    }
}

@Composable
private fun ReviewContent(
    state: ScanUiState,
    viewModel: ScanReceiptViewModel,
    onDone: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Куда записать", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                LazyRowChips(state, viewModel)
            }
            itemsIndexed(state.drafts) { index, draft ->
                DraftCard(index, draft, state, viewModel)
            }
        }
        Button(
            onClick = { viewModel.save(onDone) },
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text("Сохранить операций: ${state.includedCount}")
        }
    }
}

@Composable
private fun LazyRowChips(state: ScanUiState, viewModel: ScanReceiptViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        state.accounts.chunked(2).forEach { rowAccounts ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowAccounts.forEach { acc ->
                    FilterChip(
                        selected = state.selectedAccountId == acc.id,
                        onClick = { viewModel.selectAccount(acc.id) },
                        label = { Text(acc.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftCard(
    index: Int,
    draft: ReceiptDraft,
    state: ScanUiState,
    viewModel: ScanReceiptViewModel
) {
    FinoraCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = draft.include, onCheckedChange = { viewModel.toggleInclude(index) })
            Spacer(Modifier.width(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = draft.type == TransactionType.EXPENSE,
                    onClick = { viewModel.setType(index, TransactionType.EXPENSE) },
                    label = { Text("Расход") }
                )
                FilterChip(
                    selected = draft.type == TransactionType.INCOME,
                    onClick = { viewModel.setType(index, TransactionType.INCOME) },
                    label = { Text("Доход") }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = draft.amountText,
            onValueChange = { viewModel.setAmount(index, it) },
            label = { Text("Сумма, ₽") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        CategoryDropdown(index, draft, state, viewModel)
        if (draft.note.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                draft.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryDropdown(
    index: Int,
    draft: ReceiptDraft,
    state: ScanUiState,
    viewModel: ScanReceiptViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val cats = state.categoriesFor(draft.type)
    val current = cats.firstOrNull { it.id == draft.categoryId }?.name ?: "Без категории"
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(current, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Без категории") },
                onClick = { viewModel.setCategory(index, null); expanded = false }
            )
            cats.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c.name) },
                    onClick = { viewModel.setCategory(index, c.id); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) { content() }
}

@Composable
private fun Loading(text: String = "Распознаю…") {
    CircularProgressIndicator()
    Spacer(Modifier.height(12.dp))
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun NotConfigured() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Сканирование недоступно",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Добавь ключ GEMINI_API_KEY в сборку, чтобы распознавать чеки.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
