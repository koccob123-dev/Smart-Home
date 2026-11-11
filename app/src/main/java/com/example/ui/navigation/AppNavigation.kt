package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.automation.ui.AutomationScreen
import com.example.automation.ui.AutomationViewModel
import com.example.ui.ClockViewModel
import com.example.ui.SmartHomeScreenContent
import com.example.ui.SmartHomeViewModel
import com.example.ui.components.HardwareGuideDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LedClockScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.StatusDemo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(
    smartHomeViewModel: SmartHomeViewModel,
    clockViewModel: ClockViewModel,
    automationViewModel: AutomationViewModel,
    modifier: Modifier = Modifier
) {
    // 0: Dashboard (All Devices), 1: Smart Home, 2: LED Clock, 3: Automations, 4: Settings
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val smartHomeState by smartHomeViewModel.uiState.collectAsStateWithLifecycle()

    // Handle system back navigation to return gracefully to Dashboard tab
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    val screenTitle = when (selectedTab) {
        0 -> "SmartHome IO Dashboard"
        1 -> "Smart Home (Device 1)"
        2 -> "LED Digital Clock (Device 2)"
        3 -> "Automations & Schedules"
        else -> "Settings & Devices"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    AnimatedContent(
                        targetState = screenTitle,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)) togetherWith
                                    fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing))
                        },
                        label = "topBarTitleTransition"
                    ) { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    if (smartHomeState.isDemoMode) {
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

                    IconButton(
                        onClick = { smartHomeViewModel.openGuideDialog() },
                        modifier = Modifier.testTag("app_guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeveloperBoard,
                            contentDescription = "Hardware Guide"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                AnimatedNavItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    selectedIcon = Icons.Filled.Dashboard,
                    unselectedIcon = Icons.Outlined.Dashboard,
                    label = "Dashboard",
                    testTag = "nav_dashboard"
                )

                AnimatedNavItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    selectedIcon = Icons.Outlined.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    label = "Smart Home",
                    testTag = "nav_smart_home"
                )

                AnimatedNavItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    selectedIcon = Icons.Filled.Schedule,
                    unselectedIcon = Icons.Outlined.Schedule,
                    label = "LED Clock",
                    testTag = "nav_led_clock"
                )

                AnimatedNavItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    selectedIcon = Icons.Filled.ElectricBolt,
                    unselectedIcon = Icons.Filled.ElectricBolt,
                    label = "Automation",
                    testTag = "nav_automations"
                )

                AnimatedNavItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    selectedIcon = Icons.Filled.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    label = "Settings",
                    testTag = "nav_settings"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clipToBounds()
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val duration = 350
                    val slideEasing = FastOutSlowInEasing
                    val fadeEasing = FastOutSlowInEasing

                    if (targetState > initialState) {
                        // Forward transition (moving to a tab on the right):
                        // Incoming screen slides in from the right (with subtle 32% parallax offset) + fade-in
                        (slideInHorizontally(
                            animationSpec = tween(durationMillis = duration, easing = slideEasing),
                            initialOffsetX = { fullWidth -> (fullWidth * 0.32f).toInt() }
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = duration, easing = fadeEasing)
                        )).togetherWith(
                            // Outgoing screen slides out to the left + fade-out
                            slideOutHorizontally(
                                animationSpec = tween(durationMillis = duration, easing = slideEasing),
                                targetOffsetX = { fullWidth -> (-fullWidth * 0.32f).toInt() }
                            ) + fadeOut(
                                animationSpec = tween(durationMillis = 220, easing = fadeEasing)
                            )
                        )
                    } else {
                        // Backward transition (moving to a tab on the left):
                        // Incoming screen slides in from the left (-32% offset) + fade-in
                        (slideInHorizontally(
                            animationSpec = tween(durationMillis = duration, easing = slideEasing),
                            initialOffsetX = { fullWidth -> (-fullWidth * 0.32f).toInt() }
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = duration, easing = fadeEasing)
                        )).togetherWith(
                            // Outgoing screen slides out to the right + fade-out
                            slideOutHorizontally(
                                animationSpec = tween(durationMillis = duration, easing = slideEasing),
                                targetOffsetX = { fullWidth -> (fullWidth * 0.32f).toInt() }
                            ) + fadeOut(
                                animationSpec = tween(durationMillis = 220, easing = fadeEasing)
                            )
                        )
                    }
                },
                label = "mainTabTransition",
                modifier = Modifier.fillMaxSize()
            ) { target ->
                when (target) {
                    0 -> DashboardScreen(
                        smartHomeViewModel = smartHomeViewModel,
                        clockViewModel = clockViewModel,
                        onNavigateToSmartHome = { selectedTab = 1 },
                        onNavigateToClock = { selectedTab = 2 },
                        onNavigateToAutomations = { selectedTab = 3 },
                        onNavigateToSettings = { selectedTab = 4 }
                    )
                    1 -> SmartHomeScreenContent(viewModel = smartHomeViewModel)
                    2 -> LedClockScreen(viewModel = clockViewModel)
                    3 -> AutomationScreen(viewModel = automationViewModel)
                    4 -> SettingsScreen(
                        smartHomeViewModel = smartHomeViewModel,
                        clockViewModel = clockViewModel,
                        onOpenGuide = { smartHomeViewModel.openGuideDialog() }
                    )
                }
            }
        }
    }

    // Hardware & Firmware Guide Dialog
    if (smartHomeState.showGuideDialog) {
        HardwareGuideDialog(
            onDismiss = { smartHomeViewModel.closeGuideDialog() }
        )
    }

    // Settings Dialog (can also be invoked from error prompts)
    if (smartHomeState.showSettingsDialog) {
        SettingsDialog(
            currentUsername = smartHomeState.username,
            currentAioKey = smartHomeState.aioKey,
            isDemoMode = smartHomeState.isDemoMode,
            onSave = { username, key -> smartHomeViewModel.saveCredentials(username, key) },
            onToggleDemoMode = { enabled -> smartHomeViewModel.toggleDemoMode(enabled) },
            onClearCredentials = { smartHomeViewModel.clearCredentials() },
            onDismiss = { smartHomeViewModel.closeSettingsDialog() }
        )
    }
}

/**
 * Animated bottom navigation bar item with subtle micro-scale and alpha transitions,
 * maintaining clean solid Material 3 styling without flashy bounces or gradients.
 */
@Composable
private fun RowScope.AnimatedNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    testTag: String
) {
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1.0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "navIconScale"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.72f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "navIconAlpha"
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.72f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "navLabelAlpha"
    )

    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                modifier = Modifier.graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    alpha = iconAlpha
                }
            )
        },
        label = {
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.graphicsLayer { alpha = labelAlpha }
            )
        },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.testTag(testTag)
    )
}
