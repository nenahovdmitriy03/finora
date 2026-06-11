package com.finora.data.ai

import android.graphics.Bitmap
import com.finora.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

/**
 * Thin wrapper around the Gemini API. The key is injected at build time from
 * local.properties (see app/build.gradle.kts) into BuildConfig.GEMINI_API_KEY,
 * so it is never hard-coded in source.
 */
class GeminiClient(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val modelName: String = DEFAULT_MODEL
) : AiEngine {
    override val label: String = "Gemini"
    override val isConfigured: Boolean get() = apiKey.isNotBlank()

    private val model: GenerativeModel by lazy {
        GenerativeModel(modelName = modelName, apiKey = apiKey)
    }

    /** Sends [prompt] and returns the model's plain-text reply. Throws on network/API errors. */
    override suspend fun generate(prompt: String): String {
        val response = model.generateContent(prompt)
        return response.text?.trim().orEmpty()
    }

    /** Vision: sends [bitmap] together with [prompt] and returns the plain-text reply. */
    suspend fun describeImage(bitmap: Bitmap, prompt: String): String {
        val input = content {
            image(bitmap)
            text(prompt)
        }
        return model.generateContent(input).text?.trim().orEmpty()
    }

    companion object {
        // Free-tier model. Change here if you want another (e.g. "gemini-2.0-flash").
        const val DEFAULT_MODEL = "gemini-1.5-flash"
    }
}
