package com.example.data.model

enum class RdpMode(val displayName: String) {
    PROXY_TUNNEL("Remote Internet Routing (Proxy)"),
    WEB_DESKTOP("Web Remote Desktop (Guacamole/VNC)")
}

enum class RdpProtocol(val displayName: String, val defaultPort: Int) {
    HTTP("HTTP Proxy", 8080),
    HTTPS("HTTPS Proxy", 8443),
    SOCKS5("SOCKS5 Proxy", 1080),
    WEB_RDP("Web RDP Gateway", 8080)
}

data class RdpProfile(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val mode: RdpMode = RdpMode.PROXY_TUNNEL,
    val host: String,
    val port: Int = 8080,
    val protocol: RdpProtocol = RdpProtocol.HTTP,
    val username: String = "",
    val password: String = "",
    val webRdpUrl: String = "",
    val isEnabled: Boolean = false
) {
    val proxyRule: String
        get() = when (protocol) {
            RdpProtocol.HTTP -> "http://$host:$port"
            RdpProtocol.HTTPS -> "https://$host:$port"
            RdpProtocol.SOCKS5 -> "socks5://$host:$port"
            RdpProtocol.WEB_RDP -> "http://$host:$port"
        }

    val displayAddress: String
        get() = if (mode == RdpMode.WEB_DESKTOP && webRdpUrl.isNotBlank()) webRdpUrl else "$host:$port"
}
