package io.github.yueby.musictogether.network

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Logical server URL plus an optional address used only for DNS resolution.
 * The logical host remains in the request URL so HTTPS certificate, SNI and
 * Host stay on the configured domain while traffic can use a preferred IP.
 */
data class ServerAddress(
    val httpBase: HttpUrl,
    val preferredHost: String? = null,
) {
    val displayUrl: String = httpBase.toString().trimEnd('/')
    val webSocketUrl: String = httpBase.newBuilder()
        .addPathSegment("ws")
        .build()
        .toString()
        .replaceFirst(if (httpBase.isHttps) "https://" else "http://", if (httpBase.isHttps) "wss://" else "ws://")

    fun api(vararg segments: String): HttpUrl {
        val builder = httpBase.newBuilder().addPathSegment("api")
        segments.forEach(builder::addPathSegment)
        return builder.build()
    }

    companion object {
        fun parse(value: String, preferredHost: String? = null): ServerAddress? {
            val trimmed = value.trim().trimEnd('/')
            if (trimmed.isBlank()) return null
            val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                "http://$trimmed"
            }
            val base = withScheme.toHttpUrlOrNull() ?: return null
            val normalizedPreferred = preferredHost?.trim()?.trimEnd('/')?.takeIf { it.isNotBlank() }
                ?.let(::normalizePreferredHost)
            return ServerAddress(base, normalizedPreferred)
        }

        private fun normalizePreferredHost(value: String): String? {
            val candidate = if (value.startsWith("http://") || value.startsWith("https://")) {
                value
            } else {
                "http://$value"
            }
            return candidate.toHttpUrlOrNull()?.host?.takeIf { it.isNotBlank() }
        }
    }
}
