package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.NovaCyan

enum class ThemeMode(val displayName: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

enum class SearchEngine(val displayName: String, val searchUrl: String) {
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s"),
    GOOGLE("Google", "https://www.google.com/search?q=%s"),
    BING("Bing", "https://www.bing.com/search?q=%s"),
    BRAVE("Brave", "https://search.brave.com/search?q=%s"),
    ECOSIA("Ecosia", "https://www.ecosia.org/search?q=%s")
}

enum class StartupOption(val displayName: String) {
    NEW_TAB("New Tab Dashboard"),
    CONTINUE_SESSION("Continue Previous Session"),
    CUSTOM_URL("Specific Page")
}

data class BrowserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColorHex: String = "#06B6D4",
    val searchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    val showBookmarksBar: Boolean = false,
    val showHomeButton: Boolean = true,
    val javaScriptEnabled: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val doNotTrack: Boolean = true,
    val startupOption: StartupOption = StartupOption.NEW_TAB,
    val customStartupUrl: String = "https://duckduckgo.com"
) {
    val accentColor: Color
        get() = try {
            Color(android.graphics.Color.parseColor(accentColorHex))
        } catch (_: Exception) {
            NovaCyan
        }
}
