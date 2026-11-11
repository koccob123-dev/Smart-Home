package com.example.automation.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

enum class TargetDeviceType {
    SMART_HOME_SOCKET,
    SMART_HOME_ALL_SOCKETS,
    SMART_HOME_FAN,
    CLOCK_POWER,
    CLOCK_NIGHT_MODE,
    CLOCK_BRIGHTNESS,
    CLOCK_AUTO_BRIGHTNESS,
    CLOCK_EFFECT
}

@Entity(tableName = "device_schedules")
data class DeviceScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetType: TargetDeviceType,
    val targetId: String,          // e.g. "socket-1", "all", "fan", "clock"
    val actionPayload: String,     // e.g. "ON", "OFF", "80", "effect:1"
    val startHour: Int,            // 0..23
    val startMinute: Int,          // 0..59
    val hasEndTime: Boolean = false,
    val endHour: Int = 0,          // 0..23
    val endMinute: Int = 0,        // 0..59
    val repeatDays: String = "DAILY", // e.g. "DAILY", "WEEKDAYS", "WEEKENDS", "ONCE", or "MON,TUE,WED"
    val isEnabled: Boolean = true,
    val nextRunEpochMs: Long = 0L,
    val lastRunEpochMs: Long = 0L
) {
    /**
     * Human readable time representation: e.g. "07:00 AM" or "07:00 AM – 11:00 PM"
     */
    fun formattedTimeRange(): String {
        val startFormatted = formatTime(startHour, startMinute)
        return if (hasEndTime) {
            val endFormatted = formatTime(endHour, endMinute)
            "$startFormatted → $endFormatted"
        } else {
            startFormatted
        }
    }

    /**
     * Compute human readable summary of repeating schedule
     */
    fun formattedRepeatDays(): String {
        return when (repeatDays) {
            "DAILY" -> "Every Day"
            "WEEKDAYS" -> "Mon to Fri"
            "WEEKENDS" -> "Sat & Sun"
            "ONCE" -> "Once"
            else -> repeatDays.replace(",", ", ")
        }
    }

    companion object {
        fun formatTime(hour: Int, minute: Int): String {
            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = when (hour % 12) {
                0 -> 12
                else -> hour % 12
            }
            return "%d:%02d %s".format(displayHour, minute, amPm)
        }
    }
}
