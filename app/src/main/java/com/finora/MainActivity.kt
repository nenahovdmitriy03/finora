package com.finora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.finora.domain.model.AccentColor
import com.finora.domain.model.ThemeMode
import com.finora.presentation.navigation.FinoraNavHost
import com.finora.presentation.theme.FinoraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val settings = (application as FinoraApp).container.settings
        val startRouteOverride = intent.getStringExtra("start_route")
        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val accent by settings.accentColor.collectAsStateWithLifecycle(initialValue = AccentColor.BLUE)
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            FinoraTheme(darkTheme = darkTheme, accent = accent) {
                FinoraNavHost(startRouteOverride = startRouteOverride)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Pay out any interest that became due while the app was in the background,
        // so capitalization appears as soon as the user opens the app (not only on
        // a cold start). Idempotent — only books periods that actually elapsed.
        val repository = (application as FinoraApp).container.repository
        lifecycleScope.launch { repository.applyInterestAccruals() }
    }
}
