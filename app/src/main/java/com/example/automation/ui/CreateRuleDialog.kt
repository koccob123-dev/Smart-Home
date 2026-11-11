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
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import com.example.automation.data.AutomationRuleEntity
import com.example.automation.data.RuleActionType
import com.example.automation.data.RuleConditionType
import kotlin.math.roundToInt

@Composable
fun CreateRuleDialog(
    ruleToEdit: AutomationRuleEntity?,
    onSave: (
        id: Long,
        title: String,
        conditionType: RuleConditionType,
        conditionTarget: String,
        conditionValue: String,
        actionType: RuleActionType,
        actionTarget: String,
        actionPayload: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(ruleToEdit?.title ?: "") }
    var conditionType by remember {
        mutableStateOf(ruleToEdit?.conditionType ?: RuleConditionType.LDR_BELOW_THRESHOLD)
    }
    var ldrThreshold by remember {
        mutableIntStateOf(ruleToEdit?.conditionValue?.toIntOrNull() ?: 250)
    }
    var socketTriggerKey by remember {
        mutableStateOf(ruleToEdit?.conditionTarget ?: "socket-1")
    }
    var socketTriggerState by remember {
        mutableStateOf(ruleToEdit?.conditionValue ?: "ON")
    }

    var actionType by remember {
        mutableStateOf(ruleToEdit?.actionType ?: RuleActionType.CONTROL_SOCKET)
    }
    var actionTarget by remember {
        mutableStateOf(ruleToEdit?.actionTarget ?: "socket-1")
    }
    var actionPayload by remember {
        mutableStateOf(ruleToEdit?.actionPayload ?: "ON")
    }
    var fanSpeedSlider by remember {
        mutableIntStateOf(ruleToEdit?.actionPayload?.toIntOrNull() ?: 75)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (ruleToEdit == null) "New IF/THEN Rule" else "Edit Automation Rule",
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
                    label = { Text("Rule Name") },
                    placeholder = { Text("e.g. Dusk Auto Lighting, Heat Ventilation") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_title_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // IF Section
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("IF (Trigger Condition)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = conditionType == RuleConditionType.LDR_BELOW_THRESHOLD,
                                    onClick = {
                                        conditionType = RuleConditionType.LDR_BELOW_THRESHOLD
                                        ldrThreshold = 250
                                    },
                                    label = { Text("Dusk / Darkness (A0)", fontSize = 11.5.sp) }
                                )
                                FilterChip(
                                    selected = conditionType == RuleConditionType.LDR_ABOVE_THRESHOLD,
                                    onClick = {
                                        conditionType = RuleConditionType.LDR_ABOVE_THRESHOLD
                                        ldrThreshold = 700
                                    },
                                    label = { Text("Daylight (A0)", fontSize = 11.5.sp) }
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = conditionType == RuleConditionType.SOCKET_STATE_CHANGED,
                                    onClick = { conditionType = RuleConditionType.SOCKET_STATE_CHANGED },
                                    label = { Text("Socket Switched", fontSize = 11.5.sp) }
                                )
                                FilterChip(
                                    selected = conditionType == RuleConditionType.DEVICE_OFFLINE,
                                    onClick = { conditionType = RuleConditionType.DEVICE_OFFLINE },
                                    label = { Text("Device Offline", fontSize = 11.5.sp) }
                                )
                            }
                        }

                        // Condition Parameter Tuning
                        when (conditionType) {
                            RuleConditionType.LDR_BELOW_THRESHOLD,
                            RuleConditionType.LDR_ABOVE_THRESHOLD -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("A0 Sensor Threshold: $ldrThreshold ADC", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Slider(
                                    value = ldrThreshold.toFloat(),
                                    onValueChange = { ldrThreshold = it.roundToInt() },
                                    valueRange = 50f..950f
                                )
                            }
                            RuleConditionType.SOCKET_STATE_CHANGED -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("socket-1" to "Socket 1", "socket-2" to "Socket 2", "socket-3" to "Socket 3", "socket-4" to "Socket 4").forEach { (k, label) ->
                                        FilterChip(
                                            selected = socketTriggerKey == k,
                                            onClick = { socketTriggerKey = k },
                                            label = { Text(label, fontSize = 10.5.sp) }
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    FilterChip(selected = socketTriggerState == "ON", onClick = { socketTriggerState = "ON" }, label = { Text("Turns ON") })
                                    FilterChip(selected = socketTriggerState == "OFF", onClick = { socketTriggerState = "OFF" }, label = { Text("Turns OFF") })
                                }
                            }
                            else -> {}
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // THEN Section
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("THEN (Action to Execute)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = actionType == RuleActionType.CONTROL_SOCKET,
                                    onClick = {
                                        actionType = RuleActionType.CONTROL_SOCKET
                                        actionTarget = "socket-1"
                                    },
                                    label = { Text("Control Socket", fontSize = 11.5.sp) }
                                )
                                FilterChip(
                                    selected = actionType == RuleActionType.CONTROL_ALL_SOCKETS,
                                    onClick = {
                                        actionType = RuleActionType.CONTROL_ALL_SOCKETS
                                        actionTarget = "all"
                                    },
                                    label = { Text("All Sockets", fontSize = 11.5.sp) }
                                )
                                FilterChip(
                                    selected = actionType == RuleActionType.SET_FAN_SPEED,
                                    onClick = {
                                        actionType = RuleActionType.SET_FAN_SPEED
                                        actionTarget = "fan"
                                    },
                                    label = { Text("Fan Speed", fontSize = 11.5.sp) }
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = actionType == RuleActionType.CLOCK_SET_NIGHT_MODE,
                                    onClick = {
                                        actionType = RuleActionType.CLOCK_SET_NIGHT_MODE
                                        actionTarget = "clock"
                                    },
                                    label = { Text("Clock Night Dim", fontSize = 11.5.sp) }
                                )
                                FilterChip(
                                    selected = actionType == RuleActionType.CLOCK_AUTO_BRIGHTNESS,
                                    onClick = {
                                        actionType = RuleActionType.CLOCK_AUTO_BRIGHTNESS
                                        actionTarget = "clock"
                                    },
                                    label = { Text("Clock Auto-LDR", fontSize = 11.5.sp) }
                                )
                                FilterChip(
                                    selected = actionType == RuleActionType.SEND_ALERT_NOTIFICATION,
                                    onClick = {
                                        actionType = RuleActionType.SEND_ALERT_NOTIFICATION
                                        actionTarget = "alert"
                                    },
                                    label = { Text("Send Alert", fontSize = 11.5.sp) }
                                )
                            }
                        }

                        // Target adjustments
                        when (actionType) {
                            RuleActionType.CONTROL_SOCKET,
                            RuleActionType.CONTROL_ALL_SOCKETS -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(selected = actionPayload == "ON", onClick = { actionPayload = "ON" }, label = { Text("Turn ON") })
                                    FilterChip(selected = actionPayload == "OFF", onClick = { actionPayload = "OFF" }, label = { Text("Turn OFF") })
                                }
                            }
                            RuleActionType.SET_FAN_SPEED -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Target Speed: $fanSpeedSlider%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Slider(
                                    value = fanSpeedSlider.toFloat(),
                                    onValueChange = {
                                        fanSpeedSlider = it.roundToInt()
                                        actionPayload = fanSpeedSlider.toString()
                                    },
                                    valueRange = 0f..100f
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val condTarget = when (conditionType) {
                        RuleConditionType.LDR_BELOW_THRESHOLD,
                        RuleConditionType.LDR_ABOVE_THRESHOLD -> "A0"
                        RuleConditionType.SOCKET_STATE_CHANGED -> socketTriggerKey
                        RuleConditionType.DEVICE_OFFLINE -> "smart_home"
                        RuleConditionType.TEMPERATURE_ABOVE -> "temp"
                    }
                    val condValue = when (conditionType) {
                        RuleConditionType.LDR_BELOW_THRESHOLD,
                        RuleConditionType.LDR_ABOVE_THRESHOLD -> ldrThreshold.toString()
                        RuleConditionType.SOCKET_STATE_CHANGED -> socketTriggerState
                        RuleConditionType.DEVICE_OFFLINE -> "OFFLINE"
                        RuleConditionType.TEMPERATURE_ABOVE -> "35"
                    }
                    onSave(
                        ruleToEdit?.id ?: 0L,
                        title,
                        conditionType,
                        condTarget,
                        condValue,
                        actionType,
                        actionTarget,
                        actionPayload
                    )
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_rule_button")
            ) {
                Text("Save Rule")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Cancel")
            }
        }
    )
}
