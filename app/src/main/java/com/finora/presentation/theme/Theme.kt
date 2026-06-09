package com.finora.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.finora.domain.model.AccentColor

/** Extra semantic colors not covered by Material's [androidx.compose.material3.ColorScheme]. */
data class FinoraColors(
    val income: Color,
    val expense: Color,
    val cardElevated: Color
)

val LocalFinoraColors = staticCompositionLocalOf {
    FinoraColors(income = IncomeGreen, expense = ExpenseRed, cardElevated = LightSurface)
}

private fun lightColors(accent: AccentColor) = lightColorScheme(
    primary = Color(accent.seed),
    onPrimary = Color.White,
    primaryContainer = Color(accent.container),
    onPrimaryContainer = Color(accent.seedDark),
    secondary = IncomeGreen,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = ExpenseRed
)

private fun darkColors(accent: AccentColor) = darkColorScheme(
    primary = Color(accent.seed),
    onPrimary = Color.White,
    primaryContainer = Color(accent.seedDark),
    onPrimaryContainer = Color.White,
    secondary = IncomeGreen,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariantSolid,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = ExpenseRed
)

@Composable
fun FinoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: AccentColor = AccentColor.VIOLET,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColors(accent) else lightColors(accent)
    val finoraColors = FinoraColors(
        income = IncomeGreen,
        expense = ExpenseRed,
        cardElevated = if (darkTheme) DarkSurfaceVariantSolid else LightSurface
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = colorScheme.background.luminance() > 0.5f
            controller.isAppearanceLightNavigationBars = colorScheme.background.luminance() > 0.5f
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalFinoraColors provides finoraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FinoraTypography,
            shapes = FinoraShapes,
            content = content
        )
    }
}
