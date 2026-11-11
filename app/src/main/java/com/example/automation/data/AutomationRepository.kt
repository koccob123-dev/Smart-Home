package com.example.automation.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class AutomationRepository(context: Context) {
    private val database = AutomationDatabase.getDatabase(context)
    private val scheduleDao = database.scheduleDao()
    private val ruleDao = database.automationRuleDao()
    private val logDao = database.automationLogDao()

    val allSchedules: Flow<List<DeviceScheduleEntity>> = scheduleDao.getAllSchedules()
    val allRules: Flow<List<AutomationRuleEntity>> = ruleDao.getAllRules()
    val allLogs: Flow<List<AutomationLogEntity>> = logDao.getRecentLogs()

    suspend fun insertSchedule(schedule: DeviceScheduleEntity): Long = scheduleDao.insertSchedule(schedule)
    suspend fun updateSchedule(schedule: DeviceScheduleEntity) = scheduleDao.updateSchedule(schedule)
    suspend fun setScheduleEnabled(id: Long, enabled: Boolean) = scheduleDao.setScheduleEnabled(id, enabled)
    suspend fun deleteSchedule(schedule: DeviceScheduleEntity) = scheduleDao.deleteSchedule(schedule)
    suspend fun deleteScheduleById(id: Long) = scheduleDao.deleteScheduleById(id)
    suspend fun getScheduleById(id: Long): DeviceScheduleEntity? = scheduleDao.getScheduleById(id)
    suspend fun getActiveSchedules(): List<DeviceScheduleEntity> = scheduleDao.getActiveSchedules()
    suspend fun updateRunTimestamps(id: Long, nextRun: Long, lastRun: Long) = scheduleDao.updateRunTimestamps(id, nextRun, lastRun)

    suspend fun insertRule(rule: AutomationRuleEntity): Long = ruleDao.insertRule(rule)
    suspend fun updateRule(rule: AutomationRuleEntity) = ruleDao.updateRule(rule)
    suspend fun setRuleEnabled(id: Long, enabled: Boolean) = ruleDao.setRuleEnabled(id, enabled)
    suspend fun deleteRule(rule: AutomationRuleEntity) = ruleDao.deleteRule(rule)
    suspend fun deleteRuleById(id: Long) = ruleDao.deleteRuleById(id)
    suspend fun getActiveRules(): List<AutomationRuleEntity> = ruleDao.getActiveRules()
    suspend fun updateRuleLastTriggered(id: Long, timestamp: Long) = ruleDao.updateLastTriggered(id, timestamp)

    suspend fun insertLog(log: AutomationLogEntity) = logDao.insertLog(log)
    suspend fun clearLogs() = logDao.clearAllLogs()
}
