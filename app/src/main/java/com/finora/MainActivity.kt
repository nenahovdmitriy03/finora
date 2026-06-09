package com.finora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finora.domain.model.AccentColor
import com.finora.domain.model.ThemeMode
import com.finora.presentation.navigation.FinoraNavHost
import com.finora.presentation.theme.FinoraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val settings = (application as FinoraApp).container.settings
        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val accent by settings.accentColor.collectAsStateWithLifecycle(initialValue = AccentColor.VIOLET)
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            FinoraTheme(darkTheme = darkTheme, accent = accent) {
                FinoraNavHost()
            }
        }
    }
}
