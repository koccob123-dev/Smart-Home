package com.example.automation.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RuleConditionType {
    LDR_BELOW_THRESHOLD,     // Real A0 sensor < threshold (Darkness)
    LDR_ABOVE_THRESHOLD,     // Real A0 sensor > threshold (Bright Light)
    SOCKET_STATE_CHANGED,    // When a specific socket turns ON / OFF
    DEVICE_OFFLINE,          // When Device 1 or Device 2 goes offline
    TEMPERATURE_ABOVE        // Telemetry temp exceeds threshold
}

enum class RuleActionType {
    CONTROL_SOCKET,          // Turn specific socket ON / OFF
    CONTROL_ALL_SOCKETS,     // Turn all sockets ON / OFF
    SET_FAN_SPEED,           // Set fan speed (0..100)
    CLOCK_SET_NIGHT_MODE,    // Turn on Night Mode (dim amber)
    CLOCK_AUTO_BRIGHTNESS,   // Toggle auto brightness (LDR control)
    CLOCK_SET_BRIGHTNESS,    // Set fixed brightness
    SEND_ALERT_NOTIFICATION  // Dispatch alert notification
}

@Entity(tableName = "automation_rules")
data class AutomationRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val conditionType: RuleConditionType,
    val conditionTarget: String,     // e.g. "A0", "socket-1", "device-status"
    val conditionValue: String,      // e.g. "250" (for LDR), "ON", "35"
    val actionType: RuleActionType,
    val actionTarget: String,        // e.g. "socket-1", "all", "fan", "clock"
    val actionPayload: String,       // e.g. "ON", "OFF", "100", "night"
    val isEnabled: Boolean = true,
    val lastTriggeredEpochMs: Long = 0L,
    val cooldownSeconds: Int = 60    // Prevent rapid re-triggering within 60s
) {
    fun formattedCondition(): String {
        return when (conditionType) {
            RuleConditionType.LDR_BELOW_THRESHOLD -> "IF Light Sensor (A0) < $conditionValue ADC (Darkness)"
            RuleConditionType.LDR_ABOVE_THRESHOLD -> "IF Light Sensor (A0) > $conditionValue ADC (Daylight)"
            RuleConditionType.SOCKET_STATE_CHANGED -> "IF $conditionTarget is $conditionValue"
            RuleConditionType.DEVICE_OFFLINE -> "IF $conditionTarget goes Offline"
            RuleConditionType.TEMPERATURE_ABOVE -> "IF Temperature > $conditionValue°C"
        }
    }

    fun formattedAction(): String {
        return when (actionType) {
            RuleActionType.CONTROL_SOCKET -> "THEN Turn $actionTarget $actionPayload"
            RuleActionType.CONTROL_ALL_SOCKETS -> "THEN Turn All Sockets $actionPayload"
            RuleActionType.SET_FAN_SPEED -> "THEN Set Fan Speed to $actionPayload%"
            RuleActionType.CLOCK_SET_NIGHT_MODE -> "THEN Activate LED Clock Night Mode"
            RuleActionType.CLOCK_AUTO_BRIGHTNESS -> "THEN Enable Clock Auto-Brightness"
            RuleActionType.CLOCK_SET_BRIGHTNESS -> "THEN Set Clock Brightness to $actionPayload"
            RuleActionType.SEND_ALERT_NOTIFICATION -> "THEN Send Push Notification"
        }
    }
}
