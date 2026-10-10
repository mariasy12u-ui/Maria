package com.example.web

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.model.BrowserSettings
import com.example.data.model.BrowserTab
import java.util.concurrent.ConcurrentHashMap

class TabWebViewManager(private val context: Context) {
    private val webViewMap = ConcurrentHashMap<String, WebView>()

    private val desktopUserAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private var defaultUserAgent: String = ""

    interface TabCallback {
        fun onTitleChanged(tabId: String, title: String)
        fun onUrlChanged(tabId: String, url: String)
        fun onProgressChanged(tabId: String, progress: Int)
        fun onLoadingStateChanged(tabId: String, isLoading: Boolean)
        fun onCanGoBackForwardChanged(tabId: String, canGoBack: Boolean, canGoForward: Boolean)
        fun onSecurityChanged(tabId: String, isSecure: Boolean)
        fun onErrorReceived(tabId: String, errorCode: Int, description: String, failingUrl: String)
        fun onDownloadRequested(url: String, userAgent: String, contentDisposition: String, mimeType: String, contentLength: Long)
        fun onNewWindowRequested(url: String, isUserGesture: Boolean)
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(
        tab: BrowserTab,
        settings: BrowserSettings,
        callback: TabCallback
    ): WebView {
        return webViewMap.getOrPut(tab.id) {
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                if (defaultUserAgent.isEmpty()) {
                    defaultUserAgent = this.settings.userAgentString
                }

                applySettings(this, settings, tab.isDesktopMode, tab.isIncognito)

                setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                    callback.onDownloadRequested(url, userAgent, contentDisposition, mimetype, contentLength)
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        val u = url ?: ""
                        callback.onUrlChanged(tab.id, u)
                        callback.onLoadingStateChanged(tab.id, true)
                        callback.onCanGoBackForwardChanged(tab.id, view?.canGoBack() == true, view?.canGoForward() == true)
                        callback.onSecurityChanged(tab.id, WebUtils.isSecure(u))
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val u = url ?: ""
                        callback.onLoadingStateChanged(tab.id, false)
                        callback.onCanGoBackForwardChanged(tab.id, view?.canGoBack() == true, view?.canGoForward() == true)
                        callback.onSecurityChanged(tab.id, WebUtils.isSecure(u))
                        view?.title?.let { t ->
                            if (t.isNotBlank()) callback.onTitleChanged(tab.id, t)
                        }
                    }

                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            val errCode = error?.errorCode ?: -1
                            val desc = error?.description?.toString() ?: "Unknown error"
                            val failingUrl = request.url.toString()
                            callback.onErrorReceived(tab.id, errCode, desc, failingUrl)
                        }
                    }

                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                        callback.onSecurityChanged(tab.id, false)
                        handler?.proceed()
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        callback.onProgressChanged(tab.id, newProgress)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let {
                            if (it.isNotBlank()) callback.onTitleChanged(tab.id, it)
                        }
                    }

                    override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                        val transport = resultMsg?.obj as? WebView.WebViewTransport
                        val tempWv = WebView(context)
                        tempWv.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(v: WebView?, req: WebResourceRequest?): Boolean {
                                req?.url?.toString()?.let { newUrl ->
                                    callback.onNewWindowRequested(newUrl, isUserGesture)
                                }
                                return true
                            }
                        }
                        transport?.webView = tempWv
                        resultMsg?.sendToTarget()
                        return true
                    }
                }

                if (tab.url != "nova://newtab") {
                    loadUrl(tab.url)
                }
            }
        }
    }

    fun applySettings(wv: WebView, settings: BrowserSettings, isDesktop: Boolean, isIncognito: Boolean) {
        wv.settings.apply {
            javaScriptEnabled = settings.javaScriptEnabled
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = if (isDesktop) desktopUserAgent else defaultUserAgent
        }

        if (isIncognito) {
            CookieManager.getInstance().setAcceptCookie(false)
        } else {
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(wv, !settings.blockThirdPartyCookies)
        }
    }

    fun getWebView(tabId: String): WebView? = webViewMap[tabId]

    fun destroyWebView(tabId: String) {
        webViewMap.remove(tabId)?.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }
    }

    fun destroyAll() {
        webViewMap.forEach { (_, wv) ->
            wv.stopLoading()
            wv.loadUrl("about:blank")
            wv.clearHistory()
            wv.removeAllViews()
            wv.destroy()
        }
        webViewMap.clear()
    }
}
