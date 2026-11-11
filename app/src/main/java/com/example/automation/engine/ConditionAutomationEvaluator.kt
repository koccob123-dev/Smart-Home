package com.example.automation.engine

import android.content.Context
import android.util.Log
import com.example.automation.data.AutomationLogEntity
import com.example.automation.data.AutomationRepository
import com.example.automation.data.AutomationRuleEntity
import com.example.automation.data.RuleActionType
import com.example.automation.data.RuleConditionType
import com.example.data.ClockRepository
import com.example.data.SettingsRepository
import com.example.data.SmartHomeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ConditionAutomationEvaluator(private val context: Context) {
    private val tag = "ConditionEvaluator"
    private val repository = AutomationRepository(context)
    private val settingsRepo = SettingsRepository(context)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * Evaluate live LDR reading from ESP8266 Pin A0 (0..1023).
     */
    fun evaluateLdrUpdate(ldrVal: Int) {
        coroutineScope.launch {
            try {
                val rules = repository.getActiveRules()
                val now = System.currentTimeMillis()

                rules.forEach { rule ->
                    if (now - rule.lastTriggeredEpochMs < rule.cooldownSeconds * 1000L) {
                        return@forEach // Cooldown in effect
                    }

                    var matched = false
                    when (rule.conditionType) {
                        RuleConditionType.LDR_BELOW_THRESHOLD -> {
                            val threshold = rule.conditionValue.toIntOrNull() ?: 250
                            if (ldrVal < threshold) matched = true
                        }
                        RuleConditionType.LDR_ABOVE_THRESHOLD -> {
                            val threshold = rule.conditionValue.toIntOrNull() ?: 700
                            if (ldrVal > threshold) matched = true
                        }
                        else -> {}
                    }

                    if (matched) {
                        executeRule(rule, "A0 LDR reading: $ldrVal ADC")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error evaluating LDR rules", e)
            }
        }
    }

    /**
     * Evaluate socket switch change (e.g. when physical switch toggles socket).
     */
    fun evaluateSocketChange(socketKey: String, isOn: Boolean) {
        coroutineScope.launch {
            try {
                val rules = repository.getActiveRules()
                val now = System.currentTimeMillis()

                rules.filter { it.conditionType == RuleConditionType.SOCKET_STATE_CHANGED }
                    .forEach { rule ->
                        if (now - rule.lastTriggeredEpochMs < rule.cooldownSeconds * 1000L) return@forEach

                        val expectedState = if (isOn) "ON" else "OFF"
                        if (rule.conditionTarget.equals(socketKey, ignoreCase = true) &&
                            rule.conditionValue.equals(expectedState, ignoreCase = true)
                        ) {
                            executeRule(rule, "$socketKey turned $expectedState")
                        }
                    }
            } catch (e: Exception) {
                Log.e(tag, "Error evaluating socket rules", e)
            }
        }
    }

    /**
     * Evaluate device online/offline telemetry state.
     */
    fun evaluateDeviceOffline(deviceName: String, isOnline: Boolean) {
        if (isOnline) return
        coroutineScope.launch {
            try {
                val rules = repository.getActiveRules()
                val now = System.currentTimeMillis()

                rules.filter { it.conditionType == RuleConditionType.DEVICE_OFFLINE }
                    .forEach { rule ->
                        if (now - rule.lastTriggeredEpochMs < rule.cooldownSeconds * 1000L) return@forEach

                        if (rule.conditionTarget.contains(deviceName, ignoreCase = true) || rule.conditionTarget.equals("any", ignoreCase = true)) {
                            executeRule(rule, "$deviceName went offline")
                        }
                    }
            } catch (e: Exception) {
                Log.e(tag, "Error evaluating offline rules", e)
            }
        }
    }

    /**
     * Execute action associated with matched rule.
     */
    suspend fun executeRule(rule: AutomationRuleEntity, triggerReason: String): Boolean {
        var success = true
        var detailMessage = ""
        val prefs = settingsRepo.preferencesFlow.first()

        try {
            when (rule.actionType) {
                RuleActionType.CONTROL_SOCKET -> {
                    val socketKey = rule.actionTarget
                    val command = if (rule.actionPayload.equals("ON", ignoreCase = true)) "1" else "0"
                    SmartHomeManager(context).publishValue(socketKey, command)
                    detailMessage = "Triggered by $triggerReason -> Switched $socketKey to ${rule.actionPayload}"
                }

                RuleActionType.CONTROL_ALL_SOCKETS -> {
                    val command = if (rule.actionPayload.equals("ON", ignoreCase = true)) "1" else "0"
                    val mgr = SmartHomeManager(context)
                    listOf("socket-1", "socket-2", "socket-3", "socket-4").forEach { s ->
                        mgr.publishValue(s, command)
                    }
                    detailMessage = "Triggered by $triggerReason -> Switched All Sockets to ${rule.actionPayload}"
                }

                RuleActionType.SET_FAN_SPEED -> {
                    val speed = rule.actionPayload.toIntOrNull()?.coerceIn(0, 100) ?: 50
                    SmartHomeManager(context).publishValue("fan-speed", speed.toString())
                    detailMessage = "Triggered by $triggerReason -> Set Fan Speed to $speed%"
                }

                RuleActionType.CLOCK_SET_NIGHT_MODE -> {
                    val clockRepo = ClockRepository()
                    val clockIp = prefs.clockIp
                    clockRepo.setAutoBrightness(clockIp, false)
                    val res = clockRepo.setBrightness(clockIp, 15)
                    success = res.isSuccess
                    detailMessage = "Triggered by $triggerReason -> Clock Night Mode Activated"
                }

                RuleActionType.CLOCK_AUTO_BRIGHTNESS -> {
                    val clockRepo = ClockRepository()
                    val clockIp = prefs.clockIp
                    val res = clockRepo.setAutoBrightness(clockIp, true)
                    success = res.isSuccess
                    detailMessage = "Triggered by $triggerReason -> Clock Auto-Brightness Enabled"
                }

                RuleActionType.CLOCK_SET_BRIGHTNESS -> {
                    val clockRepo = ClockRepository()
                    val clockIp = prefs.clockIp
                    val b = rule.actionPayload.toIntOrNull()?.coerceIn(0, 255) ?: 180
                    val res = clockRepo.setBrightness(clockIp, b)
                    success = res.isSuccess
                    detailMessage = "Triggered by $triggerReason -> Clock Brightness set to $b"
                }

                RuleActionType.SEND_ALERT_NOTIFICATION -> {
                    detailMessage = "Rule Alert: $triggerReason"
                }
            }

            // Update timestamp
            repository.updateRuleLastTriggered(rule.id, System.currentTimeMillis())

            // Log
            repository.insertLog(
                AutomationLogEntity(
                    title = "Rule: ${rule.title}",
                    description = detailMessage,
                    sourceType = "RULE",
                    isSuccess = success
                )
            )

            // Push Notification
            AutomationNotificationHelper.showNotification(
                context = context,
                title = "Automation Triggered: ${rule.title}",
                message = detailMessage
            )

            return success
        } catch (e: Exception) {
            Log.e(tag, "Failed to execute rule #${rule.id}", e)
            repository.insertLog(
                AutomationLogEntity(
                    title = "Rule Error: ${rule.title}",
                    description = "Failed: ${e.localizedMessage}",
                    sourceType = "RULE",
                    isSuccess = false
                )
            )
            return false
        }
    }
}
