package com.finora.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.finora.domain.model.AccentColor
import com.finora.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "finora_settings")

/** Persists user preferences (theme mode + accent color) via Jetpack DataStore. */
class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val themeKey = stringPreferencesKey("theme_mode")
    private val accentKey = stringPreferencesKey("accent_color")
    private val aiSessionKey = stringPreferencesKey("ai_chat_session")
    /** The user id whose data currently lives in the local Room DB. */
    private val dataOwnerKey = stringPreferencesKey("data_owner_id")

    // ─── Onboarding / guide flags ────────────────────────────────────────
    private val authSkippedKey = booleanPreferencesKey("auth_skipped")
    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
    private val guideCompletedKey = booleanPreferencesKey("guide_completed")

    val themeMode: Flow<ThemeMode> = appContext.dataStore.data.map { prefs ->
        prefs[themeKey]?.let { stored -> runCatching { ThemeMode.valueOf(stored) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    val accentColor: Flow<AccentColor> = appContext.dataStore.data.map { prefs ->
        prefs[accentKey]?.let { stored -> runCatching { AccentColor.valueOf(stored) }.getOrNull() }
            ?: AccentColor.BLUE
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

    // ─── Local data owner (multi-account isolation) ──────────────────────

    /**
     * Returns the user id whose data is currently stored in local Room,
     * or `null` if the data is "unclaimed" (e.g. a user browsing without an
     * account). Used to decide whether to wipe local data when a *different*
     * user logs in, so accounts don't leak data into each other.
     */
    suspend fun dataOwnerId(): String? =
        appContext.dataStore.data.first()[dataOwnerKey]

    suspend fun setDataOwnerId(userId: String) {
        appContext.dataStore.edit { prefs -> prefs[dataOwnerKey] = userId }
    }

    suspend fun clearDataOwnerId() {
        appContext.dataStore.edit { prefs -> prefs.remove(dataOwnerKey) }
    }

    // ─── Auth-skip / onboarding / guide ──────────────────────────────────

    val authSkipped: Flow<Boolean> = appContext.dataStore.data.map { prefs ->
        prefs[authSkippedKey] == true
    }

    suspend fun setAuthSkipped(value: Boolean) {
        appContext.dataStore.edit { prefs -> prefs[authSkippedKey] = value }
    }

    val onboardingCompleted: Flow<Boolean> = appContext.dataStore.data.map { prefs ->
        prefs[onboardingCompletedKey] == true
    }

    suspend fun setOnboardingCompleted(value: Boolean) {
        appContext.dataStore.edit { prefs -> prefs[onboardingCompletedKey] = value }
    }

    val guideCompleted: Flow<Boolean> = appContext.dataStore.data.map { prefs ->
        prefs[guideCompletedKey] == true
    }

    suspend fun setGuideCompleted(value: Boolean) {
        appContext.dataStore.edit { prefs -> prefs[guideCompletedKey] = value }
    }

    /** Reset onboarding flags (used on account deletion). */
    suspend fun clearOnboardingFlags() {
        appContext.dataStore.edit { prefs ->
            prefs.remove(authSkippedKey)
            prefs.remove(onboardingCompletedKey)
            prefs.remove(guideCompletedKey)
        }
    }
}
