package com.finora.presentation.home

import androidx.lifecycle.ViewModel
import com.finora.data.ai.AiProviders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AiInsightUiState(
    val configured: Boolean = true
)

/**
 * Drives the "AI-аналитика" card on Home.
 * Simply reports whether any AI provider is configured.
 */
class AiInsightViewModel : ViewModel() {

    private val _state = MutableStateFlow(
        AiInsightUiState(configured = AiProviders.configured().isNotEmpty())
    )
    val state: StateFlow<AiInsightUiState> = _state.asStateFlow()
}
