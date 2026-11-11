package com.example.automation.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.automation.data.AutomationLogEntity
import com.example.automation.data.AutomationRepository
import com.example.automation.data.TargetDeviceType
import com.example.data.ClockRepository
import com.example.data.SettingsRepository
import com.example.data.SmartHomeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    private val tag = "AlarmReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(AutomationAlarmManager.EXTRA_SCHEDULE_ID, -1L)
        val isEndTime = intent.getBooleanExtra(AutomationAlarmManager.EXTRA_IS_END_TIME, false)

        if (scheduleId == -1L) {
            Log.w(tag, "Received alarm with invalid scheduleId")
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = AutomationRepository(context)
                val settingsRepo = SettingsRepository(context)
                val schedule = repository.getScheduleById(scheduleId)

                if (schedule == null || !schedule.isEnabled) {
                    Log.d(tag, "Schedule #$scheduleId is null or disabled; skipping execution.")
                    return@launch
                }

                val actionText = if (isEndTime) "OFF" else schedule.actionPayload
                val prefs = settingsRepo.preferencesFlow.first()

                var success = true
                var detailMessage = ""

                // 1. Device 1: Smart Home (Adafruit IO MQTT)
                when (schedule.targetType) {
                    TargetDeviceType.SMART_HOME_SOCKET -> {
                        val socketKey = schedule.targetId // e.g. "socket-1"
                        val command = if (actionText.equals("ON", ignoreCase = true)) "1" else "0"
                        val smartHomeManager = SmartHomeManager(context)
                        try {
                            smartHomeManager.publishValue(socketKey, command)
                            detailMessage = "Turned $socketKey $actionText via MQTT"
                        } catch (e: Exception) {
                            success = false
                            detailMessage = "Failed to switch $socketKey: ${e.localizedMessage}"
                        }
                    }

                    TargetDeviceType.SMART_HOME_ALL_SOCKETS -> {
                        val command = if (actionText.equals("ON", ignoreCase = true)) "1" else "0"
                        val smartHomeManager = SmartHomeManager(context)
                        try {
                            listOf("socket-1", "socket-2", "socket-3", "socket-4").forEach { feed ->
                                smartHomeManager.publishValue(feed, command)
                            }
                            detailMessage = "Switched All 4 Sockets $actionText"
                        } catch (e: Exception) {
                            success = false
                            detailMessage = "Failed to switch all sockets: ${e.localizedMessage}"
                        }
                    }

                    TargetDeviceType.SMART_HOME_FAN -> {
                        val speed = actionText.toIntOrNull()?.coerceIn(0, 100) ?: if (isEndTime) 0 else 50
                        val smartHomeManager = SmartHomeManager(context)
                        try {
                            smartHomeManager.publishValue("fan-speed", speed.toString())
                            detailMessage = "Set Fan Speed to $speed%"
                        } catch (e: Exception) {
                            success = false
                            detailMessage = "Failed to set fan speed: ${e.localizedMessage}"
                        }
                    }

                    // 2. Device 2: LED Digital Clock (HTTP REST)
                    TargetDeviceType.CLOCK_POWER -> {
                        val clockRepo = ClockRepository()
                        val clockIp = prefs.clockIp
                        val brightness = if (isEndTime || actionText.equals("OFF", ignoreCase = true)) 0 else 180
                        val res = clockRepo.setBrightness(clockIp, brightness)
                        success = res.isSuccess
                        detailMessage = if (success) "Clock LEDs set to ${if (brightness == 0) "OFF" else "ON"}" else "Failed to reach Clock at $clockIp"
                    }

                    TargetDeviceType.CLOCK_NIGHT_MODE -> {
                        val clockRepo = ClockRepository()
                        val clockIp = prefs.clockIp
                        if (isEndTime) {
                            clockRepo.setBrightness(clockIp, 180)
                            clockRepo.setAutoBrightness(clockIp, true)
                            detailMessage = "Night Mode ended (Restored brightness & Auto-LDR)"
                        } else {
                            clockRepo.setAutoBrightness(clockIp, false)
                            clockRepo.setBrightness(clockIp, 15) // Dim warm level
                            detailMessage = "Night Mode activated (Dimmed to 15 / Amber)"
                        }
                    }

                    TargetDeviceType.CLOCK_BRIGHTNESS -> {
                        val clockRepo = ClockRepository()
                        val clockIp = prefs.clockIp
                        val brightness = actionText.toIntOrNull()?.coerceIn(0, 255) ?: 180
                        val res = clockRepo.setBrightness(clockIp, brightness)
                        success = res.isSuccess
                        detailMessage = if (success) "Clock brightness set to $brightness" else "Clock command failed"
                    }

                    TargetDeviceType.CLOCK_AUTO_BRIGHTNESS -> {
                        val clockRepo = ClockRepository()
                        val clockIp = prefs.clockIp
                        val enabled = !isEndTime && actionText.toBooleanStrictOrNull() ?: true
                        val res = clockRepo.setAutoBrightness(clockIp, enabled)
                        success = res.isSuccess
                        detailMessage = if (success) "Clock Auto-Brightness ${if (enabled) "Enabled" else "Disabled"}" else "Clock command failed"
                    }

                    TargetDeviceType.CLOCK_EFFECT -> {
                        val clockRepo = ClockRepository()
                        val clockIp = prefs.clockIp
                        val effectId = actionText.toIntOrNull()?.coerceIn(0, 15) ?: 1
                        val res = clockRepo.setEffect(clockIp, effectId)
                        success = res.isSuccess
                        detailMessage = if (success) "Applied Clock Effect #$effectId" else "Clock effect command failed"
                    }
                }

                // Log execution
                repository.insertLog(
                    AutomationLogEntity(
                        title = "${schedule.title} (${if (isEndTime) "Auto-OFF" else "Scheduled"})",
                        description = detailMessage,
                        sourceType = "SCHEDULE",
                        isSuccess = success
                    )
                )

                // Dispatch notification to user
                AutomationNotificationHelper.showNotification(
                    context = context,
                    title = if (success) "Schedule Executed: ${schedule.title}" else "Schedule Error: ${schedule.title}",
                    message = detailMessage
                )

                // Update schedule run timestamps and reschedule next occurrence
                val alarmMgr = AutomationAlarmManager(context)
                val nextRun = alarmMgr.calculateNextRunMillis(schedule.startHour, schedule.startMinute, schedule.repeatDays)
                repository.updateRunTimestamps(schedule.id, nextRun, System.currentTimeMillis())

                if (schedule.repeatDays.equals("ONCE", ignoreCase = true) && !schedule.hasEndTime) {
                    repository.setScheduleEnabled(schedule.id, false)
                } else {
                    alarmMgr.scheduleAlarm(schedule.copy(nextRunEpochMs = nextRun))
                }

            } catch (e: Exception) {
                Log.e(tag, "Error executing schedule alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
