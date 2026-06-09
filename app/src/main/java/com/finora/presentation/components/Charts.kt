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

data class TrendBar(val label: String, val income: Float, val expense: Float)

/** Grouped income/expense bars over time. Rendered as rounded vertical bars. */
@Composable
fun TrendChart(
    bars: List<TrendBar>,
    incomeColor: Color,
    expenseColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp
) {
    val max = bars.maxOfOrNull { maxOf(it.income, it.expense) }?.takeIf { it > 0f } ?: 1f
    Canvas(modifier = modifier) {
        if (bars.isEmpty()) return@Canvas
        val groupWidth = this.size.width / bars.size
        val barWidth = (groupWidth * 0.26f).coerceAtMost(22f.dp.toPx())
        val gap = barWidth * 0.35f
        val baseY = this.size.height
        val radius = barWidth / 2f

        bars.forEachIndexed { index, bar ->
            val groupCenter = groupWidth * index + groupWidth / 2f
            val incomeHeight = (bar.income / max) * (this.size.height - 8f)
            val expenseHeight = (bar.expense / max) * (this.size.height - 8f)

            val incomeX = groupCenter - barWidth - gap / 2f
            val expenseX = groupCenter + gap / 2f

            drawRoundedBar(incomeX, baseY, barWidth, incomeHeight, radius, incomeColor)
            drawRoundedBar(expenseX, baseY, barWidth, expenseHeight, radius, expenseColor)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundedBar(
    x: Float,
    baseY: Float,
    width: Float,
    barHeight: Float,
    radius: Float,
    color: Color
) {
    val h = barHeight.coerceAtLeast(width * 0.6f)
    drawRoundRect(
        color = color,
        topLeft = Offset(x, baseY - h),
        size = Size(width, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
    )
}
