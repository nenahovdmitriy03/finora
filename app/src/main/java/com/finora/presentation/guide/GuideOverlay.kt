package com.finora.presentation.guide

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Guide step identifiers. */
object GuideStep {
    const val BALANCE_HERO = 0
    const val FAB = 1
    const val NAV_TRANSACTIONS = 2
    const val NAV_GOALS = 3
    const val NAV_SETTINGS = 4
    const val TOTAL = 5
}

/** Data for each guide step. */
private data class StepInfo(val title: String, val description: String)

private val steps = listOf(
    StepInfo("Общий баланс", "Здесь отображается баланс по всем вашим счетам, а также сколько денег свободно и сколько в целях."),
    StepInfo("Добавить операцию", "Нажмите «+» чтобы записать доход, расход или перевод между счетами."),
    StepInfo("Операции", "Все ваши транзакции в одном месте. Удобный поиск по категориям и датам."),
    StepInfo("Цели", "Создавайте финансовые цели, пополняйте с любого счёта и отслеживайте прогресс."),
    StepInfo("Настройки", "Темы, цвет акцента, управление счетами и аккаунтом — всё здесь.")
)

/**
 * Shared state for the guide overlay.
 * UI elements register their bounds via [registerTarget] + [guideTarget] modifier.
 */
@Stable
class GuideController {
    var isActive by mutableStateOf(false)
    var currentStep by mutableIntStateOf(0)
        private set
    private val _targets = mutableStateMapOf<Int, Rect>()

    fun registerTarget(step: Int, bounds: Rect) {
        _targets[step] = bounds
    }

    fun targetFor(step: Int): Rect? = _targets[step]

    fun next() {
        if (currentStep < GuideStep.TOTAL - 1) currentStep++
        else finish()
    }

    fun finish() {
        isActive = false
        currentStep = 0
    }

    fun start() {
        currentStep = 0
        isActive = true
    }
}

/** Modifier extension to register a guide target on an element. */
fun Modifier.guideTarget(controller: GuideController?, step: Int): Modifier {
    if (controller == null) return this
    return this.onGloballyPositioned { coords ->
        controller.registerTarget(step, coords.boundsInRoot())
    }
}

val LocalGuideController = staticCompositionLocalOf<GuideController?> { null }

/**
 * Full-screen overlay with spotlight cutout, mascot, and speech bubble.
 * Place this on top of the main UI (last child in a Box).
 */
@Composable
fun GuideOverlay(
    controller: GuideController,
    onFinish: () -> Unit
) {
    val step = controller.currentStep
    val target = controller.targetFor(step)
    val density = LocalDensity.current
    val stepInfo = steps.getOrNull(step) ?: return

    // Mascot floating animation
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    // Fade-in animation
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
            ) { /* block touches behind */ }
    ) {
        // Dimmed overlay with spotlight cutout
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            // Full scrim
            drawRect(Color.Black.copy(alpha = 0.72f))

            // Cutout for current target
            if (target != null && target != Rect.Zero) {
                val padding = 10f
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(target.left - padding, target.top - padding),
                    size = Size(target.width + padding * 2, target.height + padding * 2),
                    cornerRadius = CornerRadius(20f, 20f),
                    blendMode = BlendMode.Clear
                )
                // Glowing border around cutout
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.4f),
                    topLeft = Offset(target.left - padding, target.top - padding),
                    size = Size(target.width + padding * 2, target.height + padding * 2),
                    cornerRadius = CornerRadius(20f, 20f),
                    style = Stroke(width = 2f)
                )
            }
        }

        // Position the tooltip + mascot relative to the target
        val isBelowCenter = target != null && target.center.y < with(density) { 400.dp.toPx() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(if (isBelowCenter) Alignment.BottomCenter else Alignment.TopCenter)
                .padding(
                    top = if (!isBelowCenter && target != null) with(density) {
                        (target.bottom + 20f).toDp().coerceAtMost(200.dp)
                    } else 0.dp,
                    bottom = if (isBelowCenter) 32.dp else 0.dp,
                    start = 24.dp,
                    end = 24.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isBelowCenter) {
                Spacer(Modifier.height(16.dp))
            }

            // Mascot
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, floatOffset.roundToInt()) }
            ) {
                MascotCharacter(modifier = Modifier.size(72.dp))
            }

            Spacer(Modifier.height(12.dp))

            // Speech bubble
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = stepInfo.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A2E)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stepInfo.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B6B7B),
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Step counter
                        Text(
                            text = "${step + 1} / ${GuideStep.TOTAL}",
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
                                    if (step < GuideStep.TOTAL - 1) {
                                        controller.next()
                                    } else {
                                        controller.finish()
                                        onFinish()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6C5CE7)
                                )
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

