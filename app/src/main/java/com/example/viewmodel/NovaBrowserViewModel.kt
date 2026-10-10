package com.example.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import com.example.data.repository.BrowserRepository
import com.example.web.RdpNetworkManager
import com.example.web.TabWebViewManager
import com.example.web.WebUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface BrowserSheet {
    data object None : BrowserSheet
    data object TabsOverview : BrowserSheet
    data object Bookmarks : BrowserSheet
    data object History : BrowserSheet
    data object Downloads : BrowserSheet
    data object Settings : BrowserSheet
    data object SiteSecurity : BrowserSheet
    data object RdpManager : BrowserSheet
    data object MariaAi : BrowserSheet
    data class EditShortcut(val shortcut: ShortcutItem?) : BrowserSheet
    data class EditRdpProfile(val profile: RdpProfile?) : BrowserSheet
    data class ContextMenu(val target: String) : BrowserSheet
    data object Zoom : BrowserSheet
}

enum class ClearDataRange(val displayName: String, val durationMillis: Long) {
    LAST_HOUR("Last hour", 3600_000L),
    LAST_24_HOURS("Last 24 hours", 86400_000L),
    LAST_7_DAYS("Last 7 days", 7 * 86400_000L),
    ALL_TIME("All time", Long.MAX_VALUE)
}

enum class SuggestionType {
    SEARCH,
    DIRECT_URL,
    BOOKMARK,
    HISTORY
}

data class OmniboxSuggestion(
    val title: String,
    val subtitle: String,
    val type: SuggestionType,
    val actionUrl: String
)

