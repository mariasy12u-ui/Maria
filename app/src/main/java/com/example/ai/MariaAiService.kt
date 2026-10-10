package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

object MariaAiService {
    private const val MODEL = "gemini-2.5-flash"
    private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    suspend fun sendMessage(
        prompt: String,
        pageContextUrl: String? = null,
        pageContextTitle: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Throwable) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineSmartResponse(prompt, pageContextTitle)
        }

        try {
            val systemContext = buildString {
                append("You are Maria AI, an intelligent, helpful, and creative AI assistant embedded in Maria Browser. ")
                append("You provide clear, accurate, and concise assistance for browsing, web research, writing, coding, math, and daily productivity. ")
                if (!pageContextTitle.isNullOrBlank()) {
                    append("The user is currently browsing the page: \"$pageContextTitle\" ($pageContextUrl). ")
                }
            }

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val userContent = JSONObject().apply {
                    put("role", "user")
                    val partsArray = JSONArray()
                    partsArray.put(JSONObject().apply {
                        put("text", "$systemContext\n\nUser request: $prompt")
                    })
                    put("parts", partsArray)
                }
                contentsArray.put(userContent)
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 1500)
                }
                put("generationConfig", generationConfig)
            }

            val url = URL("$ENDPOINT?key=$apiKey")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 30000
                readTimeout = 30000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                parseGeminiResponse(responseText)
            } else {
                val errorStream = connection.errorStream
                val errorText = errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                "Maria AI note ($responseCode):\n\n${getOfflineSmartResponse(prompt, pageContextTitle)}"
            }
        } catch (e: Exception) {
            getOfflineSmartResponse(prompt, pageContextTitle)
        }
    }

    private fun parseGeminiResponse(jsonString: String): String {
        return try {
            val root = JSONObject(jsonString)
            val candidates = root.getJSONArray("candidates")
            if (candidates.length() > 0) {
                val first = candidates.getJSONObject(0)
                val content = first.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).getString("text").trim()
                }
            }
            "No answer generated."
        } catch (e: Exception) {
            "Failed to parse response: ${e.message}"
        }
    }

    private fun getOfflineSmartResponse(prompt: String, pageTitle: String?): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("summary") || lower.contains("summarize") -> {
                "Summary of ${pageTitle ?: "current webpage"}: This page provides interactive content and web resources. For live cloud AI synthesis, enter your Gemini API Key in the AI Studio Secrets panel."
            }
            lower.contains("code") || lower.contains("python") || lower.contains("kotlin") || lower.contains("javascript") -> {
                "Here is a clean code example for your query:\n\n```kotlin\n// Maria Browser Smart Helper\nfun executeTask(query: String) {\n    println(\"Processing: \$query with Maria AI\")\n}\n```\n\nAdd your Gemini API key in the Secrets panel to unlock full interactive coding generation!"
            }
            lower.contains("urdu") || lower.contains("kaise") || lower.contains("kya") || lower.contains("salam") -> {
                "Salam! Main Maria AI hoon, aap ki smart browser assistant. Main aap ko writing, coding, search aur research mein madad kar sakti hoon. Full live AI models chalane ke liye AI Studio ke Secrets panel mein Gemini API key set karein."
            }
            else -> {
                "Maria AI Assistant:\nI have received your request: \"$prompt\".\n\nTo activate live cloud intelligence for uncapped reasoning, summarization, and writing, enter your Gemini API Key in the AI Studio Secrets panel."
            }
        }
    }
}
