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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
                MascotGirl(modifier = Modifier.size(80.dp))
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

// ─── Anime girl mascot — silver hair, blue eyes, school uniform ──────────────

/**
 * Cute chibi anime girl mascot based on reference:
 * - Long silver / light-pink hair with ahoge (antenna strand)
 * - Red ribbon hair ties on both sides
 * - Big blue eyes with detailed highlights
 * - Dark school vest + white shirt + red bow tie
 * - Blushing pink cheeks
 * All drawn with Canvas — no external assets.
 */
@Composable
fun MascotGirl(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // ─── Colors ──────────────────────────────────────────────────
        val hairSilver = Color(0xFFD8D0E0)       // main hair
        val hairLight = Color(0xFFEDE8F0)         // highlights
        val hairShadow = Color(0xFFC0B5CC)        // shadow strands
        val hairTips = Color(0xFFE8D8E8)          // lighter tips
        val ribbonRed = Color(0xFFE03050)         // red ribbons
        val ribbonDark = Color(0xFFC02040)

        val skin = Color(0xFFFFE8D8)
        val skinShadow = Color(0xFFFFD4BC)
        val blush = Color(0xFFFFB0C0)

        val eyeBlue = Color(0xFF4090D0)
        val eyeLight = Color(0xFF70B8F0)
        val eyeDark = Color(0xFF2060A0)
        val white = Color.White
        val black = Color(0xFF303030)

        val vestDark = Color(0xFF404550)          // dark vest
        val vestMid = Color(0xFF505560)
        val shirtWhite = Color(0xFFF5F0F0)
        val collarWhite = Color(0xFFFFFFFF)
        val bowRed = Color(0xFFD03050)
        val bowDarkRed = Color(0xFFB02040)

        // ─── Body / Uniform ─────────────────────────────────────────
        val bodyTop = h * 0.63f

        // Shirt collar visible above vest
        val collarPath = Path().apply {
            moveTo(cx - w * 0.14f, bodyTop - h * 0.01f)
            lineTo(cx - w * 0.08f, bodyTop + h * 0.08f)
            lineTo(cx, bodyTop + h * 0.04f)
            lineTo(cx + w * 0.08f, bodyTop + h * 0.08f)
            lineTo(cx + w * 0.14f, bodyTop - h * 0.01f)
            close()
        }
        drawPath(collarPath, collarWhite)

        // Dark vest / jumper dress
        val vestPath = Path().apply {
            moveTo(cx - w * 0.20f, bodyTop + h * 0.02f)
            cubicTo(
                cx - w * 0.26f, bodyTop + h * 0.10f,
                cx - w * 0.22f, h * 0.92f,
                cx, h * 0.97f
            )
            cubicTo(
                cx + w * 0.22f, h * 0.92f,
                cx + w * 0.26f, bodyTop + h * 0.10f,
                cx + w * 0.20f, bodyTop + h * 0.02f
            )
            close()
        }
        drawPath(vestPath, vestDark)

        // Vest V-neckline showing shirt
        val vneckPath = Path().apply {
            moveTo(cx - w * 0.12f, bodyTop + h * 0.02f)
            lineTo(cx, bodyTop + h * 0.14f)
            lineTo(cx + w * 0.12f, bodyTop + h * 0.02f)
            close()
        }
        drawPath(vneckPath, shirtWhite)

        // Vest edge lines (subtle)
        drawLine(vestMid, Offset(cx - w * 0.12f, bodyTop + h * 0.02f),
            Offset(cx, bodyTop + h * 0.14f), strokeWidth = 1.5f, cap = StrokeCap.Round)
        drawLine(vestMid, Offset(cx + w * 0.12f, bodyTop + h * 0.02f),
            Offset(cx, bodyTop + h * 0.14f), strokeWidth = 1.5f, cap = StrokeCap.Round)

        // Red bow tie
        // Left wing
        val bowLeft = Path().apply {
            moveTo(cx - w * 0.01f, bodyTop + h * 0.06f)
            cubicTo(
                cx - w * 0.08f, bodyTop + h * 0.03f,
                cx - w * 0.10f, bodyTop + h * 0.09f,
                cx - w * 0.01f, bodyTop + h * 0.07f
            )
            close()
        }
        drawPath(bowLeft, bowRed)
        // Right wing
        val bowRight = Path().apply {
            moveTo(cx + w * 0.01f, bodyTop + h * 0.06f)
            cubicTo(
                cx + w * 0.08f, bodyTop + h * 0.03f,
                cx + w * 0.10f, bodyTop + h * 0.09f,
                cx + w * 0.01f, bodyTop + h * 0.07f
            )
            close()
        }
        drawPath(bowRight, bowRed)
        // Bow center knot
        drawCircle(bowDarkRed, radius = w * 0.02f, center = Offset(cx, bodyTop + h * 0.065f))

        // Gold buttons on vest
        drawCircle(Color(0xFFD4A850), radius = w * 0.015f, center = Offset(cx - w * 0.04f, bodyTop + h * 0.18f))
        drawCircle(Color(0xFFD4A850), radius = w * 0.015f, center = Offset(cx - w * 0.04f, bodyTop + h * 0.25f))

        // ─── Neck ───────────────────────────────────────────────────
        drawRect(skin, topLeft = Offset(cx - w * 0.06f, h * 0.58f), size = Size(w * 0.12f, h * 0.08f))

        // ─── Head (big chibi head) ──────────────────────────────────
        val headCy = h * 0.34f
        val headRx = w * 0.33f
        val headRy = h * 0.28f

        // Hair back layer (behind head)
        // Long flowing hair going down past shoulders
        val hairBackPath = Path().apply {
            moveTo(cx - headRx - w * 0.02f, headCy)
            // Left flowing strand
            cubicTo(
                cx - headRx - w * 0.06f, headCy + headRy * 1.5f,
                cx - headRx + w * 0.02f, h * 0.80f,
                cx - w * 0.12f, h * 0.90f
            )
            lineTo(cx + w * 0.12f, h * 0.90f)
            cubicTo(
                cx + headRx - w * 0.02f, h * 0.80f,
                cx + headRx + w * 0.06f, headCy + headRy * 1.5f,
                cx + headRx + w * 0.02f, headCy
            )
            close()
        }
        drawPath(hairBackPath, hairShadow)
        // Hair strand details on back hair
        drawLine(hairTips, Offset(cx - w * 0.18f, h * 0.70f), Offset(cx - w * 0.15f, h * 0.85f),
            strokeWidth = 1.5f, cap = StrokeCap.Round)
        drawLine(hairTips, Offset(cx + w * 0.18f, h * 0.70f), Offset(cx + w * 0.15f, h * 0.85f),
            strokeWidth = 1.5f, cap = StrokeCap.Round)

        // Face
        drawOval(skin, topLeft = Offset(cx - headRx, headCy - headRy), size = Size(headRx * 2, headRy * 2))
        // Subtle chin shadow
        drawArc(skinShadow, startAngle = 20f, sweepAngle = 140f, useCenter = true,
            topLeft = Offset(cx - headRx * 0.7f, headCy + headRy * 0.45f),
            size = Size(headRx * 1.4f, headRy * 0.5f))

        // ─── Hair (silver/pink — bangs + side strands) ──────────────

        // Top hair volume — big fluffy top
        val hairTopPath = Path().apply {
            moveTo(cx - headRx - w * 0.04f, headCy - headRy * 0.05f)
            cubicTo(
                cx - headRx * 0.5f, headCy - headRy - h * 0.17f,
                cx + headRx * 0.5f, headCy - headRy - h * 0.17f,
                cx + headRx + w * 0.04f, headCy - headRy * 0.05f
            )
            cubicTo(
                cx + headRx * 0.3f, headCy - headRy * 0.55f,
                cx - headRx * 0.3f, headCy - headRy * 0.55f,
                cx - headRx - w * 0.04f, headCy - headRy * 0.05f
            )
            close()
        }
        drawPath(hairTopPath, hairSilver)

        // Hair highlight on top
        drawPath(Path().apply {
            moveTo(cx - w * 0.06f, headCy - headRy - h * 0.07f)
            cubicTo(cx - w * 0.02f, headCy - headRy - h * 0.12f,
                cx + w * 0.08f, headCy - headRy - h * 0.08f,
                cx + w * 0.04f, headCy - headRy * 0.45f)
        }, hairLight, style = Stroke(width = w * 0.025f, cap = StrokeCap.Round))

        // Ahoge (antenna hair strand at top)
        val ahogePath = Path().apply {
            moveTo(cx - w * 0.02f, headCy - headRy - h * 0.08f)
            cubicTo(
                cx + w * 0.02f, headCy - headRy - h * 0.18f,
                cx + w * 0.08f, headCy - headRy - h * 0.16f,
                cx + w * 0.04f, headCy - headRy - h * 0.10f
            )
        }
        drawPath(ahogePath, hairSilver, style = Stroke(width = w * 0.02f, cap = StrokeCap.Round))

        // Bangs — soft jagged fringe
        val bangsPath = Path().apply {
            moveTo(cx - headRx * 0.92f, headCy - headRy * 0.15f)
            lineTo(cx - headRx * 0.65f, headCy + headRy * 0.12f)
            lineTo(cx - headRx * 0.42f, headCy - headRy * 0.02f)
            lineTo(cx - headRx * 0.18f, headCy + headRy * 0.18f)
            lineTo(cx + headRx * 0.08f, headCy + headRy * 0.02f)
            lineTo(cx + headRx * 0.32f, headCy + headRy * 0.15f)
            lineTo(cx + headRx * 0.58f, headCy - headRy * 0.05f)
            lineTo(cx + headRx * 0.80f, headCy + headRy * 0.08f)
            lineTo(cx + headRx * 0.92f, headCy - headRy * 0.15f)
            // Connect over the top
            cubicTo(
                cx + headRx * 0.5f, headCy - headRy - h * 0.12f,
                cx - headRx * 0.5f, headCy - headRy - h * 0.12f,
                cx - headRx * 0.92f, headCy - headRy * 0.15f
            )
            close()
        }
        drawPath(bangsPath, hairSilver)

        // Lighter streaks in bangs
        drawLine(hairLight, Offset(cx - headRx * 0.5f, headCy - headRy * 0.4f),
            Offset(cx - headRx * 0.55f, headCy + headRy * 0.05f), strokeWidth = 2f, cap = StrokeCap.Round)
        drawLine(hairLight, Offset(cx + headRx * 0.3f, headCy - headRy * 0.35f),
            Offset(cx + headRx * 0.25f, headCy + headRy * 0.1f), strokeWidth = 2f, cap = StrokeCap.Round)

        // Side hair strands (left — flowing down)
        val leftHairPath = Path().apply {
            moveTo(cx - headRx * 0.88f, headCy - headRy * 0.1f)
            cubicTo(
                cx - headRx - w * 0.08f, headCy + headRy * 0.5f,
                cx - headRx - w * 0.05f, h * 0.68f,
                cx - headRx + w * 0.03f, h * 0.78f
            )
            lineTo(cx - headRx + w * 0.10f, headCy + headRy * 0.4f)
            close()
        }
        drawPath(leftHairPath, hairSilver)

        // Side hair strands (right — flowing down)
        val rightHairPath = Path().apply {
            moveTo(cx + headRx * 0.88f, headCy - headRy * 0.1f)
            cubicTo(
                cx + headRx + w * 0.08f, headCy + headRy * 0.5f,
                cx + headRx + w * 0.05f, h * 0.68f,
                cx + headRx - w * 0.03f, h * 0.78f
            )
            lineTo(cx + headRx - w * 0.10f, headCy + headRy * 0.4f)
            close()
        }
        drawPath(rightHairPath, hairSilver)

        // ─── Red Ribbon hair ties (on each side) ────────────────────
        val ribbonY = headCy + headRy * 0.15f

        // Left ribbon
        drawRibbonTie(cx - headRx * 0.82f, ribbonY, w * 0.06f, ribbonRed, ribbonDark)
        // Right ribbon
        drawRibbonTie(cx + headRx * 0.82f, ribbonY, w * 0.06f, ribbonRed, ribbonDark)

        // ─── Eyes (big detailed anime eyes) ─────────────────────────
        val eyeY = headCy + headRy * 0.08f
        val eyeSpacing = headRx * 0.40f
        val eyeW = w * 0.095f
        val eyeH = h * 0.085f

        drawAnimeEye(cx - eyeSpacing, eyeY, eyeW, eyeH, eyeBlue, eyeLight, eyeDark, white, black)
        drawAnimeEye(cx + eyeSpacing, eyeY, eyeW, eyeH, eyeBlue, eyeLight, eyeDark, white, black)

        // Upper eyelid lines (thicker, expressive)
        drawArc(black, startAngle = 200f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(cx - eyeSpacing - eyeW * 1.1f, eyeY - eyeH * 1.2f),
            size = Size(eyeW * 2.2f, eyeH * 1.6f),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round))
        drawArc(black, startAngle = 200f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(cx + eyeSpacing - eyeW * 1.1f, eyeY - eyeH * 1.2f),
            size = Size(eyeW * 2.2f, eyeH * 1.6f),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round))

        // Eyelashes
        drawLine(black, Offset(cx - eyeSpacing - eyeW * 0.9f, eyeY - eyeH * 0.7f),
            Offset(cx - eyeSpacing - eyeW * 1.1f, eyeY - eyeH * 1.2f), strokeWidth = 1.5f, cap = StrokeCap.Round)
        drawLine(black, Offset(cx + eyeSpacing + eyeW * 0.9f, eyeY - eyeH * 0.7f),
            Offset(cx + eyeSpacing + eyeW * 1.1f, eyeY - eyeH * 1.2f), strokeWidth = 1.5f, cap = StrokeCap.Round)

        // ─── Eyebrows (subtle, through bangs) ───────────────────────
        drawLine(hairShadow, Offset(cx - eyeSpacing - eyeW * 0.4f, eyeY - eyeH * 1.6f),
            Offset(cx - eyeSpacing + eyeW * 0.5f, eyeY - eyeH * 1.7f), strokeWidth = 2f, cap = StrokeCap.Round)
        drawLine(hairShadow, Offset(cx + eyeSpacing - eyeW * 0.5f, eyeY - eyeH * 1.7f),
            Offset(cx + eyeSpacing + eyeW * 0.4f, eyeY - eyeH * 1.6f), strokeWidth = 2f, cap = StrokeCap.Round)

        // ─── Nose (tiny mark) ───────────────────────────────────────
        drawCircle(skinShadow, radius = w * 0.011f, center = Offset(cx, eyeY + eyeH * 1.2f))

        // ─── Mouth (small closed mouth, slightly pouty) ─────────────
        drawLine(Color(0xFFD08080), Offset(cx - w * 0.025f, eyeY + eyeH * 1.8f),
            Offset(cx + w * 0.025f, eyeY + eyeH * 1.8f), strokeWidth = 2f, cap = StrokeCap.Round)
        // Slight curve down (pouty)
        drawArc(
            Color(0xFFD08080),
            startAngle = 0f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(cx - w * 0.02f, eyeY + eyeH * 1.75f),
            size = Size(w * 0.04f, h * 0.015f),
            style = Stroke(width = 1.5f, cap = StrokeCap.Round)
        )

        // ─── Blush (hatching lines style) ───────────────────────────
        val blushY = eyeY + eyeH * 0.7f
        // Left blush — diagonal lines
        for (i in 0..3) {
            val bx = cx - eyeSpacing - w * 0.04f + i * w * 0.022f
            drawLine(blush.copy(alpha = 0.45f),
                Offset(bx, blushY - h * 0.012f),
                Offset(bx + w * 0.01f, blushY + h * 0.012f),
                strokeWidth = 1.5f, cap = StrokeCap.Round)
        }
        // Right blush
        for (i in 0..3) {
            val bx = cx + eyeSpacing - w * 0.01f + i * w * 0.022f
            drawLine(blush.copy(alpha = 0.45f),
                Offset(bx, blushY - h * 0.012f),
                Offset(bx + w * 0.01f, blushY + h * 0.012f),
                strokeWidth = 1.5f, cap = StrokeCap.Round)
        }

        // ─── Sparkles ───────────────────────────────────────────────
        drawSparkle(w * 0.10f, h * 0.12f, w * 0.014f, Color(0xFFFFD700).copy(alpha = 0.7f))
        drawSparkle(w * 0.90f, h * 0.18f, w * 0.012f, Color(0xFF90C0E8).copy(alpha = 0.6f))
        drawSparkle(w * 0.86f, h * 0.06f, w * 0.010f, Color(0xFFFFD700).copy(alpha = 0.5f))
    }
}

