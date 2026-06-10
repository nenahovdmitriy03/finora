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
import androidx.compose.ui.graphics.drawscope.Fill
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

// ─── Anime girl mascot (chibi, blue theme) ───────────────────────────────────

/**
 * Cute chibi anime girl with blue hair, big eyes, and a blue outfit.
 * All drawn with Canvas — no external assets needed.
 */
@Composable
fun MascotGirl(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // Colors
        val hairDark = Color(0xFF2B5EA7)
        val hairLight = Color(0xFF5B9BD5)
        val hairHighlight = Color(0xFF8EC8F6)
        val skin = Color(0xFFFFE0CC)
        val skinShadow = Color(0xFFFFCDB2)
        val eyeBlue = Color(0xFF3A7BD5)
        val eyeLight = Color(0xFF6FB3F2)
        val white = Color.White
        val black = Color(0xFF2D2D2D)
        val blush = Color(0xFFFF9EB1)
        val dressBlue = Color(0xFF3B6FB5)
        val dressDark = Color(0xFF2A5494)
        val collarWhite = Color(0xFFE8F0FE)

        // ── Body / Dress ──
        val bodyTop = h * 0.62f
        val bodyPath = Path().apply {
            moveTo(cx - w * 0.18f, bodyTop)
            // Shoulders + dress shape
            cubicTo(
                cx - w * 0.28f, bodyTop + h * 0.06f,
                cx - w * 0.25f, h * 0.95f,
                cx, h * 0.97f
            )
            cubicTo(
                cx + w * 0.25f, h * 0.95f,
                cx + w * 0.28f, bodyTop + h * 0.06f,
                cx + w * 0.18f, bodyTop
            )
            close()
        }
        drawPath(bodyPath, dressDark)
        drawPath(bodyPath, dressBlue)

        // Collar / ribbon detail
        val collarPath = Path().apply {
            moveTo(cx - w * 0.1f, bodyTop + h * 0.01f)
            lineTo(cx, bodyTop + h * 0.1f)
            lineTo(cx + w * 0.1f, bodyTop + h * 0.01f)
        }
        drawPath(collarPath, collarWhite, style = Stroke(width = w * 0.025f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Small bow at collar
        drawCircle(Color(0xFFFF6B8A), radius = w * 0.025f, center = Offset(cx, bodyTop + h * 0.02f))

        // ── Neck ──
        drawRect(skin, topLeft = Offset(cx - w * 0.06f, h * 0.58f), size = Size(w * 0.12f, h * 0.08f))

        // ── Head (big chibi head) ──
        val headCy = h * 0.34f
        val headRx = w * 0.32f
        val headRy = h * 0.28f

        // Hair back (behind head)
        drawOval(hairDark, topLeft = Offset(cx - headRx - w * 0.04f, headCy - headRy - h * 0.01f),
            size = Size((headRx + w * 0.04f) * 2, (headRy + h * 0.12f) * 2))

        // Face
        drawOval(skin, topLeft = Offset(cx - headRx, headCy - headRy), size = Size(headRx * 2, headRy * 2))
        // Subtle face shadow on bottom
        drawArc(skinShadow, startAngle = 20f, sweepAngle = 140f, useCenter = true,
            topLeft = Offset(cx - headRx * 0.8f, headCy + headRy * 0.3f),
            size = Size(headRx * 1.6f, headRy * 0.6f))

        // ── Hair (bangs + sides) ──
        // Top hair volume
        val hairTopPath = Path().apply {
            moveTo(cx - headRx - w * 0.03f, headCy - headRy * 0.1f)
            cubicTo(
                cx - headRx * 0.5f, headCy - headRy - h * 0.15f,
                cx + headRx * 0.5f, headCy - headRy - h * 0.15f,
                cx + headRx + w * 0.03f, headCy - headRy * 0.1f
            )
            // Crown arc
            cubicTo(
                cx + headRx * 0.3f, headCy - headRy * 0.6f,
                cx - headRx * 0.3f, headCy - headRy * 0.6f,
                cx - headRx - w * 0.03f, headCy - headRy * 0.1f
            )
            close()
        }
        drawPath(hairTopPath, hairDark)
        // Hair highlight streak
        drawPath(Path().apply {
            moveTo(cx - w * 0.05f, headCy - headRy - h * 0.06f)
            cubicTo(cx, headCy - headRy - h * 0.1f, cx + w * 0.1f, headCy - headRy - h * 0.06f,
                cx + w * 0.05f, headCy - headRy * 0.5f)
        }, hairHighlight, style = Stroke(width = w * 0.03f, cap = StrokeCap.Round))

        // Bangs — jagged fringe
        val bangsPath = Path().apply {
            moveTo(cx - headRx * 0.95f, headCy - headRy * 0.2f)
            lineTo(cx - headRx * 0.65f, headCy + headRy * 0.15f)
            lineTo(cx - headRx * 0.4f, headCy - headRy * 0.05f)
            lineTo(cx - headRx * 0.15f, headCy + headRy * 0.2f)
            lineTo(cx + headRx * 0.1f, headCy - headRy * 0.0f)
            lineTo(cx + headRx * 0.35f, headCy + headRy * 0.15f)
            lineTo(cx + headRx * 0.6f, headCy - headRy * 0.08f)
            lineTo(cx + headRx * 0.85f, headCy + headRy * 0.1f)
            lineTo(cx + headRx * 0.95f, headCy - headRy * 0.2f)
            // Connect back over the top
            cubicTo(
                cx + headRx * 0.5f, headCy - headRy - h * 0.1f,
                cx - headRx * 0.5f, headCy - headRy - h * 0.1f,
                cx - headRx * 0.95f, headCy - headRy * 0.2f
            )
            close()
        }
        drawPath(bangsPath, hairLight)

        // Side hair strands (left)
        val leftHairPath = Path().apply {
            moveTo(cx - headRx * 0.9f, headCy - headRy * 0.1f)
            cubicTo(
                cx - headRx - w * 0.08f, headCy + headRy * 0.6f,
                cx - headRx - w * 0.04f, h * 0.7f,
                cx - headRx + w * 0.02f, h * 0.72f
            )
            lineTo(cx - headRx + w * 0.08f, headCy + headRy * 0.3f)
            close()
        }
        drawPath(leftHairPath, hairDark)

        // Side hair strands (right)
        val rightHairPath = Path().apply {
            moveTo(cx + headRx * 0.9f, headCy - headRy * 0.1f)
            cubicTo(
                cx + headRx + w * 0.08f, headCy + headRy * 0.6f,
                cx + headRx + w * 0.04f, h * 0.7f,
                cx + headRx - w * 0.02f, h * 0.72f
            )
            lineTo(cx + headRx - w * 0.08f, headCy + headRy * 0.3f)
            close()
        }
        drawPath(rightHairPath, hairDark)

        // ── Eyes ──
        val eyeY = headCy + headRy * 0.08f
        val eyeSpacing = headRx * 0.42f
        val eyeW = w * 0.09f
        val eyeH = h * 0.08f

        // Left eye
        drawEye(cx - eyeSpacing, eyeY, eyeW, eyeH, eyeBlue, eyeLight, white, black)
        // Right eye
        drawEye(cx + eyeSpacing, eyeY, eyeW, eyeH, eyeBlue, eyeLight, white, black)

        // Eyelashes (small lines above eyes)
        drawLine(black, Offset(cx - eyeSpacing - eyeW * 0.7f, eyeY - eyeH * 0.8f),
            Offset(cx - eyeSpacing - eyeW * 0.3f, eyeY - eyeH * 1.1f), strokeWidth = 1.5f, cap = StrokeCap.Round)
        drawLine(black, Offset(cx + eyeSpacing + eyeW * 0.7f, eyeY - eyeH * 0.8f),
            Offset(cx + eyeSpacing + eyeW * 0.3f, eyeY - eyeH * 1.1f), strokeWidth = 1.5f, cap = StrokeCap.Round)

        // ── Eyebrows ──
        drawLine(hairDark, Offset(cx - eyeSpacing - eyeW * 0.5f, eyeY - eyeH * 1.4f),
            Offset(cx - eyeSpacing + eyeW * 0.5f, eyeY - eyeH * 1.5f), strokeWidth = 2f, cap = StrokeCap.Round)
        drawLine(hairDark, Offset(cx + eyeSpacing - eyeW * 0.5f, eyeY - eyeH * 1.5f),
            Offset(cx + eyeSpacing + eyeW * 0.5f, eyeY - eyeH * 1.4f), strokeWidth = 2f, cap = StrokeCap.Round)

        // ── Nose (tiny dot) ──
        drawCircle(skinShadow, radius = w * 0.012f, center = Offset(cx, eyeY + eyeH * 1.1f))

        // ── Mouth (small smile) ──
        drawArc(
            Color(0xFFE57373),
            startAngle = 10f, sweepAngle = 160f, useCenter = false,
            topLeft = Offset(cx - w * 0.04f, eyeY + eyeH * 1.5f),
            size = Size(w * 0.08f, h * 0.035f),
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )

        // ── Blush circles ──
        drawCircle(blush.copy(alpha = 0.3f), radius = w * 0.05f,
            center = Offset(cx - eyeSpacing - w * 0.03f, eyeY + eyeH * 0.9f))
        drawCircle(blush.copy(alpha = 0.3f), radius = w * 0.05f,
            center = Offset(cx + eyeSpacing + w * 0.03f, eyeY + eyeH * 0.9f))

        // ── Hair accessory (star clip on left) ──
        drawStar(cx - headRx * 0.75f, headCy - headRy * 0.55f, w * 0.04f, Color(0xFFFFD700), Color(0xFFFFF176))

        // ── Sparkles around ──
        drawSparkle(w * 0.08f, h * 0.15f, w * 0.015f, Color(0xFFFFD700).copy(alpha = 0.7f))
        drawSparkle(w * 0.92f, h * 0.2f, w * 0.012f, Color(0xFF5B9BD5).copy(alpha = 0.6f))
        drawSparkle(w * 0.85f, h * 0.08f, w * 0.01f, Color(0xFFFFD700).copy(alpha = 0.5f))
    }
}

private fun DrawScope.drawEye(
    cx: Float, cy: Float, w: Float, h: Float,
    irisColor: Color, irisLight: Color, white: Color, black: Color
) {
    // Eye white (oval)
    drawOval(white, topLeft = Offset(cx - w, cy - h), size = Size(w * 2, h * 2))
    // Outer eye line
    drawOval(black, topLeft = Offset(cx - w, cy - h), size = Size(w * 2, h * 2),
        style = Stroke(width = 1.5f))
    // Iris
    drawCircle(irisColor, radius = w * 0.7f, center = Offset(cx, cy + h * 0.1f))
    // Iris gradient highlight (lighter inner)
    drawCircle(irisLight, radius = w * 0.4f, center = Offset(cx - w * 0.1f, cy))
    // Pupil
    drawCircle(black, radius = w * 0.3f, center = Offset(cx, cy + h * 0.15f))
    // Main highlight
    drawCircle(white, radius = w * 0.22f, center = Offset(cx - w * 0.2f, cy - h * 0.25f))
    // Small secondary highlight
    drawCircle(white, radius = w * 0.1f, center = Offset(cx + w * 0.25f, cy + h * 0.3f))
}

private fun DrawScope.drawStar(cx: Float, cy: Float, r: Float, outer: Color, inner: Color) {
    val path = Path()
    for (i in 0 until 10) {
        val angle = (PI / 2 + i * PI / 5).toFloat()
        val rad = if (i % 2 == 0) r else r * 0.45f
        val x = cx + cos(angle) * rad
        val y = cy - sin(angle) * rad
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, outer)
    drawCircle(inner, radius = r * 0.3f, center = Offset(cx, cy))
}

private fun DrawScope.drawSparkle(cx: Float, cy: Float, r: Float, color: Color) {
    // 4-pointed sparkle
    drawLine(color, Offset(cx, cy - r * 1.5f), Offset(cx, cy + r * 1.5f), strokeWidth = 1.5f, cap = StrokeCap.Round)
    drawLine(color, Offset(cx - r * 1.5f, cy), Offset(cx + r * 1.5f, cy), strokeWidth = 1.5f, cap = StrokeCap.Round)
    drawLine(color, Offset(cx - r, cy - r), Offset(cx + r, cy + r), strokeWidth = 1f, cap = StrokeCap.Round)
    drawLine(color, Offset(cx + r, cy - r), Offset(cx - r, cy + r), strokeWidth = 1f, cap = StrokeCap.Round)
}
