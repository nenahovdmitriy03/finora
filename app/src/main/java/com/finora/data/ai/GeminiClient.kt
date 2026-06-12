package com.finora.data.ai

import android.graphics.Bitmap
import android.util.Base64
import com.finora.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Talks to the Gemini REST API directly with HttpURLConnection + org.json.
 *
 * We intentionally do NOT use the `generativeai` SDK: it bundles Ktor 2.x, which
 * clashes at runtime with the Ktor 3.x that supabase-kt pulls in
 * (NoClassDefFoundError: io.ktor.client.plugins.HttpTimeout). Going straight to
 * REST keeps the AI layer dependency-free and consistent with OpenAiCompatClient.
 */
class GeminiClient(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val modelName: String = DEFAULT_MODEL
) : AiEngine {
    override val label: String = "Gemini"
    override val isConfigured: Boolean get() = apiKey.isNotBlank()

    /** Single-shot text completion. */
    override suspend fun generate(prompt: String): String =
        request(JSONArray().put(textPart(prompt)))

    /** Multi-turn chat flattened into one prompt (matches the app's usage). */
    override suspend fun chat(messages: List<ChatTurn>): String {
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

    /** Vision: sends [bitmap] (as inline JPEG) together with [prompt]. */
    suspend fun describeImage(bitmap: Bitmap, prompt: String): String {
        val scaled = downscale(bitmap, MAX_IMAGE_DIM)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 90, baos)
        val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        val parts = JSONArray()
            .put(textPart(prompt))
            .put(
                JSONObject().put(
                    "inline_data",
                    JSONObject().put("mime_type", "image/jpeg").put("data", base64)
                )
            )
        return request(parts)
    }

    private fun textPart(text: String): JSONObject = JSONObject().put("text", text)

    private suspend fun request(parts: JSONArray): String = withContext(Dispatchers.IO) {
        // Via reverse-proxy (api.nenahov.shop:8443/gemini → generativelanguage.googleapis.com)
        // to bypass regional geo-blocking of Google AI endpoints.
        val url = URL(
            "https://api.nenahov.shop:8443/gemini/v1beta/models/$modelName:generateContent?key=$apiKey"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        val body = JSONObject().put(
            "contents",
            JSONArray().put(JSONObject().put("parts", parts))
        )
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException("HTTP $code: ${extractError(text)}")
            parseText(text)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseText(body: String): String {
        val candidates = JSONObject(body).optJSONArray("candidates") ?: return ""
        if (candidates.length() == 0) return ""
        val content = candidates.getJSONObject(0).optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until parts.length()) sb.append(parts.getJSONObject(i).optString("text"))
        return sb.toString().trim()
    }

    private fun extractError(body: String): String =
        runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: body.take(300)

    private fun downscale(bmp: Bitmap, max: Int): Bitmap {
        val largest = maxOf(bmp.width, bmp.height)
        if (largest <= max) return bmp
        val scale = max.toFloat() / largest
        return Bitmap.createScaledBitmap(
            bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true
        )
    }

    companion object {
        // Current free-tier multimodal model (1.5-flash was retired → 404 on v1beta).
        const val DEFAULT_MODEL = "gemini-2.0-flash"
        private const val MAX_IMAGE_DIM = 1536
    }
}
