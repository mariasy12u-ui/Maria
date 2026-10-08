package com.example.data.model

enum class SearchEngine(val displayName: String, val searchUrl: String, val suggestUrl: String) {
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s", "https://duckduckgo.com/ac/?q=%s&type=list"),
    GOOGLE("Google", "https://www.google.com/search?q=%s", "https://suggestqueries.google.com/complete/search?client=firefox&q=%s"),
    BING("Bing", "https://www.bing.com/search?q=%s", "https://api.bing.com/osjson.aspx?query=%s"),
    BRAVE("Brave Search", "https://search.brave.com/search?q=%s", ""),
    ECOSIA("Ecosia", "https://www.ecosia.org/search?q=%s", "")
}

enum class ThemeMode(val displayName: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class AccentColor(val displayName: String, val hex: Long) {
    NOVA_CYAN("Nova Cyan", 0xFF0EA5E9),
    ELECTRIC_VIOLET("Electric Violet", 0xFF8B5CF6),
    EMERALD("Emerald Green", 0xFF10B981),
    SUNSET_ORANGE("Sunset Orange", 0xFFF97316),
    ROSE("Neon Rose", 0xFFF43F5E)
}

enum class StartupOption(val displayName: String) {
    NEW_TAB("Open New Tab"),
    CONTINUE_SESSION("Continue where you left off"),
    CUSTOM_URL("Open custom page")
}

data class BrowserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.NOVA_CYAN,
    val searchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    val startupOption: StartupOption = StartupOption.NEW_TAB,
    val customStartupUrl: String = "https://duckduckgo.com",
    val homeUrl: String = "nova://newtab",
    val showBookmarksBar: Boolean = true,
    val showHomeButton: Boolean = true,
    val searchSuggestionsEnabled: Boolean = true,
    val searchHistoryEnabled: Boolean = true,
    val doNotTrack: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val javascriptEnabled: Boolean = true,
    val defaultDesktopMode: Boolean = false,
    val compactToolbar: Boolean = false
)
