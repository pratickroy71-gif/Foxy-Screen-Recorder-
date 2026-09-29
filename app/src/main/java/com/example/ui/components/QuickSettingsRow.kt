package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioSourceType
import com.example.model.FrameRate
import com.example.model.RecorderConfig
import com.example.model.VideoResolution
import com.example.ui.theme.FoxyAccentPink
import com.example.ui.theme.FoxyBorder
import com.example.ui.theme.FoxyCardElevated
import com.example.ui.theme.FoxyPrimary
import com.example.ui.theme.FoxyPrimaryGlow
import com.example.ui.theme.FoxySurfaceVariant
import com.example.ui.theme.FoxyTextPrimary
import com.example.ui.theme.FoxyTextSecondary
import com.example.ui.theme.FoxyTextTertiary

@Composable
fun QuickSettingsRow(
    config: RecorderConfig,
    onConfigChange: (RecorderConfig) -> Unit,
    onFacecamToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResMenu by remember { mutableStateOf(false) }
    var showFpsMenu by remember { mutableStateOf(false) }
    var showAudioMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Resolution Button
        Box(modifier = Modifier.weight(1f)) {
            QuickSettingChip(
                icon = Icons.Default.Tv,
                title = "Res",
                value = when (config.resolution) {
                    VideoResolution.R_1080P -> "1080p"
                    VideoResolution.R_720P -> "720p"
                    VideoResolution.R_480P -> "480p"
                },
                isActive = true,
                onClick = { showResMenu = true },
                testTag = "quick_setting_resolution"
            )

            DropdownMenu(
                expanded = showResMenu,
                onDismissRequest = { showResMenu = false },
                modifier = Modifier.background(FoxyCardElevated)
            ) {
                VideoResolution.entries.forEach { res ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = res.label,
                                color = if (config.resolution == res) FoxyPrimaryGlow else Color.White
                            )
                        },
                        onClick = {
                            onConfigChange(config.copy(resolution = res))
                            showResMenu = false
                        }
                    )
                }
            }
        }

        // FPS Button
        Box(modifier = Modifier.weight(1f)) {
            QuickSettingChip(
                icon = Icons.Default.Speed,
                title = "FPS",
                value = "${config.frameRate.fps} fps",
                isActive = true,
                onClick = { showFpsMenu = true },
                testTag = "quick_setting_fps"
            )

            DropdownMenu(
                expanded = showFpsMenu,
                onDismissRequest = { showFpsMenu = false },
                modifier = Modifier.background(FoxyCardElevated)
            ) {
                FrameRate.entries.forEach { rate ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = rate.label,
                                color = if (config.frameRate == rate) FoxyPrimaryGlow else Color.White
                            )
                        },
                        onClick = {
                            onConfigChange(config.copy(frameRate = rate))
                            showFpsMenu = false
                        }
                    )
                }
            }
        }

        // Audio Button
        Box(modifier = Modifier.weight(1f)) {
            val audioIcon = if (config.audioSource == AudioSourceType.MUTE) Icons.Default.MicOff else Icons.Default.Mic
            QuickSettingChip(
                icon = audioIcon,
                title = "Audio",
                value = when (config.audioSource) {
                    AudioSourceType.MUTE -> "Muted"
                    AudioSourceType.MIC -> "Mic"
                    AudioSourceType.INTERNAL -> "Internal"
                    AudioSourceType.INTERNAL_AND_MIC -> "Dual"
                },
                isActive = config.audioSource != AudioSourceType.MUTE,
                onClick = { showAudioMenu = true },
                testTag = "quick_setting_audio"
            )

            DropdownMenu(
                expanded = showAudioMenu,
                onDismissRequest = { showAudioMenu = false },
                modifier = Modifier.background(FoxyCardElevated)
            ) {
                AudioSourceType.entries.forEach { src ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = src.label,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (config.audioSource == src) FoxyPrimaryGlow else Color.White
                                )
                                Text(
                                    text = src.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FoxyTextTertiary
                                )
                            }
                        },
                        onClick = {
                            onConfigChange(config.copy(audioSource = src))
                            showAudioMenu = false
                        }
                    )
                }
            }
        }

        // Facecam Toggle Button
        Box(modifier = Modifier.weight(1f)) {
            QuickSettingChip(
                icon = Icons.Default.CameraFront,
                title = "Facecam",
                value = if (config.facecamEnabled) "ON" else "OFF",
                isActive = config.facecamEnabled,
                onClick = onFacecamToggle,
                testTag = "quick_setting_facecam"
            )
        }
    }
}

@Composable
private fun QuickSettingChip(
    icon: ImageVector,
    title: String,
    value: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isActive) FoxyPrimary.copy(alpha = 0.6f) else FoxyBorder,
        label = "BorderColor"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        color = FoxySurfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isActive) FoxyPrimaryGlow else FoxyTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = FoxyTextTertiary,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isActive) FoxyTextPrimary else FoxyTextSecondary,
                maxLines = 1
            )
        }
    }
}