private fun DrawScope.drawAnimeEye(
    cx: Float, cy: Float, w: Float, h: Float,
    irisColor: Color, irisLight: Color, irisDark: Color,
    white: Color, black: Color
) {
    // Eye white (slightly tall oval)
    drawOval(white, topLeft = Offset(cx - w, cy - h), size = Size(w * 2, h * 2))

    // Iris — large relative to eye
    val irisR = w * 0.75f
    drawCircle(irisColor, radius = irisR, center = Offset(cx, cy + h * 0.08f))

    // Iris ring
    drawCircle(irisDark, radius = irisR, center = Offset(cx, cy + h * 0.08f),
        style = Stroke(width = 1.5f))

    // Inner gradient: lighter center top
    drawCircle(irisLight, radius = irisR * 0.5f, center = Offset(cx - w * 0.08f, cy - h * 0.05f))

    // Pupil
    drawCircle(black, radius = w * 0.3f, center = Offset(cx, cy + h * 0.12f))

    // Main highlight (large, upper-left)
    drawCircle(white, radius = w * 0.25f, center = Offset(cx - w * 0.22f, cy - h * 0.3f))

    // Secondary highlight (small, lower-right)
    drawCircle(white, radius = w * 0.10f, center = Offset(cx + w * 0.25f, cy + h * 0.25f))

    // Tiny third highlight
    drawCircle(white.copy(alpha = 0.7f), radius = w * 0.06f,
        center = Offset(cx - w * 0.35f, cy + h * 0.15f))
}

