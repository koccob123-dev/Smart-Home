package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DeviceStatusCard
import com.example.ui.components.FanControlCard
import com.example.ui.components.LiveFeedMonitor
import com.example.ui.components.SocketCard

@Composable
fun SmartHomeScreenContent(
    viewModel: SmartHomeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val isWideScreen = maxWidth > 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .widthIn(max = 700.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Connection & Telemetry Status Card (device-status)
            DeviceStatusCard(
                connectionState = uiState.connectionState,
                deviceStatus = uiState.deviceStatus,
                isDemoMode = uiState.isDemoMode,
                username = uiState.username,
                onConnect = { viewModel.connect() },
                onDisconnect = { viewModel.disconnect() },
                onOpenSettings = { viewModel.openSettingsDialog() }
            )

            // 2. Sockets Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AC Socket Relays (Device 1)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val activeCount = uiState.sockets.count { it.isOn }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "$activeCount of 4 Active",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 3. Socket Cards Grid (2 columns on wide or stacked on compact)
            if (isWideScreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val firstHalf = uiState.sockets.take(2)
                    val secondHalf = uiState.sockets.drop(2)

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        firstHalf.forEach { socket ->
                            SocketCard(
                                socket = socket,
                                onToggle = { viewModel.toggleSocket(socket.key) }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        secondHalf.forEach { socket ->
                            SocketCard(
                                socket = socket,
                                onToggle = { viewModel.toggleSocket(socket.key) }
                            )
                        }
                    }
                }
            } else {
                for (i in uiState.sockets.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val socketA = uiState.sockets[i]
                        SocketCard(
                            socket = socketA,
                            onToggle = { viewModel.toggleSocket(socketA.key) },
                            modifier = Modifier.weight(1f)
                        )

                        if (i + 1 < uiState.sockets.size) {
                            val socketB = uiState.sockets[i + 1]
                            SocketCard(
                                socket = socketB,
                                onToggle = { viewModel.toggleSocket(socketB.key) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // 4. Fan Speed Regulation Card
            FanControlCard(
                fanSpeed = uiState.fanSpeed,
                onSpeedChange = { newSpeed -> viewModel.setFanSpeed(newSpeed, immediatePublish = false) },
                onSpeedChangeFinished = { finalSpeed -> viewModel.setFanSpeed(finalSpeed, immediatePublish = true) }
            )

            // 5. Real-Time MQTT Feed Traffic Monitor
            LiveFeedMonitor(
                logs = uiState.logs,
                onClearLogs = { viewModel.clearLogs() }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
