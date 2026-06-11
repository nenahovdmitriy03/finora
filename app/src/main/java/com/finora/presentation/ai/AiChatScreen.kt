package com.finora.presentation.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finora.presentation.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    onBack: () -> Unit,
    onScan: () -> Unit = {},
    viewModel: AiChatViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.loading) {
        val count = state.messages.size + if (state.loading) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI-аналитика", style = MaterialTheme.typography.titleMedium)
                        state.provider?.let {
                            Text(
                                "через $it",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onScan) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = "Сканировать чек")
                    }
                    if (state.configured && state.messages.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clear() }) { Text("Новый анализ") }
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
                .imePadding()
        ) {
            if (!state.configured) {
                NotConfigured()
                return@Column
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.messages.size) { index ->
                    val msg = state.messages[index]
                    if (msg.role == "user") UserBubble(msg.content) else AssistantBubble(msg.content)
                }
                if (state.loading) {
                    item { TypingBubble() }
                }
                state.error?.let { err ->
                    item {
                        Text(
                            text = "Ошибка: $err",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            InputBar(
                value = input,
                onValueChange = { input = it },
                enabled = !state.loading,
                onSend = {
                    viewModel.send(input)
                    input = ""
                }
            )
        }
    }
}

@Composable
private fun AssistantBubble(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)
        ) {
            AiMarkdown(
                text = text,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color.White
            )
        }
    }
}

@Composable
private fun TypingBubble() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "Думаю…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NotConfigured() {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Чтобы включить ИИ, добавь в local.properties ключ одного из провайдеров " +
                "(OPENROUTER_API_KEY, GROQ_API_KEY или GEMINI_API_KEY) и пересобери приложение.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    onSend: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Спроси что-нибудь о финансах…") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (enabled) onSend() })
            )
            Spacer(Modifier.width(8.dp))
            val canSend = enabled && value.isNotBlank()
            IconButton(
                onClick = onSend,
                enabled = canSend
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Отправить",
                    tint = if (canSend) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Renders a tiny subset of Markdown: bullet lines and **bold** spans. */
@Composable
private fun AiMarkdown(text: String, modifier: Modifier = Modifier) {
    val lines = text.replace("\r\n", "\n").split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lines.forEach { raw ->
            val isBullet = raw.startsWith("* ") || raw.startsWith("- ") || raw.startsWith("•")
            val isHeader = raw.startsWith("#")
            val content = raw
                .removePrefix("* ").removePrefix("- ").removePrefix("•")
                .trimStart('#').trim()
            if (isBullet) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = parseInline(content),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                Text(
                    text = parseInline(content),
                    style = if (isHeader) MaterialTheme.typography.titleSmall
                    else MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isHeader) FontWeight.SemiBold else null,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

/** Converts **bold** markers into bold spans; leaves everything else as-is. */
private fun parseInline(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val start = text.indexOf("**", i)
        if (start < 0) {
            append(text.substring(i))
            break
        }
        append(text.substring(i, start))
        val end = text.indexOf("**", start + 2)
        if (end < 0) {
            append(text.substring(start))
            break
        }
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append(text.substring(start + 2, end))
        }
        i = end + 2
    }
}
