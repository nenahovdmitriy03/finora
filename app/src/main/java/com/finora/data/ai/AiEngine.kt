package com.finora.data.ai

import com.finora.BuildConfig

/** One turn in a conversation. [role] is "system", "user" or "assistant". */
data class ChatTurn(val role: String, val content: String)

/** A text-generation backend (Gemini, OpenRouter, Groq, …). */
interface AiEngine {
    /** Human-readable provider name shown in the UI. */
    val label: String
    val isConfigured: Boolean

    /** Single-shot completion. */
    suspend fun generate(prompt: String): String

    /**
     * Multi-turn chat. Default flattens the conversation into one prompt and
     * delegates to [generate]; providers that support native chat (OpenAI-compatible)
     * override this for better quality.
     */
    suspend fun chat(messages: List<ChatTurn>): String {
        val sb = StringBuilder()
        messages.forEach { turn ->
            when (turn.role) {
                "system" -> sb.appendLine(turn.content).appendLine()
                "user" -> sb.appendLine("Пользователь: ${turn.content}")
                else -> sb.appendLine("Ассистент: ${turn.content}")
            }
        }
        sb.append("Ассистент:")
        return generate(sb.toString())
    }
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
            // Via our reverse-proxy (api.nenahov.shop:8443/ai → openrouter.ai/api)
            // to bypass regional geo-blocking.
            baseUrl = "https://api.nenahov.shop:8443/ai/v1",
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
            // Via reverse-proxy (api.nenahov.shop:8443/groq → api.groq.com/openai)
            baseUrl = "https://api.nenahov.shop:8443/groq/v1",
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
