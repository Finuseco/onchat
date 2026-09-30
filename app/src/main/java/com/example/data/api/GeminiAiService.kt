package com.example.data.api

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiGenerateContentRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>,
    @Json(name = "role") val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float? = 0.4f,
    @Json(name = "topP") val topP: Float? = 0.95f,
    @Json(name = "topK") val topK: Int? = 40
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateContentResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null,
    @Json(name = "error") val error: GeminiErrorResponse? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiErrorResponse(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: String? = null
)

class GeminiAiService(private val context: Context) {

    private val prefs = context.getSharedPreferences("onchat_ai_settings", Context.MODE_PRIVATE)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiGenerateContentRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiGenerateContentResponse::class.java)

    /**
     * Retrieves active Gemini API key:
     * 1. Custom user key stored in preferences
     * 2. BuildConfig.GEMINI_API_KEY
     */
    fun getApiKey(): String {
        val customKey = prefs.getString("custom_gemini_api_key", "")?.trim() ?: ""
        if (customKey.isNotEmpty()) return customKey

        val buildConfigKey = try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (e: Throwable) {
            ""
        }
        return if (buildConfigKey != "MY_GEMINI_API_KEY") buildConfigKey else ""
    }

    fun saveCustomApiKey(key: String) {
        prefs.edit().putString("custom_gemini_api_key", key.trim()).apply()
    }

    fun hasValidApiKey(): Boolean {
        val key = getApiKey()
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Uses Gemini 3.5 Flash to analyze speech voice input,
     * correct phonetics, remove noise words, format punctuation, and return clean readable text.
     */
    suspend fun analyzeAndRefineVoiceInput(
        rawVoiceText: String,
        targetLanguage: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                // Fallback: If no API key is yet configured, return cleaned local text
                return@withContext Result.success(cleanupLocally(rawVoiceText))
            }

            val systemPrompt = """
                Tu es l'assistant vocal intelligent d'OnChat (Écosystème CashPay).
                L'utilisateur a parlé au microphone et nous avons capturé son message vocal.
                Ta mission :
                1. Corriger les fautes de dictée vocale, phonétiques et répétitions involontaires.
                2. Ajouter la bonne ponctuation, les majuscules et les accents.
                3. Préserver fidèlement le ton et l'intention du locuteur (qu'il parle en français, anglais, lingala, swahili, etc.).
                ${if (!targetLanguage.isNullOrBlank()) "4. Traduire ou adapter fidèlement vers la langue demandée : $targetLanguage." else ""}
                5. Réponds STRICTEMENT avec le texte final corrigé prêt à être envoyé, sans guillemets, sans explications ni préambule.
            """.trimIndent()

            val request = GeminiGenerateContentRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = "Message vocal dicté : \"$rawVoiceText\""))
                    )
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemPrompt))
                ),
                generationConfig = GeminiGenerationConfig(temperature = 0.2f)
            )

            val jsonBody = requestAdapter.toJson(request)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val httpRequest = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(httpRequest).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e("GeminiAiService", "API error ${response.code}: $responseBody")
                return@withContext Result.success(cleanupLocally(rawVoiceText))
            }

            val parsed = responseAdapter.fromJson(responseBody)
            val candidateText = parsed?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()

            if (!candidateText.isNullOrBlank()) {
                Result.success(candidateText)
            } else {
                Result.success(cleanupLocally(rawVoiceText))
            }
        } catch (e: Exception) {
            Log.e("GeminiAiService", "Failed to process voice text with Gemini", e)
            Result.success(cleanupLocally(rawVoiceText))
        }
    }

    /**
     * Applies smart AI transformation (Polishing, Translation, Concise formatting, Smart Reply)
     */
    suspend fun assistMessage(
        inputText: String,
        instruction: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(Exception("Clé API Gemini non configurée. Vous pouvez l'ajouter dans les paramètres AI."))
            }

            val systemPrompt = """
                Tu es l'assistant de messagerie OnChat.
                Traite le message selon l'instruction : "$instruction".
                Garde un style naturel et adapté à une messagerie instantanée moderne.
                Réponds DIRECTEMENT avec le texte transformé, sans fioritures ni commentaires.
            """.trimIndent()

            val request = GeminiGenerateContentRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = inputText))
                    )
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemPrompt))
                ),
                generationConfig = GeminiGenerationConfig(temperature = 0.3f)
            )

            val jsonBody = requestAdapter.toJson(request)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val httpRequest = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(httpRequest).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return@withContext Result.failure(Exception("Erreur API Gemini : ${response.code}"))
            }

            val parsed = responseAdapter.fromJson(responseBody)
            val candidateText = parsed?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()

            if (!candidateText.isNullOrBlank()) {
                Result.success(candidateText)
            } else {
                Result.failure(Exception("Aucune réponse générée par l'IA"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanupLocally(text: String): String {
        return text.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
