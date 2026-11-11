package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserPreferences(
    val username: String,
    val aioKey: String,
    val isDemoMode: Boolean,
    val socket1: Boolean,
    val socket2: Boolean,
    val socket3: Boolean,
    val socket4: Boolean,
    val fanSpeed: Int,
    val clockIp: String = "192.168.1.102"
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("smart_home_node_mcu_prefs", Context.MODE_PRIVATE)

    private val _preferencesFlow = MutableStateFlow(loadPreferences())
    val preferencesFlow: StateFlow<UserPreferences> = _preferencesFlow.asStateFlow()

    private fun loadPreferences(): UserPreferences {
        return UserPreferences(
            username = prefs.getString(KEY_USERNAME, "") ?: "",
            aioKey = prefs.getString(KEY_AIO_KEY, "") ?: "",
            isDemoMode = prefs.getBoolean(KEY_DEMO_MODE, false),
            socket1 = prefs.getBoolean(KEY_SOCKET_1, false),
            socket2 = prefs.getBoolean(KEY_SOCKET_2, false),
            socket3 = prefs.getBoolean(KEY_SOCKET_3, false),
            socket4 = prefs.getBoolean(KEY_SOCKET_4, false),
            fanSpeed = prefs.getInt(KEY_FAN_SPEED, 0),
            clockIp = prefs.getString(KEY_CLOCK_IP, "192.168.1.102") ?: "192.168.1.102"
        )
    }

    fun saveCredentials(username: String, aioKey: String) {
        prefs.edit()
            .putString(KEY_USERNAME, username.trim())
            .putString(KEY_AIO_KEY, aioKey.trim())
            .apply()
        _preferencesFlow.value = loadPreferences()
    }

    fun saveClockIp(ip: String) {
        val cleanIp = ip.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        prefs.edit().putString(KEY_CLOCK_IP, cleanIp).apply()
        _preferencesFlow.value = loadPreferences()
    }

    fun setDemoMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEMO_MODE, enabled).apply()
        _preferencesFlow.value = loadPreferences()
    }

    fun saveSocketState(feedKey: String, state: Boolean) {
        val prefKey = when (feedKey) {
            "relay1", "socket-1" -> KEY_SOCKET_1
            "relay2", "socket-2" -> KEY_SOCKET_2
            "relay3", "socket-3" -> KEY_SOCKET_3
            "relay4", "socket-4" -> KEY_SOCKET_4
            else -> return
        }
        prefs.edit().putBoolean(prefKey, state).apply()
        _preferencesFlow.value = loadPreferences()
    }

    fun saveFanSpeed(speed: Int) {
        prefs.edit().putInt(KEY_FAN_SPEED, speed.coerceIn(0, 100)).apply()
        _preferencesFlow.value = loadPreferences()
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_USERNAME)
            .remove(KEY_AIO_KEY)
            .apply()
        _preferencesFlow.value = loadPreferences()
    }

    companion object {
        private const val KEY_USERNAME = "pref_aio_username"
        private const val KEY_AIO_KEY = "pref_aio_key"
        private const val KEY_DEMO_MODE = "pref_demo_mode"
        private const val KEY_SOCKET_1 = "pref_socket_1"
        private const val KEY_SOCKET_2 = "pref_socket_2"
        private const val KEY_SOCKET_3 = "pref_socket_3"
        private const val KEY_SOCKET_4 = "pref_socket_4"
        private const val KEY_FAN_SPEED = "pref_fan_speed"
        private const val KEY_CLOCK_IP = "pref_clock_ip"
    }
}
