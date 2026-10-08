package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.db.NovaDatabase
import com.example.data.model.AccentColor
import com.example.data.model.Bookmark
import com.example.data.model.BrowserSettings
import com.example.data.model.BrowserTab
import com.example.data.model.DownloadEntry
import com.example.data.model.HistoryEntry
import com.example.data.model.SearchEngine
import com.example.data.model.ShortcutItem
import com.example.data.model.StartupOption
import com.example.data.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BrowserRepository(context: Context) {
    private val db = NovaDatabase.getInstance(context)
    private val bookmarkDao = db.bookmarkDao()
    private val historyDao = db.historyDao()
    private val downloadDao = db.downloadDao()
    private val prefs: SharedPreferences = context.getSharedPreferences("nova_prefs", Context.MODE_PRIVATE)

    // Bookmarks
    val allBookmarks: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()
    fun searchBookmarks(query: String): Flow<List<Bookmark>> = bookmarkDao.searchBookmarks(query)
    suspend fun getBookmarkByUrl(url: String): Bookmark? = withContext(Dispatchers.IO) {
        bookmarkDao.getBookmarkByUrl(url)
    }
    suspend fun insertBookmark(bookmark: Bookmark) = withContext(Dispatchers.IO) {
        bookmarkDao.insert(bookmark)
    }
    suspend fun updateBookmark(bookmark: Bookmark) = withContext(Dispatchers.IO) {
        bookmarkDao.update(bookmark)
    }
    suspend fun deleteBookmark(bookmark: Bookmark) = withContext(Dispatchers.IO) {
        bookmarkDao.delete(bookmark)
    }
    suspend fun deleteBookmarkByUrl(url: String) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteByUrl(url)
    }

    // History
    val allHistory: Flow<List<HistoryEntry>> = historyDao.getAllHistory()
    fun getRecentHistory(limit: Int): Flow<List<HistoryEntry>> = historyDao.getRecentHistory(limit)
    fun searchHistory(query: String): Flow<List<HistoryEntry>> = historyDao.searchHistory(query)
    suspend fun recordHistoryVisit(title: String, url: String) = withContext(Dispatchers.IO) {
        if (url.startsWith("nova://") || url.isBlank()) return@withContext
        val existing = historyDao.getEntryByUrl(url)
        if (existing != null) {
            historyDao.insert(existing.copy(
                title = if (title.isNotBlank()) title else existing.title,
                timestamp = System.currentTimeMillis(),
                visitCount = existing.visitCount + 1
            ))
        } else {
            historyDao.insert(HistoryEntry(
                title = if (title.isNotBlank()) title else url,
                url = url,
                timestamp = System.currentTimeMillis(),
                visitCount = 1
            ))
        }
    }
    suspend fun deleteHistoryEntry(entry: HistoryEntry) = withContext(Dispatchers.IO) {
        historyDao.delete(entry)
    }
    suspend fun clearHistorySince(sinceTimestamp: Long) = withContext(Dispatchers.IO) {
        historyDao.deleteSince(sinceTimestamp)
    }
    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        historyDao.clearAll()
    }

    // Downloads
    val allDownloads: Flow<List<DownloadEntry>> = downloadDao.getAllDownloads()
    suspend fun addDownload(entry: DownloadEntry) = withContext(Dispatchers.IO) {
        downloadDao.insert(entry)
    }
    suspend fun updateDownload(entry: DownloadEntry) = withContext(Dispatchers.IO) {
        downloadDao.update(entry)
    }
    suspend fun deleteDownload(entry: DownloadEntry) = withContext(Dispatchers.IO) {
        downloadDao.delete(entry)
    }
    suspend fun clearAllDownloads() = withContext(Dispatchers.IO) {
        downloadDao.clearAll()
    }

    // Settings
    private val _settings = MutableStateFlow(loadSettings())
    val settings = _settings.asStateFlow()

    private fun loadSettings(): BrowserSettings {
        val themeModeStr = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val accentColorStr = prefs.getString("accent_color", AccentColor.NOVA_CYAN.name) ?: AccentColor.NOVA_CYAN.name
        val searchEngineStr = prefs.getString("search_engine", SearchEngine.DUCKDUCKGO.name) ?: SearchEngine.DUCKDUCKGO.name
        val startupStr = prefs.getString("startup_option", StartupOption.NEW_TAB.name) ?: StartupOption.NEW_TAB.name

        return BrowserSettings(
            themeMode = try { ThemeMode.valueOf(themeModeStr) } catch (_: Exception) { ThemeMode.SYSTEM },
            accentColor = try { AccentColor.valueOf(accentColorStr) } catch (_: Exception) { AccentColor.NOVA_CYAN },
            searchEngine = try { SearchEngine.valueOf(searchEngineStr) } catch (_: Exception) { SearchEngine.DUCKDUCKGO },
            startupOption = try { StartupOption.valueOf(startupStr) } catch (_: Exception) { StartupOption.NEW_TAB },
            customStartupUrl = prefs.getString("custom_startup_url", "https://duckduckgo.com") ?: "https://duckduckgo.com",
            homeUrl = prefs.getString("home_url", "nova://newtab") ?: "nova://newtab",
            showBookmarksBar = prefs.getBoolean("show_bookmarks_bar", true),
            showHomeButton = prefs.getBoolean("show_home_button", true),
            searchSuggestionsEnabled = prefs.getBoolean("search_suggestions_enabled", true),
            searchHistoryEnabled = prefs.getBoolean("search_history_enabled", true),
            doNotTrack = prefs.getBoolean("do_not_track", true),
            blockThirdPartyCookies = prefs.getBoolean("block_third_party_cookies", true),
            javascriptEnabled = prefs.getBoolean("javascript_enabled", true),
            defaultDesktopMode = prefs.getBoolean("default_desktop_mode", false),
            compactToolbar = prefs.getBoolean("compact_toolbar", false)
        )
    }

    fun updateSettings(newSettings: BrowserSettings) {
        _settings.value = newSettings
        prefs.edit()
            .putString("theme_mode", newSettings.themeMode.name)
            .putString("accent_color", newSettings.accentColor.name)
            .putString("search_engine", newSettings.searchEngine.name)
            .putString("startup_option", newSettings.startupOption.name)
            .putString("custom_startup_url", newSettings.customStartupUrl)
            .putString("home_url", newSettings.homeUrl)
            .putBoolean("show_bookmarks_bar", newSettings.showBookmarksBar)
            .putBoolean("show_home_button", newSettings.showHomeButton)
            .putBoolean("search_suggestions_enabled", newSettings.searchSuggestionsEnabled)
            .putBoolean("search_history_enabled", newSettings.searchHistoryEnabled)
            .putBoolean("do_not_track", newSettings.doNotTrack)
            .putBoolean("block_third_party_cookies", newSettings.blockThirdPartyCookies)
            .putBoolean("javascript_enabled", newSettings.javascriptEnabled)
            .putBoolean("default_desktop_mode", newSettings.defaultDesktopMode)
            .putBoolean("compact_toolbar", newSettings.compactToolbar)
            .apply()
    }

    // Speed Dial Shortcuts
    private val _shortcuts = MutableStateFlow(loadShortcuts())
    val shortcuts = _shortcuts.asStateFlow()

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
            // Do not save incognito tabs in session persistence!
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
    val rdpProfiles = _rdpProfiles.asStateFlow()

    private val _isRdpActive = MutableStateFlow(prefs.getBoolean("is_rdp_active", false))
    val isRdpActive = _isRdpActive.asStateFlow()

    private fun loadRdpProfiles(): List<com.example.data.model.RdpProfile> {
        val json = prefs.getString("rdp_profiles", null)
        if (json != null) {
            try {
                val array = JSONArray(json)
                val list = mutableListOf<com.example.data.model.RdpProfile>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(com.example.data.model.RdpProfile(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        mode = com.example.data.model.RdpMode.valueOf(obj.optString("mode", com.example.data.model.RdpMode.PROXY_TUNNEL.name)),
                        host = obj.getString("host"),
                        port = obj.getInt("port"),
                        protocol = com.example.data.model.RdpProtocol.valueOf(obj.optString("protocol", com.example.data.model.RdpProtocol.HTTP.name)),
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
            com.example.data.model.RdpProfile(
                name = "⚡ 60 Mbps Cloud RDP Desktop (Live)",
                mode = com.example.data.model.RdpMode.WEB_DESKTOP,
                host = "browserling.com",
                port = 443,
                protocol = com.example.data.model.RdpProtocol.WEB_RDP,
                webRdpUrl = "https://www.browserling.com/browse/win10/chrome/https%3A%2F%2Fgoogle.com",
                isEnabled = true
            ),
            com.example.data.model.RdpProfile(
                name = "🎬 100 Mbps Movie & Streaming Portal",
                mode = com.example.data.model.RdpMode.WEB_DESKTOP,
                host = "proxyium.com",
                port = 443,
                protocol = com.example.data.model.RdpProtocol.WEB_RDP,
                webRdpUrl = "https://proxyium.com",
                isEnabled = false
            ),
            com.example.data.model.RdpProfile(
                name = "🌐 Croxy Ultra-Fast Cloud Proxy (60 Mbps)",
                mode = com.example.data.model.RdpMode.WEB_DESKTOP,
                host = "croxyproxy.com",
                port = 443,
                protocol = com.example.data.model.RdpProtocol.WEB_RDP,
                webRdpUrl = "https://www.croxyproxy.com",
                isEnabled = false
            ),
            com.example.data.model.RdpProfile(
                name = "🛡️ BlockAway Privacy Cloud Tunnel",
                mode = com.example.data.model.RdpMode.WEB_DESKTOP,
                host = "blockaway.net",
                port = 443,
                protocol = com.example.data.model.RdpProtocol.WEB_RDP,
                webRdpUrl = "https://www.blockaway.net",
                isEnabled = false
            ),
            com.example.data.model.RdpProfile(
                name = "Custom RDP / Proxy Server (Manual)",
                mode = com.example.data.model.RdpMode.PROXY_TUNNEL,
                host = "192.168.1.100",
                port = 8080,
                protocol = com.example.data.model.RdpProtocol.HTTP,
                isEnabled = false
            )
        )
    }

    fun saveRdpProfiles(list: List<com.example.data.model.RdpProfile>) {
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
