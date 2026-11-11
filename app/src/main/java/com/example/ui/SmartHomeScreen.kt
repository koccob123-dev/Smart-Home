package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ConnectionState
import com.example.ui.components.DeviceStatusCard
import com.example.ui.components.FanControlCard
import com.example.ui.components.HardwareGuideDialog
import com.example.ui.components.LiveFeedMonitor
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SocketCard
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusConnecting
import com.example.ui.theme.StatusDemo
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.StatusError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartHomeScreen(
    viewModel: SmartHomeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Home,
                                contentDescription = "App Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "SmartHome IO",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ESP8266 NodeMCU • Adafruit IO",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Demo indicator or toggle chip
                    if (uiState.isDemoMode) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusDemo.copy(alpha = 0.15f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "DEMO",
                                color = StatusDemo,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Guide button
                    IconButton(
                        onClick = { viewModel.openGuideDialog() },
                        modifier = Modifier.testTag("guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeveloperBoard,
                            contentDescription = "Hardware & ESP8266 Guide",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Settings button
                    IconButton(
                        onClick = { viewModel.openSettingsDialog() },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Adafruit IO Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                        text = "AC Socket Relays",
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
                    // Mobile vertical flow with 2x2 cards
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

    // Settings Dialog
    if (uiState.showSettingsDialog) {
        SettingsDialog(
            currentUsername = uiState.username,
            currentAioKey = uiState.aioKey,
            isDemoMode = uiState.isDemoMode,
            onSave = { username, key -> viewModel.saveCredentials(username, key) },
            onToggleDemoMode = { enabled -> viewModel.toggleDemoMode(enabled) },
            onClearCredentials = { viewModel.clearCredentials() },
            onDismiss = { viewModel.closeSettingsDialog() }
        )
    }

    // Hardware & Firmware Guide Dialog
    if (uiState.showGuideDialog) {
        HardwareGuideDialog(
            onDismiss = { viewModel.closeGuideDialog() }
        )
    }
}
