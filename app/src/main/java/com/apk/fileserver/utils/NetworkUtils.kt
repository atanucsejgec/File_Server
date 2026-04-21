package com.apk.fileserver.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {

    // ═══════════════════════════════════════════════
    //              GET LOCAL IP ADDRESS
    // ═══════════════════════════════════════════════

    /**
     * Gets device IP - works for WiFi AND Mobile Hotspot
     */
    fun getLocalIpAddress(context: Context): String {
        // Method 1: NetworkInterface (best for hotspot + wifi)
        val networkIp = getIpFromNetworkInterface()
        if (networkIp != null && networkIp != "0.0.0.0") {
            return networkIp
        }

        // Method 2: WifiManager fallback
        val wifiIp = getIpFromWifiManager(context)
        if (wifiIp != null && wifiIp != "0.0.0.0") {
            return wifiIp
        }

        // Method 3: localhost fallback
        return "127.0.0.1"
    }

    /**
     * NetworkInterface - finds ALL network interfaces
     * Works for: WiFi client, Mobile Hotspot, USB tethering
     */
    private fun getIpFromNetworkInterface(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
                ?.toList() ?: return null

            // Priority order for interface names
            val priorityPrefixes = listOf(
                "wlan",   // WiFi client
                "ap",     // Access Point (Hotspot)
                "swlan",  // Software WLAN (Hotspot on some devices)
                "rndis",  // USB tethering
                "eth",    // Ethernet
                "rmnet"   // Mobile data
            )

            // Try priority interfaces first
            for (prefix in priorityPrefixes) {
                for (networkInterface in interfaces) {
                    val name = networkInterface.name.lowercase()
                    if (!name.startsWith(prefix)) continue
                    if (networkInterface.isLoopback) continue
                    if (!networkInterface.isUp) continue

                    for (address in networkInterface.inetAddresses) {
                        if (!address.isLoopbackAddress &&
                            address is Inet4Address
                        ) {
                            val ip = address.hostAddress ?: continue
                            if (ip != "0.0.0.0") return ip
                        }
                    }
                }
            }

            // Fallback: any non-loopback IPv4
            for (networkInterface in interfaces) {
                if (networkInterface.isLoopback) continue
                if (!networkInterface.isUp) continue

                for (address in networkInterface.inetAddresses) {
                    if (!address.isLoopbackAddress &&
                        address is Inet4Address
                    ) {
                        val ip = address.hostAddress ?: continue
                        if (ip != "0.0.0.0") return ip
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun getIpFromWifiManager(context: Context): String? {
        return try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ipInt = wifiInfo.ipAddress
            if (ipInt == 0) return null
            String.format(
                "%d.%d.%d.%d",
                ipInt and 0xff,
                ipInt shr 8 and 0xff,
                ipInt shr 16 and 0xff,
                ipInt shr 24 and 0xff
            )
        } catch (e: Exception) {
            null
        }
    }

    // ═══════════════════════════════════════════════
    //           CHECK NETWORK CONNECTION
    // ═══════════════════════════════════════════════

    /**
     * Check if device has any usable network
     * Includes WiFi client AND Mobile Hotspot
     */
    fun isWifiConnected(context: Context): Boolean {
        // First check if we have a valid non-loopback IP
        // This works for hotspot even without "wifi connected" state
        val ip = getIpFromNetworkInterface()
        if (ip != null && ip != "127.0.0.1" && ip != "0.0.0.0") {
            return true
        }

        // Fallback: ConnectivityManager check
        val connectivityManager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
                ?: return false
            val capabilities = connectivityManager
                .getNetworkCapabilities(network) ?: return false

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo != null && networkInfo.isConnected
        }
    }

    // ═══════════════════════════════════════════════
    //           BUILD SERVER URL
    // ═══════════════════════════════════════════════

    fun buildServerUrl(context: Context, port: Int): String {
        val ip = getLocalIpAddress(context)
        return "http://$ip:$port"
    }

    fun buildQrContent(context: Context, port: Int): String {
        return buildServerUrl(context, port)
    }

    fun getFormattedIp(context: Context): String {
        return getLocalIpAddress(context)
    }

    fun isValidLocalIp(ip: String): Boolean {
        if (ip == "127.0.0.1" || ip == "0.0.0.0") return false
        return ip.matches(
            Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")
        )
    }

    /**
     * Get all available IPs (for display)
     * Shows WiFi IP, Hotspot IP etc
     */
    fun getAllAvailableIps(): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
                ?.toList() ?: return result

            for (networkInterface in interfaces) {
                if (networkInterface.isLoopback) continue
                if (!networkInterface.isUp) continue

                val name = networkInterface.name.lowercase()
                val label = when {
                    name.startsWith("wlan")  -> "WiFi"
                    name.startsWith("ap")    -> "Hotspot"
                    name.startsWith("swlan") -> "Hotspot"
                    name.startsWith("rndis") -> "USB Tether"
                    name.startsWith("eth")   -> "Ethernet"
                    else -> name
                }

                for (address in networkInterface.inetAddresses) {
                    if (!address.isLoopbackAddress &&
                        address is Inet4Address
                    ) {
                        val ip = address.hostAddress ?: continue
                        if (ip != "0.0.0.0") {
                            result.add(Pair(label, ip))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return result
    }
}