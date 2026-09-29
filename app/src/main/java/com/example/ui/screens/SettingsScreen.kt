package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StorageHelper
import com.example.model.AudioSourceType
import com.example.model.Bitrate
import com.example.model.CountdownDuration
import com.example.model.FacecamShape
import com.example.model.FacecamSize
import com.example.model.FrameRate
import com.example.model.RecorderConfig
import com.example.model.RecordingOrientation
import com.example.model.ThemeMode
import com.example.model.VideoResolution
import com.example.ui.components.GlassCard
import com.example.ui.theme.FoxyAccentCyan
import com.example.ui.theme.FoxyAccentPink
import com.example.ui.theme.FoxyBackground
import com.example.ui.theme.FoxyCardElevated
import com.example.ui.theme.FoxyPrimary
import com.example.ui.theme.FoxyPrimaryGlow
import com.example.ui.theme.FoxySurface
import com.example.ui.theme.FoxySurfaceVariant
import com.example.ui.theme.FoxyTextPrimary
import com.example.ui.theme.FoxyTextSecondary
import com.example.ui.theme.FoxyTextTertiary

@Composable
fun SettingsScreen(
    config: RecorderConfig,
    onConfigChange: (RecorderConfig) -> Unit,
    onOpenPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showResDialog by remember { mutableStateOf(false) }
    var showFpsDialog by remember { mutableStateOf(false) }
    var showBitrateDialog by remember { mutableStateOf(false) }
    var showOrientationDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showCountdownDialog by remember { mutableStateOf(false) }
    var showFacecamSizeDialog by remember { mutableStateOf(false) }
    var showFacecamShapeDialog by remember { mutableStateOf(false) }
    var showTouchTipDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(FoxyBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = FoxyTextPrimary
            )
            Text(
                text = "Configure video, audio, facecam & creator controls",
                style = MaterialTheme.typography.bodySmall,
                color = FoxyTextTertiary
            )
        }

        // Section: Appearance & Accessibility (Theme Selector)
        item {
            SettingsCategoryHeader(title = "Appearance & Accessibility")
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(FoxyPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Theme",
                                    tint = FoxyPrimaryGlow,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Theme & Contrast",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = FoxyTextPrimary
                                )
                                Text(
                                    text = config.themeMode.title,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = FoxyAccentCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3-way Theme Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionCard(
                            title = "System Default",
                            icon = Icons.Default.BrightnessAuto,
                            isSelected = config.themeMode == ThemeMode.SYSTEM,
                            onClick = { onConfigChange(config.copy(themeMode = ThemeMode.SYSTEM)) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionCard(
                            title = "Dark Mode",
                            icon = Icons.Default.DarkMode,
                            isSelected = config.themeMode == ThemeMode.DARK,
                            onClick = { onConfigChange(config.copy(themeMode = ThemeMode.DARK)) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionCard(
                            title = "Light Mode",
                            icon = Icons.Default.LightMode,
                            isSelected = config.themeMode == ThemeMode.LIGHT,
                            onClick = { onConfigChange(config.copy(themeMode = ThemeMode.LIGHT)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = config.themeMode.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = FoxyTextTertiary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        // Section: Video Quality
        item {
            SettingsCategoryHeader(title = "Video Quality")
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 8.dp) {
                Column {
                    SettingsNavigationItem(
                        icon = Icons.Default.Tv,
                        title = "Resolution",
                        subtitle = config.resolution.label,
                        onClick = { showResDialog = true }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        icon = Icons.Default.Speed,
                        title = "Frame Rate",
                        subtitle = config.frameRate.label,
                        onClick = { showFpsDialog = true }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        icon = Icons.Default.Videocam,
                        title = "Bitrate",
                        subtitle = config.bitrate.label,
                        onClick = { showBitrateDialog = true }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        icon = Icons.Default.Tv,
                        title = "Orientation",
                        subtitle = config.orientation.label,
                        onClick = { showOrientationDialog = true }
                    )
                }
            }
        }

        // Section: Audio
        item {
            SettingsCategoryHeader(title = "Audio Recording")
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 8.dp) {
                Column {
                    SettingsNavigationItem(
                        icon = Icons.Default.Mic,
                        title = "Audio Source",
                        subtitle = "${config.audioSource.label} (${config.audioSource.description})",
                        onClick = { showAudioDialog = true }
                    )
                }
            }
        }

        // Section: Facecam
        item {
            SettingsCategoryHeader(title = "Facecam Overlay")
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 8.dp) {
                Column {
                    SettingsToggleItem(
                        icon = Icons.Default.CameraAlt,
                        title = "Enable Front-Camera Facecam",
                        subtitle = "Show your reactions in a draggable floating window",
                        checked = config.facecamEnabled,
                        onCheckedChange = { checked ->
                            onConfigChange(config.copy(facecamEnabled = checked))
                        }
                    )
                    if (config.facecamEnabled) {
                        SettingsDivider()
                        SettingsNavigationItem(
                            icon = Icons.Default.CameraAlt,
                            title = "Facecam Size",
                            subtitle = config.facecamSize.label,
                            onClick = { showFacecamSizeDialog = true }
                        )
                        SettingsDivider()
                        SettingsNavigationItem(
                            icon = Icons.Default.CameraAlt,
                            title = "Facecam Shape",
                            subtitle = config.facecamShape.label,
                            onClick = { showFacecamShapeDialog = true }
                        )
                    }
                }
            }
        }

        // Section: Recording Controls
        item {
            SettingsCategoryHeader(title = "Controls & Overlays")
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 8.dp) {
                Column {
                    SettingsNavigationItem(
                        icon = Icons.Default.Timer,
                        title = "Countdown Timer",
                        subtitle = config.countdown.label,
                        onClick = { showCountdownDialog = true }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        icon = Icons.Default.Layers,
                        title = "Floating Recording Controls",
                        subtitle = "Draggable widget with timer, pause, screenshot & stop",
                        checked = config.floatingControlsEnabled,
                        onCheckedChange = { checked ->
                            onConfigChange(config.copy(floatingControlsEnabled = checked))
                        }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        icon = Icons.Default.VisibilityOff,
                        title = "Hide Controls While Recording",
                        subtitle = "Prevents overlay buttons from being captured in the recorded video",
                        checked = config.hideOverlayDuringRecording,
                        onCheckedChange = { checked ->
                            onConfigChange(config.copy(hideOverlayDuringRecording = checked))
                        }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        icon = Icons.Default.Vibration,
                        title = "Shake Phone to Stop",
                        subtitle = "Shake your phone to finish recording cleanly without showing menus",
                        checked = config.shakeToStop,
                        onCheckedChange = { checked ->
                            onConfigChange(config.copy(shakeToStop = checked))
                        }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        icon = Icons.Default.TouchApp,
                        title = "Show Touch Indicators",
                        subtitle = "Highlight tap locations during recording",
                        checked = config.showTouchIndicators,
                        onCheckedChange = { checked ->
                            onConfigChange(config.copy(showTouchIndicators = checked))
                            if (checked) {
                                showTouchTipDialog = true
                            }
                        }
                    )
                }
            }
        }

        // Section: Storage & Output
        item {
            SettingsCategoryHeader(title = "Output & Performance")
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 8.dp) {
                Column {
                    SettingsNavigationItem(
                        icon = Icons.Default.Folder,
                        title = "Save Folder",
                        subtitle = StorageHelper.getRecordingDirectory(context).path,
                        onClick = {
                            Toast.makeText(context, "Recordings are saved to app's dedicated storage directory", Toast.LENGTH_SHORT).show()
                        }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        icon = Icons.Default.WaterDrop,
                        title = "Watermark",
                        subtitle = "Subtle 'Recorded with Foxy' branding",
                        checked = config.watermarkEnabled,
                        onCheckedChange = { checked ->
                            onConfigChange(config.copy(watermarkEnabled = checked))
                        }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        icon = Icons.Default.BatterySaver,
                        title = "Battery-Friendly Mode",
                        subtitle = "Optimizes bitrate and power usage for long gaming sessions",
                        checked = config.batterySaverMode,
                        onCheckedChange = { checked ->
                            onConfigChange(config.copy(batterySaverMode = checked))
                        }
                    )
                }
            }
        }

        // Section: Permissions & System
        item {
            SettingsCategoryHeader(title = "System & Permissions")
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 8.dp) {
                Column {
                    SettingsNavigationItem(
                        icon = Icons.Default.Security,
                        title = "Permission Management",
                        subtitle = "Check and grant Audio, Camera, Overlay & Notification access",
                        onClick = onOpenPermissions
                    )
                }
            }
        }

        // Section: About
        item {
            SettingsCategoryHeader(title = "About App")
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 16.dp) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(FoxyPrimary, FoxyAccentPink)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Videocam, null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Foxy Screen Recorder",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = FoxyTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = FoxyPrimary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "v1.0",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = FoxyPrimaryGlow,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "App Created By FoxyPlayzZ",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = FoxyAccentCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Engineered with native Android MediaProjection API, hardware-accelerated MediaCodec video compression, lossless video trimming, and high-performance creator controls.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FoxyTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // YouTube Channel Link Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                try {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://youtube.com/@foxyplayzz?si=yEFjwsLs7oek6d4w")
                                    )
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open YouTube link", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .testTag("youtube_channel_link_button"),
                        color = Color(0xFF28111A),
                        border = BorderStroke(1.dp, Color(0xFFFF0033).copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF0033)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "YouTube",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "FoxyPlayzZ Official YouTube",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "youtube.com/@foxyplayzz",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    ),
                                    color = Color(0xFFFFB3BA)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Visit YouTube",
                                tint = Color(0xFFFF4D6D),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Dialog: Resolution
    if (showResDialog) {
        OptionSelectDialog(
            title = "Select Resolution",
            options = VideoResolution.entries,
            selected = config.resolution,
            labelProvider = { it.label },
            onSelect = {
                onConfigChange(config.copy(resolution = it))
                showResDialog = false
            },
            onDismiss = { showResDialog = false }
        )
    }

    // Dialog: FPS
    if (showFpsDialog) {
        OptionSelectDialog(
            title = "Select Frame Rate",
            options = FrameRate.entries,
            selected = config.frameRate,
            labelProvider = { it.label },
            onSelect = {
                onConfigChange(config.copy(frameRate = it))
                showFpsDialog = false
            },
            onDismiss = { showFpsDialog = false }
        )
    }

    // Dialog: Bitrate
    if (showBitrateDialog) {
        OptionSelectDialog(
            title = "Select Bitrate",
            options = Bitrate.entries,
            selected = config.bitrate,
            labelProvider = { it.label },
            onSelect = {
                onConfigChange(config.copy(bitrate = it))
                showBitrateDialog = false
            },
            onDismiss = { showBitrateDialog = false }
        )
    }

    // Dialog: Orientation
    if (showOrientationDialog) {
        OptionSelectDialog(
            title = "Select Orientation",
            options = RecordingOrientation.entries,
            selected = config.orientation,
            labelProvider = { it.label },
            onSelect = {
                onConfigChange(config.copy(orientation = it))
                showOrientationDialog = false
            },
            onDismiss = { showOrientationDialog = false }
        )
    }

    // Dialog: Audio Source
    if (showAudioDialog) {
        OptionSelectDialog(
            title = "Select Audio Source",
            options = AudioSourceType.entries,
            selected = config.audioSource,
            labelProvider = { "${it.label} - ${it.description}" },
            onSelect = {
                onConfigChange(config.copy(audioSource = it))
                showAudioDialog = false
            },
            onDismiss = { showAudioDialog = false }
        )
    }

    // Dialog: Countdown
    if (showCountdownDialog) {
        OptionSelectDialog(
            title = "Countdown Duration",
            options = CountdownDuration.entries,
            selected = config.countdown,
            labelProvider = { it.label },
            onSelect = {
                onConfigChange(config.copy(countdown = it))
                showCountdownDialog = false
            },
            onDismiss = { showCountdownDialog = false }
        )
    }

    // Dialog: Facecam Size
    if (showFacecamSizeDialog) {
        OptionSelectDialog(
            title = "Facecam Size",
            options = FacecamSize.entries,
            selected = config.facecamSize,
            labelProvider = { it.label },
            onSelect = {
                onConfigChange(config.copy(facecamSize = it))
                showFacecamSizeDialog = false
            },
            onDismiss = { showFacecamSizeDialog = false }
        )
    }

    // Dialog: Facecam Shape
    if (showFacecamShapeDialog) {
        OptionSelectDialog(
            title = "Facecam Shape",
            options = FacecamShape.entries,
            selected = config.facecamShape,
            labelProvider = { it.label },
            onSelect = {
                onConfigChange(config.copy(facecamShape = it))
                showFacecamShapeDialog = false
            },
            onDismiss = { showFacecamShapeDialog = false }
        )
    }

    // Dialog: Touch Indicators Explanation
    if (showTouchTipDialog) {
        AlertDialog(
            onDismissRequest = { showTouchTipDialog = false },
            title = { Text("Touch Indicators", color = FoxyTextPrimary) },
            text = {
                Text(
                    "To enable native system touch circles, Android requires 'Show taps' in Developer Options.\n\nWould you like to open Developer Settings to toggle it on?",
                    color = FoxyTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showTouchTipDialog = false
                    try {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                    } catch (_: Exception) {
                        Toast.makeText(context, "Developer settings not available", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Open Settings", color = FoxyPrimaryGlow)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTouchTipDialog = false }) {
                    Text("OK", color = FoxyTextSecondary)
                }
            },
            containerColor = FoxySurface
        )
    }
}

@Composable
private fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = FoxyPrimaryGlow,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun SettingsNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(FoxySurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = FoxyPrimaryGlow, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = FoxyTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = FoxyTextTertiary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(FoxySurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = FoxyPrimaryGlow, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = FoxyTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = FoxyTextTertiary
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = FoxyPrimary,
                uncheckedThumbColor = FoxyTextTertiary,
                uncheckedTrackColor = FoxySurfaceVariant
            )
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = Color(0x228C52FF),
        thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

@Composable
private fun <T> OptionSelectDialog(
    title: String,
    options: List<T>,
    selected: T,
    labelProvider: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, color = FoxyTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { option ->
                    val isSelected = option == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(option) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelect(option) },
                            colors = RadioButtonDefaults.colors(selectedColor = FoxyPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = labelProvider(option),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) FoxyPrimaryGlow else FoxyTextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = FoxyTextSecondary)
            }
        },
        containerColor = FoxySurface
    )
}

@Composable
private fun ThemeOptionCard(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("theme_selector_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) FoxyPrimary.copy(alpha = 0.18f) else FoxySurfaceVariant,
        border = BorderStroke(
            1.5.dp,
            if (isSelected) FoxyPrimary else FoxyBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) FoxyPrimary else FoxyTextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) FoxyPrimary else FoxyTextPrimary,
                maxLines = 1
            )
        }
    }
}
