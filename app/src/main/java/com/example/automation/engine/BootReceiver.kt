package com.example.automation.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.automation.data.AutomationDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("BootReceiver", "Device reboot detected. Rescheduling active smart home automations...")
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AutomationDatabase.getDatabase(context)
                    val activeSchedules = db.scheduleDao().getActiveSchedules()
                    val alarmManager = AutomationAlarmManager(context)

                    activeSchedules.forEach { schedule ->
                        val nextRun = alarmManager.calculateNextRunMillis(schedule.startHour, schedule.startMinute, schedule.repeatDays)
                        db.scheduleDao().updateRunTimestamps(schedule.id, nextRun, schedule.lastRunEpochMs)
                        alarmManager.scheduleAlarm(schedule.copy(nextRunEpochMs = nextRun))
                    }
                    Log.d("BootReceiver", "Rescheduled ${activeSchedules.size} active automations.")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error restoring alarms on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
