package io.github.yueby.musictogether.network

import org.json.JSONArray
import org.json.JSONObject

data class ServerEndpoint(
    val url: String,
    val preferredHost: String? = null,
)

object ServerCatalog {
    private const val MAX_SERVERS = 10

    fun decodeEndpoints(raw: String?, fallback: ServerEndpoint): List<ServerEndpoint> {
        val values = runCatching {
            val array = JSONArray(raw ?: "[]")
            List(array.length()) { index ->
                when (val value = array.get(index)) {
                    is JSONObject -> ServerEndpoint(
                        url = value.optString("url"),
                        preferredHost = value.optString("preferredHost").takeIf { it.isNotBlank() },
                    )
                    else -> ServerEndpoint(value.toString())
                }
            }
        }.getOrDefault(emptyList())
        return normalizeEndpoints(listOf(fallback) + values)
    }

    fun decode(raw: String?, fallback: String): List<String> =
        decodeEndpoints(raw, ServerEndpoint(fallback)).map(ServerEndpoint::url)

    fun encodeEndpoints(endpoints: List<ServerEndpoint>): String = JSONArray(
        normalizeEndpoints(endpoints).map { endpoint ->
            JSONObject().apply {
                put("url", endpoint.url)
                endpoint.preferredHost?.let { put("preferredHost", it) }
            }
        },
    ).toString()

    fun encode(urls: List<String>): String = encodeEndpoints(urls.map(::ServerEndpoint))

    fun normalizeEndpoints(endpoints: List<ServerEndpoint>): List<ServerEndpoint> = endpoints
        .mapNotNull { endpoint ->
            val address = ServerAddress.parse(endpoint.url, endpoint.preferredHost) ?: return@mapNotNull null
            ServerEndpoint(address.displayUrl, address.preferredHost)
        }
        .distinctBy(ServerEndpoint::url)
        .take(MAX_SERVERS)

    fun normalize(urls: List<String>): List<String> = urls
        .let { normalizeEndpoints(it.map(::ServerEndpoint)).map(ServerEndpoint::url) }
}
