package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ClockViewModel
import com.example.ui.components.ClockBrightnessSection
import com.example.ui.components.ClockColorPickerSection
import com.example.ui.components.ClockEffectsSection
import com.example.ui.components.ClockStatusHero
import com.example.ui.components.ClockTimeDateSection

@Composable
fun LedClockScreen(
    viewModel: ClockViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .widthIn(max = 700.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Live Digital Preview & IP Configuration
            ClockStatusHero(
                clockStatus = uiState.clockStatus,
                connectionState = uiState.connectionState,
                clockIp = uiState.clockIp,
                isDemoMode = uiState.isDemoMode,
                isBusy = uiState.isBusy,
                onIpChange = { viewModel.setClockIp(it) },
                onTestConnection = { viewModel.testConnection() }
            )

            // 2. RGB Color Pickers (Global, Per-digit, Weekday, Colon, Date)
            ClockColorPickerSection(
                clockColor = uiState.clockStatus.clockColor,
                weekColor = uiState.clockStatus.weekColor,
                colonColor = uiState.clockStatus.colonColor,
                dateColor = uiState.clockStatus.dateColor,
                digit1Color = uiState.clockStatus.digit1Color,
                digit2Color = uiState.clockStatus.digit2Color,
                digit3Color = uiState.clockStatus.digit3Color,
                digit4Color = uiState.clockStatus.digit4Color,
                onClockColorChange = { viewModel.setClockColor(it) },
                onWeekColorChange = { viewModel.setWeekColor(it) },
                onColonColorChange = { viewModel.setColonColor(it) },
                onDateColorChange = { viewModel.setDateColor(it) },
                onDigitColorChange = { digit, color -> viewModel.setDigitColor(digit, color) }
            )

            // 3. 16 LED Clock Effects & Speed
            ClockEffectsSection(
                currentEffectId = uiState.clockStatus.effect,
                effectsEnabled = uiState.clockStatus.effectsEnabled,
                currentSpeed = uiState.clockStatus.speed,
                onEffectSelect = { viewModel.setEffect(it) },
                onToggleEffectsEnabled = { viewModel.toggleEffectsEnabled(it) },
                onSpeedChange = { viewModel.setSpeed(it, immediate = false) },
                onSpeedChangeFinished = { viewModel.setSpeed(it, immediate = true) }
            )

            // 4. Brightness, Auto LDR & Sensor Test
            ClockBrightnessSection(
                brightness = uiState.clockStatus.brightness,
                autoBrightness = uiState.clockStatus.autoBrightness,
                ldrValue = uiState.clockStatus.ldrRawValue,
                ldrResultText = uiState.ldrTestResult,
                isBusy = uiState.isBusy,
                onBrightnessChange = { viewModel.setBrightness(it, immediate = false) },
                onBrightnessChangeFinished = { viewModel.setBrightness(it, immediate = true) },
                onAutoBrightnessToggle = { viewModel.setAutoBrightness(it) },
                onTestLdr = { viewModel.testLdr() }
            )

            // 5. Time Format, Colon Blink, Show Date, NTP Sync, Save & Reset
            ClockTimeDateSection(
                hour24 = uiState.clockStatus.hour24,
                blinkColon = uiState.clockStatus.blinkColon,
                showDate = uiState.clockStatus.showDate,
                timezone = uiState.clockStatus.timezone,
                ntpSynced = uiState.clockStatus.ntpSynced,
                isBusy = uiState.isBusy,
                onHour24Toggle = { viewModel.setHour24(it) },
                onBlinkColonToggle = { viewModel.setBlinkColon(it) },
                onShowDateToggle = { viewModel.setShowDate(it) },
                onNtpSync = { viewModel.triggerNtpSync() },
                onSaveSettings = { viewModel.saveSettings() },
                onResetSettings = { viewModel.resetSettings() }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
