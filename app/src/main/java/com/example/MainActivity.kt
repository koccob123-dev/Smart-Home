package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.ClockViewModel
import com.example.ui.SmartHomeViewModel
import com.example.ui.navigation.MainAppContainer
import com.example.ui.theme.SmartHomeTheme

class MainActivity : ComponentActivity() {
    private val smartHomeViewModel: SmartHomeViewModel by viewModels()
    private val clockViewModel: ClockViewModel by viewModels()
    private val automationViewModel: com.example.automation.ui.AutomationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartHomeTheme {
                MainAppContainer(
                    smartHomeViewModel = smartHomeViewModel,
                    clockViewModel = clockViewModel,
                    automationViewModel = automationViewModel
                )
            }
        }
    }
}
