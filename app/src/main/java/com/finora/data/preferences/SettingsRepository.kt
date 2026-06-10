package com.finora.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.finora.domain.model.AccentColor
import com.finora.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "finora_settings")

/** Persists user preferences (theme mode + accent color) via Jetpack DataStore. */
class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val themeKey = stringPreferencesKey("theme_mode")
    private val accentKey = stringPreferencesKey("accent_color")
    private val aiSessionKey = stringPreferencesKey("ai_chat_session")

    val themeMode: Flow<ThemeMode> = appContext.dataStore.data.map { prefs ->
        prefs[themeKey]?.let { stored -> runCatching { ThemeMode.valueOf(stored) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    val accentColor: Flow<AccentColor> = appContext.dataStore.data.map { prefs ->
        prefs[accentKey]?.let { stored -> runCatching { AccentColor.valueOf(stored) }.getOrNull() }
            ?: AccentColor.VIOLET
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        appContext.dataStore.edit { prefs -> prefs[themeKey] = mode.name }
    }

    suspend fun setAccentColor(accent: AccentColor) {
        appContext.dataStore.edit { prefs -> prefs[accentKey] = accent.name }
    }

    /** Persisted AI chat session (JSON), so the conversation survives app restarts. */
    val aiChatSession: Flow<String> = appContext.dataStore.data.map { prefs ->
        prefs[aiSessionKey].orEmpty()
    }

    suspend fun setAiChatSession(json: String) {
        appContext.dataStore.edit { prefs -> prefs[aiSessionKey] = json }
    }

    suspend fun clearAiChatSession() {
        appContext.dataStore.edit { prefs -> prefs.remove(aiSessionKey) }
    }
}
