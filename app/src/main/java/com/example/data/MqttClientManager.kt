package com.example.data

import android.util.Log
import com.example.model.ActivityLog
import com.example.model.ConnectionState
import com.example.model.FeedConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLSocketFactory

class MqttClientManager(
    private val scope: CoroutineScope
) {
    private val tag = "MqttClientManager"

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Feeds received from broker (feedKey to payload string)
    private val _incomingFeedUpdates = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 64)
    val incomingFeedUpdates: SharedFlow<Pair<String, String>> = _incomingFeedUpdates.asSharedFlow()

    // Activity log event stream
    private val _activityLogs = MutableSharedFlow<ActivityLog>(extraBufferCapacity = 64)
    val activityLogs: SharedFlow<ActivityLog> = _activityLogs.asSharedFlow()

    private var mqttClient: MqttAsyncClient? = null
    private var currentUsername: String = ""
    private var currentAioKey: String = ""
    private var isDemoMode: Boolean = false
    private var demoHeartbeatJob: Job? = null

    // Track recently published values to prevent publish-subscribe echo loops
    // Key: feedKey, Value: Pair(lastSentPayload, timestamp)
    private val recentLocalPublishes = ConcurrentHashMap<String, Pair<String, Long>>()

    fun connect(username: String, aioKey: String, demoMode: Boolean) {
        disconnectCurrent(reconnecting = true)

        isDemoMode = demoMode
        currentUsername = username.trim()
        currentAioKey = aioKey.trim()

        if (demoMode) {
            startDemoSimulation()
            return
        }

        if (currentUsername.isEmpty() || currentAioKey.isEmpty()) {
            _connectionState.value = ConnectionState.Error(
                "Missing Adafruit IO credentials. Please open Settings and enter your Username and AIO Key."
            )
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                _connectionState.value = ConnectionState.Connecting("Connecting to io.adafruit.com:8883 (TLS)...")
                val serverUri = "${FeedConstants.PROTOCOL}://${FeedConstants.HOST}:${FeedConstants.PORT}"
                val clientId = "Android_ESP8266_${UUID.randomUUID().toString().take(8)}"

                val client = MqttAsyncClient(serverUri, clientId, MemoryPersistence())
                mqttClient = client

                client.setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        Log.i(tag, "connectComplete, reconnect=$reconnect")
                        _connectionState.value = ConnectionState.Connected(
                            broker = "io.adafruit.com:8883",
                            connectedAt = System.currentTimeMillis()
                        )
                        // Subscribe to all feeds
                        subscribeToFeeds(client, currentUsername)
                        if (reconnect) {
                            emitLog("system", "Reconnected to Adafruit IO broker", isOutgoing = false)
                        } else {
                            emitLog("system", "Connected over TLS to io.adafruit.com:8883", isOutgoing = false)
                        }
                    }

                    override fun connectionLost(cause: Throwable?) {
                        Log.w(tag, "connectionLost: ${cause?.message}")
                        val errorMsg = cause?.localizedMessage ?: "Connection lost"
                        _connectionState.value = ConnectionState.Error("Disconnected: $errorMsg")
                        emitLog("system", "Connection lost: $errorMsg", isOutgoing = false)
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        if (topic == null || message == null) return
                        val payload = String(message.payload).trim()
                        val rawFeedKey = FeedConstants.feedKeyFromTopic(topic) ?: return
                        val feedKey = FeedConstants.normalizeFeedKey(rawFeedKey)

                        Log.d(tag, "messageArrived topic=$topic raw=$rawFeedKey normalized=$feedKey payload=$payload")

                        // Echo loop suppression: check if this message matches a recent local publish
                        val recentPublish = recentLocalPublishes[feedKey] ?: recentLocalPublishes[rawFeedKey]
                        if (recentPublish != null) {
                            val (sentPayload, sentAt) = recentPublish
                            val elapsed = System.currentTimeMillis() - sentAt
                            if (sentPayload == payload && elapsed < 4000L) {
                                Log.d(tag, "Ignored echo for feed=$feedKey payload=$payload (sent ${elapsed}ms ago)")
                                // Clear pending publish
                                recentLocalPublishes.remove(feedKey)
                                recentLocalPublishes.remove(rawFeedKey)
                                return
                            }
                        }

                        // Pass received feed update to UI
                        scope.launch {
                            _incomingFeedUpdates.emit(Pair(feedKey, payload))
                            emitLog(feedKey, payload, isOutgoing = false)
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        // Publish delivery acknowledged
                    }
                })

                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    userName = currentUsername
                    password = currentAioKey.toCharArray()
                    connectionTimeout = 15
                    keepAliveInterval = 60
                    isAutomaticReconnect = true
                    // Configure SSL/TLS
                    try {
                        socketFactory = SSLSocketFactory.getDefault()
                    } catch (e: Exception) {
                        Log.e(tag, "Error setting SSLSocketFactory", e)
                    }
                }

                client.connect(options, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.i(tag, "MQTT client connect action succeeded")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val errorText = when (exception) {
                            is MqttException -> {
                                when (exception.reasonCode) {
                                    4 -> "Bad username or AIO key (Authentication failed)"
                                    5 -> "Not authorized (Check Adafruit IO permissions/feeds)"
                                    else -> "Connection failed (Code ${exception.reasonCode}): ${exception.message}"
                                }
                            }
                            else -> exception?.localizedMessage ?: "Unknown connection failure"
                        }
                        Log.e(tag, "MQTT client connect failed: $errorText", exception)
                        _connectionState.value = ConnectionState.Error(errorText)
                        emitLog("system", "Connection error: $errorText", isOutgoing = false)
                    }
                })

            } catch (e: Exception) {
                Log.e(tag, "Failed to initialize MQTT connection", e)
                _connectionState.value = ConnectionState.Error("Init error: ${e.localizedMessage}")
            }
        }
    }

    private fun subscribeToFeeds(client: MqttAsyncClient, username: String) {
        FeedConstants.ALL_FEEDS.forEach { feedKey ->
            val topic = FeedConstants.topicForFeed(username, feedKey)
            try {
                client.subscribe(topic, 1, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d(tag, "Subscribed to $topic")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e(tag, "Failed to subscribe to $topic", exception)
                    }
                })
            } catch (e: Exception) {
                Log.e(tag, "Exception subscribing to $topic", e)
            }
        }
    }

    fun publishFeedValue(feedKey: String, payload: String) {
        val cleanPayload = payload.trim()

        if (isDemoMode) {
            handleDemoPublish(feedKey, cleanPayload)
            return
        }

        val client = mqttClient
        if (client == null || !client.isConnected) {
            Log.w(tag, "Cannot publish: MQTT client not connected")
            return
        }

        val topic = FeedConstants.topicForFeed(currentUsername, feedKey)
        try {
            // Register pending publish for echo suppression
            recentLocalPublishes[feedKey] = Pair(cleanPayload, System.currentTimeMillis())

            val message = MqttMessage(cleanPayload.toByteArray()).apply {
                qos = 1
                isRetained = false
            }
            client.publish(topic, message)
            emitLog(feedKey, cleanPayload, isOutgoing = true)
            Log.d(tag, "Published to $topic: $cleanPayload")
        } catch (e: Exception) {
            Log.e(tag, "Publish error to $topic", e)
            emitLog(feedKey, "Failed to publish: ${e.message}", isOutgoing = true)
        }
    }

    fun disconnect() {
        disconnectCurrent(reconnecting = false)
    }

    private fun disconnectCurrent(reconnecting: Boolean) {
        demoHeartbeatJob?.cancel()
        demoHeartbeatJob = null

        val client = mqttClient
        mqttClient = null

        if (client != null && client.isConnected) {
            try {
                client.disconnect()
                client.close()
            } catch (e: Exception) {
                Log.w(tag, "Error closing client: ${e.message}")
            }
        }

        if (!reconnecting) {
            _connectionState.value = ConnectionState.Disconnected
            emitLog("system", "Disconnected", isOutgoing = false)
        }
    }

    // ==========================================
    // DEMO MODE SIMULATOR (Works without hardware)
    // ==========================================
    private fun startDemoSimulation() {
        scope.launch {
            _connectionState.value = ConnectionState.Connecting("Starting Demo Mode simulation...")
            delay(400)
            _connectionState.value = ConnectionState.Connected(
                broker = "Demo Simulator (ESP8266 Virtual)",
                connectedAt = System.currentTimeMillis()
            )
            emitLog("system", "Demo Mode activated (Virtual ESP8266)", isOutgoing = false, isDemo = true)

            // Initial status from virtual ESP8266
            delay(300)
            val initialStatus = "ESP8266 NodeMCU Ready • WiFi: -52 dBm • Free Heap: 41.2 KB [DEMO]"
            _incomingFeedUpdates.emit(Pair(FeedConstants.FEED_DEVICE_STATUS, initialStatus))
            emitLog(FeedConstants.FEED_DEVICE_STATUS, initialStatus, isOutgoing = false, isDemo = true)

            // Periodic heartbeat simulation
            demoHeartbeatJob?.cancel()
            demoHeartbeatJob = scope.launch {
                var counter = 1
                while (isActive) {
                    delay(15000)
                    if (isDemoMode && _connectionState.value is ConnectionState.Connected) {
                        val pingStatus = "ESP8266 Heartbeat #$counter • Uptime: ${counter * 15}s • WiFi: OK [DEMO]"
                        _incomingFeedUpdates.emit(Pair(FeedConstants.FEED_DEVICE_STATUS, pingStatus))
                        emitLog(FeedConstants.FEED_DEVICE_STATUS, pingStatus, isOutgoing = false, isDemo = true)
                        counter++
                    }
                }
            }
        }
    }

    private fun handleDemoPublish(feedKey: String, payload: String) {
        emitLog(feedKey, payload, isOutgoing = true, isDemo = true)
        scope.launch {
            delay(120) // Realistic network round-trip simulation
            val normalized = FeedConstants.normalizeFeedKey(feedKey)
            val simulatedStatus = when (normalized) {
                FeedConstants.FEED_SOCKET_1 -> "Socket 1 (Relay D8) set to ${if (payload == "1") "ON" else "OFF"} [DEMO]"
                FeedConstants.FEED_SOCKET_2 -> "Socket 2 (Relay D7) set to ${if (payload == "1") "ON" else "OFF"} [DEMO]"
                FeedConstants.FEED_SOCKET_3 -> "Socket 3 (Relay D6) set to ${if (payload == "1") "ON" else "OFF"} [DEMO]"
                FeedConstants.FEED_SOCKET_4 -> "Socket 4 (Relay D5) set to ${if (payload == "1") "ON" else "OFF"} [DEMO]"
                FeedConstants.FEED_FAN_SPEED -> "Fan PWM (GPIO16/D0) set to $payload% (Duty: ${(payload.toIntOrNull() ?: 0) * 1023 / 100}/1023) [DEMO]"
                else -> "Feed $feedKey received $payload [DEMO]"
            }
            _incomingFeedUpdates.emit(Pair(FeedConstants.FEED_DEVICE_STATUS, simulatedStatus))
            emitLog(FeedConstants.FEED_DEVICE_STATUS, simulatedStatus, isOutgoing = false, isDemo = true)
        }
    }

    private fun emitLog(feedKey: String, payload: String, isOutgoing: Boolean, isDemo: Boolean = isDemoMode) {
        scope.launch {
            _activityLogs.emit(
                ActivityLog(
                    feedKey = feedKey,
                    payload = payload,
                    isOutgoing = isOutgoing,
                    isDemo = isDemo
                )
            )
        }
    }
}
