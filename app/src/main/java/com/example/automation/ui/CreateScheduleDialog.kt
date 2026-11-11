package com.example.automation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automation.data.DeviceScheduleEntity
import com.example.automation.data.TargetDeviceType
import kotlin.math.roundToInt

@Composable
fun CreateScheduleDialog(
    scheduleToEdit: DeviceScheduleEntity?,
    onSave: (
        id: Long,
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
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(scheduleToEdit?.title ?: "") }
    var selectedTargetType by remember {
        mutableStateOf(scheduleToEdit?.targetType ?: TargetDeviceType.SMART_HOME_SOCKET)
    }
    var selectedTargetId by remember {
        mutableStateOf(scheduleToEdit?.targetId ?: "socket-1")
    }
    var actionPayload by remember {
        mutableStateOf(scheduleToEdit?.actionPayload ?: "ON")
    }
    var startHour by remember { mutableIntStateOf(scheduleToEdit?.startHour ?: 7) }
    var startMinute by remember { mutableIntStateOf(scheduleToEdit?.startMinute ?: 0) }

    var hasEndTime by remember { mutableStateOf(scheduleToEdit?.hasEndTime ?: false) }
    var endHour by remember { mutableIntStateOf(scheduleToEdit?.endHour ?: 23) }
    var endMinute by remember { mutableIntStateOf(scheduleToEdit?.endMinute ?: 0) }

    var repeatDays by remember { mutableStateOf(scheduleToEdit?.repeatDays ?: "DAILY") }
    var fanSliderSpeed by remember { mutableIntStateOf(scheduleToEdit?.actionPayload?.toIntOrNull() ?: 70) }
    var brightnessSlider by remember { mutableIntStateOf(scheduleToEdit?.actionPayload?.toIntOrNull() ?: 180) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (scheduleToEdit == null) "New Device Schedule" else "Edit Schedule",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Schedule Name") },
                    placeholder = { Text("e.g. Morning Coffee, Living Room Night OFF") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("schedule_title_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Target Device Category
                Text("Select Target Device", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedTargetType == TargetDeviceType.SMART_HOME_SOCKET && selectedTargetId == "socket-1",
                            onClick = {
                                selectedTargetType = TargetDeviceType.SMART_HOME_SOCKET
                                selectedTargetId = "socket-1"
                            },
                            label = { Text("Socket 1", fontSize = 11.5.sp) }
                        )
                        FilterChip(
                            selected = selectedTargetType == TargetDeviceType.SMART_HOME_SOCKET && selectedTargetId == "socket-2",
                            onClick = {
                                selectedTargetType = TargetDeviceType.SMART_HOME_SOCKET
                                selectedTargetId = "socket-2"
                            },
                            label = { Text("Socket 2", fontSize = 11.5.sp) }
                        )
                        FilterChip(
                            selected = selectedTargetType == TargetDeviceType.SMART_HOME_SOCKET && selectedTargetId == "socket-3",
                            onClick = {
                                selectedTargetType = TargetDeviceType.SMART_HOME_SOCKET
                                selectedTargetId = "socket-3"
                            },
                            label = { Text("Socket 3", fontSize = 11.5.sp) }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedTargetType == TargetDeviceType.SMART_HOME_SOCKET && selectedTargetId == "socket-4",
                            onClick = {
                                selectedTargetType = TargetDeviceType.SMART_HOME_SOCKET
                                selectedTargetId = "socket-4"
                            },
                            label = { Text("Socket 4", fontSize = 11.5.sp) }
                        )
                        FilterChip(
                            selected = selectedTargetType == TargetDeviceType.SMART_HOME_ALL_SOCKETS,
                            onClick = {
                                selectedTargetType = TargetDeviceType.SMART_HOME_ALL_SOCKETS
                                selectedTargetId = "all"
                            },
                            label = { Text("All 4 Sockets", fontSize = 11.5.sp) }
                        )
                        FilterChip(
                            selected = selectedTargetType == TargetDeviceType.SMART_HOME_FAN,
                            onClick = {
                                selectedTargetType = TargetDeviceType.SMART_HOME_FAN
                                selectedTargetId = "fan"
                            },
                            label = { Text("Fan Speed", fontSize = 11.5.sp) }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedTargetType == TargetDeviceType.CLOCK_NIGHT_MODE,
                            onClick = {
                                selectedTargetType = TargetDeviceType.CLOCK_NIGHT_MODE
                                selectedTargetId = "clock"
                                actionPayload = "night"
                            },
                            label = { Text("Clock Night Dim", fontSize = 11.5.sp) }
                        )
                        FilterChip(
                            selected = selectedTargetType == TargetDeviceType.CLOCK_POWER,
                            onClick = {
                                selectedTargetType = TargetDeviceType.CLOCK_POWER
                                selectedTargetId = "clock"
                            },
                            label = { Text("Clock LEDs", fontSize = 11.5.sp) }
                        )
                        FilterChip(
                            selected = selectedTargetType == TargetDeviceType.CLOCK_BRIGHTNESS,
                            onClick = {
                                selectedTargetType = TargetDeviceType.CLOCK_BRIGHTNESS
                                selectedTargetId = "clock"
                            },
                            label = { Text("Clock Brightness", fontSize = 11.5.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Settings
                when (selectedTargetType) {
                    TargetDeviceType.SMART_HOME_SOCKET,
                    TargetDeviceType.SMART_HOME_ALL_SOCKETS,
                    TargetDeviceType.CLOCK_POWER -> {
                        Text("Action to Trigger", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                            FilterChip(
                                selected = actionPayload.equals("ON", ignoreCase = true),
                                onClick = { actionPayload = "ON" },
                                label = { Text("Turn ON") }
                            )
                            FilterChip(
                                selected = actionPayload.equals("OFF", ignoreCase = true),
                                onClick = { actionPayload = "OFF" },
                                label = { Text("Turn OFF") }
                            )
                        }
                    }

                    TargetDeviceType.SMART_HOME_FAN -> {
                        Text("Target Fan Speed: $fanSliderSpeed%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Slider(
                            value = fanSliderSpeed.toFloat(),
                            onValueChange = {
                                fanSliderSpeed = it.roundToInt()
                                actionPayload = fanSliderSpeed.toString()
                            },
                            valueRange = 0f..100f
                        )
                    }

                    TargetDeviceType.CLOCK_BRIGHTNESS -> {
                        Text("Target Brightness: $brightnessSlider / 255", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Slider(
                            value = brightnessSlider.toFloat(),
                            onValueChange = {
                                brightnessSlider = it.roundToInt()
                                actionPayload = brightnessSlider.toString()
                            },
                            valueRange = 0f..255f
                        )
                    }

                    else -> {}
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Start Time Picker
                Text("Start Execution Time", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                TimeSelector(
                    hour = startHour,
                    minute = startMinute,
                    onTimeChanged = { h, m ->
                        startHour = h
                        startMinute = m
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Auto Turn-Off / End Time Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Set Auto-Stop Time", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Automatically revert or turn OFF after duration", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = hasEndTime, onCheckedChange = { hasEndTime = it })
                }

                AnimatedVisibility(visible = hasEndTime) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        TimeSelector(
                            hour = endHour,
                            minute = endMinute,
                            onTimeChanged = { h, m ->
                                endHour = h
                                endMinute = m
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Repeat Days Selection
                Text("Repeat Schedule", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("DAILY" to "Daily", "WEEKDAYS" to "Weekdays", "WEEKENDS" to "Weekends", "ONCE" to "Once").forEach { (code, label) ->
                        FilterChip(
                            selected = repeatDays == code,
                            onClick = { repeatDays = code },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        scheduleToEdit?.id ?: 0L,
                        title,
                        selectedTargetType,
                        selectedTargetId,
                        actionPayload,
                        startHour,
                        startMinute,
                        hasEndTime,
                        endHour,
                        endMinute,
                        repeatDays
                    )
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_schedule_button")
            ) {
                Text("Save Schedule")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TimeSelector(
    hour: Int,
    minute: Int,
    onTimeChanged: (Int, Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = DeviceScheduleEntity.formatTime(hour, minute),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Quick adjustment buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = {
                        val newH = (hour + 1) % 24
                        onTimeChanged(newH, minute)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+1h", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = {
                        val newM = (minute + 15) % 60
                        val newH = if (minute + 15 >= 60) (hour + 1) % 24 else hour
                        onTimeChanged(newH, newM)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+15m", fontSize = 11.sp)
                }
            }
        }
    }
}
