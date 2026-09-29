package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RecordingState
import com.example.ui.theme.FoxyPrimary
import com.example.ui.theme.FoxyPrimaryGlow
import com.example.ui.theme.FoxyRecordRed
import com.example.ui.theme.FoxySurface
import com.example.ui.theme.FoxySurfaceVariant
import com.example.ui.theme.FoxyTextPrimary
import com.example.ui.theme.FoxyTextSecondary
import com.example.ui.theme.FoxyTextTertiary
import com.example.ui.theme.FoxyWarning
import com.example.util.TimeUtils

enum class NavigationTab(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    RECORDINGS("Recordings", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun MainBottomBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    recordingState: RecordingState,
    elapsedDurationMs: Long,
    onPauseResumeClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecordingOrPaused = recordingState == RecordingState.RECORDING || recordingState == RecordingState.PAUSED

    Column(modifier = modifier.fillMaxWidth()) {
        // Active recording floating bar (if user is navigating other tabs while recording)
        AnimatedVisibility(
            visible = isRecordingOrPaused,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp)),
                color = if (recordingState == RecordingState.RECORDING) Color(0xEE2A1224) else Color(0xEE2A2012),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (recordingState == RecordingState.RECORDING) FoxyRecordRed.copy(alpha = 0.5f) else FoxyWarning.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (recordingState == RecordingState.RECORDING) FoxyRecordRed else FoxyWarning)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (recordingState == RecordingState.RECORDING) "REC" else "PAUSED",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (recordingState == RecordingState.RECORDING) FoxyRecordRed else FoxyWarning
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = TimeUtils.formatDuration(elapsedDurationMs),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF3B2A50))
                                .clickable(onClick = onPauseResumeClick)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (recordingState == RecordingState.PAUSED) "Resume" else "Pause",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(FoxyRecordRed)
                                .clickable(onClick = onStopClick)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Stop",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Standard Navigation Bar
        NavigationBar(
            containerColor = FoxySurface,
            contentColor = FoxyTextPrimary,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            NavigationTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                val icon = if (isSelected) tab.selectedIcon else tab.unselectedIcon

                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onTabSelected(tab) },
                    icon = {
                        if (tab == NavigationTab.HOME && isRecordingOrPaused) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = if (recordingState == RecordingState.RECORDING) FoxyRecordRed else FoxyWarning,
                                        modifier = Modifier.size(8.dp)
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tab.title
                                )
                            }
                        } else {
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.title
                            )
                        }
                    },
                    label = {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = FoxyPrimaryGlow,
                        indicatorColor = FoxyPrimary,
                        unselectedIconColor = FoxyTextTertiary,
                        unselectedTextColor = FoxyTextTertiary
                    ),
                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                )
            }
        }
    }
}
