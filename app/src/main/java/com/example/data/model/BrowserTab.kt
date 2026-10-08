package com.example.data.model

data class BrowserTab(
    val id: String = java.util.UUID.randomUUID().toString(),
    val url: String = "nova://newtab",
    val title: String = "New Tab",
    val faviconUrl: String? = null,
    val isIncognito: Boolean = false,
    val isPinned: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val zoomLevel: Float = 1.0f,
    val isDesktopMode: Boolean = false,
    val lastAccessed: Long = System.currentTimeMillis()
) {
    val isNewTab: Boolean get() = url == "nova://newtab" || url.isBlank()
    val isSecure: Boolean get() = url.startsWith("https://")
    val displayHost: String get() {
        return try {
            if (isNewTab) "nova://newtab"
            else {
                val uri = android.net.Uri.parse(url)
                uri.host ?: url
            }
        } catch (_: Exception) {
            url
        }
    }
}
