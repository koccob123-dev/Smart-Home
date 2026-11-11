package com.example.automation.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.automation.data.AutomationLogEntity
import com.example.automation.data.AutomationRepository
import com.example.automation.data.AutomationRuleEntity
import com.example.automation.data.DeviceScheduleEntity
import com.example.automation.data.RuleActionType
import com.example.automation.data.RuleConditionType
import com.example.automation.data.TargetDeviceType
import com.example.automation.engine.AutomationAlarmManager
import com.example.automation.engine.ConditionAutomationEvaluator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AutomationUiState(
    val schedules: List<DeviceScheduleEntity> = emptyList(),
    val rules: List<AutomationRuleEntity> = emptyList(),
    val logs: List<AutomationLogEntity> = emptyList(),
    val activeTab: Int = 0, // 0: Schedules, 1: IF/THEN Rules, 2: Activity Log
    val showCreateScheduleDialog: Boolean = false,
    val editingSchedule: DeviceScheduleEntity? = null,
    val showCreateRuleDialog: Boolean = false,
    val editingRule: AutomationRuleEntity? = null
)

class AutomationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AutomationRepository(application)
    private val alarmManager = AutomationAlarmManager(application)
    private val conditionEvaluator = ConditionAutomationEvaluator(application)

    private val _uiState = MutableStateFlow(AutomationUiState())
    val uiState: StateFlow<AutomationUiState> = combine(
        _uiState,
        repository.allSchedules,
        repository.allRules,
        repository.allLogs
    ) { current, schedules, rules, logs ->
        current.copy(
            schedules = schedules,
            rules = rules,
            logs = logs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AutomationUiState()
    )

    private val _userMessage = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    fun setActiveTab(tabIndex: Int) {
        _uiState.update { it.copy(activeTab = tabIndex.coerceIn(0, 2)) }
    }

    fun openCreateScheduleDialog(scheduleToEdit: DeviceScheduleEntity? = null) {
        _uiState.update {
            it.copy(showCreateScheduleDialog = true, editingSchedule = scheduleToEdit)
        }
    }

    fun closeCreateScheduleDialog() {
        _uiState.update {
            it.copy(showCreateScheduleDialog = false, editingSchedule = null)
        }
    }

    fun openCreateRuleDialog(ruleToEdit: AutomationRuleEntity? = null) {
        _uiState.update {
            it.copy(showCreateRuleDialog = true, editingRule = ruleToEdit)
        }
    }

    fun closeCreateRuleDialog() {
        _uiState.update {
            it.copy(showCreateRuleDialog = false, editingRule = null)
        }
    }

    fun saveSchedule(
        id: Long = 0,
        title: String,
        targetType: TargetDeviceType,
        targetId: String,
        actionPayload: String,
        startHour: Int,
        startMinute: Int,
        hasEndTime: Boolean,
        endHour: Int,
        endMinute: Int,
        repeatDays: String
    ) {
        viewModelScope.launch {
            val nextRun = alarmManager.calculateNextRunMillis(startHour, startMinute, repeatDays)
            val schedule = DeviceScheduleEntity(
                id = id,
                title = title.ifBlank { "Scheduled ${targetType.name}" },
                targetType = targetType,
                targetId = targetId,
                actionPayload = actionPayload,
                startHour = startHour,
                startMinute = startMinute,
                hasEndTime = hasEndTime,
                endHour = endHour,
                endMinute = endMinute,
                repeatDays = repeatDays,
                isEnabled = true,
                nextRunEpochMs = nextRun
            )

            val savedId = if (id == 0L) {
                repository.insertSchedule(schedule)
            } else {
                repository.updateSchedule(schedule)
                id
            }

            // Schedule the alarm
            alarmManager.scheduleAlarm(schedule.copy(id = savedId))
            closeCreateScheduleDialog()
            emitMessage("Schedule saved: Next run ${DeviceScheduleEntity.formatTime(startHour, startMinute)}")
        }
    }

    fun toggleSchedule(schedule: DeviceScheduleEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.setScheduleEnabled(schedule.id, enabled)
            if (enabled) {
                val nextRun = alarmManager.calculateNextRunMillis(schedule.startHour, schedule.startMinute, schedule.repeatDays)
                repository.updateRunTimestamps(schedule.id, nextRun, schedule.lastRunEpochMs)
                alarmManager.scheduleAlarm(schedule.copy(isEnabled = true, nextRunEpochMs = nextRun))
                emitMessage("${schedule.title} enabled")
            } else {
                alarmManager.cancelAlarm(schedule.id)
                emitMessage("${schedule.title} paused")
            }
        }
    }

    fun deleteSchedule(schedule: DeviceScheduleEntity) {
        viewModelScope.launch {
            alarmManager.cancelAlarm(schedule.id)
            repository.deleteSchedule(schedule)
            emitMessage("Schedule deleted: ${schedule.title}")
        }
    }

    fun testRunSchedule(schedule: DeviceScheduleEntity) {
        viewModelScope.launch {
            val smartHomeManager = com.example.data.SmartHomeManager(getApplication())
            val clockRepo = com.example.data.ClockRepository()
            val settingsRepo = com.example.data.SettingsRepository(getApplication())
            val prefs = settingsRepo.preferencesFlow.first()

            var message = ""
            var success = true

            when (schedule.targetType) {
                TargetDeviceType.SMART_HOME_SOCKET -> {
                    val cmd = if (schedule.actionPayload.equals("ON", ignoreCase = true)) "1" else "0"
                    smartHomeManager.publishValue(schedule.targetId, cmd)
                    message = "Test: ${schedule.targetId} set to ${schedule.actionPayload}"
                }
                TargetDeviceType.SMART_HOME_ALL_SOCKETS -> {
                    val cmd = if (schedule.actionPayload.equals("ON", ignoreCase = true)) "1" else "0"
                    listOf("socket-1", "socket-2", "socket-3", "socket-4").forEach { s ->
                        smartHomeManager.publishValue(s, cmd)
                    }
                    message = "Test: All sockets set to ${schedule.actionPayload}"
                }
                TargetDeviceType.SMART_HOME_FAN -> {
                    val speed = schedule.actionPayload.toIntOrNull() ?: 50
                    smartHomeManager.publishValue("fan-speed", speed.toString())
                    message = "Test: Fan speed set to $speed%"
                }
                TargetDeviceType.CLOCK_POWER -> {
                    val b = if (schedule.actionPayload.equals("OFF", ignoreCase = true)) 0 else 180
                    val res = clockRepo.setBrightness(prefs.clockIp, b)
                    success = res.isSuccess
                    message = if (success) "Test: Clock LEDs set to ${schedule.actionPayload}" else "Test: Clock unreachable"
                }
                TargetDeviceType.CLOCK_NIGHT_MODE -> {
                    clockRepo.setAutoBrightness(prefs.clockIp, false)
                    val res = clockRepo.setBrightness(prefs.clockIp, 15)
                    success = res.isSuccess
                    message = if (success) "Test: Clock Night Mode activated (15)" else "Test: Clock unreachable"
                }
                TargetDeviceType.CLOCK_BRIGHTNESS -> {
                    val b = schedule.actionPayload.toIntOrNull() ?: 180
                    val res = clockRepo.setBrightness(prefs.clockIp, b)
                    success = res.isSuccess
                    message = if (success) "Test: Clock Brightness set to $b" else "Test: Clock unreachable"
                }
                TargetDeviceType.CLOCK_AUTO_BRIGHTNESS -> {
                    val res = clockRepo.setAutoBrightness(prefs.clockIp, true)
                    success = res.isSuccess
                    message = if (success) "Test: Clock Auto-Brightness enabled" else "Test: Clock unreachable"
                }
                TargetDeviceType.CLOCK_EFFECT -> {
                    val eff = schedule.actionPayload.toIntOrNull() ?: 1
                    val res = clockRepo.setEffect(prefs.clockIp, eff)
                    success = res.isSuccess
                    message = if (success) "Test: Clock Effect #$eff applied" else "Test: Clock unreachable"
                }
            }

            repository.insertLog(
                AutomationLogEntity(
                    title = "Manual Test: ${schedule.title}",
                    description = message,
                    sourceType = "TEST",
                    isSuccess = success
                )
            )
            emitMessage(message)
        }
    }

    fun saveRule(
        id: Long = 0,
        title: String,
        conditionType: RuleConditionType,
        conditionTarget: String,
        conditionValue: String,
        actionType: RuleActionType,
        actionTarget: String,
        actionPayload: String
    ) {
        viewModelScope.launch {
            val rule = AutomationRuleEntity(
                id = id,
                title = title.ifBlank { "Rule ${conditionType.name}" },
                conditionType = conditionType,
                conditionTarget = conditionTarget,
                conditionValue = conditionValue,
                actionType = actionType,
                actionTarget = actionTarget,
                actionPayload = actionPayload,
                isEnabled = true
            )

            if (id == 0L) {
                repository.insertRule(rule)
            } else {
                repository.updateRule(rule)
            }
            closeCreateRuleDialog()
            emitMessage("Automation Rule saved: ${rule.title}")
        }
    }

    fun toggleRule(rule: AutomationRuleEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.setRuleEnabled(rule.id, enabled)
            emitMessage("Rule ${if (enabled) "enabled" else "paused"}: ${rule.title}")
        }
    }

    fun deleteRule(rule: AutomationRuleEntity) {
        viewModelScope.launch {
            repository.deleteRule(rule)
            emitMessage("Rule deleted: ${rule.title}")
        }
    }

    fun testRunRule(rule: AutomationRuleEntity) {
        viewModelScope.launch {
            val res = conditionEvaluator.executeRule(rule, "Manual User Test")
            emitMessage(if (res) "Rule test succeeded!" else "Rule test failed")
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            emitMessage("Activity log cleared")
        }
    }

    private fun emitMessage(msg: String) {
        viewModelScope.launch {
            _userMessage.emit(msg)
        }
    }
}
