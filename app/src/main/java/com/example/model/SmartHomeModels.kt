package com.example.model

/**
 * Feed keys supported for Adafruit IO integration with ESP8266 NodeMCU (DEVICE 1).
 * Exact feeds required by user:
 *   socket-1
 *   socket-2
 *   socket-3
 *   socket-4
 *   fan-speed
 *   device-status
 *
 * Physical GPIO Pin Mapping on ESP8266 Smart Home board:
 *   Relay 1 = GPIO15 (D8) -> Feed: socket-1 (alias: relay1)
 *   Relay 2 = GPIO13 (D7) -> Feed: socket-2 (alias: relay2)
 *   Relay 3 = GPIO12 (D6) -> Feed: socket-3 (alias: relay3)
 *   Relay 4 = GPIO14 (D5) -> Feed: socket-4 (alias: relay4)
 *   Physical Switch 1 = GPIO2 (D4)
 *   Physical Switch 2 = GPIO0 (D3)
 *   Physical Switch 3 = GPIO4 (D2)
 *   Physical Switch 4 = GPIO5 (D1)
 *   Fan PWM = GPIO16 (D0) -> Feed: fan-speed
 */
object FeedConstants {
    const val HOST = "io.adafruit.com"
    const val PORT = 8883
    const val PROTOCOL = "ssl" // TLS URI ssl://io.adafruit.com:8883

    // Exact required feed keys
    const val FEED_SOCKET_1 = "socket-1"
    const val FEED_SOCKET_2 = "socket-2"
    const val FEED_SOCKET_3 = "socket-3"
    const val FEED_SOCKET_4 = "socket-4"

    // Backwards-compatible aliases if broker or legacy code refers to relay1..4
    const val ALIAS_RELAY_1 = "relay1"
    const val ALIAS_RELAY_2 = "relay2"
    const val ALIAS_RELAY_3 = "relay3"
    const val ALIAS_RELAY_4 = "relay4"

    const val FEED_FAN_SPEED = "fan-speed"
    const val FEED_DEVICE_STATUS = "device-status"

    val ALL_FEEDS = listOf(
        FEED_SOCKET_1,
        FEED_SOCKET_2,
        FEED_SOCKET_3,
        FEED_SOCKET_4,
        ALIAS_RELAY_1,
        ALIAS_RELAY_2,
        ALIAS_RELAY_3,
        ALIAS_RELAY_4,
        FEED_FAN_SPEED,
        FEED_DEVICE_STATUS
    )

    fun normalizeFeedKey(rawKey: String): String {
        return when (rawKey) {
            ALIAS_RELAY_1 -> FEED_SOCKET_1
            ALIAS_RELAY_2 -> FEED_SOCKET_2
            ALIAS_RELAY_3 -> FEED_SOCKET_3
            ALIAS_RELAY_4 -> FEED_SOCKET_4
            else -> rawKey
        }
    }

    fun topicForFeed(username: String, feedKey: String): String {
        val cleanUsername = username.trim()
        return "$cleanUsername/feeds/$feedKey"
    }

    fun feedKeyFromTopic(topic: String): String? {
        val parts = topic.split("/")
        return if (parts.size >= 3 && parts[1] == "feeds") {
            normalizeFeedKey(parts[2])
        } else {
            null
        }
    }
}

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data class Connecting(val message: String = "Connecting to io.adafruit.com:8883...") : ConnectionState()
    data class Connected(val broker: String = "io.adafruit.com:8883", val connectedAt: Long = System.currentTimeMillis()) : ConnectionState()
    data class Error(val errorMessage: String, val timestamp: Long = System.currentTimeMillis()) : ConnectionState()
}

data class SocketItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val relayPin: String,
    val switchPin: String,
    val isOn: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class ActivityLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val feedKey: String,
    val payload: String,
    val isOutgoing: Boolean,
    val isDemo: Boolean = false
)

data class SmartHomeUiState(
    val sockets: List<SocketItem> = listOf(
        SocketItem(
            key = FeedConstants.FEED_SOCKET_1,
            title = "Socket 1",
            subtitle = "Relay 1",
            relayPin = "GPIO15 (D8)",
            switchPin = "GPIO2 (D4)"
        ),
        SocketItem(
            key = FeedConstants.FEED_SOCKET_2,
            title = "Socket 2",
            subtitle = "Relay 2",
            relayPin = "GPIO13 (D7)",
            switchPin = "GPIO0 (D3)"
        ),
        SocketItem(
            key = FeedConstants.FEED_SOCKET_3,
            title = "Socket 3",
            subtitle = "Relay 3",
            relayPin = "GPIO12 (D6)",
            switchPin = "GPIO4 (D2)"
        ),
        SocketItem(
            key = FeedConstants.FEED_SOCKET_4,
            title = "Socket 4",
            subtitle = "Relay 4",
            relayPin = "GPIO14 (D5)",
            switchPin = "GPIO5 (D1)"
        )
    ),
    val fanSpeed: Int = 0,
    val fanPin: String = "GPIO16 (D0)",
    val deviceStatus: String = "Waiting for ESP8266 NodeMCU status...",
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val username: String = "",
    val aioKey: String = "",
    val isDemoMode: Boolean = false,
    val logs: List<ActivityLog> = emptyList(),
    val showSettingsDialog: Boolean = false,
    val showGuideDialog: Boolean = false
)
