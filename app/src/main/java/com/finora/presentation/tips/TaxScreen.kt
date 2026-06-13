package com.finora.presentation.tips

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finora.presentation.AppViewModelProvider
import com.finora.presentation.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxScreen(
    onBack: () -> Unit,
    viewModel: TaxViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Налоговый вычет") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Hero card with potential refund ──────────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "💰", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Возможный возврат за ${state.year}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = formatMoney(state.potentialRefund),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "≈ ориентировочная оценка",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(12.dp))

                        val progress = if (state.socialCap > 0)
                            (state.totalDeductible / state.socialCap).toFloat().coerceIn(0f, 1f)
                        else 0f
                        val animatedProgress by animateFloatAsState(
                            targetValue = progress,
                            animationSpec = tween(800),
                            label = "progress"
                        )
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                            strokeCap = StrokeCap.Round
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${formatMoney(state.totalDeductible)} из ${formatMoney(state.socialCap)} лимита",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // ── Breakdown by category ───────────────────────────────────
            if (state.items.isNotEmpty()) {
                item {
                    Text(
                        text = "Расходы по вычетам",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(state.items.size) { index ->
                    DeductionRow(state.items[index])
                }
            }

            // ── Empty state ─────────────────────────────────────────────
            if (!state.loading && state.items.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔍", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Пока нет расходов по вычетам",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Добавляй расходы в категории «Здоровье», «Образование» или «Спорт» — и мы посчитаем, сколько можно вернуть.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ── Collapsible: What is a tax deduction? ───────────────────
            item {
                CollapsibleSection(
                    title = "Что такое налоговый вычет?",
                    emoji = "💡"
                ) {
                    Text(
                        text = "По ст. 219 НК РФ государство возвращает 13% от расходов на лечение, " +
                            "обучение, спорт, страхование жизни и благотворительность.\n\n" +
                            "Общий лимит социальных вычетов — 150 000 ₽/год (с 2024 г.). " +
                            "Максимальный возврат: 19 500 ₽ в год.\n\n" +
                            "Вычет доступен официально трудоустроенным плательщикам НДФЛ 13%.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            // ── Collapsible: What counts ────────────────────────────────
            item {
                CollapsibleSection(
                    title = "Что подходит для вычета",
                    emoji = "📋"
                ) {
                    val hints = listOf(
                        "🏥 Лечение — приёмы врачей, анализы, стоматология, медикаменты по рецепту, ДМС",
                        "🎓 Обучение — своё (любое), детей до 24 лет (очное), курсы, автошкола, вуз",
                        "🏋️ Фитнес — абонемент в зал, бассейн, спортивные секции (организация из реестра Минспорта)",
                        "🛡️ Страхование жизни — договор от 5 лет (не страхование имущества)",
                        "❤️ Благотворительность — пожертвования НКО (до 25% годового дохода, отдельный лимит)"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        hints.forEach { hint ->
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            // ── Collapsible: How to apply ───────────────────────────────
            item {
                CollapsibleSection(
                    title = "Как оформить вычет",
                    emoji = "📝"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        StepBlock(
                            number = "1",
                            title = "Собери документы",
                            body = "• Справка 2-НДФЛ от работодателя (или данные в ЛК ФНС)\n" +
                                "• Договор с клиникой / учебным заведением / фитнес-клубом\n" +
                                "• Чеки и квитанции об оплате\n" +
                                "• Лицензия организации (обычно есть на сайте)\n" +
                                "• Для лечения: справка об оплате мед. услуг\n" +
                                "• Для обучения детей: свидетельство о рождении, справка об очной форме"
                        )
                        StepBlock(
                            number = "2",
                            title = "Выбери способ",
                            body = "Вариант А — через работодателя (быстрее):\n" +
                                "  Подай заявление в ЛК nalog.gov.ru → получи уведомление → " +
                                "отнеси в бухгалтерию → с зарплаты перестанут удерживать НДФЛ\n\n" +
                                "Вариант Б — через декларацию 3-НДФЛ (после конца года):\n" +
                                "  Заполни 3-НДФЛ в ЛК ФНС или в приложении «Налоги ФЛ» → " +
                                "приложи документы → подай → деньги вернут на счёт"
                        )
                        StepBlock(
                            number = "3",
                            title = "Подай онлайн через ЛК ФНС",
                            body = "1. Зайди на lkfl2.nalog.ru через Госуслуги или по ИНН+пароль\n" +
                                "2. «Жизненные ситуации» → «Подать декларацию 3-НДФЛ»\n" +
                                "3. Заполни данные (доходы подтянутся автоматически)\n" +
                                "4. Выбери тип вычета (социальный), внеси суммы расходов\n" +
                                "5. Приложи сканы документов\n" +
                                "6. Подпиши неквалифицированной ЭП (бесплатно, тут же)\n" +
                                "7. Отправь"
                        )
                        StepBlock(
                            number = "4",
                            title = "Дождись проверки",
                            body = "• Камеральная проверка: до 3 месяцев\n" +
                                "• Заявление на возврат (если не подавал раньше)\n" +
                                "• Перевод на счёт: до 1 месяца после заявления\n" +
                                "• Статус можно отслеживать в ЛК ФНС"
                        )
                    }
                }
            }

            // ── Collapsible: Useful links ───────────────────────────────
            item {
                CollapsibleSection(
                    title = "Полезные ресурсы",
                    emoji = "🌐"
                ) {
                    val links = listOf(
                        "🌐 lkfl2.nalog.ru — Личный кабинет ФНС (подача 3-НДФЛ онлайн)",
                        "📱 Приложение «Налоги ФЛ» — мобильная версия ЛК ФНС",
                        "📞 8-800-222-22-22 — горячая линия ФНС (бесплатно)",
                        "🏢 Ближайшая ИФНС — можно подать документы лично",
                        "📋 gosuslugi.ru — вход в ЛК ФНС через подтверждённый аккаунт"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        links.forEach { link ->
                            Text(
                                text = link,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            // ── Collapsible: Important notes ────────────────────────────
            item {
                CollapsibleSection(
                    title = "Важно помнить",
                    emoji = "⚠️"
                ) {
                    val notes = listOf(
                        "• Вычет можно получить за последние 3 года (например, в 2026 — за 2023–2025)",
                        "• Возврат не может превышать сумму уплаченного НДФЛ за год",
                        "• Дорогостоящее лечение (код 2) — без лимита 150 000 ₽",
                        "• Вычет за обучение детей — отдельный лимит 110 000 ₽/год на ребёнка",
                        "• Самозанятые на НПД не имеют права на вычет (нет НДФЛ 13%)",
                        "• Сохраняй все чеки и договоры — они нужны для подтверждения"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        notes.forEach { note ->
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            // ── Bottom disclaimer ───────────────────────────────────────
            item {
                Text(
                    text = "Информация носит ознакомительный характер и не является налоговой консультацией. " +
                        "Суммы рассчитаны приблизительно на основе категорий расходов. " +
                        "Для точного расчёта обратитесь в ФНС или к налоговому консультанту.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/* ── Collapsible section (accordion) ─────────────────────────────────── */

@Composable
private fun CollapsibleSection(
    title: String,
    emoji: String,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(250),
        label = "arrow"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header (always visible, clickable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(arrowRotation)
                )
            }

            // Body (animated expand/collapse)
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(250)),
                exit = shrinkVertically(tween(200))
            ) {
                Box(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    )
                ) {
                    content()
                }
            }
        }
    }
}

/* ── Step block (inside collapsible) ─────────────────────────────────── */

@Composable
private fun StepBlock(number: String, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )
        }
    }
}

/* ── Deduction row ───────────────────────────────────────────────────── */

@Composable
private fun DeductionRow(item: DeductionItem) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.emoji, fontSize = 20.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = item.deductionType.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatMoney(item.spent),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "возврат ~${formatMoney(item.spent * 0.13)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
