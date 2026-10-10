package com.example.web

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.example.data.model.RdpProfile
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class RdpNetworkManager(private val context: Context) {
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "RdpNetworkManager"
    }

    fun applyRdpProxy(
        profile: RdpProfile,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        if (profile.mode == com.example.data.model.RdpMode.WEB_DESKTOP) {
            onResult(true, "Web Desktop mode: Direct cloud screen accessible in new tab")
            return
        }

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            onResult(false, "Proxy override not supported by current Android WebView engine")
            return
        }

        try {
            val proxyRule = profile.proxyRule
            val proxyConfig = ProxyConfig.Builder()
                .addProxyRule(proxyRule)
                .addBypassRule("<local>")
                .build()

            ProxyController.getInstance().setProxyOverride(
                proxyConfig,
                executor,
                Runnable {
                    mainHandler.post {
                        Log.d(TAG, "Proxy override applied: $proxyRule")
                        onResult(true, "🇺🇸 RDP Internet Tunnel Active via ${profile.name} (${profile.host}:${profile.port})")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply proxy override", e)
            onResult(false, "Failed to route internet: ${e.message}")
        }
    }

    fun clearProxy(onComplete: () -> Unit) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            try {
                ProxyController.getInstance().clearProxyOverride(
                    executor,
                    Runnable {
                        mainHandler.post {
                            Log.d(TAG, "Proxy override cleared")
                            onComplete()
                        }
                    }
                )
                return
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear proxy override", e)
            }
        }
        onComplete()
    }

    fun checkPublicIp(
        activeProfile: RdpProfile?,
        callback: (ip: String?, error: String?) -> Unit
    ) {
        executor.execute {
            try {
                val url = URL("https://api.ipify.org")
                val useProxy = activeProfile != null &&
                        activeProfile.isEnabled &&
                        activeProfile.mode == com.example.data.model.RdpMode.PROXY_TUNNEL &&
                        activeProfile.host.isNotBlank()

                val connection = if (useProxy) {
                    val proxyType = when (activeProfile!!.protocol) {
                        com.example.data.model.RdpProtocol.SOCKS5 -> Proxy.Type.SOCKS
                        else -> Proxy.Type.HTTP
                    }
                    val proxy = Proxy(proxyType, InetSocketAddress(activeProfile.host, activeProfile.port))
                    url.openConnection(proxy) as HttpURLConnection
                } else {
                    url.openConnection() as HttpURLConnection
                }

                connection.connectTimeout = 6000
                connection.readTimeout = 6000
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val ip = connection.inputStream.bufferedReader().use { it.readText() }.trim()
                    mainHandler.post { callback(ip, null) }
                } else {
                    mainHandler.post { callback(null, "HTTP $responseCode") }
                }
                connection.disconnect()
            } catch (e: Exception) {
                mainHandler.post { callback(null, e.localizedMessage ?: "Connection error") }
            }
        }
    }
}
