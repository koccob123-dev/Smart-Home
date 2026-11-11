package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ClockRepository
import com.example.data.SettingsRepository
import com.example.model.ClockColor
import com.example.model.ClockConnectionState
import com.example.model.ClockStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClockUiState(
    val clockStatus: ClockStatus = ClockStatus(ip = "192.168.1.102"),
    val connectionState: ClockConnectionState = ClockConnectionState.Idle,
    val clockIp: String = "192.168.1.102",
    val isDemoMode: Boolean = false,
    val isBusy: Boolean = false,
    val lastSuccessMessage: String? = null,
    val ldrTestResult: String? = null
)

class ClockViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val clockRepository = ClockRepository()
    private val conditionEvaluator = com.example.automation.engine.ConditionAutomationEvaluator(application)

    private val _uiState = MutableStateFlow(ClockUiState())
    val uiState: StateFlow<ClockUiState> = _uiState.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private var brightnessDebounceJob: Job? = null
    private var speedDebounceJob: Job? = null
    private var ldrPollingJob: Job? = null
    private var isUserInteractingWithBrightness = false
    private var isUserInteractingWithSpeed = false

    init {
        viewModelScope.launch {
            settingsRepository.preferencesFlow.collect { prefs ->
                _uiState.update { current ->
                    current.copy(
                        clockIp = prefs.clockIp,
                        isDemoMode = prefs.isDemoMode,
                        clockStatus = current.clockStatus.copy(ip = prefs.clockIp)
                    )
                }
            }
        }
        startLdrPolling()
    }

    fun setClockIp(ip: String) {
        val clean = ip.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        settingsRepository.saveClockIp(clean)
        _uiState.update { it.copy(clockIp = clean, clockStatus = it.clockStatus.copy(ip = clean)) }
    }

    fun testConnection() {
        val ip = _uiState.value.clockIp
        if (_uiState.value.isDemoMode) {
            _uiState.update {
                it.copy(
                    connectionState = ClockConnectionState.Connected(ip),
                    clockStatus = it.clockStatus.copy(isOnline = true),
                    lastSuccessMessage = "Connected to Virtual Clock [DEMO]"
                )
            }
            emitMessage("Demo Mode: Virtual LED Clock responded successfully!")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(connectionState = ClockConnectionState.Testing(ip), isBusy = true) }
            val result = clockRepository.getStatus(ip)
            result.fold(
                onSuccess = { status ->
                    _uiState.update {
                        it.copy(
                            clockStatus = status,
                            connectionState = ClockConnectionState.Connected(ip),
                            isBusy = false,
                            lastSuccessMessage = "Connected to $ip"
                        )
                    }
                    emitMessage("Connected to LED Clock at $ip successfully!")
                },
                onFailure = { err ->
                    val errorMsg = err.localizedMessage ?: "Failed to connect to $ip:80"
                    _uiState.update {
                        it.copy(
                            connectionState = ClockConnectionState.Error(errorMsg),
                            clockStatus = it.clockStatus.copy(isOnline = false),
                            isBusy = false
                        )
                    }
                    emitMessage("Connection failed: $errorMsg")
                }
            )
        }
    }

    fun setClockColor(color: ClockColor) {
        val ip = _uiState.value.clockIp
        if (_uiState.value.isDemoMode) {
            _uiState.update { it.copy(clockStatus = it.clockStatus.copy(clockColor = color)) }
            return
        }

        viewModelScope.launch {
            val result = clockRepository.setClockColor(ip, color)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(clockStatus = it.clockStatus.copy(clockColor = color)) }
                    emitMessage("Clock color updated")
                },
                onFailure = { handleError("Set clock color", it) }
            )
        }
    }

    fun setDigitColor(digitIndex: Int, color: ClockColor) {
        val ip = _uiState.value.clockIp
        if (_uiState.value.isDemoMode) {
            _uiState.update {
                val updatedStatus = when (digitIndex) {
                    1 -> it.clockStatus.copy(digit1Color = color)
                    2 -> it.clockStatus.copy(digit2Color = color)
                    3 -> it.clockStatus.copy(digit3Color = color)
                    4 -> it.clockStatus.copy(digit4Color = color)
                    else -> it.clockStatus
                }
                it.copy(clockStatus = updatedStatus)
            }
            return
        }

        viewModelScope.launch {
            val result = clockRepository.setDigitColor(ip, digitIndex, color)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        val updatedStatus = when (digitIndex) {
                            1 -> it.clockStatus.copy(digit1Color = color)
                            2 -> it.clockStatus.copy(digit2Color = color)
                            3 -> it.clockStatus.copy(digit3Color = color)
                            4 -> it.clockStatus.copy(digit4Color = color)
                            else -> it.clockStatus
                        }
                        it.copy(clockStatus = updatedStatus)
                    }
                    emitMessage("Digit $digitIndex color updated")
                },
                onFailure = { handleError("Set digit $digitIndex color", it) }
            )
        }
    }

    fun setWeekColor(color: ClockColor) {
        val ip = _uiState.value.clockIp
        if (_uiState.value.isDemoMode) {
            _uiState.update { it.copy(clockStatus = it.clockStatus.copy(weekColor = color)) }
            return
        }

        viewModelScope.launch {
            val result = clockRepository.setWeekColor(ip, color)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(clockStatus = it.clockStatus.copy(weekColor = color)) }
                    emitMessage("Weekday color updated")
                },
                onFailure = { handleError("Set week color", it) }
            )
        }
    }

    fun setColonColor(color: ClockColor) {
        val ip = _uiState.value.clockIp
        if (_uiState.value.isDemoMode) {
            _uiState.update { it.copy(clockStatus = it.clockStatus.copy(colonColor = color)) }
            return
        }

        viewModelScope.launch {
            val result = clockRepository.setColonColor(ip, color)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(clockStatus = it.clockStatus.copy(colonColor = color)) }
                    emitMessage("Colon color updated")
                },
                onFailure = { handleError("Set colon color", it) }
            )
        }
    }

    fun setDateColor(color: ClockColor) {
        val ip = _uiState.value.clockIp
        if (_uiState.value.isDemoMode) {
            _uiState.update { it.copy(clockStatus = it.clockStatus.copy(dateColor = color)) }
            return
        }

        viewModelScope.launch {
            val result = clockRepository.setDateColor(ip, color)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(clockStatus = it.clockStatus.copy(dateColor = color)) }
                    emitMessage("Date color updated")
                },
                onFailure = { handleError("Set date color", it) }
            )
        }
    }

    fun setEffect(effectId: Int) {
        val ip = _uiState.value.clockIp
        if (_uiState.value.isDemoMode) {
            _uiState.update { it.copy(clockStatus = it.clockStatus.copy(effect = effectId, effectsEnabled = true)) }
            return
        }

        viewModelScope.launch {
            val result = clockRepository.setEffect(ip, effectId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(clockStatus = it.clockStatus.copy(effect = effectId, effectsEnabled = true)) }
                    emitMessage("Effect #$effectId applied")
                },
                onFailure = { handleError("Set effect", it) }
            )
        }
    }

    fun toggleEffectsEnabled(enabled: Boolean) {
        val ip = _uiState.value.clockIp
        if (_uiState.value.isDemoMode) {
            _uiState.update { it.copy(clockStatus = it.clockStatus.copy(effectsEnabled = enabled)) }
            return
        }

        viewModelScope.launch {
            val result = clockRepository.setEffectsEnabled(ip, enabled)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(clockStatus = it.clockStatus.copy(effectsEnabled = enabled)) }
                    emitMessage("Effects ${if (enabled) "Enabled" else "Disabled"}")
                },
                onFailure = { handleError("Toggle effects", it) }
            )
        }
    }

    fun setBrightness(brightness: Int, immediate: Boolean = false) {
        val ip = _uiState.value.clockIp
        val clamped = brightness.coerceIn(0, 255)
        isUserInteractingWithBrightness = !immediate

        // Immediate responsive UI update so slider glides without any lag
        _uiState.update { it.copy(clockStatus = it.clockStatus.copy(brightness = clamped)) }

        if (_uiState.value.isDemoMode) {
            if (immediate) isUserInteractingWithBrightness = false
            return
        }

        brightnessDebounceJob?.cancel()
        if (immediate) {
            isUserInteractingWithBrightness = false
            viewModelScope.launch {
                val res = clockRepository.setBrightness(ip, clamped)
                res.onFailure { handleError("Set brightness", it) }
            }
        } else {
            brightnessDebounceJob = viewModelScope.launch {
                delay(150)
                val res = clockRepository.setBrightness(ip, clamped)
                res.onFailure { handleError("Set brightness", it) }
            }
        }
    }

    fun setAutoBrightness(enabled: Boolean) {
        val ip = _uiState.value.clockIp
        if (_uiState.value.isDemoMode) {
            _uiState.update { it.copy(clockStatus = it.clockStatus.copy(autoBrightness = enabled)) }
            return
        }

        viewModelScope.launch {
            val result = clockRepository.setAutoBrightness(ip, enabled)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(clockStatus = it.clockStatus.copy(autoBrightness = enabled)) }
                    emitMessage("Auto brightness ${if (enabled) "enabled" else "disabled"}")
                },
                onFailure = { handleError("Set auto brightness", it) }
            )
        }
    }

    fun setHour24(is24Hour: Boolean) {
        val ip = _uiState.value.clockIp
        if (_uiState.value.isDemoMode) {
            _uiState.update { it.copy(clockStatus = it.clockStatus.copy(hour24 = is24Hour)) }
            return
        }

        viewModelScope.launch {
            val result = clockRepository.setHour24(ip, is24Hour)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(clockStatus = it.clockStatus.copy(hour24 = is24Hour)) }
                    emitMessage("Time format: ${if (is24Hour) "24-Hour" else "12-Hour"}")
                },
                onFailure = { handleError("Set hour format", it) }
            )
        }
    }

    fun setBlinkColon(blink: Boolean) {
        val ip = _uiState.value.clockIp
        if (_uiState.value.isDemoMode) {
            _uiState.update { it.copy(clockStatus = it.clockStatus.copy(blinkColon = blink)) }
            return
        }

        viewModelScope.launch {
            val result = clockRepository.setBlinkColon(ip, blink)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(clockStatus = it.clockStatus.copy(blinkColon = blink)) }
                    emitMessage("Colon blink ${if (blink) "ON" else "OFF"}")
                },
                onFailure = { handleError("Set colon blinking", it) }
            )
        }
    }

    fun setShowDate(show: Boolean) {
        val ip = _uiState.value.clockIp
        if (_uiState.value.isDemoMode) {
            _uiState.update { it.copy(clockStatus = it.clockStatus.copy(showDate = show)) }
            return
        }

        viewModelScope.launch {
            val result = clockRepository.setShowDate(ip, show)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(clockStatus = it.clockStatus.copy(showDate = show)) }
                    emitMessage("Date display ${if (show) "enabled" else "disabled"}")
                },
                onFailure = { handleError("Set show date", it) }
            )
        }
    }

    fun setSpeed(speed: Int, immediate: Boolean = false) {
        val ip = _uiState.value.clockIp
        val clamped = speed.coerceIn(1, 100)
        isUserInteractingWithSpeed = !immediate

        // Immediate responsive UI update so slider glides without any lag
        _uiState.update { it.copy(clockStatus = it.clockStatus.copy(speed = clamped)) }

        if (_uiState.value.isDemoMode) {
            if (immediate) isUserInteractingWithSpeed = false
            return
        }

        speedDebounceJob?.cancel()
        if (immediate) {
            isUserInteractingWithSpeed = false
            viewModelScope.launch {
                val res = clockRepository.setSpeed(ip, clamped)
                res.onFailure { handleError("Set effect speed", it) }
            }
        } else {
            speedDebounceJob = viewModelScope.launch {
                delay(150)
                val res = clockRepository.setSpeed(ip, clamped)
                res.onFailure { handleError("Set effect speed", it) }
            }
        }
    }

    fun triggerNtpSync() {
        val ip = _uiState.value.clockIp
        if (_uiState.value.isDemoMode) {
            _uiState.update { it.copy(clockStatus = it.clockStatus.copy(ntpSynced = true)) }
            emitMessage("NTP time synchronized: Asia/Dhaka (UTC+6) [DEMO]")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = clockRepository.triggerNtpSync(ip)
            _uiState.update { it.copy(isBusy = false) }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(clockStatus = it.clockStatus.copy(ntpSynced = true)) }
                    emitMessage("NTP Time Synchronized: Asia/Dhaka (UTC+6)")
                },
                onFailure = { handleError("NTP sync", it) }
            )
        }
    }

    fun saveSettings() {
        val ip = _uiState.value.clockIp
        if (_uiState.value.isDemoMode) {
            emitMessage("Settings saved to Virtual EEPROM [DEMO]")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = clockRepository.saveSettings(ip)
            _uiState.update { it.copy(isBusy = false) }
            result.fold(
                onSuccess = { emitMessage("Settings saved to ESP8266 EEPROM successfully!") },
                onFailure = { handleError("Save settings", it) }
            )
        }
    }

    fun resetSettings() {
        val ip = _uiState.value.clockIp
        if (_uiState.value.isDemoMode) {
            _uiState.update { it.copy(clockStatus = ClockStatus(ip = ip)) }
            emitMessage("Clock reset to factory defaults [DEMO]")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = clockRepository.resetSettings(ip)
            _uiState.update { it.copy(isBusy = false) }
            result.fold(
                onSuccess = {
                    emitMessage("Clock reset to factory defaults!")
                    testConnection()
                },
                onFailure = { handleError("Reset settings", it) }
            )
        }
    }

    fun testLdr() {
        val ip = _uiState.value.clockIp
        if (_uiState.value.isDemoMode) {
            val randomLdr = (350..850).random()
            _uiState.update {
                it.copy(
                    ldrTestResult = "LDR Value: $randomLdr ADC [DEMO]",
                    clockStatus = it.clockStatus.copy(ldrRawValue = randomLdr)
                )
            }
            emitMessage("LDR sensor test: $randomLdr ADC [DEMO]")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, ldrTestResult = "Reading LDR sensor (A0)...") }
            val result = clockRepository.getLdr(ip)
            _uiState.update { it.copy(isBusy = false) }
            result.fold(
                onSuccess = { ldr ->
                    _uiState.update {
                        it.copy(
                            ldrTestResult = "LDR (A0): $ldr / 1023 ADC",
                            clockStatus = it.clockStatus.copy(ldrRawValue = ldr, isOnline = true),
                            connectionState = if (it.connectionState is ClockConnectionState.Idle || it.connectionState is ClockConnectionState.Error) {
                                ClockConnectionState.Connected(ip)
                            } else {
                                it.connectionState
                            }
                        )
                    }
                    emitMessage("LDR Sensor (A0): $ldr / 1023 ADC")
                },
                onFailure = {
                    val err = it.localizedMessage ?: "Failed to read LDR"
                    _uiState.update { current -> current.copy(ldrTestResult = "Error reading A0: $err") }
                    handleError("Test LDR", it)
                }
            )
        }
    }

    fun startLdrPolling() {
        if (ldrPollingJob?.isActive == true) return
        ldrPollingJob = viewModelScope.launch {
            var counter = 0
            while (true) {
                delay(1000)
                val state = _uiState.value
                val ip = state.clockIp
                if (!state.isDemoMode && ip.isNotBlank() && ip != "0.0.0.0") {
                    // Poll real LDR sensor reading every 1s
                    val ldrResult = clockRepository.getLdr(ip)
                    ldrResult.onSuccess { ldr ->
                        _uiState.update { current ->
                            current.copy(
                                clockStatus = current.clockStatus.copy(
                                    ldrRawValue = ldr,
                                    isOnline = true
                                ),
                                ldrTestResult = if (current.ldrTestResult == null || current.ldrTestResult.startsWith("LDR") || current.ldrTestResult.startsWith("Live")) {
                                    "LDR (A0): $ldr / 1023 ADC"
                                } else {
                                    current.ldrTestResult
                                },
                                connectionState = if (current.connectionState is ClockConnectionState.Idle || current.connectionState is ClockConnectionState.Error) {
                                    ClockConnectionState.Connected(ip)
                                } else {
                                    current.connectionState
                                }
                            )
                        }
                        conditionEvaluator.evaluateLdrUpdate(ldr)
                    }

                    // Every 5s, synchronize full clock status unless user is interacting with sliders
                    counter++
                    if (counter >= 5) {
                        counter = 0
                        if (!isUserInteractingWithBrightness && !isUserInteractingWithSpeed) {
                            val statusResult = clockRepository.getStatus(ip)
                            statusResult.onSuccess { status ->
                                _uiState.update { current ->
                                    val safeBrightness = if (isUserInteractingWithBrightness) current.clockStatus.brightness else status.brightness
                                    val safeSpeed = if (isUserInteractingWithSpeed) current.clockStatus.speed else status.speed
                                    current.copy(
                                        clockStatus = status.copy(
                                            brightness = safeBrightness,
                                            speed = safeSpeed
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun stopLdrPolling() {
        ldrPollingJob?.cancel()
        ldrPollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopLdrPolling()
        brightnessDebounceJob?.cancel()
        speedDebounceJob?.cancel()
    }

    private fun handleError(action: String, error: Throwable) {
        val msg = "$action failed: ${error.localizedMessage ?: "Connection error"}"
        _uiState.update {
            it.copy(
                connectionState = ClockConnectionState.Error(msg),
                clockStatus = it.clockStatus.copy(isOnline = false)
            )
        }
        emitMessage(msg)
    }

    private fun emitMessage(msg: String) {
        viewModelScope.launch {
            _userMessage.emit(msg)
        }
    }
}
