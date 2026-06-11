package com.finora.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finora.data.backup.BackupManager
import com.finora.data.remote.AuthRepository
import com.finora.domain.model.AccentColor
import com.finora.domain.model.ThemeMode
import com.finora.presentation.AppViewModelProvider
import com.finora.presentation.components.FinoraCard
import com.finora.presentation.components.SectionHeader
import com.finora.presentation.guide.GuideStep
import com.finora.presentation.guide.LocalGuideController
import com.finora.presentation.guide.guideTarget
import com.finora.presentation.theme.IncomeGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    onOpenAccounts: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val accent by viewModel.accentColor.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val deleteStatus by viewModel.deleteStatus.collectAsStateWithLifecycle()
    val isSigningOut by viewModel.isSigningOut.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // SAF launcher: pick where to save the JSON backup, then write it.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup { jsonText ->
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(jsonText.toByteArray())
                    } ?: error("Не удалось открыть файл для записи")
                }
            }
        }
    }

    // SAF launcher: pick a JSON backup file, then restore from it.
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { ins ->
                        ins.readBytes().decodeToString()
                    } ?: error("Не удалось открыть файл")
                }
            }
        }
    }

    val guideController = LocalGuideController.current
    val auth = authState
    val backupWorking = backupStatus is SettingsViewModel.BackupStatus.Working

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Text(
                "Настройки",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // ─── Profile / account state ─────────────────────────────────────
        item {
            if (auth is AuthRepository.AuthState.Authenticated) {
                ProfileHeader(email = auth.email ?: "Пользователь")
            } else {
                SignInPrompt(onSignIn = { viewModel.goToRegister() })
            }
        }

        // ─── Appearance (theme + accent in one card) ─────────────────────
        item { SectionHeader(title = "Оформление") }
        item {
            FinoraCard {
                Column {
                    GroupLabel("Тема")
                    Spacer(Modifier.height(10.dp))
                    Box(modifier = Modifier.guideTarget(guideController, GuideStep.SETTINGS_THEME)) {
                        ThemeSelector(selected = themeMode, onSelect = viewModel::setTheme)
                    }
                    Spacer(Modifier.height(22.dp))
                    GroupLabel("Цвет акцента")
                    Spacer(Modifier.height(14.dp))
                    AccentSelector(selected = accent, onSelect = viewModel::setAccent)
                }
            }
        }

        // ─── Data (accounts + backup grouped) ────────────────────────────
        item { SectionHeader(title = "Данные") }
        item {
            FinoraCard(padding = PaddingValues(0.dp)) {
                Column {
                    SettingRow(
                        icon = Icons.Rounded.AccountBalanceWallet,
                        title = "Мои счета",
                        subtitle = "Банки, карты и кошельки",
                        onClick = onOpenAccounts
                    )
                    RowDivider()
                    SettingRow(
                        icon = Icons.Rounded.FileDownload,
                        title = "Экспорт в файл",
                        subtitle = "Сохранить копию всех данных в JSON",
                        onClick = {
                            if (!backupWorking) exportLauncher.launch(BackupManager.suggestedFileName())
                        },
                        showChevron = false,
                        trailing = { if (backupWorking) RowProgress() }
                    )
                    RowDivider()
                    SettingRow(
                        icon = Icons.Rounded.FileUpload,
                        title = "Импорт из файла",
                        subtitle = "Восстановить данные из JSON-файла",
                        onClick = { if (!backupWorking) showImportDialog = true },
                        showChevron = false
                    )
                }
            }
        }

        // ─── Account actions (only when signed in) ───────────────────────
        if (auth is AuthRepository.AuthState.Authenticated) {
            item { SectionHeader(title = "Аккаунт") }
            item {
                FinoraCard(padding = PaddingValues(0.dp)) {
                    Column {
                        SettingRow(
                            icon = Icons.AutoMirrored.Rounded.Logout,
                            title = if (isSigningOut) "Выход…" else "Выйти",
                            subtitle = if (isSigningOut) "Сохраняю данные" else "Выйти из аккаунта",
                            onClick = { if (!isSigningOut) viewModel.signOut() },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            showChevron = false,
                            trailing = { if (isSigningOut) RowProgress() }
                        )
                        RowDivider()
                        SettingRow(
                            icon = Icons.Rounded.DeleteForever,
                            title = "Удалить аккаунт",
                            subtitle = "Удалить все данные безвозвратно",
                            onClick = { showDeleteDialog = true },
                            tint = MaterialTheme.colorScheme.error,
                            showChevron = false,
                            trailing = {
                                if (deleteStatus is SettingsViewModel.DeleteStatus.Deleting) RowProgress()
                            }
                        )
                    }
                }
            }
        }

        // ─── App footer ──────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Finora",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Версия 1.0 · Сделано с ❤",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // ─── Delete confirmation dialog ──────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Удалить аккаунт?",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    "Все ваши данные будут удалены из облака и с устройства. " +
                        "Это действие необратимо.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // ─── Import confirmation dialog (import overwrites current data) ───────
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            icon = {
                Icon(
                    Icons.Rounded.FileUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Импортировать данные?",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    "Текущие данные на устройстве будут заменены содержимым файла. " +
                        "Рекомендуем сначала сделать экспорт.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportDialog = false
                        importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    }
                ) {
                    Text("Выбрать файл")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // ─── Backup result dialog ─────────────────────────────────────────────
    val status = backupStatus
    val resultMessage: String? = when (status) {
        is SettingsViewModel.BackupStatus.Exported ->
            "Данные сохранены в файл (${status.records} записей)."
        is SettingsViewModel.BackupStatus.Imported ->
            "Данные восстановлены (${status.records} записей)."
        is SettingsViewModel.BackupStatus.Error ->
            "Ошибка: ${status.message}"
        else -> null
    }
    if (resultMessage != null) {
        val isError = status is SettingsViewModel.BackupStatus.Error
        AlertDialog(
            onDismissRequest = { viewModel.clearBackupStatus() },
            icon = {
                Icon(
                    if (isError) Icons.Rounded.Warning else Icons.Rounded.Check,
                    contentDescription = null,
                    tint = if (isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    if (isError) "Не получилось" else "Готово",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    resultMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.clearBackupStatus() }) {
                    Text("Ок")
                }
            }
        )
    }
}

// ─── Profile header ──────────────────────────────────────────────────────
@Composable
private fun ProfileHeader(email: String) {
    val initial = email.trim().firstOrNull()?.uppercase() ?: "?"
    FinoraCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    initial,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    email,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Синхронизация включена",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SignInPrompt(onSignIn: () -> Unit) {
    FinoraCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Войдите, чтобы сохранить данные",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Данные хранятся только на устройстве. Создайте аккаунт — " +
                            "и они будут в облаке, на всех устройствах.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
                Text("Войти или зарегистрироваться")
            }
        }
    }
}

// ─── Appearance selectors ──────────────────────────────────────────────────
@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ThemeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        Triple(ThemeMode.LIGHT, Icons.Rounded.LightMode, "Светлая"),
        Triple(ThemeMode.DARK, Icons.Rounded.DarkMode, "Тёмная"),
        Triple(ThemeMode.SYSTEM, Icons.Rounded.SettingsBrightness, "Системная")
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (mode, icon, label) ->
            ThemeOption(
                icon = icon,
                label = label,
                selected = selected == mode,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(mode) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentSelector(selected: AccentColor, onSelect: (AccentColor) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AccentColor.entries.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(option.seed))
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = MaterialTheme.colorScheme.onBackground,
                        shape = CircleShape
                    )
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = option.title,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Reusable row pieces ───────────────────────────────────────────────────
@Composable
private fun RowProgress() {
    CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        strokeWidth = 2.dp
    )
}

/** Thin divider inset to line up with the row text (past the icon chip). */
@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 74.dp, end = 16.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary,
    showChevron: Boolean = true,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        when {
            trailing != null -> trailing()
            showChevron -> Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
