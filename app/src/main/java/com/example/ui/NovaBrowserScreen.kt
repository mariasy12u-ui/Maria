package com.example.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ShortcutItem
import com.example.ui.components.BookmarksSheet
import com.example.ui.components.BrowserContextMenu
import com.example.ui.components.DownloadsSheet
import com.example.ui.components.EditRdpProfileDialog
import com.example.ui.components.ErrorPage
import com.example.ui.components.FindInPageBar
import com.example.ui.components.HistorySheet
import com.example.ui.components.NewTabDashboard
import com.example.ui.components.NovaBookmarksBar
import com.example.ui.components.NovaTabBar
import com.example.ui.components.NovaToolbar
import com.example.ui.components.OmniboxSuggestions
import com.example.ui.components.RdpManagerSheet
import com.example.ui.components.SecurityInfoSheet
import com.example.ui.components.SettingsSheet
import com.example.ui.components.ShortcutEditorDialog
import com.example.ui.components.TabsOverviewSheet
import com.example.ui.components.ZoomDialog
import com.example.viewmodel.BrowserSheet
import com.example.viewmodel.ClearDataRange
import com.example.viewmodel.NovaBrowserViewModel

@Composable
fun NovaBrowserScreen(
    viewModel: NovaBrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val focusManager = LocalFocusManager.current

    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull()

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val shortcuts by viewModel.shortcuts.collectAsStateWithLifecycle()

    val rdpProfiles by viewModel.rdpProfiles.collectAsStateWithLifecycle()
    val isRdpActive by viewModel.isRdpActive.collectAsStateWithLifecycle()
    val activeRdpProfile by viewModel.activeRdpProfile.collectAsStateWithLifecycle()
    val currentPublicIp by viewModel.currentPublicIp.collectAsStateWithLifecycle()
    val isCheckingIp by viewModel.isCheckingIp.collectAsStateWithLifecycle()

    val omniboxQuery by viewModel.omniboxQuery.collectAsStateWithLifecycle()
    val isOmniboxFocused by viewModel.isOmniboxFocused.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val activeSheet by viewModel.activeSheet.collectAsStateWithLifecycle()
    val isCurrentBookmarked by viewModel.isCurrentTabBookmarked.collectAsStateWithLifecycle()

    val findInPageActive by viewModel.findInPageActive.collectAsStateWithLifecycle()
    val findQuery by viewModel.findQuery.collectAsStateWithLifecycle()
    val findMatchCount by viewModel.findMatchCount.collectAsStateWithLifecycle()
    val findActiveIndex by viewModel.findActiveIndex.collectAsStateWithLifecycle()

    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val pageErrors by viewModel.pageErrors.collectAsStateWithLifecycle()
    val currentTabError = pageErrors[activeTabId]

    var showZoomDialog by remember { mutableStateOf(false) }

    // Toast snackbar handler
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissToast()
        }
    }

    // Android Hardware Back Handler
    BackHandler(enabled = true) {
        when {
            isOmniboxFocused -> {
                viewModel.setOmniboxFocused(false)
                focusManager.clearFocus()
            }
            findInPageActive -> {
                viewModel.closeFindInPage()
            }
            activeSheet !is BrowserSheet.None -> {
                viewModel.closeSheet()
            }
            activeTab?.canGoBack == true -> {
                viewModel.goBack()
            }
            tabs.size > 1 -> {
                activeTab?.let { viewModel.closeTab(it.id) }
            }
            else -> {
                activity?.finish()
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            )
        },
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Tab Bar: always visible in landscape or when multiple tabs exist
                if (isLandscape || tabs.size > 1) {
                    NovaTabBar(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        onSelectTab = { viewModel.selectTab(it) },
                        onCloseTab = { viewModel.closeTab(it) },
                        onNewTab = { viewModel.openNewTab() }
                    )
                }

                // Top Omnibox Navigation Toolbar
                NovaToolbar(
                    activeTab = activeTab,
                    tabCount = tabs.size,
                    settings = settings,
                    isBookmarked = isCurrentBookmarked,
                    isRdpActive = isRdpActive,
                    activeRdpHost = activeRdpProfile?.host,
                    omniboxQuery = omniboxQuery,
                    isOmniboxFocused = isOmniboxFocused,
                    onOmniboxQueryChange = { viewModel.setOmniboxQuery(it) },
                    onOmniboxFocusChange = { viewModel.setOmniboxFocused(it) },
                    onOmniboxSubmit = { viewModel.submitOmnibox(it) },
                    onBack = { viewModel.goBack() },
                    onForward = { viewModel.goForward() },
                    onReload = { viewModel.reload() },
                    onStop = { viewModel.stopLoading() },
                    onHome = { viewModel.goHome() },
                    onToggleBookmark = { viewModel.toggleCurrentBookmark() },
                    onOpenTabsGrid = { viewModel.openSheet(BrowserSheet.TabsGrid) },
                    onOpenSecurityInfo = { viewModel.openSheet(BrowserSheet.SiteSecurity) },
                    onOpenRdpManager = { viewModel.openSheet(BrowserSheet.RdpManager) },
                    onMenuAction = { action ->
                        when (action) {
                            "rdp_manager" -> viewModel.openSheet(BrowserSheet.RdpManager)
                            "new_tab" -> viewModel.openNewTab()
                            "new_incognito_tab" -> viewModel.openNewTab(isIncognito = true)
                            "reopen_tab" -> viewModel.reopenRecentlyClosedTab()
                            "bookmarks" -> viewModel.openSheet(BrowserSheet.Bookmarks)
                            "history" -> viewModel.openSheet(BrowserSheet.History)
                            "downloads" -> viewModel.openSheet(BrowserSheet.Downloads)
                            "find" -> viewModel.startFindInPage()
                            "desktop_mode" -> viewModel.toggleDesktopMode()
                            "share" -> viewModel.sharePage(context)
                            "print" -> activity?.let { viewModel.printPage(it) }
                            "zoom_dialog" -> showZoomDialog = true
                            "settings" -> viewModel.openSheet(BrowserSheet.Settings)
                        }
                    }
                )

                // Optional Bookmarks Bar
                if (settings.showBookmarksBar && bookmarks.isNotEmpty()) {
                    NovaBookmarksBar(
                        bookmarks = bookmarks,
                        onOpenBookmark = { viewModel.loadUrl(it) }
                    )
                }

                // Find In Page Bar (animated reveal)
                AnimatedVisibility(
                    visible = findInPageActive,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    FindInPageBar(
                        query = findQuery,
                        matchCount = findMatchCount,
                        activeIndex = findActiveIndex,
                        onQueryChange = { viewModel.setFindQuery(it) },
                        onNext = { viewModel.findNext() },
                        onPrevious = { viewModel.findPrevious() },
                        onClose = { viewModel.closeFindInPage() }
                    )
                }

                // Main Web Page / New Tab / Error Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (activeTab != null) {
                        when {
                            activeTab.isNewTab -> {
                                NewTabDashboard(
                                    isIncognito = activeTab.isIncognito,
                                    settings = settings,
                                    shortcuts = shortcuts,
                                    recentHistory = history,
                                    onOpenUrl = { viewModel.loadUrl(it) },
                                    onSearch = { viewModel.submitOmnibox(it) },
                                    onAddShortcut = { viewModel.openSheet(BrowserSheet.EditShortcut(null)) },
                                    onRemoveShortcut = { viewModel.removeShortcut(it) },
                                    onLaunchCloudRdp = {
                                        val cloudProfile = rdpProfiles.find { it.mode == com.example.data.model.RdpMode.WEB_DESKTOP } ?: rdpProfiles.firstOrNull()
                                        if (cloudProfile != null) {
                                            viewModel.openWebRdpSession(cloudProfile)
                                        } else {
                                            viewModel.openSheet(BrowserSheet.RdpManager)
                                        }
                                    }
                                )
                            }
                            currentTabError != null -> {
                                ErrorPage(
                                    failingUrl = currentTabError.third,
                                    description = currentTabError.second,
                                    onRetry = { viewModel.reload() },
                                    onGoBack = { viewModel.goBack() },
                                    onGoHome = { viewModel.goHome() }
                                )
                            }
                            else -> {
                                AndroidView(
                                    factory = { ctx ->
                                        viewModel.webViewManager.getOrCreateWebView(
                                            tab = activeTab,
                                            onPageStarted = { url -> viewModel.onPageStarted(activeTab.id, url) },
                                            onPageFinished = { url, title -> viewModel.onPageFinished(activeTab.id, url, title) },
                                            onProgressChanged = { progress -> viewModel.onProgressChanged(activeTab.id, progress) },
                                            onReceivedTitle = { title -> viewModel.onReceivedTitle(activeTab.id, title) },
                                            onReceivedError = { code, desc, url -> viewModel.onReceivedError(activeTab.id, code, desc, url) },
                                            onDownloadRequested = { url, ua, cd, mime, len ->
                                                viewModel.handleDownloadRequest(url, ua, cd, mime, len)
                                            },
                                            onLongClickHitResult = { type, extra ->
                                                if (extra != null) {
                                                    viewModel.openSheet(BrowserSheet.ContextMenu(type, extra))
                                                }
                                            }
                                        )
                                    },
                                    update = { view ->
                                        // Ensure url loaded if different
                                        if (view.url != activeTab.url && !activeTab.isNewTab && activeTab.url.isNotBlank()) {
                                            view.loadUrl(activeTab.url)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // Omnibox Suggestions Dropdown Overlay
                    if (isOmniboxFocused && suggestions.isNotEmpty()) {
                        OmniboxSuggestions(
                            suggestions = suggestions,
                            onSelectSuggestion = { suggestion ->
                                viewModel.loadUrl(suggestion.actionUrl)
                                viewModel.setOmniboxFocused(false)
                                focusManager.clearFocus()
                            },
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            }

            // MODAL SHEETS & DIALOGS
            when (val sheet = activeSheet) {
                is BrowserSheet.TabsGrid -> {
                    TabsOverviewSheet(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        onSelectTab = { viewModel.selectTab(it) },
                        onCloseTab = { viewModel.closeTab(it) },
                        onNewTab = { isIncognito -> viewModel.openNewTab(isIncognito = isIncognito) },
                        onCloseOtherTabs = { viewModel.closeOtherTabs(it) },
                        onReopenClosedTab = { viewModel.reopenRecentlyClosedTab() },
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                is BrowserSheet.Bookmarks -> {
                    BookmarksSheet(
                        bookmarks = bookmarks,
                        onOpenBookmark = { viewModel.loadUrl(it) },
                        onDeleteBookmark = { viewModel.deleteBookmark(it) },
                        onUpdateBookmark = { viewModel.updateBookmark(it) },
                        onAddBookmark = { title, url ->
                            viewModel.addBookmark(title, url)
                        },
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                is BrowserSheet.History -> {
                    HistorySheet(
                        history = history,
                        onOpenUrl = { viewModel.loadUrl(it) },
                        onDeleteEntry = { viewModel.deleteHistoryEntry(it) },
                        onClearRange = { viewModel.clearHistory(it) },
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                is BrowserSheet.Downloads -> {
                    DownloadsSheet(
                        downloads = downloads,
                        onDeleteDownload = { viewModel.deleteDownload(it) },
                        onClearAll = { viewModel.clearDownloads() },
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                is BrowserSheet.Settings -> {
                    SettingsSheet(
                        settings = settings,
                        onUpdateSettings = { viewModel.updateSettings(it) },
                        onClearAllData = { viewModel.clearAllBrowsingData(context) },
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                is BrowserSheet.SiteSecurity -> {
                    SecurityInfoSheet(
                        tab = activeTab,
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                is BrowserSheet.EditShortcut -> {
                    ShortcutEditorDialog(
                        initialShortcut = sheet.shortcut,
                        onSave = { title, url ->
                            if (sheet.shortcut == null) {
                                viewModel.addShortcut(title, url)
                            } else {
                                viewModel.updateShortcut(sheet.shortcut.id, title, url)
                            }
                        },
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                is BrowserSheet.RdpManager -> {
                    RdpManagerSheet(
                        isRdpActive = isRdpActive,
                        activeProfile = activeRdpProfile,
                        profiles = rdpProfiles,
                        currentPublicIp = currentPublicIp,
                        isCheckingIp = isCheckingIp,
                        onToggleRdp = { viewModel.toggleRdpInternet() },
                        onSelectProfile = { viewModel.selectRdpProfile(it) },
                        onEditProfile = { viewModel.openSheet(BrowserSheet.EditRdpProfile(it)) },
                        onDeleteProfile = { viewModel.deleteRdpProfile(it) },
                        onCheckIp = { viewModel.checkPublicIp() },
                        onOpenIpInTab = { viewModel.openIpCheckTab() },
                        onOpenWebRdp = { viewModel.openWebRdpSession(it) },
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                is BrowserSheet.EditRdpProfile -> {
                    EditRdpProfileDialog(
                        initialProfile = sheet.profile,
                        onSave = { viewModel.saveRdpProfile(it) },
                        onDismiss = { viewModel.openSheet(BrowserSheet.RdpManager) }
                    )
                }
                is BrowserSheet.ContextMenu -> {
                    BrowserContextMenu(
                        targetUrl = sheet.target,
                        onOpenInNewTab = { viewModel.openNewTab(it) },
                        onOpenInIncognito = { viewModel.openNewTab(it, isIncognito = true) },
                        onCopyLink = { url ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Link", url)
                            clipboard.setPrimaryClip(clip)
                            viewModel.showToast("Link copied to clipboard")
                        },
                        onShareLink = { url ->
                            viewModel.sharePage(context)
                        },
                        onDismiss = { viewModel.closeSheet() }
                    )
                }
                BrowserSheet.None, BrowserSheet.ClearData -> Unit
            }

            // Zoom Dialog
            if (showZoomDialog) {
                ZoomDialog(
                    onZoomIn = { viewModel.zoomIn() },
                    onZoomOut = { viewModel.zoomOut() },
                    onDismiss = { showZoomDialog = false }
                )
            }
        }
    }
}
