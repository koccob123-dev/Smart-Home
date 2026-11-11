package com.example.automation.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        DeviceScheduleEntity::class,
        AutomationRuleEntity::class,
        AutomationLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AutomationDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun automationRuleDao(): AutomationRuleDao
    abstract fun automationLogDao(): AutomationLogDao

    companion object {
        @Volatile
        private var INSTANCE: AutomationDatabase? = null

        fun getDatabase(context: Context): AutomationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AutomationDatabase::class.java,
                    "smarthome_automation.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate default sample schedules and rules in background
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getDatabase(context)
                            prepopulateDefaults(database)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun prepopulateDefaults(db: AutomationDatabase) {
            // Default Schedule 1: Morning Routine - Turn Socket 1 ON at 7:00 AM
            db.scheduleDao().insertSchedule(
                DeviceScheduleEntity(
                    title = "Morning Coffee & Lights",
                    targetType = TargetDeviceType.SMART_HOME_SOCKET,
                    targetId = "socket-1",
                    actionPayload = "ON",
                    startHour = 7,
                    startMinute = 0,
                    hasEndTime = true,
                    endHour = 9,
                    endMinute = 0,
                    repeatDays = "DAILY",
                    isEnabled = true
                )
            )

            // Default Schedule 2: LED Clock Night Mode at 11:00 PM
            db.scheduleDao().insertSchedule(
                DeviceScheduleEntity(
                    title = "Clock Night Dimming",
                    targetType = TargetDeviceType.CLOCK_NIGHT_MODE,
                    targetId = "clock",
                    actionPayload = "night",
                    startHour = 23,
                    startMinute = 0,
                    hasEndTime = true,
                    endHour = 6,
                    endMinute = 30,
                    repeatDays = "DAILY",
                    isEnabled = true
                )
            )

            // Default Rule 1: Darkness Detected (LDR A0 < 250) -> Turn Socket 1 ON
            db.automationRuleDao().insertRule(
                AutomationRuleEntity(
                    title = "Ambient Dusk Auto-Light",
                    conditionType = RuleConditionType.LDR_BELOW_THRESHOLD,
                    conditionTarget = "A0",
                    conditionValue = "250",
                    actionType = RuleActionType.CONTROL_SOCKET,
                    actionTarget = "socket-1",
                    actionPayload = "ON",
                    isEnabled = true
                )
            )

            // Default Rule 2: Bright Daylight (LDR A0 > 750) -> Turn All Sockets OFF
            db.automationRuleDao().insertRule(
                AutomationRuleEntity(
                    title = "Daylight Energy Saver",
                    conditionType = RuleConditionType.LDR_ABOVE_THRESHOLD,
                    conditionTarget = "A0",
                    conditionValue = "750",
                    actionType = RuleActionType.CONTROL_ALL_SOCKETS,
                    actionTarget = "all",
                    actionPayload = "OFF",
                    isEnabled = false
                )
            )

            // Initial log
            db.automationLogDao().insertLog(
                AutomationLogEntity(
                    title = "System Initialized",
                    description = "Automation engine and local database ready.",
                    sourceType = "SYSTEM",
                    isSuccess = true
                )
            )
        }
    }
}
