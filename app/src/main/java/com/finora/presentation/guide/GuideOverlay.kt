package com.finora.presentation.guide

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy



import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

import kotlin.math.roundToInt


// ─── Guide steps ─────────────────────────────────────────────────────────────

object GuideStep {
    // Home screen
    const val BALANCE_HERO = 0
    const val ACCOUNTS_STRIP = 1
    const val AI_INSIGHT = 2
    const val RECENT_TRANSACTIONS = 3
    const val FAB = 4
    // Navigate → Transactions
    const val NAV_TRANSACTIONS = 5
    const val TX_BREAKDOWN = 6
    const val TX_FILTERS = 7
    // Navigate → Goals
    const val NAV_GOALS = 8
    const val GOALS_CREATE = 9
    // Navigate → Settings
    const val NAV_SETTINGS = 10
    const val SETTINGS_THEME = 11

    const val TOTAL = 12
}

/** Which screen a guide step belongs to. */
enum class GuideScreen { HOME, TRANSACTIONS, GOALS, SETTINGS }

private data class StepInfo(
    val title: String,
    val description: String,
    val screen: GuideScreen
)

private val steps = listOf(
    /* 0  */ StepInfo("Общий баланс", "Привет! Здесь ты видишь баланс по всем счетам — сколько свободно и сколько лежит в целях.", GuideScreen.HOME),
    /* 1  */ StepInfo("Твои счета", "Тут карточки счетов — банки, карты и кошельки. Нажми на любой чтобы перейти к управлению.", GuideScreen.HOME),
    /* 2  */ StepInfo("AI-помощник", "Я анализирую твои расходы и дам полезные советы! Нажми чтобы открыть чат.", GuideScreen.HOME),
    /* 3  */ StepInfo("Последние операции", "Здесь краткий список последних доходов и расходов. Удобно чтобы быстро посмотреть что изменилось.", GuideScreen.HOME),
    /* 4  */ StepInfo("Добавить операцию", "Нажми «+» чтобы записать доход, расход или перевод между счетами!", GuideScreen.HOME),
    /* 5  */ StepInfo("Раздел «Операции»", "Здесь все твои транзакции — с графиком по категориям и фильтрами. Переходи!", GuideScreen.HOME),
    /* 6  */ StepInfo("График расходов", "Тут диаграмма расходов по категориям за месяц. Листай стрелками чтобы сравнить с прошлым месяцем.", GuideScreen.TRANSACTIONS),
    /* 7  */ StepInfo("Фильтры", "Фильтруй — все, доходы или расходы. Помогает быстро найти нужное.", GuideScreen.TRANSACTIONS),
    /* 8  */ StepInfo("Раздел «Цели»", "Ставь финансовые цели — на отпуск, технику или ремонт. Переходи!", GuideScreen.TRANSACTIONS),
    /* 9  */ StepInfo("Создать цель", "Нажми «Новая» чтобы создать цель. Можно пополнять с любого счёта и отслеживать прогресс!", GuideScreen.GOALS),
    /* 10 */ StepInfo("Раздел «Настройки»", "Тут темы, цвета и управление аккаунтом. Переходи!", GuideScreen.GOALS),
    /* 11 */ StepInfo("Выбор темы", "Выбирай светлую, тёмную или системную тему — и цвет акцента по вкусу! Всё — ты готов! :)", GuideScreen.SETTINGS),
)

// ─── GuideController ─────────────────────────────────────────────────────────

@Stable
class GuideController {
    var isActive by mutableStateOf(false)
    var currentStep by mutableIntStateOf(0)
        private set
    private val _targets = mutableStateMapOf<Int, Rect>()

    /** Called by FinoraNavHost to handle step-driven navigation. */
    var navigateToScreen: ((GuideScreen) -> Unit)? = null

    fun registerTarget(step: Int, bounds: Rect) {
        _targets[step] = bounds
    }

    fun targetFor(step: Int): Rect? = _targets[step]

    /** Current step's required screen. */
    fun currentScreen(): GuideScreen = steps.getOrNull(currentStep)?.screen ?: GuideScreen.HOME

