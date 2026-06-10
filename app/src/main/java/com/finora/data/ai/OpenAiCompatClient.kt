package com.finora.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal client for any OpenAI-compatible chat-completions endpoint
 * (OpenRouter, Groq, Together, local Ollama, …). Uses only HttpURLConnection +
 * org.json (both built into Android) so it adds no extra dependency.
 */
class OpenAiCompatClient(
    private val apiKey: String,
    private val baseUrl: String,
    /** Models tried in order; on rate-limit/5xx errors the next one is attempted. */
    private val models: List<String>,
    override val label: String
) : AiEngine {

    constructor(apiKey: String, baseUrl: String, model: String, label: String) :
        this(apiKey, baseUrl, listOf(model), label)

    override val isConfigured: Boolean get() = apiKey.isNotBlank()

    override suspend fun generate(prompt: String): String =
        chat(listOf(ChatTurn("user", prompt)))

    override suspend fun chat(messages: List<ChatTurn>): String {
        val payloadMessages = JSONArray().apply {
            messages.forEach { turn ->
                put(JSONObject().put("role", turn.role).put("content", turn.content))
            }
        }
        var lastError: Exception? = null
        for (model in models) {
            try {
                return requestOnce(payloadMessages, model)
            } catch (e: Exception) {
                lastError = e
                // Only fall through to the next model on transient/limit errors.
                val msg = e.message.orEmpty()
                val transient = msg.contains("HTTP 429") || msg.contains("HTTP 5") ||
                    msg.contains("rate", ignoreCase = true) || msg.contains("quota", ignoreCase = true)
                if (!transient) throw e
            }
        }
        throw lastError ?: IOException("Не удалось получить ответ")
    }

    private suspend fun requestOnce(messages: JSONArray, model: String): String =
        withContext(Dispatchers.IO) {
            val url = URL("${baseUrl.trimEnd('/')}/chat/completions")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 30_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                // Optional but recommended by OpenRouter for attribution.
                setRequestProperty("HTTP-Referer", "https://github.com/nenahovdmitriy03/finora")
                setRequestProperty("X-Title", "Finora")
            }

            val payload = JSONObject().apply {
                put("model", model)
                put("temperature", 0.7)
                put("messages", messages)
            }

            try {
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    throw IOException("HTTP $code: ${extractError(text)}")
                }
                parseContent(text)
            } finally {
                conn.disconnect()
            }
        }

    private fun parseContent(body: String): String {
        val json = JSONObject(body)
        val choices = json.optJSONArray("choices") ?: return ""
        if (choices.length() == 0) return ""
        val message = choices.getJSONObject(0).optJSONObject("message") ?: return ""
        return message.optString("content").trim()
    }

    private fun extractError(body: String): String = try {
        JSONObject(body).optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() } ?: body
    } catch (_: Exception) {
        body
    }
}
