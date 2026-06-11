package com.finora.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finora.data.remote.AuthRepository
import com.finora.domain.model.AccentColor
import com.finora.domain.model.ThemeMode
import com.finora.presentation.AppViewModelProvider
import com.finora.presentation.components.FinoraCard
import com.finora.presentation.components.SectionHeader
import com.finora.presentation.guide.GuideStep
import com.finora.presentation.guide.LocalGuideController
import com.finora.presentation.guide.guideTarget

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
    var showDeleteDialog by remember { mutableStateOf(false) }

    val guideController = LocalGuideController.current

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Настройки",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // ─── Account section ─────────────────────────────────────────────
        item { SectionHeader(title = "Аккаунт") }
        item {
            val auth = authState
            if (auth is AuthRepository.AuthState.Authenticated) {
                FinoraCard(padding = PaddingValues(0.dp)) {
                    Column {
                        SettingRow(
                            icon = Icons.Rounded.Person,
                            title = auth.email ?: "Пользователь",
                            subtitle = "Вы вошли в аккаунт",
                            onClick = {}
                        )
                        SettingRow(
                            icon = Icons.Rounded.Cloud,
                            title = "Облачная синхронизация",
                            subtitle = "Данные синхронизируются автоматически",
                            onClick = {}
                        )
                        // Logout
                        SettingRow(
                            icon = Icons.AutoMirrored.Rounded.Logout,
                            title = if (isSigningOut) "Выход…" else "Выйти",
                            subtitle = if (isSigningOut) "Сохраняю данные" else "Выйти из аккаунта",
                            onClick = { if (!isSigningOut) viewModel.signOut() },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            trailing = {
                                if (isSigningOut) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        )
                        // Delete account
                        SettingRow(
                            icon = Icons.Rounded.DeleteForever,
                            title = "Удалить аккаунт",
                            subtitle = "Удалить все данные безвозвратно",
                            onClick = { showDeleteDialog = true },
                            tint = MaterialTheme.colorScheme.error,
                            trailing = {
                                if (deleteStatus is SettingsViewModel.DeleteStatus.Deleting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        )
                    }
                }
            } else {
                FinoraCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
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
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    "Сейчас данные хранятся только на устройстве. " +
                                        "Создайте аккаунт — и они будут в облаке, " +
                                        "доступны на всех устройствах.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.goToRegister() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Войти или зарегистрироваться")
                        }
                    }
                }
            }
        }

        item { SectionHeader(title = "Внешний вид") }
        item {
            Box(modifier = Modifier.guideTarget(guideController, GuideStep.SETTINGS_THEME)) {
                ThemeSelector(selected = themeMode, onSelect = viewModel::setTheme)
            }
        }

        item { SectionHeader(title = "Цвет акцента") }
        item {
            FinoraCard {
                AccentSelector(selected = accent, onSelect = viewModel::setAccent)
            }
        }

        item { SectionHeader(title = "Управление") }
        item {
            FinoraCard(padding = PaddingValues(0.dp)) {
                SettingRow(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    title = "Мои счета",
                    subtitle = "Банки, карты и кошельки",
                    onClick = onOpenAccounts
                )
            }
        }

        item {
            FinoraCard {
                Column {
                    Text(
                        "Finora",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Версия 1.0 · Сделано с ❤",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
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
    Column {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AccentColor.entries.forEach { option ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .size(46.dp)
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
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Выбрано: ${selected.title}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 14.dp),
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

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
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
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
