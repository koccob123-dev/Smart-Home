package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClockConnectionState
import com.example.model.ClockStatus
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusConnecting
import com.example.ui.theme.StatusDemo
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.StatusError
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClockStatusHero(
    clockStatus: ClockStatus,
    connectionState: ClockConnectionState,
    clockIp: String,
    isDemoMode: Boolean,
    isBusy: Boolean,
    onIpChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(Date()) }

    // Live clock ticker
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }

    val timeFormatPattern = if (clockStatus.hour24) "HH:mm:ss" else "hh:mm:ss a"
    val timeFormatter = remember(clockStatus.hour24) { SimpleDateFormat(timeFormatPattern, Locale.getDefault()) }
    val timeParts = timeFormatter.format(currentTime).split(":")
    val hours = timeParts.getOrNull(0) ?: "12"
    val minutes = timeParts.getOrNull(1) ?: "34"
    val secondsAndAmPm = timeParts.getOrNull(2) ?: "56"

    val weekdayFormatter = remember { SimpleDateFormat("EEEE", Locale.getDefault()) }
    val weekdayStr = weekdayFormatter.format(currentTime).uppercase()

    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()) }
    val dateStr = dateFormatter.format(currentTime)

    // Colon blink animation
    val infiniteTransition = rememberInfiniteTransition(label = "colonBlink")
    val colonAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (clockStatus.blinkColon) 0.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "colonAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("clock_status_hero"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
        ),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Connection status chip & Demo badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (statusColor, statusText) = when {
                        isDemoMode -> Pair(StatusDemo, "VIRTUAL DEMO")
                        connectionState is ClockConnectionState.Connected -> Pair(StatusConnected, "ONLINE")
                        connectionState is ClockConnectionState.Testing -> Pair(StatusConnecting, "TESTING...")
                        connectionState is ClockConnectionState.Error -> Pair(StatusError, "OFFLINE")
                        else -> Pair(StatusDisconnected, "READY")
                    }

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B)
                ) {
                    Text(
                        text = "ESP8266 LED CLOCK • PORT 80",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // LED Segment Display Digital Preview
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF020617),
                border = BorderStroke(1.5.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Weekday bar
                    Text(
                        text = "• $weekdayStr •",
                        color = clockStatus.weekColor.toComposeColor(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Digital Clock Digits
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = hours,
                            color = clockStatus.clockColor.toComposeColor(),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = ":",
                            color = clockStatus.colonColor.toComposeColor(),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .alpha(colonAlpha)
                        )

                        Text(
                            text = minutes,
                            color = clockStatus.clockColor.toComposeColor(),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = ":",
                            color = clockStatus.colonColor.toComposeColor(),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .alpha(colonAlpha)
                        )

                        Text(
                            text = secondsAndAmPm,
                            color = clockStatus.clockColor.toComposeColor(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }

                    // Optional Date Preview
                    if (clockStatus.showDate) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dateStr,
                            color = clockStatus.dateColor.toComposeColor(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // IP Configuration & Test Connection Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = clockIp,
                    onValueChange = onIpChange,
                    label = { Text("Clock IP (Device 2)", color = Color(0xFF94A3B8)) },
                    placeholder = { Text("192.168.1.102", color = Color(0xFF64748B)) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("clock_ip_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = onTestConnection,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("test_connection_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Test connection",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Error display if failed
            if (connectionState is ClockConnectionState.Error) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StatusError.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, StatusError.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Error",
                            tint = StatusError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = connectionState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusError,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }
    }
}