private fun DrawScope.drawRibbonTie(cx: Float, cy: Float, size: Float, color: Color, dark: Color) {
    // Left ribbon wing
    val left = Path().apply {
        moveTo(cx, cy)
        cubicTo(cx - size * 1.2f, cy - size * 0.8f,
            cx - size * 1.5f, cy + size * 0.2f,
            cx - size * 0.3f, cy + size * 0.5f)
        close()
    }
    drawPath(left, color)

    // Right ribbon wing
    val right = Path().apply {
        moveTo(cx, cy)
        cubicTo(cx + size * 0.8f, cy - size * 0.6f,
            cx + size * 1.2f, cy + size * 0.4f,
            cx + size * 0.2f, cy + size * 0.5f)
        close()
    }
    drawPath(right, color)

    // Trailing ribbon tails
    val tailLeft = Path().apply {
        moveTo(cx - size * 0.2f, cy + size * 0.3f)
        cubicTo(cx - size * 0.8f, cy + size * 1.2f,
            cx - size * 0.4f, cy + size * 1.5f,
            cx - size * 0.5f, cy + size * 1.8f)
    }
    drawPath(tailLeft, dark, style = Stroke(width = size * 0.2f, cap = StrokeCap.Round))

    val tailRight = Path().apply {
        moveTo(cx + size * 0.1f, cy + size * 0.3f)
        cubicTo(cx + size * 0.5f, cy + size * 1.0f,
            cx + size * 0.3f, cy + size * 1.5f,
            cx + size * 0.4f, cy + size * 1.8f)
    }
    drawPath(tailRight, dark, style = Stroke(width = size * 0.2f, cap = StrokeCap.Round))

    // Center knot
    drawCircle(dark, radius = size * 0.2f, center = Offset(cx, cy + size * 0.1f))
}

private fun DrawScope.drawSparkle(cx: Float, cy: Float, r: Float, color: Color) {
    drawLine(color, Offset(cx, cy - r * 1.5f), Offset(cx, cy + r * 1.5f), strokeWidth = 1.5f, cap = StrokeCap.Round)
    drawLine(color, Offset(cx - r * 1.5f, cy), Offset(cx + r * 1.5f, cy), strokeWidth = 1.5f, cap = StrokeCap.Round)
    drawLine(color, Offset(cx - r, cy - r), Offset(cx + r, cy + r), strokeWidth = 1f, cap = StrokeCap.Round)
    drawLine(color, Offset(cx + r, cy - r), Offset(cx - r, cy + r), strokeWidth = 1f, cap = StrokeCap.Round)
}