/**
 * Cute coin mascot drawn with Canvas.
 * A friendly golden coin with expressive eyes and a smile.
 */
@Composable
fun MascotCharacter(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = size.minDimension / 2 - 4f

        // Shadow
        drawCircle(
            color = Color.Black.copy(alpha = 0.12f),
            radius = radius,
            center = Offset(cx + 2f, cy + 3f)
        )

        // Coin body — golden gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFE082), Color(0xFFFFC107), Color(0xFFF9A825)),
                center = Offset(cx - radius * 0.25f, cy - radius * 0.25f),
                radius = radius * 1.6f
            ),
            radius = radius,
            center = Offset(cx, cy)
        )

        // Outer edge ring
        drawCircle(
            color = Color(0xFFE09800),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 3f)
        )

        // Inner decorative ring
        drawCircle(
            color = Color(0xFFE09800).copy(alpha = 0.25f),
            radius = radius * 0.78f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f)
        )

        // ─ Eyes ─
        val eyeY = cy - radius * 0.1f
        val eyeSpacing = radius * 0.32f

        // Left eye
        drawCircle(Color.White, radius = radius * 0.2f, center = Offset(cx - eyeSpacing, eyeY))
        drawCircle(Color(0xFF2D2D2D), radius = radius * 0.11f, center = Offset(cx - eyeSpacing + 1f, eyeY + 1f))
        drawCircle(Color.White, radius = radius * 0.045f, center = Offset(cx - eyeSpacing - 2.5f, eyeY - 3.5f))

        // Right eye
        drawCircle(Color.White, radius = radius * 0.2f, center = Offset(cx + eyeSpacing, eyeY))
        drawCircle(Color(0xFF2D2D2D), radius = radius * 0.11f, center = Offset(cx + eyeSpacing + 1f, eyeY + 1f))
        drawCircle(Color.White, radius = radius * 0.045f, center = Offset(cx + eyeSpacing - 2.5f, eyeY - 3.5f))

        // ─ Smile ─
        drawArc(
            color = Color(0xFF5D4037),
            startAngle = 15f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(cx - radius * 0.25f, cy + radius * 0.08f),
            size = Size(radius * 0.5f, radius * 0.28f),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )

        // ─ Blush spots ─
        drawCircle(Color(0xFFFF8A65).copy(alpha = 0.25f), radius = radius * 0.1f,
            center = Offset(cx - eyeSpacing - radius * 0.12f, cy + radius * 0.15f))
        drawCircle(Color(0xFFFF8A65).copy(alpha = 0.25f), radius = radius * 0.1f,
            center = Offset(cx + eyeSpacing + radius * 0.12f, cy + radius * 0.15f))

        // ─ Shine highlight ─
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = radius * 0.12f,
            center = Offset(cx - radius * 0.35f, cy - radius * 0.45f)
        )

        // ─ ₽ symbol on forehead ─
        val symX = cx
        val symY = cy - radius * 0.45f
        val symSize = radius * 0.18f
        // Vertical line of ₽
        drawLine(Color(0xFFE09800).copy(alpha = 0.5f), Offset(symX - symSize * 0.3f, symY - symSize),
            Offset(symX - symSize * 0.3f, symY + symSize), strokeWidth = 2f, cap = StrokeCap.Round)
        // Top arc of ₽
        drawArc(
            color = Color(0xFFE09800).copy(alpha = 0.5f),
            startAngle = -90f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(symX - symSize * 0.3f, symY - symSize),
            size = Size(symSize, symSize),
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )
        // Horizontal bars of ₽
        drawLine(Color(0xFFE09800).copy(alpha = 0.5f), Offset(symX - symSize * 0.6f, symY + symSize * 0.1f),
            Offset(symX + symSize * 0.3f, symY + symSize * 0.1f), strokeWidth = 1.5f, cap = StrokeCap.Round)
    }
}
