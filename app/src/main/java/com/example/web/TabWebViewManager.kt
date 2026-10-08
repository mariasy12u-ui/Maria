package com.example.web

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.model.BrowserTab
import java.util.concurrent.ConcurrentHashMap

class TabWebViewManager(private val context: Context) {
    private val webViewMap = ConcurrentHashMap<String, WebView>()

    companion object {
        const val DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
    }

    fun getOrCreateWebView(
        tab: BrowserTab,
        onPageStarted: (url: String) -> Unit,
        onPageFinished: (url: String, title: String) -> Unit,
        onProgressChanged: (progress: Int) -> Unit,
        onReceivedTitle: (title: String) -> Unit,
        onReceivedError: (errorCode: Int, description: String, failingUrl: String) -> Unit,
        onDownloadRequested: (url: String, userAgent: String, contentDisposition: String, mimeType: String, contentLength: Long) -> Unit,
        onLongClickHitResult: (type: Int, extra: String?) -> Unit
    ): WebView {
        val existing = webViewMap[tab.id]
        if (existing != null) {
            updateSettingsForTab(existing, tab)
            return existing
        }

        val newWebView = createConfiguredWebView(
            tab = tab,
            onPageStarted = onPageStarted,
            onPageFinished = onPageFinished,
            onProgressChanged = onProgressChanged,
            onReceivedTitle = onReceivedTitle,
            onReceivedError = onReceivedError,
            onDownloadRequested = onDownloadRequested,
            onLongClickHitResult = onLongClickHitResult
        )
        webViewMap[tab.id] = newWebView

        if (!tab.isNewTab) {
            newWebView.loadUrl(tab.url)
        }

        return newWebView
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createConfiguredWebView(
        tab: BrowserTab,
        onPageStarted: (url: String) -> Unit,
        onPageFinished: (url: String, title: String) -> Unit,
        onProgressChanged: (progress: Int) -> Unit,
        onReceivedTitle: (title: String) -> Unit,
        onReceivedError: (errorCode: Int, description: String, failingUrl: String) -> Unit,
        onDownloadRequested: (url: String, userAgent: String, contentDisposition: String, mimeType: String, contentLength: Long) -> Unit,
        onLongClickHitResult: (type: Int, extra: String?) -> Unit
    ): WebView {
        val webView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isFocusable = true
            isFocusableInTouchMode = true
        }

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        if (tab.isDesktopMode) {
            settings.userAgentString = DESKTOP_USER_AGENT
        }

        if (tab.isIncognito) {
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            webView.clearCache(true)
            webView.clearFormData()
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptThirdPartyCookies(webView, false)
        } else {
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, false)
        }

        val trackerDomains = arrayOf(
            "google-analytics.com",
            "googletagmanager.com",
            "doubleclick.net",
            "adservice.google.com",
            "scorecardresearch.com",
            "criteo.com",
            "adnxs.com",
            "facebook.net/tr",
            "taboola.com",
            "outbrain.com",
            "popads.net",
            "propellerads.com"
        )

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val reqUrl = request?.url?.toString() ?: return null
                for (tracker in trackerDomains) {
                    if (reqUrl.contains(tracker, ignoreCase = true)) {
                        return WebResourceResponse(
                            "text/plain",
                            "UTF-8",
                            java.io.ByteArrayInputStream(ByteArray(0))
                        )
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let { onPageStarted(it) }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val currentUrl = url ?: view?.url ?: ""
                val currentTitle = view?.title ?: ""
                onPageFinished(currentUrl, currentTitle)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        error?.errorCode ?: -1
                    } else -1
                    val desc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        error?.description?.toString() ?: "Unknown error"
                    } else "Network error"
                    val failingUrl = request.url?.toString() ?: ""
                    onReceivedError(code, desc, failingUrl)
                }
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                // By default for security, cancel untrusted SSL
                handler?.cancel()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                onProgressChanged(newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                title?.let { onReceivedTitle(it) }
            }
        }

        webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            onDownloadRequested(url, userAgent, contentDisposition, mimetype, contentLength)
        })

        webView.setOnLongClickListener {
            val result = webView.hitTestResult
            val type = result.type
            val extra = result.extra
            if (type != WebView.HitTestResult.UNKNOWN_TYPE && extra != null) {
                onLongClickHitResult(type, extra)
                true
            } else {
                false
            }
        }

        return webView
    }

    private fun updateSettingsForTab(webView: WebView, tab: BrowserTab) {
        val settings = webView.settings
        val currentUa = settings.userAgentString
        if (tab.isDesktopMode && currentUa != DESKTOP_USER_AGENT) {
            settings.userAgentString = DESKTOP_USER_AGENT
            webView.reload()
        } else if (!tab.isDesktopMode && currentUa == DESKTOP_USER_AGENT) {
            settings.userAgentString = null // Reset to default
            webView.reload()
        }
    }

    fun removeWebView(tabId: String) {
        val webView = webViewMap.remove(tabId)
        webView?.let {
            it.stopLoading()
            it.clearHistory()
            it.removeAllViews()
            it.destroy()
        }
    }

    fun getWebView(tabId: String): WebView? = webViewMap[tabId]

    fun destroyAll() {
        for ((_, wv) in webViewMap) {
            wv.stopLoading()
            wv.clearHistory()
            wv.removeAllViews()
            wv.destroy()
        }
        webViewMap.clear()
    }
}
