package com.example.automation.engine

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.automation.data.DeviceScheduleEntity
import java.util.Calendar

class AutomationAlarmManager(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    /**
     * Compute next trigger timestamp in epoch millis for the given schedule.
     */
    fun calculateNextRunMillis(hour: Int, minute: Int, repeatDays: String): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If today's time has already passed, advance by at least 1 day
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Advance until target matches repeatDays pattern
        var iterations = 0
        while (!matchesRepeatDays(target.get(Calendar.DAY_OF_WEEK), repeatDays) && iterations < 7) {
            target.add(Calendar.DAY_OF_YEAR, 1)
            iterations++
        }

        return target.timeInMillis
    }

    private fun matchesRepeatDays(calendarDayOfWeek: Int, repeatDays: String): Boolean {
        return when (repeatDays.uppercase()) {
            "DAILY" -> true
            "WEEKDAYS" -> calendarDayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
            "WEEKENDS" -> calendarDayOfWeek == Calendar.SATURDAY || calendarDayOfWeek == Calendar.SUNDAY
            "ONCE" -> true
            else -> {
                val dayStr = when (calendarDayOfWeek) {
                    Calendar.MONDAY -> "MON"
                    Calendar.TUESDAY -> "TUE"
                    Calendar.WEDNESDAY -> "WED"
                    Calendar.THURSDAY -> "THU"
                    Calendar.FRIDAY -> "FRI"
                    Calendar.SATURDAY -> "SAT"
                    Calendar.SUNDAY -> "SUN"
                    else -> ""
                }
                repeatDays.contains(dayStr, ignoreCase = true)
            }
        }
    }

    /**
     * Schedule an alarm for this DeviceScheduleEntity.
     */
    fun scheduleAlarm(schedule: DeviceScheduleEntity) {
        if (!schedule.isEnabled || alarmManager == null) return

        val triggerTime = calculateNextRunMillis(schedule.startHour, schedule.startMinute, schedule.repeatDays)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(EXTRA_IS_END_TIME, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d("AutomationAlarmManager", "Scheduled alarm for schedule #${schedule.id} (${schedule.title}) at $triggerTime")
        } catch (e: SecurityException) {
            // In Android 12+, if exact alarm permission is revoked, fall back to windowed alarm
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            Log.w("AutomationAlarmManager", "Fell back to standard alarm due to exact alarm restriction", e)
        }

        // Schedule corresponding end-time turn-off alarm if configured
        if (schedule.hasEndTime) {
            val endTriggerTime = calculateNextRunMillis(schedule.endHour, schedule.endMinute, schedule.repeatDays)
            val endIntent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_SCHEDULE_ID, schedule.id)
                putExtra(EXTRA_IS_END_TIME, true)
            }
            val endPendingIntent = PendingIntent.getBroadcast(
                context,
                (schedule.id + 100000).toInt(),
                endIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTriggerTime, endPendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, endTriggerTime, endPendingIntent)
                }
            } catch (e: Exception) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, endTriggerTime, endPendingIntent)
            }
        }
    }

    /**
     * Cancel any pending alarm for this schedule ID.
     */
    fun cancelAlarm(scheduleId: Long) {
        if (alarmManager == null) return

        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        // Cancel end-time alarm if any
        val endPendingIntent = PendingIntent.getBroadcast(
            context,
            (scheduleId + 100000).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (endPendingIntent != null) {
            alarmManager.cancel(endPendingIntent)
            endPendingIntent.cancel()
        }
        Log.d("AutomationAlarmManager", "Cancelled alarm for schedule #$scheduleId")
    }

    companion object {
        const val EXTRA_SCHEDULE_ID = "com.example.automation.EXTRA_SCHEDULE_ID"
        const val EXTRA_IS_END_TIME = "com.example.automation.EXTRA_IS_END_TIME"
    }
}
