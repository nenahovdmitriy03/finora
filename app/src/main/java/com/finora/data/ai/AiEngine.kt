package com.finora.data.ai

import com.finora.BuildConfig

/** A text-generation backend (Gemini, OpenRouter, Groq, …). */
interface AiEngine {
    /** Human-readable provider name shown in the UI. */
    val label: String
    val isConfigured: Boolean
    suspend fun generate(prompt: String): String
}

/**
 * Central place that knows every supported provider and which keys are set.
 * Priority order: OpenRouter → Groq → Gemini. Add a key in local.properties to
 * enable a provider (see app/build.gradle.kts).
 */
object AiProviders {
    fun all(): List<AiEngine> = listOf(
        OpenAiCompatClient(
            apiKey = BuildConfig.OPENROUTER_API_KEY,
            baseUrl = "https://openrouter.ai/api/v1",
            model = "meta-llama/llama-3.3-70b-instruct:free",
            label = "OpenRouter"
        ),
        OpenAiCompatClient(
            apiKey = BuildConfig.GROQ_API_KEY,
            baseUrl = "https://api.groq.com/openai/v1",
            model = "llama-3.3-70b-versatile",
            label = "Groq"
        ),
        GeminiClient()
    )

    /** First provider that has a key configured, or null if none. */
    fun firstConfigured(): AiEngine? = all().firstOrNull { it.isConfigured }
}
