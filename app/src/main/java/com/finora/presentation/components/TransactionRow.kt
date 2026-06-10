package com.finora.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finora.domain.model.TransactionDetails
import com.finora.domain.model.TransactionType
import com.finora.presentation.util.formatSigned

@Composable
fun TransactionRow(
    details: TransactionDetails,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val tx = details.transaction
    val category = details.category
    val accentColor = Color(category?.color ?: 0xFFB2BEC3)
    val title = category?.name ?: if (tx.type == TransactionType.INCOME) "Доход" else "Расход"
    val subtitle = buildString {
        append(details.account?.name ?: "Счёт")
        if (tx.note.isNotBlank()) append(" · ${tx.note}")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconChip(iconKey = category?.iconKey ?: "category", color = accentColor)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatSigned(tx.amount, tx.type),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            // Neutral style — income/expense is conveyed by the +/- sign, not colour.
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
