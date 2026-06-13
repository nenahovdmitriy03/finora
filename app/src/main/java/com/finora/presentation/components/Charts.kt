package com.finora.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class DonutSlice(val value: Float, val color: Color)

/**
 * A modern animated donut chart with tap-to-select, gradient arcs and smooth spring animation.
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 28.dp,
    trackColor: Color = Color(0x0A000000),
    onSliceSelected: (Int) -> Unit = {},
    center: @Composable () -> Unit = {}
) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // Animate sweep from 0→1
    val sweepAnim = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        sweepAnim.snapTo(0f)
        sweepAnim.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }

    // Animate selection pop
    val selectionScale by animateFloatAsState(
        targetValue = if (selectedIndex >= 0) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "selectionScale"
    )

    val sliceAngles = remember(slices, total) {
        if (total <= 0f) emptyList()
        else {
            var start = -90f
            slices.map { slice ->
                val sweep = (slice.value / total) * 360f
                val result = start to sweep
                start += sweep
                result
            }
        }
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(sliceAngles) {
                    if (sliceAngles.isEmpty()) return@pointerInput
                    detectTapGestures { offset ->
                        val cx = this.size.width / 2f
                        val cy = this.size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val dist = sqrt(dx * dx + dy * dy)
                        val sw = strokeWidth.toPx()
                        val radius = (this.size.width - sw) / 2f

                        if (dist < radius - sw * 0.8f || dist > radius + sw * 0.8f) {
                            selectedIndex = -1; onSliceSelected(-1); return@detectTapGestures
                        }

                        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        val gap = if (sliceAngles.size > 1) 3f else 0f
                        for ((i, pair) in sliceAngles.withIndex()) {
                            val (start, sweep) = pair
                            val s = start + gap / 2f
                            val e = s + (sweep - gap).coerceAtLeast(0.5f)
                            if (angleInRange(angle, s, e)) {
                                selectedIndex = if (selectedIndex == i) -1 else i
                                onSliceSelected(selectedIndex); return@detectTapGestures
                            }
                        }
                        selectedIndex = -1; onSliceSelected(-1)
                    }
                }
        ) {
            val sw = strokeWidth.toPx()
            val selectionOffset = sw * 0.3f
            val inset = sw / 2f + selectionOffset
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            // Background track
            drawArc(
                color = trackColor,
                startAngle = 0f, sweepAngle = 360f,
                useCenter = false, topLeft = topLeft, size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )

            if (total <= 0f || sliceAngles.isEmpty()) return@Canvas

            val gap = if (sliceAngles.size > 1) 3f else 0f
            val animProgress = sweepAnim.value

            sliceAngles.forEachIndexed { i, (startAngle, sweep) ->
                val animatedSweep = sweep * animProgress
                if (animatedSweep <= 0f) return@forEachIndexed

                val isSelected = (i == selectedIndex)
                val currentSw = if (isSelected) sw * 1.2f else sw

                // Pop outward on selection
                val offset = if (isSelected) {
                    val midAngle = Math.toRadians((startAngle + sweep / 2f).toDouble())
                    Offset(
                        (cos(midAngle) * selectionOffset * selectionScale).toFloat(),
                        (sin(midAngle) * selectionOffset * selectionScale).toFloat()
                    )
                } else Offset.Zero

                val sliceColor = slices[i].color
                val alpha = when {
                    isSelected -> 1f
                    selectedIndex >= 0 -> 0.35f
                    else -> 1f
                }

                // Gradient arc: slightly lighter at start → full color at end
                val arcStart = startAngle + gap / 2f
                val arcSweep = (animatedSweep - gap).coerceAtLeast(0.5f)

                val sliceTopLeft = topLeft + offset
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            sliceColor.copy(alpha = alpha * 0.7f),
                            sliceColor.copy(alpha = alpha),
                            sliceColor.copy(alpha = alpha)
                        )
                    ),
                    startAngle = arcStart,
                    sweepAngle = arcSweep,
                    useCenter = false,
                    topLeft = sliceTopLeft,
                    size = arcSize,
                    style = Stroke(width = currentSw, cap = StrokeCap.Round)
                )
            }
        }
        center()
    }
}

private fun angleInRange(angle: Float, start: Float, end: Float): Boolean {
    fun norm(a: Float): Float { var r = a % 360f; if (r < 0) r += 360f; return r }
    val a = norm(angle); val s = norm(start); val e = norm(end)
    return if (s <= e) a in s..e else (a >= s || a <= e)
}

data class NetBar(val label: String, val net: Float)

/**
 * Net cash-flow per period drawn as rounded bars around a zero baseline.
 */
@Composable
fun NetTrendChart(
    bars: List<NetBar>,
    positiveColor: Color,
    negativeColor: Color,
    baselineColor: Color,
    modifier: Modifier = Modifier
) {
    val maxAbs = bars.maxOfOrNull { kotlin.math.abs(it.net) }?.takeIf { it > 0f } ?: 1f
    Canvas(modifier = modifier) {
        if (bars.isEmpty()) return@Canvas
        val groupWidth = this.size.width / bars.size
        val barWidth = (groupWidth * 0.42f).coerceAtMost(26f.dp.toPx())
        val radius = barWidth / 2f
        val zeroY = this.size.height / 2f
        val halfSpan = this.size.height / 2f - 8f

        drawRoundRect(
            color = baselineColor,
            topLeft = Offset(0f, zeroY - 0.5.dp.toPx()),
            size = Size(this.size.width, 1.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(0.5.dp.toPx(), 0.5.dp.toPx())
        )

        bars.forEachIndexed { index, bar ->
            val center = groupWidth * index + groupWidth / 2f
            val x = center - barWidth / 2f
            val h = (kotlin.math.abs(bar.net) / maxAbs) * halfSpan
            val barH = h.coerceAtLeast(barWidth * 0.5f)
            if (bar.net >= 0f) {
                drawRoundRect(
                    color = positiveColor,
                    topLeft = Offset(x, zeroY - barH),
                    size = Size(barWidth, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                )
            } else {
                drawRoundRect(
                    color = negativeColor,
                    topLeft = Offset(x, zeroY),
                    size = Size(barWidth, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                )
            }
        }
    }
}
