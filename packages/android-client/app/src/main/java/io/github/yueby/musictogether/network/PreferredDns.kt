package io.github.yueby.musictogether.network

import okhttp3.Dns
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

/** Resolves a logical server host through its optional preferred IP/domain. */
internal class PreferredDns : Dns {
    private val preferredHosts = ConcurrentHashMap<String, String>()

    fun register(server: ServerAddress) {
        val preferred = server.preferredHost
        if (preferred.isNullOrBlank() || preferred.equals(server.httpBase.host, ignoreCase = true)) {
            preferredHosts.remove(server.httpBase.host.lowercase())
        } else {
            preferredHosts[server.httpBase.host.lowercase()] = preferred
        }
    }

    override fun lookup(hostname: String): List<InetAddress> {
        val preferred = preferredHosts[hostname.lowercase()]
        return if (preferred.isNullOrBlank()) Dns.SYSTEM.lookup(hostname) else Dns.SYSTEM.lookup(preferred)
    }
}
