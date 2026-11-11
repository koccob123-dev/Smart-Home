package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FanActiveContainer
import com.example.ui.theme.FanActiveSky
import com.example.ui.theme.OnWarningContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningContainer
import kotlin.math.roundToInt

@Composable
fun FanControlCard(
    fanSpeed: Int,
    onSpeedChange: (Int) -> Unit,
    onSpeedChangeFinished: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = fanSpeed > 0

    val infiniteTransition = rememberInfiniteTransition(label = "fanSpinTransition")
    val durationMillis = if (isRunning) {
        val speedFactor = (100 - fanSpeed).coerceAtLeast(10)
        (speedFactor * 18).coerceIn(250, 2000)
    } else {
        10000
    }

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isRunning) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fanSpinRotation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("fan_control_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.5.dp,
            if (isRunning) FanActiveSky.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRunning) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Icon, Title, and Speed Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isRunning) FanActiveContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_fan),
                            contentDescription = "Fan speed icon",
                            tint = if (isRunning) FanActiveSky else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(26.dp)
                                .rotate(if (isRunning) rotationAngle else 0f)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Fan Speed Control",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Feed: fan-speed • PWM Pin: GPIO16 (D0)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp
                        )
                    }
                }

                // Speed Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isRunning) FanActiveSky else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.testTag("fan_speed_badge")
                ) {
                    Text(
                        text = "$fanSpeed%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isRunning) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Slider
            Slider(
                value = fanSpeed.toFloat(),
                onValueChange = { onSpeedChange(it.roundToInt()) },
                onValueChangeFinished = { onSpeedChangeFinished(fanSpeed) },
                valueRange = 0f..100f,
                steps = 99,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fan_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = FanActiveSky,
                    activeTrackColor = FanActiveSky,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            // Preset Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(0 to "0%", 25 to "25%", 50 to "50%", 75 to "75%", 100 to "MAX").forEach { (preset, label) ->
                    val isSelected = fanSpeed == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSpeedChangeFinished(preset) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("fan_preset_$preset"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FanActiveSky,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Important Hardware Notice Callout
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = WarningContainer.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Hardware Notice",
                        tint = OnWarningContainer,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hardware Notice: Fan speed regulation on GPIO16 (D0) requires a compatible PWM driver (MOSFET module for DC fans or TRIAC AC dimmer for mains fans). A mechanical relay alone CANNOT regulate fan speed (relays are strictly ON/OFF).",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnWarningContainer,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
