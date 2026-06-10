package com.finora.presentation.onboarding

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val accentColors: List<Color>
)

private val pages = listOf(
    OnboardingPage(
        title = "Добро пожаловать\nв Finora!",
        subtitle = "Привет! Я твой финансовый помощник.\nПомогу вести учёт легко и красиво!",
        accentColors = listOf(Color(0xFF3A7BD5), Color(0xFF5B9BD5))
    ),
    OnboardingPage(
        title = "Учёт финансов\nбез лишнего",
        subtitle = "Добавляйте доходы, расходы, переводы.\nВсё наглядно: графики, категории, статистика.",
        accentColors = listOf(Color(0xFF3FB18C), Color(0xFF67D4AD))
    ),
    OnboardingPage(
        title = "Цели и\nнакопления",
        subtitle = "Ставьте финансовые цели, откладывайте\nс разных счетов, отслеживайте прогресс.",
        accentColors = listOf(Color(0xFFE07685), Color(0xFFF0A0AC))
    )
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardingPageContent(
                page = pages[page],
                pageIndex = page
            )
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.forEachIndexed { index, _ ->
                    val width by animateFloatAsState(
                        targetValue = if (index == pagerState.currentPage) 28f else 8f,
                        animationSpec = tween(300),
                        label = "dot"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(8.dp)
                            .width(width.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Main button
            Button(
                onClick = {
                    if (isLastPage) {
                        onFinish()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = pages[pagerState.currentPage].accentColors[0]
                )
            ) {
                Text(
                    if (isLastPage) "Начать" else "Далее",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(8.dp))

            // Skip button
            AnimatedVisibility(
                visible = !isLastPage,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TextButton(onClick = onFinish) {
                    Text(
                        "Пропустить",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Keep consistent spacing when skip disappears
            if (isLastPage) {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, pageIndex: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))

        // Illustration area
        Box(
            modifier = Modifier
                .size(280.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (pageIndex == 0) {
                // Mascot girl on welcome page
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    com.finora.presentation.guide.MascotGirl(
                        modifier = Modifier
                            .size(width = 160.dp, height = 220.dp)
                            .graphicsLayer { translationY = floatOffset }
                    )
                }
            } else {
                Canvas(modifier = Modifier.size(240.dp)) {
                    when (pageIndex) {
                        1 -> drawFinanceIllustration(page.accentColors, floatOffset)
                        2 -> drawGoalsIllustration(page.accentColors, floatOffset)
                    }
                }
            }
        }

        Spacer(Modifier.height(48.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

// ─── Canvas illustrations ────────────────────────────────────────────────

private fun DrawScope.drawWelcomeIllustration(colors: List<Color>, floatY: Float) {
    val cx = size.width / 2
    val cy = size.height / 2 + floatY

    // Background ring
    drawCircle(
        color = colors[1].copy(alpha = 0.12f),
        radius = size.minDimension * 0.48f,
        center = Offset(cx, cy)
    )

    // Coin body
    val coinRadius = size.minDimension * 0.3f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFD54F), Color(0xFFFFA726)),
            center = Offset(cx - coinRadius * 0.2f, cy - coinRadius * 0.2f),
            radius = coinRadius * 1.5f
        ),
        radius = coinRadius,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = Color(0xFFE09800),
        radius = coinRadius,
        center = Offset(cx, cy),
        style = Stroke(width = 3f)
    )
    drawCircle(
        color = Color(0xFFE09800).copy(alpha = 0.3f),
        radius = coinRadius * 0.78f,
        center = Offset(cx, cy),
        style = Stroke(width = 2f)
    )

    // Eyes
    val eyeY = cy - coinRadius * 0.12f
    val eyeSpacing = coinRadius * 0.3f
    listOf(cx - eyeSpacing, cx + eyeSpacing).forEach { ex ->
        drawCircle(Color.White, radius = coinRadius * 0.16f, center = Offset(ex, eyeY))
        drawCircle(Color(0xFF333333), radius = coinRadius * 0.09f, center = Offset(ex + 1f, eyeY + 1f))
        drawCircle(Color.White, radius = coinRadius * 0.04f, center = Offset(ex - 2f, eyeY - 3f))
    }

    // Smile
    drawArc(
        color = Color(0xFF5D4037),
        startAngle = 15f,
        sweepAngle = 150f,
        useCenter = false,
        topLeft = Offset(cx - coinRadius * 0.25f, cy + coinRadius * 0.05f),
        size = Size(coinRadius * 0.5f, coinRadius * 0.3f),
        style = Stroke(width = 3f, cap = StrokeCap.Round)
    )

    // Sparkles
    drawSparkle(Offset(cx + coinRadius * 1.1f, cy - coinRadius * 0.7f), 12f, colors[0])
    drawSparkle(Offset(cx - coinRadius * 0.9f, cy - coinRadius * 1.0f), 8f, colors[1])
    drawSparkle(Offset(cx + coinRadius * 0.5f, cy - coinRadius * 1.2f), 10f, Color(0xFFFFD54F))
}

private fun DrawScope.drawFinanceIllustration(colors: List<Color>, floatY: Float) {
    val cx = size.width / 2
    val cy = size.height / 2 + floatY

    // Background
    drawCircle(
        color = colors[0].copy(alpha = 0.08f),
        radius = size.minDimension * 0.48f,
        center = Offset(cx, cy)
    )

    // Chart bars
    val barWidth = size.width * 0.09f
    val baseY = cy + size.height * 0.18f
    val barData = listOf(0.4f, 0.65f, 0.35f, 0.8f, 0.55f, 0.7f, 0.45f)
    val startX = cx - (barData.size * barWidth * 1.5f) / 2

    barData.forEachIndexed { i, h ->
        val bx = startX + i * barWidth * 1.6f
        val barH = size.height * 0.3f * h
        val isHighlight = i == 3
        drawRoundRect(
            color = if (isHighlight) colors[0] else colors[0].copy(alpha = 0.25f),
            topLeft = Offset(bx, baseY - barH),
            size = Size(barWidth, barH),
            cornerRadius = CornerRadius(barWidth / 2)
        )
    }

    // Trend line
    val points = listOf(0.6f, 0.45f, 0.55f, 0.3f, 0.4f, 0.25f, 0.35f)
    val path = Path()
    points.forEachIndexed { i, p ->
        val px = startX + i * barWidth * 1.6f + barWidth / 2
        val py = baseY - size.height * 0.32f * (1f - p)
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    drawPath(path, color = Color(0xFFE07685), style = Stroke(width = 3f, cap = StrokeCap.Round))

    // Arrow up
    val arrowX = cx + size.width * 0.25f
    val arrowY = cy - size.height * 0.22f
    drawLine(colors[0], Offset(arrowX, arrowY + 30f), Offset(arrowX, arrowY - 10f), strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(colors[0], Offset(arrowX - 10f, arrowY), Offset(arrowX, arrowY - 10f), strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(colors[0], Offset(arrowX + 10f, arrowY), Offset(arrowX, arrowY - 10f), strokeWidth = 3f, cap = StrokeCap.Round)

    // Sparkle
    drawSparkle(Offset(cx - size.width * 0.3f, cy - size.height * 0.25f), 10f, colors[1])
}

private fun DrawScope.drawGoalsIllustration(colors: List<Color>, floatY: Float) {
    val cx = size.width / 2
    val cy = size.height / 2 + floatY

    // Background
    drawCircle(
        color = colors[0].copy(alpha = 0.08f),
        radius = size.minDimension * 0.48f,
        center = Offset(cx, cy)
    )

    // Target circles
    val targetRadius = size.minDimension * 0.32f
    drawCircle(colors[0].copy(alpha = 0.15f), radius = targetRadius, center = Offset(cx, cy))
    drawCircle(colors[0].copy(alpha = 0.25f), radius = targetRadius * 0.7f, center = Offset(cx, cy))
    drawCircle(colors[0].copy(alpha = 0.5f), radius = targetRadius * 0.4f, center = Offset(cx, cy))
    drawCircle(colors[0], radius = targetRadius * 0.15f, center = Offset(cx, cy))

    // Progress arc (75% filled)
    drawArc(
        color = colors[0],
        startAngle = -90f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = Offset(cx - targetRadius, cy - targetRadius),
        size = Size(targetRadius * 2, targetRadius * 2),
        style = Stroke(width = 6f, cap = StrokeCap.Round)
    )
    drawArc(
        color = colors[0].copy(alpha = 0.15f),
        startAngle = 180f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(cx - targetRadius, cy - targetRadius),
        size = Size(targetRadius * 2, targetRadius * 2),
        style = Stroke(width = 6f, cap = StrokeCap.Round)
    )

    // Flag at top
    val flagX = cx
    val flagY = cy - targetRadius - 15f
    drawLine(Color(0xFF5D4037), Offset(flagX, flagY), Offset(flagX, flagY - 28f), strokeWidth = 2.5f, cap = StrokeCap.Round)
    val flagPath = Path().apply {
        moveTo(flagX, flagY - 28f)
        lineTo(flagX + 18f, flagY - 22f)
        lineTo(flagX, flagY - 16f)
        close()
    }
    drawPath(flagPath, color = colors[0])

    // Stars
    drawSparkle(Offset(cx + targetRadius * 1.1f, cy - targetRadius * 0.5f), 10f, colors[1])
    drawSparkle(Offset(cx - targetRadius * 0.8f, cy - targetRadius * 0.9f), 8f, Color(0xFFFFD54F))
    drawSparkle(Offset(cx + targetRadius * 0.6f, cy + targetRadius * 0.9f), 6f, colors[0])
}

private fun DrawScope.drawSparkle(center: Offset, armLen: Float, color: Color) {
    for (i in 0 until 4) {
        val angle = i * 45f * (PI.toFloat() / 180f)
        val dx = cos(angle) * armLen
        val dy = sin(angle) * armLen
        drawLine(
            color = color,
            start = Offset(center.x - dx * 0.3f, center.y - dy * 0.3f),
            end = Offset(center.x + dx, center.y + dy),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}