    fun next() {
        if (currentStep < GuideStep.TOTAL - 1) {
            currentStep++
            val targetScreen = steps[currentStep].screen
            navigateToScreen?.invoke(targetScreen)
        } else {
            finish()
        }
    }

    fun finish() {
        isActive = false
        currentStep = 0
    }

    fun start() {
        currentStep = 0
        isActive = true
        navigateToScreen?.invoke(GuideScreen.HOME)
    }
}

/** Modifier to register a guide target on an element. */
fun Modifier.guideTarget(controller: GuideController?, step: Int): Modifier {
    if (controller == null) return this
    return this.onGloballyPositioned { coords ->
        controller.registerTarget(step, coords.boundsInRoot())
    }
}

val LocalGuideController = staticCompositionLocalOf<GuideController?> { null }

// ─── GuideOverlay composable ─────────────────────────────────────────────────

@Composable
fun GuideOverlay(
    controller: GuideController,
    onFinish: () -> Unit
) {
    val step = controller.currentStep
    val target = controller.targetFor(step)
    val density = LocalDensity.current
    val stepInfo = steps.getOrNull(step) ?: return

    val infiniteTransition = rememberInfiniteTransition(label = "mascot_float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    val alpha by animateFloatAsState(
        targetValue = if (controller.isActive) 1f else 0f,
        animationSpec = tween(400),
        label = "overlay_alpha"
    )

    if (alpha == 0f) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* block touches */ }
    ) {
        // Scrim + spotlight cutout
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            drawRect(Color.Black.copy(alpha = 0.72f))
            if (target != null && target != Rect.Zero) {
                val pad = 12f
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(target.left - pad, target.top - pad),
                    size = Size(target.width + pad * 2, target.height + pad * 2),
                    cornerRadius = CornerRadius(20f, 20f),
                    blendMode = BlendMode.Clear
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(target.left - pad, target.top - pad),
                    size = Size(target.width + pad * 2, target.height + pad * 2),
                    cornerRadius = CornerRadius(20f, 20f),
                    style = Stroke(width = 2f)
                )
            }
        }

        // Determine if tooltip should go above or below the target
        val isBelowCenter = target != null && target.center.y < with(density) { 400.dp.toPx() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(if (isBelowCenter) Alignment.BottomCenter else Alignment.TopCenter)
                .padding(
                    top = if (!isBelowCenter && target != null) with(density) {
                        (target.bottom + 24f).toDp().coerceAtMost(220.dp)
                    } else 0.dp,
                    bottom = if (isBelowCenter) 36.dp else 0.dp,
                    start = 20.dp,
                    end = 20.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isBelowCenter) Spacer(Modifier.height(16.dp))

            // Mascot
            Box(modifier = Modifier.offset { IntOffset(0, floatOffset.roundToInt()) }) {
                MascotGirl(modifier = Modifier.size(width = 120.dp, height = 160.dp))
            }
            Spacer(Modifier.height(10.dp))

            // Speech bubble
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        stepInfo.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A2E)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stepInfo.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B6B7B)
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${step + 1} / ${GuideStep.TOTAL}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF9C9CAE)
                        )
                        Row {
                            TextButton(onClick = {
                                controller.finish()
                                onFinish()
                            }) {
                                Text("Пропустить", color = Color(0xFF9C9CAE))
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (step < GuideStep.TOTAL - 1) controller.next()
                                    else { controller.finish(); onFinish() }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90D9))
                            ) {
                                Text(
                                    if (step == GuideStep.TOTAL - 1) "Готово!" else "Далее",
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ─── Anime girl mascot «Алия» — PNG drawable ────────────────────────────────

/**
 * Mascot character «Алия» — loaded from a detailed PNG drawable.
 * The image lives at res/drawable-nodpi/mascot_aliya.png.
 */
@Composable
fun MascotGirl(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = com.finora.R.drawable.mascot_aliya),
        contentDescription = "Алия — маскот приложения",
        modifier = modifier,
        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
    )
}
