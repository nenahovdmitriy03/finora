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
            // Several free models — if one is rate-limited (429), the next is tried.
            models = listOf(
                "meta-llama/llama-3.3-70b-instruct:free",
                "deepseek/deepseek-chat-v3-0324:free",
                "google/gemini-2.0-flash-exp:free",
                "qwen/qwen-2.5-72b-instruct:free",
                "mistralai/mistral-small-3.1-24b-instruct:free"
            ),
            label = "OpenRouter"
        ),
        OpenAiCompatClient(
            apiKey = BuildConfig.GROQ_API_KEY,
            baseUrl = "https://api.groq.com/openai/v1",
            models = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant"),
            label = "Groq"
        ),
        GeminiClient()
    )

    /** Every provider that has a key configured, in priority order. */
    fun configured(): List<AiEngine> = all().filter { it.isConfigured }

    /** First provider that has a key configured, or null if none. */
    fun firstConfigured(): AiEngine? = configured().firstOrNull()
}
