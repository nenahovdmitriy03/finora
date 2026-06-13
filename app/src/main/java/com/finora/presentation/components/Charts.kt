package com.finora.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
 * An animated donut chart with tap-to-select interaction.
 *
 * Improvements over the previous minimal version:
 * - Smooth arc-sweep animation on first draw / data change
 * - Tap a slice to select it (pops outward slightly)
 * - The [onSliceSelected] callback reports which slice was tapped (-1 = deselected)
 * - Customisable gap, stroke width, and selection offset
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    strokeWidth: Dp = 26.dp,
    trackColor: Color = Color(0x14000000),
    onSliceSelected: (Int) -> Unit = {},
    center: @Composable () -> Unit = {}
) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // Animate the sweep multiplier from 0 → 1
    val sweepAnim = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        sweepAnim.snapTo(0f)
        sweepAnim.animateTo(
            1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    // Precompute slice angles for hit testing
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

                        // Check if tap is on the donut ring
                        if (dist < radius - sw / 2f || dist > radius + sw / 2f) {
                            selectedIndex = -1
                            onSliceSelected(-1)
                            return@detectTapGestures
                        }

                        // Compute angle in degrees from 12 o'clock (top)
                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        // Canvas starts at 3 o'clock; our arcs start at -90° (12 o'clock)
                        // So adjust: angle from canvas 0°, then compare to startAngle
                        val gap = if (sliceAngles.size > 1) 4f else 0f

                        for ((i, pair) in sliceAngles.withIndex()) {
                            val (start, sweep) = pair
                            val s = start + gap / 2f
                            val e = s + (sweep - gap).coerceAtLeast(0.5f)
                            if (angleInRange(angle, s, e)) {
                                selectedIndex = if (selectedIndex == i) -1 else i
                                onSliceSelected(selectedIndex)
                                return@detectTapGestures
                            }
                        }
                        selectedIndex = -1
                        onSliceSelected(-1)
                    }
                }
        ) {
            val sw = strokeWidth.toPx()
            val selectionOffset = sw * 0.25f
            val inset = sw / 2f + selectionOffset
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f

            // Background track
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )

            if (total <= 0f || sliceAngles.isEmpty()) return@Canvas

            val gap = if (sliceAngles.size > 1) 4f else 0f
            val animProgress = sweepAnim.value

            sliceAngles.forEachIndexed { i, (startAngle, sweep) ->
                val animatedSweep = (sweep * animProgress)
                if (animatedSweep <= 0f) return@forEachIndexed

                val isSelected = (i == selectedIndex)
                val currentSw = if (isSelected) sw * 1.15f else sw

                // Pop selected slice outward
                val offset = if (isSelected) {
                    val midAngle = Math.toRadians((startAngle + sweep / 2f).toDouble())
                    Offset(
                        (cos(midAngle) * selectionOffset).toFloat(),
                        (sin(midAngle) * selectionOffset).toFloat()
                    )
                } else Offset.Zero

                val sliceTopLeft = topLeft + offset
                drawArc(
                    color = if (isSelected) slices[i].color
                            else slices[i].color.copy(alpha = if (selectedIndex >= 0) 0.5f else 1f),
                    startAngle = startAngle + gap / 2f,
                    sweepAngle = (animatedSweep - gap).coerceAtLeast(0.5f),
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

/** Check if [angle] falls within the arc from [start] to [start + sweep] (in degrees). */
private fun angleInRange(angle: Float, start: Float, end: Float): Boolean {
    // Normalize all angles to 0..360
    fun norm(a: Float): Float { var r = a % 360f; if (r < 0) r += 360f; return r }
    val a = norm(angle)
    val s = norm(start)
    val e = norm(end)
    return if (s <= e) a in s..e else (a >= s || a <= e)
}

data class NetBar(val label: String, val net: Float)

/**
 * Net cash-flow per period (income − expense) drawn as bars around a zero baseline.
 * Up bars (surplus) use [positiveColor], down bars (deficit) use [negativeColor] —
 * dynamics are shown by direction, not by red/green.
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

        // zero baseline
        drawRoundRect(
            color = baselineColor,
            topLeft = Offset(0f, zeroY - 1.dp.toPx()),
            size = Size(this.size.width, 2.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
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
