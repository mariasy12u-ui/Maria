package com.example.data.model

data class BrowserTab(
    val id: String = java.util.UUID.randomUUID().toString(),
    val url: String = "nova://newtab",
    val title: String = "New Tab",
    val faviconUrl: String? = null,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isSecure: Boolean = false,
    val isPinned: Boolean = false,
    val isIncognito: Boolean = false,
    val isDesktopMode: Boolean = false
) {
    val isNewTab: Boolean
        get() = url == "nova://newtab"

    val displayHost: String
        get() = when {
            isNewTab -> "New Tab"
            url.startsWith("https://") -> url.removePrefix("https://").substringBefore("/")
            url.startsWith("http://") -> url.removePrefix("http://").substringBefore("/")
            else -> url.substringBefore("/")
        }
}
