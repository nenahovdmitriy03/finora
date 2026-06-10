package com.finora.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class DonutSlice(val value: Float, val color: Color)

/** A minimalist donut chart. Slices are drawn clockwise with small gaps. */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    strokeWidth: Dp = 26.dp,
    trackColor: Color = Color(0x14000000),
    center: @Composable () -> Unit = {}
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        val total = slices.sumOf { it.value.toDouble() }.toFloat()
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2f
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            // background track
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )

            if (total <= 0f) return@Canvas
            var startAngle = -90f
            val gap = if (slices.size > 1) 4f else 0f
            slices.forEach { slice ->
                val sweep = (slice.value / total) * 360f
                if (sweep > 0f) {
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle + gap / 2f,
                        sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )
                }
                startAngle += sweep
            }
        }
        center()
    }
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
