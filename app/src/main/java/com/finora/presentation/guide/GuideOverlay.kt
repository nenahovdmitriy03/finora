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

import androidx.compose.ui.graphics.drawscope.DrawScope
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
                MascotGirl(modifier = Modifier.size(width = 90.dp, height = 130.dp))
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

// ─── Anime girl mascot «Алия» — full-body, realistic proportions ─────────────

/**
 * Full-body anime girl mascot with realistic proportions (~6.5 heads tall):
 * - Silver/lavender long hair with ahoge + red ribbon hair ties
 * - Big blue eyes with detailed highlights
 * - White shirt + dark charcoal vest + beige/cream blazer
 * - Large red bow tie, gold buttons
 * - Dark pleated skirt with white stripe at hem
 * - White thigh-high stockings
 * - Dark brown penny loafers
 * All drawn with Canvas — no external assets.
 */
@Composable
fun MascotGirl(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // ─── Colors ──────────────────────────────────────────────────
        val hairSilver = Color(0xFFD5CDD8)
        val hairLight = Color(0xFFEAE4EE)
        val hairShadow = Color(0xFFC0B4C8)
        val hairTips = Color(0xFFE0D4E0)
        val ribbonRed = Color(0xFFC82040)
        val ribbonDark = Color(0xFFA01830)

        val skin = Color(0xFFFFE4D4)
        val skinShadow = Color(0xFFF8D4BC)
        val blush = Color(0xFFFFB0C0)

        val eyeBlue = Color(0xFF50A0E0)
        val eyeLight = Color(0xFF80C8F8)
        val eyeDark = Color(0xFF2868B0)
        val white = Color.White
        val black = Color(0xFF2A2A2A)
        val outline = Color(0xFF484050)

        val vestDark = Color(0xFF3C4048)
        val vestEdge = Color(0xFF50545C)
        val shirtWhite = Color(0xFFF2EEF0)
        val collarWhite = Color(0xFFFCF8FA)
        val blazerBeige = Color(0xFFE8DDD0)
        val blazerShadow = Color(0xFFD8CCBC)
        val blazerLight = Color(0xFFF0E8DC)
        val bowRed = Color(0xFFC02038)
        val bowDarkRed = Color(0xFFA01828)

        val skirtDark = Color(0xFF383C44)
        val skirtLight = Color(0xFF484C54)
        val skirtTrim = Color(0xFFD4B870)     // gold trim
        val skirtStripe = Color(0xFFE8E4E0)   // white stripe near hem

        val sockWhite = Color(0xFFF8F6F4)
        val sockShadow = Color(0xFFE8E4E0)
        val shoeBrown = Color(0xFF5C4030)
        val shoeDark = Color(0xFF3C2820)
        val shoeLight = Color(0xFF7C5A48)

        // ─── Proportions (realistic: ~6.5 heads tall) ───────────────
        val headCy = h * 0.095f
        val headRx = w * 0.135f
        val headRy = h * 0.072f
        val chinY = headCy + headRy * 0.92f
        val neckBot = h * 0.195f
        val shoulderY = h * 0.205f
        val shoulderW = w * 0.22f     // half-width from center to shoulder edge
        val bustY = h * 0.27f
        val waistY = h * 0.35f
        val hipY = h * 0.38f
        val skirtEnd = h * 0.49f
        val thighSkinEnd = h * 0.525f // tiny strip of skin between skirt and stocking
        val stockTop = h * 0.525f
        val kneeY = h * 0.62f
        val ankleY = h * 0.895f
        val shoeTop = h * 0.89f
        val shoeBot = h * 0.96f

        val legSpacing = w * 0.058f   // half-distance between leg centers

        // ─── 1. Hair back (long, flowing past hips) ─────────────────
        val hairBackPath = Path().apply {
            moveTo(cx - headRx - w * 0.04f, headCy - headRy * 0.1f)
            cubicTo(
                cx - headRx - w * 0.08f, headCy + headRy * 2f,
                cx - w * 0.26f, waistY,
                cx - w * 0.18f, skirtEnd + h * 0.06f
            )
            cubicTo(cx - w * 0.14f, skirtEnd + h * 0.14f,
                cx - w * 0.06f, skirtEnd + h * 0.16f,
                cx, skirtEnd + h * 0.18f)
            cubicTo(cx + w * 0.06f, skirtEnd + h * 0.16f,
                cx + w * 0.14f, skirtEnd + h * 0.14f,
                cx + w * 0.18f, skirtEnd + h * 0.06f)
            cubicTo(
                cx + w * 0.26f, waistY,
                cx + headRx + w * 0.08f, headCy + headRy * 2f,
                cx + headRx + w * 0.04f, headCy - headRy * 0.1f
            )
            close()
        }
        drawPath(hairBackPath, hairShadow)
        // Strand highlights
        for (i in -2..2) {
            val sx = cx + i * w * 0.055f
            drawLine(hairTips,
                Offset(sx, shoulderY + h * 0.04f),
                Offset(sx + w * 0.01f * i, skirtEnd + h * 0.10f),
                strokeWidth = 1.2f, cap = StrokeCap.Round)
        }

        // ─── 2. Legs (skin) ────────────────────────────────────────
        // Left leg
        drawPath(Path().apply {
            moveTo(cx - legSpacing - w * 0.046f, skirtEnd - h * 0.01f)
            cubicTo(cx - legSpacing - w * 0.050f, kneeY,
                cx - legSpacing - w * 0.042f, kneeY + h * 0.08f,
                cx - legSpacing - w * 0.038f, ankleY)
            lineTo(cx - legSpacing + w * 0.038f, ankleY)
            cubicTo(cx - legSpacing + w * 0.042f, kneeY + h * 0.08f,
                cx - legSpacing + w * 0.046f, kneeY,
                cx - legSpacing + w * 0.042f, skirtEnd - h * 0.01f)
            close()
        }, skin)
        // Right leg
        drawPath(Path().apply {
            moveTo(cx + legSpacing - w * 0.042f, skirtEnd - h * 0.01f)
            cubicTo(cx + legSpacing - w * 0.046f, kneeY,
                cx + legSpacing - w * 0.042f, kneeY + h * 0.08f,
                cx + legSpacing - w * 0.038f, ankleY)
            lineTo(cx + legSpacing + w * 0.038f, ankleY)
            cubicTo(cx + legSpacing + w * 0.042f, kneeY + h * 0.08f,
                cx + legSpacing + w * 0.050f, kneeY,
                cx + legSpacing + w * 0.046f, skirtEnd - h * 0.01f)
            close()
        }, skin)

        // ─── 3. Thigh-high stockings ────────────────────────────────
        // Left stocking
        drawPath(Path().apply {
            moveTo(cx - legSpacing - w * 0.048f, stockTop)
            cubicTo(cx - legSpacing - w * 0.050f, kneeY,
                cx - legSpacing - w * 0.042f, kneeY + h * 0.08f,
                cx - legSpacing - w * 0.038f, ankleY)
            lineTo(cx - legSpacing + w * 0.038f, ankleY)
            cubicTo(cx - legSpacing + w * 0.042f, kneeY + h * 0.08f,
                cx - legSpacing + w * 0.046f, kneeY,
                cx - legSpacing + w * 0.044f, stockTop)
            close()
        }, sockWhite)
        // Right stocking
        drawPath(Path().apply {
            moveTo(cx + legSpacing - w * 0.044f, stockTop)
            cubicTo(cx + legSpacing - w * 0.046f, kneeY,
                cx + legSpacing - w * 0.042f, kneeY + h * 0.08f,
                cx + legSpacing - w * 0.038f, ankleY)
            lineTo(cx + legSpacing + w * 0.038f, ankleY)
            cubicTo(cx + legSpacing + w * 0.042f, kneeY + h * 0.08f,
                cx + legSpacing + w * 0.050f, kneeY,
                cx + legSpacing + w * 0.048f, stockTop)
            close()
        }, sockWhite)
        // Stocking top bands
        drawLine(sockShadow,
            Offset(cx - legSpacing - w * 0.048f, stockTop),
            Offset(cx - legSpacing + w * 0.044f, stockTop), strokeWidth = 2f, cap = StrokeCap.Round)
        drawLine(sockShadow,
            Offset(cx + legSpacing - w * 0.044f, stockTop),
            Offset(cx + legSpacing + w * 0.048f, stockTop), strokeWidth = 2f, cap = StrokeCap.Round)
        // Inner shadow on stockings
        drawLine(sockShadow,
            Offset(cx - legSpacing + w * 0.02f, stockTop + h * 0.03f),
            Offset(cx - legSpacing + w * 0.015f, ankleY - h * 0.02f),
            strokeWidth = 1.5f, cap = StrokeCap.Round)
        drawLine(sockShadow,
            Offset(cx + legSpacing - w * 0.02f, stockTop + h * 0.03f),
            Offset(cx + legSpacing - w * 0.015f, ankleY - h * 0.02f),
            strokeWidth = 1.5f, cap = StrokeCap.Round)

        // ─── 4. Shoes (dark brown loafers) ──────────────────────────
        // Left shoe
        drawPath(Path().apply {
            moveTo(cx - legSpacing - w * 0.05f, shoeTop)
            cubicTo(cx - legSpacing - w * 0.065f, shoeBot,
                cx - legSpacing + w * 0.055f, shoeBot,
                cx - legSpacing + w * 0.045f, shoeTop)
            close()
        }, shoeBrown)
        // Shoe opening detail
        drawArc(shoeLight, 0f, 180f, useCenter = false,
            topLeft = Offset(cx - legSpacing - w * 0.030f, shoeTop - h * 0.006f),
            size = Size(w * 0.058f, h * 0.018f),
            style = Stroke(width = 1.2f, cap = StrokeCap.Round))
        // Sole
        drawLine(shoeDark,
            Offset(cx - legSpacing - w * 0.055f, shoeBot - h * 0.005f),
            Offset(cx - legSpacing + w * 0.050f, shoeBot - h * 0.005f),
            strokeWidth = 2.5f, cap = StrokeCap.Round)

        // Right shoe
        drawPath(Path().apply {
            moveTo(cx + legSpacing - w * 0.045f, shoeTop)
            cubicTo(cx + legSpacing - w * 0.055f, shoeBot,
                cx + legSpacing + w * 0.065f, shoeBot,
                cx + legSpacing + w * 0.05f, shoeTop)
            close()
        }, shoeBrown)
        drawArc(shoeLight, 0f, 180f, useCenter = false,
            topLeft = Offset(cx + legSpacing - w * 0.028f, shoeTop - h * 0.006f),
            size = Size(w * 0.058f, h * 0.018f),
            style = Stroke(width = 1.2f, cap = StrokeCap.Round))
        drawLine(shoeDark,
            Offset(cx + legSpacing - w * 0.050f, shoeBot - h * 0.005f),
            Offset(cx + legSpacing + w * 0.055f, shoeBot - h * 0.005f),
            strokeWidth = 2.5f, cap = StrokeCap.Round)

        // ─── 5. Torso — white shirt base ────────────────────────────
        drawPath(Path().apply {
            moveTo(cx - shoulderW, shoulderY)
            cubicTo(cx - shoulderW - w * 0.02f, bustY,
                cx - w * 0.14f, waistY,
                cx - w * 0.10f, hipY)
            lineTo(cx + w * 0.10f, hipY)
            cubicTo(cx + w * 0.14f, waistY,
                cx + shoulderW + w * 0.02f, bustY,
                cx + shoulderW, shoulderY)
            close()
        }, shirtWhite)

        // ─── 6. Vest (dark charcoal, over shirt) ────────────────────
        drawPath(Path().apply {
            moveTo(cx - w * 0.14f, shoulderY + h * 0.015f)
            cubicTo(cx - w * 0.16f, bustY,
                cx - w * 0.12f, waistY,
                cx - w * 0.09f, hipY - h * 0.005f)
            lineTo(cx + w * 0.09f, hipY - h * 0.005f)
            cubicTo(cx + w * 0.12f, waistY,
                cx + w * 0.16f, bustY,
                cx + w * 0.14f, shoulderY + h * 0.015f)
            close()
        }, vestDark)

        // Vest V-neckline revealing shirt
        drawPath(Path().apply {
            moveTo(cx - w * 0.08f, shoulderY + h * 0.015f)
            lineTo(cx, bustY + h * 0.02f)
            lineTo(cx + w * 0.08f, shoulderY + h * 0.015f)
            close()
        }, shirtWhite)
        // V-neckline edges
        drawLine(vestEdge, Offset(cx - w * 0.08f, shoulderY + h * 0.015f),
            Offset(cx, bustY + h * 0.02f), strokeWidth = 1f, cap = StrokeCap.Round)
        drawLine(vestEdge, Offset(cx + w * 0.08f, shoulderY + h * 0.015f),
            Offset(cx, bustY + h * 0.02f), strokeWidth = 1f, cap = StrokeCap.Round)

        // Gold buttons on vest
        val btn1Y = bustY + h * 0.005f
        val btn2Y = bustY + h * 0.04f
        val btn3Y = bustY + h * 0.075f
        drawCircle(Color(0xFFD0A448), radius = w * 0.010f, center = Offset(cx - w * 0.025f, btn1Y))
        drawCircle(Color(0xFFD0A448), radius = w * 0.010f, center = Offset(cx - w * 0.020f, btn2Y))
        drawCircle(Color(0xFFD0A448), radius = w * 0.010f, center = Offset(cx + w * 0.025f, btn1Y))
        drawCircle(Color(0xFFD0A448), radius = w * 0.010f, center = Offset(cx + w * 0.020f, btn2Y))

        // ─── 7. Blazer / jacket (beige, open front) ────────────────
        // Left blazer panel
        drawPath(Path().apply {
            moveTo(cx - shoulderW, shoulderY)
            cubicTo(cx - shoulderW - w * 0.02f, bustY,
                cx - w * 0.16f, waistY,
                cx - w * 0.12f, hipY)
            lineTo(cx - w * 0.06f, hipY)
            cubicTo(cx - w * 0.08f, waistY,
                cx - w * 0.10f, bustY,
                cx - w * 0.08f, shoulderY + h * 0.01f)
            close()
        }, blazerBeige)
        // Right blazer panel
        drawPath(Path().apply {
            moveTo(cx + shoulderW, shoulderY)
            cubicTo(cx + shoulderW + w * 0.02f, bustY,
                cx + w * 0.16f, waistY,
                cx + w * 0.12f, hipY)
            lineTo(cx + w * 0.06f, hipY)
            cubicTo(cx + w * 0.08f, waistY,
                cx + w * 0.10f, bustY,
                cx + w * 0.08f, shoulderY + h * 0.01f)
            close()
        }, blazerBeige)
        // Blazer lapel shadows
        drawLine(blazerShadow,
            Offset(cx - w * 0.06f, shoulderY + h * 0.015f),
            Offset(cx - w * 0.06f, hipY), strokeWidth = 1f, cap = StrokeCap.Round)
        drawLine(blazerShadow,
            Offset(cx + w * 0.06f, shoulderY + h * 0.015f),
            Offset(cx + w * 0.06f, hipY), strokeWidth = 1f, cap = StrokeCap.Round)

        // ─── 8. Collar (white, over blazer) ─────────────────────────
        // Left collar flap
        drawPath(Path().apply {
            moveTo(cx - w * 0.10f, shoulderY - h * 0.005f)
            lineTo(cx - w * 0.05f, shoulderY + h * 0.04f)
            lineTo(cx - w * 0.01f, shoulderY - h * 0.002f)
            close()
        }, collarWhite)
        // Red edge on collar
        drawLine(ribbonRed,
            Offset(cx - w * 0.10f, shoulderY - h * 0.005f),
            Offset(cx - w * 0.05f, shoulderY + h * 0.04f),
            strokeWidth = 1f, cap = StrokeCap.Round)
        // Right collar flap
        drawPath(Path().apply {
            moveTo(cx + w * 0.10f, shoulderY - h * 0.005f)
            lineTo(cx + w * 0.05f, shoulderY + h * 0.04f)
            lineTo(cx + w * 0.01f, shoulderY - h * 0.002f)
            close()
        }, collarWhite)
        drawLine(ribbonRed,
            Offset(cx + w * 0.10f, shoulderY - h * 0.005f),
            Offset(cx + w * 0.05f, shoulderY + h * 0.04f),
            strokeWidth = 1f, cap = StrokeCap.Round)

        // ─── 9. Red bow tie ────────────────────────────────────────
        val bowY = shoulderY + h * 0.022f
        // Left wing
        drawPath(Path().apply {
            moveTo(cx, bowY)
            cubicTo(cx - w * 0.065f, bowY - h * 0.020f,
                cx - w * 0.07f, bowY + h * 0.022f,
                cx - w * 0.005f, bowY + h * 0.012f)
            close()
        }, bowRed)
        // Right wing
        drawPath(Path().apply {
            moveTo(cx, bowY)
            cubicTo(cx + w * 0.065f, bowY - h * 0.020f,
                cx + w * 0.07f, bowY + h * 0.022f,
                cx + w * 0.005f, bowY + h * 0.012f)
            close()
        }, bowRed)
        // Knot center
        drawCircle(bowDarkRed, radius = w * 0.012f, center = Offset(cx, bowY + h * 0.005f))
        // Ribbon tails
        drawPath(Path().apply {
            moveTo(cx - w * 0.005f, bowY + h * 0.012f)
            cubicTo(cx - w * 0.02f, bowY + h * 0.03f,
                cx - w * 0.01f, bowY + h * 0.04f,
                cx - w * 0.015f, bowY + h * 0.05f)
        }, bowDarkRed, style = Stroke(width = 1.5f, cap = StrokeCap.Round))
        drawPath(Path().apply {
            moveTo(cx + w * 0.005f, bowY + h * 0.012f)
            cubicTo(cx + w * 0.02f, bowY + h * 0.03f,
                cx + w * 0.01f, bowY + h * 0.04f,
                cx + w * 0.015f, bowY + h * 0.05f)
        }, bowDarkRed, style = Stroke(width = 1.5f, cap = StrokeCap.Round))

        // ─── 10. Skirt (dark pleated, white stripe near hem) ────────
        drawPath(Path().apply {
            moveTo(cx - w * 0.11f, hipY - h * 0.008f)
            cubicTo(cx - w * 0.18f, hipY + h * 0.02f,
                cx - w * 0.20f, skirtEnd - h * 0.02f,
                cx - w * 0.17f, skirtEnd)
            lineTo(cx + w * 0.17f, skirtEnd)
            cubicTo(cx + w * 0.20f, skirtEnd - h * 0.02f,
                cx + w * 0.18f, hipY + h * 0.02f,
                cx + w * 0.11f, hipY - h * 0.008f)
            close()
        }, skirtDark)
        // Pleat lines
        for (i in -3..3) {
            val px = cx + i * w * 0.038f
            drawLine(skirtLight, Offset(px, hipY + h * 0.005f),
                Offset(px + i * w * 0.005f, skirtEnd - h * 0.003f),
                strokeWidth = 0.8f, cap = StrokeCap.Round)
        }
        // White stripe near hem
        drawLine(skirtStripe,
            Offset(cx - w * 0.17f, skirtEnd - h * 0.015f),
            Offset(cx + w * 0.17f, skirtEnd - h * 0.015f),
            strokeWidth = 2f, cap = StrokeCap.Round)
        // Gold trim above stripe
        drawLine(skirtTrim,
            Offset(cx - w * 0.17f, skirtEnd - h * 0.022f),
            Offset(cx + w * 0.17f, skirtEnd - h * 0.022f),
            strokeWidth = 1f, cap = StrokeCap.Round)
        // Waistband
        drawRect(vestEdge,
            topLeft = Offset(cx - w * 0.115f, hipY - h * 0.012f),
            size = Size(w * 0.23f, h * 0.013f))

        // ─── 11. Arms with blazer sleeves ───────────────────────────
        // Left arm — resting at side, hand near hip
        drawPath(Path().apply {
            moveTo(cx - shoulderW, shoulderY + h * 0.005f)
            cubicTo(cx - shoulderW - w * 0.04f, shoulderY + h * 0.05f,
                cx - w * 0.28f, bustY + h * 0.02f,
                cx - w * 0.24f, waistY + h * 0.03f)
            lineTo(cx - w * 0.20f, waistY + h * 0.03f)
            cubicTo(cx - w * 0.22f, bustY + h * 0.02f,
                cx - shoulderW - w * 0.01f, shoulderY + h * 0.05f,
                cx - shoulderW + w * 0.04f, shoulderY + h * 0.005f)
            close()
        }, blazerBeige)
        // Blazer sleeve shadow
        drawLine(blazerShadow,
            Offset(cx - shoulderW - w * 0.01f, shoulderY + h * 0.03f),
            Offset(cx - w * 0.23f, waistY), strokeWidth = 1f, cap = StrokeCap.Round)
        // Gold cuff
        drawLine(Color(0xFFD0A448),
            Offset(cx - w * 0.245f, waistY + h * 0.020f),
            Offset(cx - w * 0.195f, waistY + h * 0.020f),
            strokeWidth = 1.5f, cap = StrokeCap.Round)
        // Left hand (skin)
        drawOval(skin,
            topLeft = Offset(cx - w * 0.25f, waistY + h * 0.025f),
            size = Size(w * 0.055f, h * 0.028f))

        // Right arm — raised slightly, hand near chest
        drawPath(Path().apply {
            moveTo(cx + shoulderW, shoulderY + h * 0.005f)
            cubicTo(cx + shoulderW + w * 0.03f, shoulderY + h * 0.03f,
                cx + w * 0.26f, shoulderY + h * 0.06f,
                cx + w * 0.20f, bustY - h * 0.01f)
            cubicTo(cx + w * 0.16f, bustY + h * 0.02f,
                cx + w * 0.10f, bustY + h * 0.01f,
                cx + w * 0.08f, bustY - h * 0.005f)
            lineTo(cx + w * 0.12f, shoulderY + h * 0.02f)
            cubicTo(cx + shoulderW - w * 0.02f, shoulderY + h * 0.015f,
                cx + shoulderW, shoulderY + h * 0.01f,
                cx + shoulderW - w * 0.04f, shoulderY + h * 0.005f)
            close()
        }, blazerBeige)
        // Right hand near chest
        drawOval(skin,
            topLeft = Offset(cx + w * 0.06f, bustY - h * 0.018f),
            size = Size(w * 0.050f, h * 0.028f))
        // Fingers hint
        drawLine(skinShadow,
            Offset(cx + w * 0.075f, bustY - h * 0.014f),
            Offset(cx + w * 0.070f, bustY + h * 0.004f),
            strokeWidth = 0.8f, cap = StrokeCap.Round)
        drawLine(skinShadow,
            Offset(cx + w * 0.088f, bustY - h * 0.014f),
            Offset(cx + w * 0.085f, bustY + h * 0.006f),
            strokeWidth = 0.8f, cap = StrokeCap.Round)

        // ─── 12. Neck ──────────────────────────────────────────────
        drawPath(Path().apply {
            moveTo(cx - w * 0.038f, chinY)
            cubicTo(cx - w * 0.042f, chinY + h * 0.02f,
                cx - w * 0.04f, neckBot,
                cx - w * 0.05f, neckBot + h * 0.005f)
            lineTo(cx + w * 0.05f, neckBot + h * 0.005f)
            cubicTo(cx + w * 0.04f, neckBot,
                cx + w * 0.042f, chinY + h * 0.02f,
                cx + w * 0.038f, chinY)
            close()
        }, skin)
        // Neck shadow
        drawLine(skinShadow,
            Offset(cx + w * 0.01f, chinY + h * 0.005f),
            Offset(cx + w * 0.02f, neckBot), strokeWidth = 1f, cap = StrokeCap.Round)

        // ─── 13. Head (face) ────────────────────────────────────────
        drawOval(skin,
            topLeft = Offset(cx - headRx, headCy - headRy),
            size = Size(headRx * 2, headRy * 2))
        // Chin shadow
        drawArc(skinShadow, startAngle = 20f, sweepAngle = 140f, useCenter = true,
            topLeft = Offset(cx - headRx * 0.5f, headCy + headRy * 0.55f),
            size = Size(headRx, headRy * 0.35f))

        // ─── 14. Hair front — bangs, side strands ───────────────────

        // Hair top volume
        drawPath(Path().apply {
            moveTo(cx - headRx - w * 0.05f, headCy - headRy * 0.1f)
            cubicTo(
                cx - headRx * 0.5f, headCy - headRy - h * 0.06f,
                cx + headRx * 0.5f, headCy - headRy - h * 0.06f,
                cx + headRx + w * 0.05f, headCy - headRy * 0.1f
            )
            cubicTo(
                cx + headRx * 0.3f, headCy - headRy * 0.6f,
                cx - headRx * 0.3f, headCy - headRy * 0.6f,
                cx - headRx - w * 0.05f, headCy - headRy * 0.1f
            )
            close()
        }, hairSilver)

        // Ahoge (cute antenna strand)
        drawPath(Path().apply {
            moveTo(cx - w * 0.01f, headCy - headRy - h * 0.025f)
            cubicTo(
                cx + w * 0.01f, headCy - headRy - h * 0.07f,
                cx + w * 0.05f, headCy - headRy - h * 0.06f,
                cx + w * 0.03f, headCy - headRy - h * 0.03f
            )
        }, hairSilver, style = Stroke(width = w * 0.014f, cap = StrokeCap.Round))

        // Hair highlight arc
        drawPath(Path().apply {
            moveTo(cx - w * 0.03f, headCy - headRy - h * 0.02f)
            cubicTo(cx, headCy - headRy - h * 0.04f,
                cx + w * 0.05f, headCy - headRy - h * 0.025f,
                cx + w * 0.03f, headCy - headRy * 0.4f)
        }, hairLight, style = Stroke(width = w * 0.018f, cap = StrokeCap.Round))

        // Bangs — soft jagged fringe
        drawPath(Path().apply {
            moveTo(cx - headRx * 0.92f, headCy - headRy * 0.15f)
            lineTo(cx - headRx * 0.62f, headCy + headRy * 0.18f)
            lineTo(cx - headRx * 0.38f, headCy + headRy * 0.02f)
            lineTo(cx - headRx * 0.12f, headCy + headRy * 0.22f)
            lineTo(cx + headRx * 0.12f, headCy + headRy * 0.06f)
            lineTo(cx + headRx * 0.38f, headCy + headRy * 0.20f)
            lineTo(cx + headRx * 0.60f, headCy - headRy * 0.02f)
            lineTo(cx + headRx * 0.80f, headCy + headRy * 0.12f)
            lineTo(cx + headRx * 0.92f, headCy - headRy * 0.15f)
            cubicTo(
                cx + headRx * 0.5f, headCy - headRy - h * 0.04f,
                cx - headRx * 0.5f, headCy - headRy - h * 0.04f,
                cx - headRx * 0.92f, headCy - headRy * 0.15f
            )
            close()
        }, hairSilver)

        // Bang highlights
        drawLine(hairLight,
            Offset(cx - headRx * 0.48f, headCy - headRy * 0.3f),
            Offset(cx - headRx * 0.50f, headCy + headRy * 0.10f),
            strokeWidth = 1.3f, cap = StrokeCap.Round)
        drawLine(hairLight,
            Offset(cx + headRx * 0.25f, headCy - headRy * 0.25f),
            Offset(cx + headRx * 0.22f, headCy + headRy * 0.12f),
            strokeWidth = 1.3f, cap = StrokeCap.Round)

        // Left side hair (flowing over shoulder, in front of blazer)
        drawPath(Path().apply {
            moveTo(cx - headRx * 0.88f, headCy - headRy * 0.05f)
            cubicTo(cx - headRx - w * 0.06f, headCy + headRy * 0.6f,
                cx - headRx - w * 0.05f, shoulderY + h * 0.06f,
                cx - headRx + w * 0.02f, waistY)
            // Tapered ends
            cubicTo(cx - headRx + w * 0.03f, waistY - h * 0.01f,
                cx - headRx + w * 0.06f, shoulderY + h * 0.02f,
                cx - headRx + w * 0.08f, headCy + headRy * 0.3f)
            close()
        }, hairSilver)

        // Right side hair
        drawPath(Path().apply {
            moveTo(cx + headRx * 0.88f, headCy - headRy * 0.05f)
            cubicTo(cx + headRx + w * 0.06f, headCy + headRy * 0.6f,
                cx + headRx + w * 0.05f, shoulderY + h * 0.06f,
                cx + headRx - w * 0.02f, waistY)
            cubicTo(cx + headRx - w * 0.03f, waistY - h * 0.01f,
                cx + headRx - w * 0.06f, shoulderY + h * 0.02f,
                cx + headRx - w * 0.08f, headCy + headRy * 0.3f)
            close()
        }, hairSilver)

        // ─── 15. Red ribbon hair ties ───────────────────────────────
        val ribbonY = headCy + headRy * 0.12f
        drawRibbonTie(cx - headRx * 0.82f, ribbonY, w * 0.035f, ribbonRed, ribbonDark)
        drawRibbonTie(cx + headRx * 0.82f, ribbonY, w * 0.035f, ribbonRed, ribbonDark)

        // ─── 16. Eyes (big, detailed anime eyes) ────────────────────
        val eyeY = headCy + headRy * 0.12f
        val eyeSpacing = headRx * 0.38f
        val eyeW = w * 0.055f
        val eyeH = h * 0.032f

        drawAnimeEye(cx - eyeSpacing, eyeY, eyeW, eyeH, eyeBlue, eyeLight, eyeDark, white, black)
        drawAnimeEye(cx + eyeSpacing, eyeY, eyeW, eyeH, eyeBlue, eyeLight, eyeDark, white, black)

        // Upper eyelid arcs (thick, expressive)
        drawArc(outline, startAngle = 200f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(cx - eyeSpacing - eyeW * 1.15f, eyeY - eyeH * 1.4f),
            size = Size(eyeW * 2.3f, eyeH * 1.7f),
            style = Stroke(width = 2f, cap = StrokeCap.Round))
        drawArc(outline, startAngle = 200f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(cx + eyeSpacing - eyeW * 1.15f, eyeY - eyeH * 1.4f),
            size = Size(eyeW * 2.3f, eyeH * 1.7f),
            style = Stroke(width = 2f, cap = StrokeCap.Round))

        // Eyelashes (two per eye)
        drawLine(outline,
            Offset(cx - eyeSpacing - eyeW * 0.9f, eyeY - eyeH * 0.8f),
            Offset(cx - eyeSpacing - eyeW * 1.15f, eyeY - eyeH * 1.5f),
            strokeWidth = 1.3f, cap = StrokeCap.Round)
        drawLine(outline,
            Offset(cx - eyeSpacing - eyeW * 0.6f, eyeY - eyeH * 1.0f),
            Offset(cx - eyeSpacing - eyeW * 0.85f, eyeY - eyeH * 1.6f),
            strokeWidth = 1f, cap = StrokeCap.Round)
        drawLine(outline,
            Offset(cx + eyeSpacing + eyeW * 0.9f, eyeY - eyeH * 0.8f),
            Offset(cx + eyeSpacing + eyeW * 1.15f, eyeY - eyeH * 1.5f),
            strokeWidth = 1.3f, cap = StrokeCap.Round)
        drawLine(outline,
            Offset(cx + eyeSpacing + eyeW * 0.6f, eyeY - eyeH * 1.0f),
            Offset(cx + eyeSpacing + eyeW * 0.85f, eyeY - eyeH * 1.6f),
            strokeWidth = 1f, cap = StrokeCap.Round)

        // Eyebrows (thin, slightly arched, visible through bangs)
        drawPath(Path().apply {
            moveTo(cx - eyeSpacing - eyeW * 0.5f, eyeY - eyeH * 2.2f)
            quadraticTo(cx - eyeSpacing, eyeY - eyeH * 2.6f,
                cx - eyeSpacing + eyeW * 0.5f, eyeY - eyeH * 2.3f)
        }, hairShadow, style = Stroke(width = 1.5f, cap = StrokeCap.Round))
        drawPath(Path().apply {
            moveTo(cx + eyeSpacing - eyeW * 0.5f, eyeY - eyeH * 2.3f)
            quadraticTo(cx + eyeSpacing, eyeY - eyeH * 2.6f,
                cx + eyeSpacing + eyeW * 0.5f, eyeY - eyeH * 2.2f)
        }, hairShadow, style = Stroke(width = 1.5f, cap = StrokeCap.Round))

        // ─── 17. Nose (tiny L-shape mark) ───────────────────────────
        drawLine(skinShadow,
            Offset(cx, eyeY + eyeH * 1.5f),
            Offset(cx - w * 0.006f, eyeY + eyeH * 2.0f),
            strokeWidth = 1f, cap = StrokeCap.Round)

        // ─── 18. Mouth (small, gentle) ──────────────────────────────
        drawArc(
            Color(0xFFD88080), startAngle = 5f, sweepAngle = 170f, useCenter = false,
            topLeft = Offset(cx - w * 0.018f, eyeY + eyeH * 2.7f),
            size = Size(w * 0.036f, h * 0.010f),
            style = Stroke(width = 1.2f, cap = StrokeCap.Round)
        )

        // ─── 19. Blush (soft diagonal lines) ────────────────────────
        val blushY = eyeY + eyeH * 1.0f
        for (i in 0..2) {
            val bx = cx - eyeSpacing - w * 0.025f + i * w * 0.015f
            drawLine(blush.copy(alpha = 0.35f),
                Offset(bx, blushY - h * 0.005f),
                Offset(bx + w * 0.005f, blushY + h * 0.005f),
                strokeWidth = 1.2f, cap = StrokeCap.Round)
        }
        for (i in 0..2) {
            val bx = cx + eyeSpacing - w * 0.005f + i * w * 0.015f
            drawLine(blush.copy(alpha = 0.35f),
                Offset(bx, blushY - h * 0.005f),
                Offset(bx + w * 0.005f, blushY + h * 0.005f),
                strokeWidth = 1.2f, cap = StrokeCap.Round)
        }

        // ─── 20. Sparkles ───────────────────────────────────────────
        drawSparkle(w * 0.08f, h * 0.04f, w * 0.010f, Color(0xFFFFD700).copy(alpha = 0.6f))
        drawSparkle(w * 0.92f, h * 0.08f, w * 0.008f, Color(0xFF90C0E8).copy(alpha = 0.5f))
        drawSparkle(w * 0.06f, h * 0.35f, w * 0.007f, Color(0xFFFFD700).copy(alpha = 0.4f))
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
    val irisR = w * 0.78f
    drawCircle(irisColor, radius = irisR, center = Offset(cx, cy + h * 0.1f))
    drawCircle(irisDark, radius = irisR, center = Offset(cx, cy + h * 0.1f),
        style = Stroke(width = 1.2f))
    // Inner lighter area
    drawCircle(irisLight, radius = irisR * 0.45f, center = Offset(cx - w * 0.1f, cy - h * 0.08f))
    // Pupil
    drawCircle(black, radius = w * 0.30f, center = Offset(cx, cy + h * 0.15f))
    // Main highlight (large, upper-left)
    drawCircle(white, radius = w * 0.24f, center = Offset(cx - w * 0.22f, cy - h * 0.3f))
    // Secondary highlight (small, lower-right)
    drawCircle(white, radius = w * 0.10f, center = Offset(cx + w * 0.24f, cy + h * 0.25f))
    // Tiny third highlight
    drawCircle(white.copy(alpha = 0.6f), radius = w * 0.06f,
        center = Offset(cx - w * 0.34f, cy + h * 0.15f))
}

