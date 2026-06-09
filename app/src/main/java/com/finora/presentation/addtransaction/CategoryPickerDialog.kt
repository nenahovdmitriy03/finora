package com.finora.presentation.addtransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.finora.domain.model.Category
import com.finora.presentation.components.ColorPickerRow
import com.finora.presentation.components.IconChip
import com.finora.presentation.components.IconPickerRow
import com.finora.presentation.util.finoraPalette

/**
 * A full-screen picker for choosing a category, with an inline "create custom
 * category" form. Shown from [AddTransactionScreen]; shares its ViewModel state
 * through the [onSelect] / [onCreate] callbacks.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPickerDialog(
    categories: List<Category>,
    selectedId: Long?,
    iconKeys: List<String>,
    accent: Color,
    onSelect: (Long) -> Unit,
    onCreate: (name: String, iconKey: String, color: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var creating by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.systemBarsPadding()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (creating) creating = false else onDismiss() }) {
                        Icon(
                            if (creating) Icons.AutoMirrored.Rounded.ArrowBack else Icons.Rounded.Close,
                            contentDescription = "Назад"
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (creating) "Новая категория" else "Выберите категорию",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (creating) {
                    CreateCategoryForm(
                        iconKeys = iconKeys,
                        onSave = { name, icon, color -> onCreate(name, icon, color); onDismiss() }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 32.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            categories.forEach { category ->
                                CategoryGridChip(
                                    name = category.name,
                                    iconKey = category.iconKey,
                                    color = Color(category.color),
                                    selected = selectedId == category.id,
                                    onClick = { onSelect(category.id); onDismiss() }
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { creating = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = MaterialTheme.shapes.large,
                            colors = ButtonDefaults.buttonColors(containerColor = accent)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Создать свою категорию", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateCategoryForm(
    iconKeys: List<String>,
    onSave: (name: String, iconKey: String, color: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf(iconKeys.firstOrNull() ?: "category") }
    var color by remember { mutableStateOf(finoraPalette[0]) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip(iconKey = icon, color = Color(color), size = 56.dp)
            Spacer(Modifier.width(14.dp))
            Text(
                if (name.isBlank()) "Без названия" else name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Название") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(18.dp))
        Text("Иконка", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        IconPickerRow(keys = iconKeys, selected = icon, color = Color(color), onSelect = { icon = it })
        Spacer(Modifier.height(18.dp))
        Text("Цвет", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        ColorPickerRow(colors = finoraPalette, selected = color, onSelect = { color = it })
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { onSave(name, icon, color) },
            enabled = name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Сохранить", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun CategoryGridChip(
    name: String,
    iconKey: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) color.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) color else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(start = 8.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconChip(iconKey = iconKey, color = color, size = 32.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
