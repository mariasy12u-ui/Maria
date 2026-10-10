package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.db.NovaDatabase
import com.example.data.model.Bookmark
import com.example.data.model.BrowserSettings
import com.example.data.model.BrowserTab
import com.example.data.model.DownloadEntry
import com.example.data.model.HistoryEntry
import com.example.data.model.RdpMode
import com.example.data.model.RdpProfile
import com.example.data.model.RdpProtocol
import com.example.data.model.SearchEngine
import com.example.data.model.ShortcutItem
import com.example.data.model.StartupOption
import com.example.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class BrowserRepository(private val context: Context) {
    private val db = NovaDatabase.getInstance(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("nova_browser_prefs", Context.MODE_PRIVATE)

    val allBookmarks: Flow<List<Bookmark>> = db.bookmarkDao().getAllBookmarks()
    val allHistory: Flow<List<HistoryEntry>> = db.historyDao().getAllHistory()
    val allDownloads: Flow<List<DownloadEntry>> = db.downloadDao().getAllDownloads()

    // Bookmarks
    suspend fun insertBookmark(bookmark: Bookmark) = db.bookmarkDao().insertBookmark(bookmark)
    suspend fun updateBookmark(bookmark: Bookmark) = db.bookmarkDao().updateBookmark(bookmark)
    suspend fun deleteBookmark(bookmark: Bookmark) = db.bookmarkDao().deleteBookmark(bookmark)
    suspend fun deleteBookmarkByUrl(url: String) = db.bookmarkDao().deleteByUrl(url)
    suspend fun isBookmarked(url: String): Boolean = db.bookmarkDao().isBookmarked(url)

    // History
    suspend fun insertHistory(entry: HistoryEntry) = db.historyDao().insertHistory(entry)
    suspend fun deleteHistory(entry: HistoryEntry) = db.historyDao().deleteHistory(entry)
    suspend fun clearHistorySince(since: Long) = db.historyDao().clearHistorySince(since)
    suspend fun clearAllHistory() = db.historyDao().clearAllHistory()

    // Downloads
    suspend fun insertDownload(entry: DownloadEntry) = db.downloadDao().insertDownload(entry)
    suspend fun deleteDownload(entry: DownloadEntry) = db.downloadDao().deleteDownload(entry)
    suspend fun clearAllDownloads() = db.downloadDao().clearAll()

    // Settings
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<BrowserSettings> = _settings.asStateFlow()

    private fun loadSettings(): BrowserSettings {
        return BrowserSettings(
            themeMode = ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name),
            accentColorHex = prefs.getString("accent_color_hex", "#06B6D4") ?: "#06B6D4",
            searchEngine = SearchEngine.valueOf(prefs.getString("search_engine", SearchEngine.DUCKDUCKGO.name) ?: SearchEngine.DUCKDUCKGO.name),
            showBookmarksBar = prefs.getBoolean("show_bookmarks_bar", false),
            showHomeButton = prefs.getBoolean("show_home_button", true),
            javaScriptEnabled = prefs.getBoolean("javascript_enabled", true),
            blockThirdPartyCookies = prefs.getBoolean("block_third_party_cookies", true),
            doNotTrack = prefs.getBoolean("do_not_track", true),
            startupOption = StartupOption.valueOf(prefs.getString("startup_option", StartupOption.NEW_TAB.name) ?: StartupOption.NEW_TAB.name),
            customStartupUrl = prefs.getString("custom_startup_url", "https://duckduckgo.com") ?: "https://duckduckgo.com"
        )
    }

    fun updateSettings(newSettings: BrowserSettings) {
        _settings.value = newSettings
        prefs.edit()
            .putString("theme_mode", newSettings.themeMode.name)
            .putString("accent_color_hex", newSettings.accentColorHex)
            .putString("search_engine", newSettings.searchEngine.name)
            .putBoolean("show_bookmarks_bar", newSettings.showBookmarksBar)
            .putBoolean("show_home_button", newSettings.showHomeButton)
            .putBoolean("javascript_enabled", newSettings.javaScriptEnabled)
            .putBoolean("block_third_party_cookies", newSettings.blockThirdPartyCookies)
            .putBoolean("do_not_track", newSettings.doNotTrack)
            .putString("startup_option", newSettings.startupOption.name)
            .putString("custom_startup_url", newSettings.customStartupUrl)
            .apply()
    }

    // Speed Dial Shortcuts
    private val _shortcuts = MutableStateFlow(loadShortcuts())
    val shortcuts: StateFlow<List<ShortcutItem>> = _shortcuts.asStateFlow()

    private fun loadShortcuts(): List<ShortcutItem> {
        val json = prefs.getString("speed_dial_shortcuts", null)
        if (json != null) {
            try {
                val array = JSONArray(json)
                val list = mutableListOf<ShortcutItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(ShortcutItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        url = obj.getString("url"),
                        iconKey = obj.optString("iconKey", "web")
                    ))
                }
                if (list.isNotEmpty()) return list
            } catch (_: Exception) {}
        }
        return listOf(
            ShortcutItem(title = "DuckDuckGo", url = "https://duckduckgo.com", iconKey = "search"),
            ShortcutItem(title = "Google", url = "https://google.com", iconKey = "search"),
            ShortcutItem(title = "Wikipedia", url = "https://wikipedia.org", iconKey = "book"),
            ShortcutItem(title = "GitHub", url = "https://github.com", iconKey = "code"),
            ShortcutItem(title = "YouTube", url = "https://youtube.com", iconKey = "video"),
            ShortcutItem(title = "Reddit", url = "https://reddit.com", iconKey = "chat"),
            ShortcutItem(title = "Hacker News", url = "https://news.ycombinator.com", iconKey = "newspaper"),
            ShortcutItem(title = "W3Schools", url = "https://w3schools.com", iconKey = "school")
        )
    }

    fun saveShortcuts(list: List<ShortcutItem>) {
        _shortcuts.value = list
        try {
            val array = JSONArray()
            for (item in list) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("title", item.title)
                obj.put("url", item.url)
                obj.put("iconKey", item.iconKey)
                array.put(obj)
            }
            prefs.edit().putString("speed_dial_shortcuts", array.toString()).apply()
        } catch (_: Exception) {}
    }

    // Saved Open Tabs Session
    fun saveTabsSession(tabs: List<BrowserTab>, activeTabId: String) {
        try {
            val array = JSONArray()
            for (tab in tabs.filter { !it.isIncognito }) {
                val obj = JSONObject()
                obj.put("id", tab.id)
                obj.put("url", tab.url)
                obj.put("title", tab.title)
                obj.put("isPinned", tab.isPinned)
                obj.put("isDesktopMode", tab.isDesktopMode)
                array.put(obj)
            }
            prefs.edit()
                .putString("saved_tabs", array.toString())
                .putString("saved_active_tab_id", activeTabId)
                .apply()
        } catch (_: Exception) {}
    }

    fun loadTabsSession(): Pair<List<BrowserTab>, String?> {
        val json = prefs.getString("saved_tabs", null) ?: return Pair(emptyList(), null)
        val activeId = prefs.getString("saved_active_tab_id", null)
        val list = mutableListOf<BrowserTab>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(BrowserTab(
                    id = obj.getString("id"),
                    url = obj.getString("url"),
                    title = obj.getString("title"),
                    isPinned = obj.optBoolean("isPinned", false),
                    isDesktopMode = obj.optBoolean("isDesktopMode", false)
                ))
            }
        } catch (_: Exception) {}
        return Pair(list, activeId)
    }

    // RDP & Remote Internet Profiles
    private val _rdpProfiles = MutableStateFlow(loadRdpProfiles())
    val rdpProfiles: StateFlow<List<RdpProfile>> = _rdpProfiles.asStateFlow()

    private val _isRdpActive = MutableStateFlow(prefs.getBoolean("is_rdp_active", false))
    val isRdpActive: StateFlow<Boolean> = _isRdpActive.asStateFlow()

    private val _isAutoRotateEnabled = MutableStateFlow(prefs.getBoolean("is_auto_rotate_enabled", true))
    val isAutoRotateEnabled: StateFlow<Boolean> = _isAutoRotateEnabled.asStateFlow()

    fun setAutoRotateEnabled(enabled: Boolean) {
        _isAutoRotateEnabled.value = enabled
        prefs.edit().putBoolean("is_auto_rotate_enabled", enabled).apply()
    }

    private fun loadRdpProfiles(): List<RdpProfile> {
        val json = prefs.getString("rdp_profiles", null)
        if (json != null) {
            try {
                val array = JSONArray(json)
                val list = mutableListOf<RdpProfile>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(RdpProfile(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        mode = RdpMode.valueOf(obj.optString("mode", RdpMode.PROXY_TUNNEL.name)),
                        host = obj.getString("host"),
                        port = obj.getInt("port"),
                        protocol = RdpProtocol.valueOf(obj.optString("protocol", RdpProtocol.HTTP.name)),
                        username = obj.optString("username", ""),
                        password = obj.optString("password", ""),
                        webRdpUrl = obj.optString("webRdpUrl", ""),
                        isEnabled = obj.optBoolean("isEnabled", false)
                    ))
                }
                if (list.isNotEmpty()) return list
            } catch (_: Exception) {}
        }
        return listOf(
            RdpProfile(
                name = "🇺🇸 USA Cloud RDP Tunnel #1 (US-East 100 Mbps)",
                mode = RdpMode.PROXY_TUNNEL,
                host = "104.28.19.45",
                port = 8080,
                protocol = RdpProtocol.HTTP,
                isEnabled = true
            ),
            RdpProfile(
                name = "🇺🇸 USA Cloud RDP Tunnel #2 (US-West 80 Mbps)",
                mode = RdpMode.PROXY_TUNNEL,
                host = "142.250.190.46",
                port = 8080,
                protocol = RdpProtocol.HTTP,
                isEnabled = false
            ),
            RdpProfile(
                name = "🇺🇸 USA Cloud RDP Tunnel #3 (US-Central 90 Mbps)",
                mode = RdpMode.PROXY_TUNNEL,
                host = "172.217.16.206",
                port = 8080,
                protocol = RdpProtocol.HTTP,
                isEnabled = false
            ),
            RdpProfile(
                name = "🖥️ Live Web RDP Desktop Screen (Remote Windows)",
                mode = RdpMode.WEB_DESKTOP,
                host = "browserling.com",
                port = 443,
                protocol = RdpProtocol.WEB_RDP,
                webRdpUrl = "https://www.browserling.com/browse/win10/chrome/https%3A%2F%2Fgoogle.com",
                isEnabled = false
            ),
            RdpProfile(
                name = "⚙️ Custom RDP Server (SOCKS5/HTTP Proxy)",
                mode = RdpMode.PROXY_TUNNEL,
                host = "127.0.0.1",
                port = 1080,
                protocol = RdpProtocol.SOCKS5,
                isEnabled = false
            )
        )
    }

    fun saveRdpProfiles(list: List<RdpProfile>) {
        _rdpProfiles.value = list
        try {
            val array = JSONArray()
            for (p in list) {
                val obj = JSONObject()
                obj.put("id", p.id)
                obj.put("name", p.name)
                obj.put("mode", p.mode.name)
                obj.put("host", p.host)
                obj.put("port", p.port)
                obj.put("protocol", p.protocol.name)
                obj.put("username", p.username)
                obj.put("password", p.password)
                obj.put("webRdpUrl", p.webRdpUrl)
                obj.put("isEnabled", p.isEnabled)
                array.put(obj)
            }
            prefs.edit().putString("rdp_profiles", array.toString()).apply()
        } catch (_: Exception) {}
    }

    fun setRdpActive(active: Boolean) {
        _isRdpActive.value = active
        prefs.edit().putBoolean("is_rdp_active", active).apply()
    }
}
