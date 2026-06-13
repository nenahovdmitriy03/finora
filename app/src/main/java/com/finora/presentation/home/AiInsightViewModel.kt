package com.finora.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.data.ai.AiProviders
import com.finora.data.preferences.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiInsightUiState(
    val configured: Boolean = true,
    val dailyInsight: String? = null,
    val dailyInsightDate: String? = null
)

/**
 * Drives the "AI-аналитика" card on Home.
 * Reads the cached daily insight produced by [DailyInsightWorker].
 */
class AiInsightViewModel(
    private val settings: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        AiInsightUiState(configured = AiProviders.configured().isNotEmpty())
    )
    val state: StateFlow<AiInsightUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val insight = settings.dailyInsight.first()
            val date = settings.dailyInsightDate.first()
            if (insight.isNotBlank()) {
                _state.update {
                    it.copy(dailyInsight = insight, dailyInsightDate = date)
                }
            }
        }
    }
}
