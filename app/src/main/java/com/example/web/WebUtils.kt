package com.example.web

import android.net.Uri
import android.util.Patterns
import com.example.data.model.SearchEngine

object WebUtils {
    private val URL_WITHOUT_PROTOCOL_REGEX = "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}(/.*)?$".toRegex()

    fun resolveInput(input: String, searchEngine: SearchEngine): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "nova://newtab"

        if (trimmed.equals("nova://newtab", ignoreCase = true) ||
            trimmed.equals("about:blank", ignoreCase = true)
        ) {
            return "nova://newtab"
        }

        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true) ||
            trimmed.startsWith("content://", ignoreCase = true)
        ) {
            return trimmed
        }

        val matchesWebUrl = try {
            Patterns.WEB_URL?.matcher(trimmed)?.matches() == true
        } catch (_: Throwable) {
            false
        }
        if (trimmed.matches(URL_WITHOUT_PROTOCOL_REGEX) || matchesWebUrl) {
            return "https://$trimmed"
        }

        return buildSearchUrl(trimmed, searchEngine)
    }

    fun buildSearchUrl(query: String, searchEngine: SearchEngine): String {
        val encoded = Uri.encode(query)
        return searchEngine.searchUrl.replace("%s", encoded)
    }

    fun extractDomain(url: String): String {
        if (url == "nova://newtab") return "New Tab"
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
}