class NovaBrowserViewModel(
    private val repository: BrowserRepository,
    val webViewManager: TabWebViewManager,
    val rdpNetworkManager: RdpNetworkManager
) : ViewModel(), TabWebViewManager.TabCallback {

    val settings: StateFlow<BrowserSettings> = repository.settings
    val bookmarks: StateFlow<List<Bookmark>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val history: StateFlow<List<HistoryEntry>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val downloads: StateFlow<List<DownloadEntry>> = repository.allDownloads
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val shortcuts: StateFlow<List<ShortcutItem>> = repository.shortcuts

    // RDP & Remote Internet State
    val rdpProfiles: StateFlow<List<RdpProfile>> = repository.rdpProfiles
    val isRdpActive: StateFlow<Boolean> = repository.isRdpActive
    val activeRdpProfile: StateFlow<RdpProfile?> = combine(rdpProfiles, isRdpActive) { list, active ->
        if (active) list.find { it.isEnabled } ?: list.firstOrNull() else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentPublicIp = MutableStateFlow<String?>(null)
    val currentPublicIp: StateFlow<String?> = _currentPublicIp.asStateFlow()

    private val _isCheckingIp = MutableStateFlow(false)
    val isCheckingIp: StateFlow<Boolean> = _isCheckingIp.asStateFlow()

    // 5-Minute USA IP Auto-Rotation
    val isAutoRotateEnabled: StateFlow<Boolean> = repository.isAutoRotateEnabled
    private val _autoRotateSecondsLeft = MutableStateFlow(300)
    val autoRotateSecondsLeft: StateFlow<Int> = _autoRotateSecondsLeft.asStateFlow()

    // Tabs State
    private val _tabs = MutableStateFlow<List<BrowserTab>>(listOf(BrowserTab()))
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String>(_tabs.value.first().id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val recentlyClosedTabs = ArrayDeque<BrowserTab>()

    // Omnibox state
    private val _omniboxQuery = MutableStateFlow("")
    val omniboxQuery: StateFlow<String> = _omniboxQuery.asStateFlow()

    private val _isOmniboxFocused = MutableStateFlow(false)
    val isOmniboxFocused: StateFlow<Boolean> = _isOmniboxFocused.asStateFlow()

    // Suggestions state
    private val _suggestions = MutableStateFlow<List<OmniboxSuggestion>>(emptyList())
    val suggestions: StateFlow<List<OmniboxSuggestion>> = _suggestions.asStateFlow()

    // Active sheet / modal
    private val _activeSheet = MutableStateFlow<BrowserSheet>(BrowserSheet.None)
    val activeSheet: StateFlow<BrowserSheet> = _activeSheet.asStateFlow()

    // Find in Page state
    private val _findInPageActive = MutableStateFlow(false)
    val findInPageActive: StateFlow<Boolean> = _findInPageActive.asStateFlow()

    private val _findQuery = MutableStateFlow("")
    val findQuery: StateFlow<String> = _findQuery.asStateFlow()

    private val _findMatchCount = MutableStateFlow(0)
    val findMatchCount: StateFlow<Int> = _findMatchCount.asStateFlow()

    private val _findActiveIndex = MutableStateFlow(0)
    val findActiveIndex: StateFlow<Int> = _findActiveIndex.asStateFlow()

    // Toast notification
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Error page state for active tab (if failed)
    private val _pageErrors = MutableStateFlow<Map<String, Triple<Int, String, String>>>(emptyMap())
    val pageErrors: StateFlow<Map<String, Triple<Int, String, String>>> = _pageErrors.asStateFlow()

    // Current page is bookmarked
    val isCurrentTabBookmarked: StateFlow<Boolean> = combine(_tabs, _activeTabId, repository.allBookmarks) { tabsList, activeId, bMarks ->
        val active = tabsList.find { it.id == activeId } ?: return@combine false
        bMarks.any { it.url == active.url }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        // Attempt session restore if configured
        viewModelScope.launch {
            val (savedTabs, savedActiveId) = repository.loadTabsSession()
            if (savedTabs.isNotEmpty() && settings.value.startupOption == com.example.data.model.StartupOption.CONTINUE_SESSION) {
                _tabs.value = savedTabs
                _activeTabId.value = savedActiveId ?: savedTabs.first().id
            } else if (settings.value.startupOption == com.example.data.model.StartupOption.CUSTOM_URL) {
                val customUrl = settings.value.customStartupUrl
                _tabs.value = listOf(BrowserTab(url = customUrl, title = customUrl))
                _activeTabId.value = _tabs.value.first().id
            }
        }

        // Auto-rotation timer loop: every second tick down when RDP is active and auto-rotate is enabled
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                if (isRdpActive.value && isAutoRotateEnabled.value) {
                    if (_autoRotateSecondsLeft.value > 1) {
                        _autoRotateSecondsLeft.value -= 1
                    } else {
                        _autoRotateSecondsLeft.value = 300
                        rotateUsaIpNow(isAutomatic = true)
                    }
                }
            }
        }
    }

    val activeTab: BrowserTab?
        get() = _tabs.value.find { it.id == _activeTabId.value }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun dismissToast() {
        _toastMessage.value = null
    }

    fun openSheet(sheet: BrowserSheet) {
        _activeSheet.value = sheet
    }

    fun closeSheet() {
        _activeSheet.value = BrowserSheet.None
    }

    fun setOmniboxFocused(focused: Boolean) {
        _isOmniboxFocused.value = focused
        if (focused) {
            val current = activeTab
            _omniboxQuery.value = if (current?.isNewTab == true) "" else current?.url ?: ""
            updateSuggestions(_omniboxQuery.value)
        } else {
            _suggestions.value = emptyList()
        }
    }

    fun updateOmniboxQuery(query: String) {
        _omniboxQuery.value = query
        updateSuggestions(query)
    }

    private fun updateSuggestions(query: String) {
        val trimmed = query.trim()
        val currentEngine = settings.value.searchEngine
        val list = mutableListOf<OmniboxSuggestion>()

        if (trimmed.isEmpty()) {
            _suggestions.value = emptyList()
            return
        }

        // Direct search suggestion
        list.add(
            OmniboxSuggestion(
                title = trimmed,
                subtitle = "Search with ${currentEngine.displayName}",
                type = SuggestionType.SEARCH,
                actionUrl = WebUtils.buildSearchUrl(trimmed, currentEngine)
            )
        )

        // If it looks like a URL
        if (trimmed.contains(".") || trimmed.startsWith("http")) {
            val resolved = WebUtils.resolveInput(trimmed, currentEngine)
            list.add(
                OmniboxSuggestion(
                    title = resolved,
                    subtitle = "Open website directly",
                    type = SuggestionType.DIRECT_URL,
                    actionUrl = resolved
                )
            )
        }

        // Bookmark suggestions
        val currentBookmarks = bookmarks.value
        val bookmarkMatches = currentBookmarks.filter {
            it.title.contains(trimmed, ignoreCase = true) || it.url.contains(trimmed, ignoreCase = true)
        }.take(3)
        bookmarkMatches.forEach { b ->
            list.add(
                OmniboxSuggestion(
                    title = b.title,
                    subtitle = b.url,
                    type = SuggestionType.BOOKMARK,
                    actionUrl = b.url
                )
            )
        }

        // History suggestions
        val currentHistory = history.value
        val historyMatches = currentHistory.filter {
            it.title.contains(trimmed, ignoreCase = true) || it.url.contains(trimmed, ignoreCase = true)
        }.take(3)
        historyMatches.forEach { h ->
            list.add(
                OmniboxSuggestion(
                    title = h.title,
                    subtitle = h.url,
                    type = SuggestionType.HISTORY,
                    actionUrl = h.url
                )
            )
        }

        _suggestions.value = list
    }

    fun submitOmnibox(input: String = _omniboxQuery.value) {
        val resolved = WebUtils.resolveInput(input, settings.value.searchEngine)
        loadUrl(resolved)
        setOmniboxFocused(false)
    }

    fun loadUrl(url: String, tabId: String? = null) {
        val targetId = tabId ?: _activeTabId.value
        clearErrorForTab(targetId)
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == targetId) {
                tab.copy(url = url, title = if (url == "nova://newtab") "New Tab" else WebUtils.extractDomain(url), isLoading = url != "nova://newtab")
            } else tab
        }

        if (url != "nova://newtab") {
            webViewManager.getWebView(targetId)?.loadUrl(url)
        }
        persistSession()
    }

    fun openNewTab(url: String = "nova://newtab", isIncognito: Boolean = false) {
        val newTab = BrowserTab(
            url = url,
            title = if (url == "nova://newtab") "New Tab" else WebUtils.extractDomain(url),
            isIncognito = isIncognito
        )
        _tabs.value = _tabs.value + newTab
        _activeTabId.value = newTab.id
        closeSheet()
        if (url != "nova://newtab") {
            loadUrl(url, newTab.id)
        }
        persistSession()
    }

    fun selectTab(tabId: String) {
        _activeTabId.value = tabId
        clearErrorForTab(tabId)
        closeSheet()
        persistSession()
    }

    fun closeTab(tabId: String) {
        val currentList = _tabs.value
        val tabToClose = currentList.find { it.id == tabId }
        if (tabToClose != null && !tabToClose.isIncognito) {
            recentlyClosedTabs.addLast(tabToClose)
            if (recentlyClosedTabs.size > 15) recentlyClosedTabs.removeFirst()
        }

        webViewManager.destroyWebView(tabId)

        if (currentList.size <= 1) {
            val freshTab = BrowserTab()
            _tabs.value = listOf(freshTab)
            _activeTabId.value = freshTab.id
        } else {
            val remaining = currentList.filter { it.id != tabId }
            _tabs.value = remaining
            if (_activeTabId.value == tabId) {
                _activeTabId.value = remaining.last().id
            }
        }
        persistSession()
    }

    fun closeOtherTabs(keptTabId: String) {
        val tabsToDestroy = _tabs.value.filter { it.id != keptTabId }
        tabsToDestroy.forEach { webViewManager.destroyWebView(it.id) }
        _tabs.value = _tabs.value.filter { it.id == keptTabId }
        _activeTabId.value = keptTabId
        persistSession()
    }

    fun reopenRecentlyClosedTab() {
        if (recentlyClosedTabs.isNotEmpty()) {
            val restored = recentlyClosedTabs.removeLast()
            _tabs.value = _tabs.value + restored
            _activeTabId.value = restored.id
            if (restored.url != "nova://newtab") {
                loadUrl(restored.url, restored.id)
            }
            showToast("Restored: ${restored.title}")
            persistSession()
        } else {
            showToast("No recently closed tabs")
        }
    }

    fun togglePinTab(tabId: String) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(isPinned = !it.isPinned) else it
        }
        persistSession()
    }

    fun toggleDesktopMode() {
        val current = activeTab ?: return
        val newMode = !current.isDesktopMode
        _tabs.value = _tabs.value.map {
            if (it.id == current.id) it.copy(isDesktopMode = newMode) else it
        }
        val wv = webViewManager.getWebView(current.id)
        if (wv != null) {
            webViewManager.applySettings(wv, settings.value, newMode, current.isIncognito)
            wv.reload()
        }
        showToast(if (newMode) "Desktop site requested" else "Mobile site requested")
    }

    fun goBack(): Boolean {
        val current = activeTab ?: return false
        val wv = webViewManager.getWebView(current.id)
        return if (wv != null && wv.canGoBack()) {
            wv.goBack()
            true
        } else false
    }

    fun goForward(): Boolean {
        val current = activeTab ?: return false
        val wv = webViewManager.getWebView(current.id)
        return if (wv != null && wv.canGoForward()) {
            wv.goForward()
            true
        } else false
    }

    fun reload() {
        val current = activeTab ?: return
        clearErrorForTab(current.id)
        webViewManager.getWebView(current.id)?.reload()
    }

    fun stop() {
        val current = activeTab ?: return
        webViewManager.getWebView(current.id)?.stopLoading()
        _tabs.value = _tabs.value.map {
            if (it.id == current.id) it.copy(isLoading = false) else it
        }
    }

    fun goHome() {
        loadUrl("nova://newtab")
    }

    // TabWebViewManager callbacks
    override fun onTitleChanged(tabId: String, title: String) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(title = title) else it
        }
    }

    override fun onUrlChanged(tabId: String, url: String) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(url = url) else it
        }
        val currentActive = activeTab
        if (currentActive?.id == tabId && !_isOmniboxFocused.value) {
            _omniboxQuery.value = if (url == "nova://newtab") "" else url
        }

        if (url != "nova://newtab" && !url.startsWith("about:")) {
            val tab = _tabs.value.find { it.id == tabId }
            if (tab?.isIncognito == false) {
                viewModelScope.launch {
                    repository.insertHistory(
                        HistoryEntry(
                            title = tab.title.ifBlank { WebUtils.extractDomain(url) },
                            url = url
                        )
                    )
                }
            }
        }
    }

    override fun onProgressChanged(tabId: String, progress: Int) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(progress = progress) else it
        }
    }

    override fun onLoadingStateChanged(tabId: String, isLoading: Boolean) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(isLoading = isLoading) else it
        }
    }

    override fun onCanGoBackForwardChanged(tabId: String, canGoBack: Boolean, canGoForward: Boolean) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(canGoBack = canGoBack, canGoForward = canGoForward) else it
        }
    }

    override fun onSecurityChanged(tabId: String, isSecure: Boolean) {
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(isSecure = isSecure) else it
        }
    }

    override fun onErrorReceived(tabId: String, errorCode: Int, description: String, failingUrl: String) {
        val map = _pageErrors.value.toMutableMap()
        map[tabId] = Triple(errorCode, description, failingUrl)
        _pageErrors.value = map
    }

    override fun onDownloadRequested(url: String, userAgent: String, contentDisposition: String, mimeType: String, contentLength: Long) {
        val fileName = Uri.parse(url).lastPathSegment ?: "downloadfile"
        viewModelScope.launch {
            repository.insertDownload(
                DownloadEntry(
                    fileName = fileName,
                    url = url,
                    mimeType = mimeType,
                    fileSizeBytes = contentLength
                )
            )
            showToast("Download started: $fileName")
        }
    }

    override fun onNewWindowRequested(url: String, isUserGesture: Boolean) {
        openNewTab(url = url)
    }

    fun clearErrorForTab(tabId: String) {
        val map = _pageErrors.value.toMutableMap()
        map.remove(tabId)
        _pageErrors.value = map
    }

    // Bookmarks management
    fun toggleCurrentBookmark() {
        val current = activeTab ?: return
        if (current.isNewTab) return

        viewModelScope.launch {
            val url = current.url
            if (isCurrentTabBookmarked.value) {
                repository.deleteBookmarkByUrl(url)
                showToast("Bookmark removed")
            } else {
                repository.insertBookmark(
                    Bookmark(
                        title = current.title.ifBlank { WebUtils.extractDomain(url) },
                        url = url
                    )
                )
                showToast("Bookmark added")
            }
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmark)
            showToast("Bookmark deleted")
        }
    }

    fun updateBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            repository.updateBookmark(bookmark)
            showToast("Bookmark updated")
        }
    }

    fun addBookmark(title: String, url: String) {
        viewModelScope.launch {
            repository.insertBookmark(
                Bookmark(
                    title = title.ifBlank { WebUtils.extractDomain(url) },
                    url = url
                )
            )
            showToast("Bookmark added")
        }
    }

    // History management
    fun deleteHistoryEntry(entry: HistoryEntry) {
        viewModelScope.launch {
            repository.deleteHistory(entry)
        }
    }

    fun clearHistory(range: ClearDataRange) {
        viewModelScope.launch {
            if (range == ClearDataRange.ALL_TIME) {
                repository.clearAllHistory()
            } else {
                val cutoff = System.currentTimeMillis() - range.durationMillis
                repository.clearHistorySince(cutoff)
            }
            showToast("History cleared (${range.displayName})")
        }
    }

    // Downloads management
    fun deleteDownload(entry: DownloadEntry) {
        viewModelScope.launch {
            repository.deleteDownload(entry)
        }
    }

    fun clearDownloads() {
        viewModelScope.launch {
            repository.clearAllDownloads()
            showToast("Downloads list cleared")
        }
    }

    // Clear all browsing data
    fun clearAllBrowsingData(context: Context) {
        viewModelScope.launch {
            repository.clearAllHistory()
            WebStorage.getInstance().deleteAllData()
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            showToast("Browsing data, cookies & cache cleared")
        }
    }

    // Speed Dial Shortcuts
    fun addShortcut(title: String, url: String) {
        val resolved = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
        val current = shortcuts.value.toMutableList()
        current.add(ShortcutItem(title = title, url = resolved))
        repository.saveShortcuts(current)
        showToast("Shortcut added")
    }

    fun updateShortcut(id: String, title: String, url: String) {
        val resolved = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
        val updated = shortcuts.value.map {
            if (it.id == id) it.copy(title = title, url = resolved) else it
        }
        repository.saveShortcuts(updated)
        showToast("Shortcut updated")
    }

    fun removeShortcut(id: String) {
        val updated = shortcuts.value.filter { it.id != id }
        repository.saveShortcuts(updated)
        showToast("Shortcut removed")
    }

    // Settings
    fun updateSettings(newSettings: BrowserSettings) {
        repository.updateSettings(newSettings)
        showToast("Settings saved")
    }

    // Find in Page
    fun startFindInPage() {
        _findInPageActive.value = true
        _findQuery.value = ""
        _findMatchCount.value = 0
        _findActiveIndex.value = 0
    }

    fun setFindQuery(query: String) {
        _findQuery.value = query
        val current = activeTab ?: return
        val wv = webViewManager.getWebView(current.id)
        if (wv != null && query.isNotBlank()) {
            wv.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
                _findMatchCount.value = numberOfMatches
                _findActiveIndex.value = if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0
            }
            wv.findAllAsync(query)
        } else {
            wv?.clearMatches()
            _findMatchCount.value = 0
            _findActiveIndex.value = 0
        }
    }

    fun findNext() {
        val current = activeTab ?: return
        webViewManager.getWebView(current.id)?.findNext(true)
    }

    fun findPrevious() {
        val current = activeTab ?: return
        webViewManager.getWebView(current.id)?.findNext(false)
    }

    fun closeFindInPage() {
        _findInPageActive.value = false
        val current = activeTab ?: return
        webViewManager.getWebView(current.id)?.clearMatches()
    }

    // Zoom
    fun zoomIn() {
        val current = activeTab ?: return
        webViewManager.getWebView(current.id)?.zoomIn()
    }

    fun zoomOut() {
        val current = activeTab ?: return
        webViewManager.getWebView(current.id)?.zoomOut()
    }

    // Print
    fun printPage(activity: Activity) {
        val current = activeTab ?: return
        val wv = webViewManager.getWebView(current.id) ?: return
        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        val printAdapter = wv.createPrintDocumentAdapter(current.title.ifBlank { "Nova_Document" })
        printManager?.print(current.title.ifBlank { "Nova_Document" }, printAdapter, PrintAttributes.Builder().build())
    }

    // Share
    fun sharePage(context: Context) {
        val current = activeTab ?: return
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, current.url)
            putExtra(Intent.EXTRA_SUBJECT, current.title)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share via Maria Browser")
        context.startActivity(shareIntent)
    }

    // RDP / Remote Internet Management
    fun toggleRdpInternet() {
        val currentActive = repository.isRdpActive.value
        val newActive = !currentActive
        repository.setRdpActive(newActive)

        if (newActive) {
            val profile = activeRdpProfile.value ?: rdpProfiles.value.firstOrNull()
            if (profile != null) {
                applyRdpRouting(profile)
            } else {
                showToast("No RDP profile configured")
            }
        } else {
            rdpNetworkManager.clearProxy {
                showToast("Switched back to local mobile internet")
                reload()
            }
        }
    }

    private fun applyRdpRouting(profile: RdpProfile) {
        rdpNetworkManager.applyRdpProxy(profile) { success, msg ->
            showToast(msg)
            if (success) {
                checkPublicIp()
                reload()
            }
        }
    }

    fun selectRdpProfile(profile: RdpProfile) {
        val updated = rdpProfiles.value.map {
            it.copy(isEnabled = it.id == profile.id)
        }
        repository.saveRdpProfiles(updated)
        if (isRdpActive.value) {
            applyRdpRouting(profile)
        } else {
            showToast("Selected: ${profile.name}")
        }
    }

    fun saveRdpProfile(profile: RdpProfile) {
        val current = rdpProfiles.value.toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            current[index] = profile
        } else {
            current.add(profile)
        }
        repository.saveRdpProfiles(current)
        if (isRdpActive.value && profile.isEnabled) {
            applyRdpRouting(profile)
        }
        showToast("RDP profile saved")
    }

    fun deleteRdpProfile(profileId: String) {
        val updated = rdpProfiles.value.filter { it.id != profileId }
        repository.saveRdpProfiles(updated)
        showToast("RDP profile removed")
    }

    fun checkPublicIp() {
        _isCheckingIp.value = true
        _currentPublicIp.value = "Checking IP..."
        val prof = if (isRdpActive.value) activeRdpProfile.value else null
        rdpNetworkManager.checkPublicIp(prof) { ip, err ->
            _isCheckingIp.value = false
            if (ip != null) {
                _currentPublicIp.value = ip
            } else {
                _currentPublicIp.value = err ?: "Unavailable"
            }
        }
    }

    fun toggleAutoRotate() {
        val next = !isAutoRotateEnabled.value
        repository.setAutoRotateEnabled(next)
        if (next) _autoRotateSecondsLeft.value = 300
        showToast(if (next) "Auto-rotate USA IP every 5 min enabled" else "Auto-rotate paused")
    }

    fun rotateUsaIpNow(isAutomatic: Boolean = false) {
        val tunnelProfiles = rdpProfiles.value.filter { it.mode == RdpMode.PROXY_TUNNEL }
        if (tunnelProfiles.isNotEmpty()) {
            val currentIndex = tunnelProfiles.indexOfFirst { it.isEnabled }
            val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % tunnelProfiles.size else 0
            val nextProfile = tunnelProfiles[nextIndex]
            selectRdpProfile(nextProfile)
            _autoRotateSecondsLeft.value = 300
            showToast(if (isAutomatic) "🇺🇸 Auto-rotated USA IP to ${nextProfile.name}" else "🇺🇸 Switched to ${nextProfile.name}")
        }
    }

    fun openWebRdpSession(profile: RdpProfile) {
        val url = if (profile.webRdpUrl.isNotBlank()) profile.webRdpUrl else "http://${profile.host}:${profile.port}"
        openNewTab(url = url)
        closeSheet()
        showToast("Connecting to Web RDP: ${profile.name}")
    }

    fun openIpCheckTab() {
        openNewTab("https://api.ipify.org")
        closeSheet()
    }

    private fun persistSession() {
        repository.saveTabsSession(_tabs.value, _activeTabId.value)
    }

    override fun onCleared() {
        super.onCleared()
        webViewManager.destroyAll()
    }
}

class NovaBrowserViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = BrowserRepository(context)
        val wvManager = TabWebViewManager(context)
        val rdpManager = RdpNetworkManager(context)
        return NovaBrowserViewModel(repo, wvManager, rdpManager) as T
    }
}
