package com.example.web

import android.net.Uri
import android.util.Patterns
import com.example.data.model.SearchEngine
import java.net.URLEncoder

object WebUtils {
    private val URL_WITHOUT_PROTOCOL_REGEX = Regex(
        "^([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(:[0-9]+)?(/.*)?$"
    )
    private val LOCALHOST_REGEX = Regex("^localhost(:[0-9]+)?(/.*)?$")

    fun resolveInput(input: String, searchEngine: SearchEngine): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "nova://newtab"
        if (trimmed.equals("nova://newtab", ignoreCase = true)) return "nova://newtab"

        // Explicit protocols
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true) ||
            trimmed.startsWith("about:", ignoreCase = true) ||
            trimmed.startsWith("nova:", ignoreCase = true)) {
            return trimmed
        }

        // Check if it's like "example.com" or "sub.example.co.uk/path" or "localhost:3000"
        if (trimmed.matches(LOCALHOST_REGEX)) {
            return "http://$trimmed"
        }
        val matchesWebUrl = try {
            Patterns.WEB_URL?.matcher(trimmed)?.matches() == true
        } catch (_: Throwable) {
            false
        }
        if (trimmed.matches(URL_WITHOUT_PROTOCOL_REGEX) || matchesWebUrl) {
            return "https://$trimmed"
        }

        // Otherwise, treatment as search query
        return buildSearchUrl(trimmed, searchEngine)
    }

    fun buildSearchUrl(query: String, searchEngine: SearchEngine): String {
        val encoded = try {
            URLEncoder.encode(query, "UTF-8")
        } catch (_: Exception) {
            query.replace(" ", "+")
        }
        return String.format(searchEngine.searchUrl, encoded)
    }

    fun extractDomain(url: String): String {
        if (url == "nova://newtab" || url.isBlank()) return "Nova New Tab"
        return try {
            val uri = Uri.parse(url)
            uri.host ?: url
        } catch (_: Exception) {
            url
        }
    }

    fun isSecure(url: String): Boolean {
        return url.startsWith("https://", ignoreCase = true)
    }

    fun isInternal(url: String): Boolean {
        return url.startsWith("nova://", ignoreCase = true)
    }
}
