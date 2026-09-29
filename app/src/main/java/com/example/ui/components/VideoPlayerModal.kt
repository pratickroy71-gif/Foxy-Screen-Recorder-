package com.example.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.RecordingEntity
import com.example.ui.theme.FoxyAccentCyan
import com.example.ui.theme.FoxyBackground
import com.example.ui.theme.FoxyCardElevated
import com.example.ui.theme.FoxyPrimary
import com.example.ui.theme.FoxyPrimaryGlow
import com.example.ui.theme.FoxyRecordRed
import com.example.ui.theme.FoxySurface
import com.example.ui.theme.FoxyTextPrimary
import com.example.ui.theme.FoxyTextSecondary
import com.example.ui.theme.FoxyTextTertiary
import com.example.util.TimeUtils
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun VideoPlayerModal(
    recording: RecordingEntity,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onTrim: () -> Unit,
    onCompress: () -> Unit
) {
    val context = LocalContext.current
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf(recording.durationMs.toInt()) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    // Progress polling loop
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            videoViewRef?.let { vv ->
                if (!isDraggingSlider && vv.isPlaying) {
                    currentPositionMs = vv.currentPosition
                    if (totalDurationMs <= 0 && vv.duration > 0) {
                        totalDurationMs = vv.duration
                    }
                    if (totalDurationMs > 0) {
                        sliderPosition = (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                    }
                }
            }
            delay(300)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = FoxyBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_player_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = recording.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = FoxyTextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = "${recording.resolution} • ${recording.fps} FPS • ${TimeUtils.formatDate(recording.timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = FoxyTextTertiary
                        )
                    }

                    Row {
                        IconButton(onClick = onCompress) {
                            Icon(
                                imageVector = Icons.Default.Compress,
                                contentDescription = "Compress",
                                tint = FoxyAccentCyan
                            )
                        }
                        IconButton(onClick = onTrim) {
                            Icon(
                                imageVector = Icons.Default.ContentCut,
                                contentDescription = "Trim",
                                tint = FoxyPrimaryGlow
                            )
                        }
                        IconButton(onClick = onShare) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Video Display Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showControls = !showControls
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val file = File(recording.filePath)
                                if (file.exists()) {
                                    setVideoURI(Uri.fromFile(file))
                                }
                                setOnPreparedListener { mp ->
                                    mediaPlayerRef = mp
                                    if (mp.duration > 0) {
                                        totalDurationMs = mp.duration
                                    }
                                    mp.isLooping = false
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        try {
                                            mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                                        } catch (_: Exception) {}
                                    }
                                }
                                setOnCompletionListener {
                                    isPlaying = false
                                    currentPositionMs = totalDurationMs
                                }
                                videoViewRef = this
                            }
                        },
                        update = { view ->
                            videoViewRef = view
                        }
                    )

                    // Overlay Controls (Fade in/out)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showControls,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                        ) {
                            // Center Play/Pause button
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        videoViewRef?.let { vv ->
                                            val newPos = (vv.currentPosition - 5000).coerceAtLeast(0)
                                            vv.seekTo(newPos)
                                            currentPositionMs = newPos
                                        }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay5,
                                        contentDescription = "Replay 5s",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(FoxyPrimary)
                                        .clickable {
                                            videoViewRef?.let { vv ->
                                                if (vv.isPlaying) {
                                                    vv.pause()
                                                    isPlaying = false
                                                } else {
                                                    vv.start()
                                                    isPlaying = true
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        videoViewRef?.let { vv ->
                                            val newPos = (vv.currentPosition + 5000).coerceAtMost(totalDurationMs)
                                            vv.seekTo(newPos)
                                            currentPositionMs = newPos
                                        }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forward5,
                                        contentDescription = "Forward 5s",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Control Sheet
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FoxySurface)
                        .padding(16.dp)
                ) {
                    // Slider & Timestamps
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = TimeUtils.formatDuration(currentPositionMs.toLong()),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = FoxyTextSecondary
                        )

                        Slider(
                            value = sliderPosition,
                            onValueChange = { newVal ->
                                isDraggingSlider = true
                                sliderPosition = newVal
                                currentPositionMs = (newVal * totalDurationMs).toInt()
                            },
                            onValueChangeFinished = {
                                isDraggingSlider = false
                                videoViewRef?.seekTo((sliderPosition * totalDurationMs).toInt())
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .testTag("video_seek_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = FoxyPrimaryGlow,
                                activeTrackColor = FoxyPrimary,
                                inactiveTrackColor = Color(0xFF332D50)
                            )
                        )

                        Text(
                            text = TimeUtils.formatDuration(totalDurationMs.toLong()),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = FoxyTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Action Bar: Speed, Rename, Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Playback Speed Selector
                        Box {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showSpeedMenu = true },
                                color = FoxyCardElevated
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = FoxyPrimaryGlow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${playbackSpeed}x",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false },
                                modifier = Modifier.background(FoxyCardElevated)
                            ) {
                                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${speed}x",
                                                color = if (playbackSpeed == speed) FoxyPrimaryGlow else Color.White
                                            )
                                        },
                                        onClick = {
                                            playbackSpeed = speed
                                            showSpeedMenu = false
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                mediaPlayerRef?.let { mp ->
                                                    try {
                                                        mp.playbackParams = mp.playbackParams.setSpeed(speed)
                                                    } catch (_: Exception) {}
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Rename Button
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClick = onRename),
                                color = FoxyCardElevated
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Rename",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }

                            // Delete Button
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClick = onDelete),
                                color = FoxyRecordRed.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = FoxyRecordRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Delete",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = FoxyRecordRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef?.stopPlayback()
            videoViewRef = null
            mediaPlayerRef = null
        }
    }
}