private fun DrawScope.drawRibbonTie(cx: Float, cy: Float, size: Float, color: Color, dark: Color) {
    // Left wing
    drawPath(Path().apply {
        moveTo(cx, cy)
        cubicTo(cx - size * 1.3f, cy - size * 0.9f,
            cx - size * 1.6f, cy + size * 0.3f,
            cx - size * 0.3f, cy + size * 0.5f)
        close()
    }, color)
    // Right wing
    drawPath(Path().apply {
        moveTo(cx, cy)
        cubicTo(cx + size * 0.9f, cy - size * 0.7f,
            cx + size * 1.3f, cy + size * 0.4f,
            cx + size * 0.2f, cy + size * 0.5f)
        close()
    }, color)
    // Trailing tails
    drawPath(Path().apply {
        moveTo(cx - size * 0.2f, cy + size * 0.3f)
        cubicTo(cx - size * 0.8f, cy + size * 1.2f,
            cx - size * 0.4f, cy + size * 1.6f,
            cx - size * 0.5f, cy + size * 2.0f)
    }, dark, style = Stroke(width = size * 0.18f, cap = StrokeCap.Round))
    drawPath(Path().apply {
        moveTo(cx + size * 0.1f, cy + size * 0.3f)
        cubicTo(cx + size * 0.5f, cy + size * 1.0f,
            cx + size * 0.3f, cy + size * 1.6f,
            cx + size * 0.4f, cy + size * 2.0f)
    }, dark, style = Stroke(width = size * 0.18f, cap = StrokeCap.Round))
    // Center knot
    drawCircle(dark, radius = size * 0.22f, center = Offset(cx, cy + size * 0.12f))
}

private fun DrawScope.drawSparkle(cx: Float, cy: Float, r: Float, color: Color) {
    drawLine(color, Offset(cx, cy - r * 1.5f), Offset(cx, cy + r * 1.5f), strokeWidth = 1.5f, cap = StrokeCap.Round)
    drawLine(color, Offset(cx - r * 1.5f, cy), Offset(cx + r * 1.5f, cy), strokeWidth = 1.5f, cap = StrokeCap.Round)
    drawLine(color, Offset(cx - r, cy - r), Offset(cx + r, cy + r), strokeWidth = 1f, cap = StrokeCap.Round)
    drawLine(color, Offset(cx + r, cy - r), Offset(cx - r, cy + r), strokeWidth = 1f, cap = StrokeCap.Round)
}
