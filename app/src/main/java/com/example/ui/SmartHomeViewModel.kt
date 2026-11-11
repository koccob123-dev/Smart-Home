package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MqttClientManager
import com.example.data.SettingsRepository
import com.example.model.ActivityLog
import com.example.model.FeedConstants
import com.example.model.SmartHomeUiState
import com.example.model.SocketItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SmartHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)
    private val mqttManager = MqttClientManager(viewModelScope)
    private val conditionEvaluator = com.example.automation.engine.ConditionAutomationEvaluator(application)

    private val _uiState = MutableStateFlow(SmartHomeUiState())
    val uiState: StateFlow<SmartHomeUiState> = _uiState.asStateFlow()

    private var fanDebounceJob: Job? = null

    init {
        // Collect repository preferences
        viewModelScope.launch {
            repository.preferencesFlow.collect { prefs ->
                _uiState.update { current ->
                    current.copy(
                        username = prefs.username,
                        aioKey = prefs.aioKey,
                        isDemoMode = prefs.isDemoMode,
                        sockets = listOf(
                            SocketItem(
                                key = FeedConstants.FEED_SOCKET_1,
                                title = "Socket 1",
                                subtitle = "Relay 1",
                                relayPin = "GPIO15 (D8)",
                                switchPin = "GPIO2 (D4)",
                                isOn = prefs.socket1
                            ),
                            SocketItem(
                                key = FeedConstants.FEED_SOCKET_2,
                                title = "Socket 2",
                                subtitle = "Relay 2",
                                relayPin = "GPIO13 (D7)",
                                switchPin = "GPIO0 (D3)",
                                isOn = prefs.socket2
                            ),
                            SocketItem(
                                key = FeedConstants.FEED_SOCKET_3,
                                title = "Socket 3",
                                subtitle = "Relay 3",
                                relayPin = "GPIO12 (D6)",
                                switchPin = "GPIO4 (D2)",
                                isOn = prefs.socket3
                            ),
                            SocketItem(
                                key = FeedConstants.FEED_SOCKET_4,
                                title = "Socket 4",
                                subtitle = "Relay 4",
                                relayPin = "GPIO14 (D5)",
                                switchPin = "GPIO5 (D1)",
                                isOn = prefs.socket4
                            )
                        ),
                        fanSpeed = prefs.fanSpeed
                    )
                }
            }
        }

        // Collect MQTT connection status
        viewModelScope.launch {
            mqttManager.connectionState.collect { status ->
                _uiState.update { it.copy(connectionState = status) }
            }
        }

        // Collect incoming MQTT feed updates
        viewModelScope.launch {
            mqttManager.incomingFeedUpdates.collect { (feedKey, payload) ->
                handleIncomingFeed(feedKey, payload)
            }
        }

        // Collect activity logs
        viewModelScope.launch {
            mqttManager.activityLogs.collect { newLog ->
                _uiState.update { current ->
                    val updatedLogs = (listOf(newLog) + current.logs).take(50)
                    current.copy(logs = updatedLogs)
                }
            }
        }

        // Initial connect: If credentials exist or demo mode is enabled, auto-connect
        val initialPrefs = repository.preferencesFlow.value
        if (initialPrefs.isDemoMode || (initialPrefs.username.isNotBlank() && initialPrefs.aioKey.isNotBlank())) {
            connect()
        } else {
            // First time launch without credentials: show friendly Demo Mode by default so the user has an instant working experience!
            repository.setDemoMode(true)
            connect()
        }
    }

    private fun handleIncomingFeed(feedKey: String, payload: String) {
        val normKey = FeedConstants.normalizeFeedKey(feedKey)
        when (normKey) {
            FeedConstants.FEED_SOCKET_1,
            FeedConstants.FEED_SOCKET_2,
            FeedConstants.FEED_SOCKET_3,
            FeedConstants.FEED_SOCKET_4 -> {
                val isOn = payload == "1" || payload.equals("ON", ignoreCase = true)
                _uiState.update { current ->
                    val updatedSockets = current.sockets.map { socket ->
                        if (socket.key == normKey) {
                            socket.copy(isOn = isOn, lastUpdated = System.currentTimeMillis())
                        } else {
                            socket
                        }
                    }
                    current.copy(sockets = updatedSockets)
                }
                repository.saveSocketState(normKey, isOn)
                conditionEvaluator.evaluateSocketChange(normKey, isOn)
            }

            FeedConstants.FEED_FAN_SPEED -> {
                val speed = payload.toIntOrNull()?.coerceIn(0, 100) ?: return
                _uiState.update { it.copy(fanSpeed = speed) }
                repository.saveFanSpeed(speed)
            }

            FeedConstants.FEED_DEVICE_STATUS -> {
                _uiState.update { it.copy(deviceStatus = payload) }
            }
        }
    }

    fun toggleSocket(feedKey: String) {
        val currentSocket = _uiState.value.sockets.find { it.key == feedKey } ?: return
        val targetState = !currentSocket.isOn
        val payload = if (targetState) "1" else "0"

        // Optimistically update UI
        _uiState.update { current ->
            val updated = current.sockets.map { socket ->
                if (socket.key == feedKey) {
                    socket.copy(isOn = targetState, lastUpdated = System.currentTimeMillis())
                } else {
                    socket
                }
            }
            current.copy(sockets = updated)
        }
        repository.saveSocketState(feedKey, targetState)

        // Publish to Adafruit IO
        mqttManager.publishFeedValue(feedKey, payload)
    }

    fun setFanSpeed(speed: Int, immediatePublish: Boolean = false) {
        val bounded = speed.coerceIn(0, 100)
        _uiState.update { it.copy(fanSpeed = bounded) }
        repository.saveFanSpeed(bounded)

        fanDebounceJob?.cancel()
        if (immediatePublish) {
            mqttManager.publishFeedValue(FeedConstants.FEED_FAN_SPEED, bounded.toString())
        } else {
            fanDebounceJob = viewModelScope.launch {
                delay(250) // Debounce rapid slider drag to avoid spamming MQTT broker
                mqttManager.publishFeedValue(FeedConstants.FEED_FAN_SPEED, bounded.toString())
            }
        }
    }

    fun connect() {
        val state = _uiState.value
        mqttManager.connect(
            username = state.username,
            aioKey = state.aioKey,
            demoMode = state.isDemoMode
        )
    }

    fun disconnect() {
        mqttManager.disconnect()
    }

    fun toggleDemoMode(enabled: Boolean) {
        repository.setDemoMode(enabled)
        _uiState.update { it.copy(isDemoMode = enabled) }
        connect()
    }

    fun saveCredentials(username: String, aioKey: String) {
        repository.saveCredentials(username, aioKey)
        _uiState.update { it.copy(username = username, aioKey = aioKey, showSettingsDialog = false) }
        // If not in demo mode, reconnect with new credentials
        if (!_uiState.value.isDemoMode) {
            connect()
        }
    }

    fun clearCredentials() {
        repository.clearCredentials()
        disconnect()
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    fun openSettingsDialog() {
        _uiState.update { it.copy(showSettingsDialog = true) }
    }

    fun closeSettingsDialog() {
        _uiState.update { it.copy(showSettingsDialog = false) }
    }

    fun openGuideDialog() {
        _uiState.update { it.copy(showGuideDialog = true) }
    }

    fun closeGuideDialog() {
        _uiState.update { it.copy(showGuideDialog = false) }
    }
}
