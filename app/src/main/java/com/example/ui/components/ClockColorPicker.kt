package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClockColor

val PRESET_COLORS = listOf(
    ClockColor(0, 200, 255) to "Cyan",
    ClockColor(0, 255, 128) to "Emerald",
    ClockColor(255, 180, 0) to "Amber",
    ClockColor(255, 40, 100) to "Ruby",
    ClockColor(180, 80, 255) to "Purple",
    ClockColor(255, 255, 255) to "White",
    ClockColor(255, 120, 0) to "Orange",
    ClockColor(50, 120, 255) to "Sky Blue",
    ClockColor(0, 255, 220) to "Aqua",
    ClockColor(255, 220, 0) to "Yellow",
    ClockColor(255, 80, 180) to "Hot Pink",
    ClockColor(120, 255, 0) to "Lime"
)

@Composable
fun ClockColorPickerSection(
    clockColor: ClockColor,
    weekColor: ClockColor,
    colonColor: ClockColor,
    dateColor: ClockColor,
    digit1Color: ClockColor,
    digit2Color: ClockColor,
    digit3Color: ClockColor,
    digit4Color: ClockColor,
    onClockColorChange: (ClockColor) -> Unit,
    onWeekColorChange: (ClockColor) -> Unit,
    onColonColorChange: (ClockColor) -> Unit,
    onDateColorChange: (ClockColor) -> Unit,
    onDigitColorChange: (Int, ClockColor) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTarget by remember { mutableIntStateOf(0) }
    val targets = listOf("All Digits", "Digit 1 (H1)", "Digit 2 (H2)", "Digit 3 (M1)", "Digit 4 (M2)", "Weekday", "Colon", "Date")

    val currentColor = when (selectedTarget) {
        0 -> clockColor
        1 -> digit1Color
        2 -> digit2Color
        3 -> digit3Color
        4 -> digit4Color
        5 -> weekColor
        6 -> colonColor
        else -> dateColor
    }

    val onCurrentColorChange: (ClockColor) -> Unit = { newColor ->
        when (selectedTarget) {
            0 -> onClockColorChange(newColor)
            1 -> onDigitColorChange(1, newColor)
            2 -> onDigitColorChange(2, newColor)
            3 -> onDigitColorChange(3, newColor)
            4 -> onDigitColorChange(4, newColor)
            5 -> onWeekColorChange(newColor)
            6 -> onColonColorChange(newColor)
            else -> onDateColorChange(newColor)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("clock_color_picker_section"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Color picker",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RGB Color Controls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = currentColor.toComposeColor(),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier.size(width = 48.dp, height = 24.dp)
                ) {}
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Segment Tabs (All Digits, Digit 1..4, Weekday, Colon, Date)
            ScrollableTabRow(
                selectedTabIndex = selectedTarget,
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                targets.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTarget == index,
                        onClick = { selectedTarget = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTarget == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Preset Color Swatches",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(PRESET_COLORS) { (presetColor, name) ->
                    val isSelected = currentColor.r == presetColor.r &&
                            currentColor.g == presetColor.g &&
                            currentColor.b == presetColor.b

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onCurrentColorChange(presetColor) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(presetColor.toComposeColor())
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = if (presetColor.r > 200 && presetColor.g > 200 && presetColor.b > 200) Color.Black else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
