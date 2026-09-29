package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.StorageHelper
import com.example.model.RecordingEntity
import com.example.ui.theme.FoxyAccentCyan
import com.example.ui.theme.FoxyCardElevated
import com.example.ui.theme.FoxyPrimary
import com.example.ui.theme.FoxyPrimaryGlow
import com.example.ui.theme.FoxySurface
import com.example.ui.theme.FoxySurfaceVariant
import com.example.ui.theme.FoxyTextPrimary
import com.example.ui.theme.FoxyTextSecondary
import com.example.ui.theme.FoxyTextTertiary
import com.example.util.TimeUtils
import com.example.util.VideoTrimmer
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoTrimSheet(
    recording: RecordingEntity,
    onDismiss: () -> Unit,
    onTrimSuccess: (RecordingEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val totalMs = recording.durationMs.coerceAtLeast(1000L).toFloat()

    var sliderRange by remember { mutableStateOf(0f..totalMs) }
    var isProcessing by remember { mutableStateOf(false) }
    var trimProgress by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val startMs = sliderRange.start.toLong()
    val endMs = sliderRange.endInclusive.toLong()
    val selectedDurationMs = (endMs - startMs).coerceAtLeast(0)

    ModalBottomSheet(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FoxySurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = "Trim",
                        tint = FoxyPrimaryGlow,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Trim Video",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = FoxyTextPrimary
                    )
                }

                if (!isProcessing) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = FoxyTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = recording.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = FoxyTextSecondary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Dual Slider Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FoxyCardElevated)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Start Time",
                                style = MaterialTheme.typography.labelSmall,
                                color = FoxyTextTertiary
                            )
                            Text(
                                text = TimeUtils.formatDuration(startMs),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = FoxyTextPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Trimmed Duration",
                                style = MaterialTheme.typography.labelSmall,
                                color = FoxyAccentCyan
                            )
                            Text(
                                text = TimeUtils.formatDuration(selectedDurationMs),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = FoxyAccentCyan
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "End Time",
                                style = MaterialTheme.typography.labelSmall,
                                color = FoxyTextTertiary
                            )
                            Text(
                                text = TimeUtils.formatDuration(endMs),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = FoxyTextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    RangeSlider(
                        value = sliderRange,
                        onValueChange = { range ->
                            sliderRange = range
                        },
                        valueRange = 0f..totalMs,
                        colors = SliderDefaults.colors(
                            thumbColor = FoxyPrimaryGlow,
                            activeTrackColor = FoxyPrimary,
                            inactiveTrackColor = FoxySurfaceVariant
                        ),
                        modifier = Modifier.testTag("trim_range_slider")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Progress or Error state
            if (isProcessing) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Trimming video... ${(trimProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = FoxyPrimaryGlow
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { trimProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = FoxyPrimary,
                        trackColor = FoxySurfaceVariant
                    )
                }
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedDurationMs < 1000) {
                        errorMessage = "Selected duration must be at least 1 second"
                        return@Button
                    }
                    isProcessing = true
                    errorMessage = null

                    coroutineScope.launch {
                        val outputDir = StorageHelper.getRecordingDirectory(context)
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        val outputPath = File(outputDir, "${recording.title}_trimmed_$timestamp.mp4").absolutePath

                        val result = VideoTrimmer.trimVideo(
                            inputPath = recording.filePath,
                            outputPath = outputPath,
                            startMs = startMs,
                            endMs = endMs,
                            onProgress = { p -> trimProgress = p }
                        )

                        isProcessing = false

                        if (result.success) {
                            val newEntity = RecordingEntity(
                                title = "${recording.title} (Trimmed)",
                                filePath = result.outputPath,
                                durationMs = result.durationMs,
                                fileSizeBytes = result.fileSizeBytes,
                                resolution = recording.resolution,
                                fps = recording.fps,
                                bitrate = recording.bitrate,
                                timestamp = System.currentTimeMillis(),
                                audioSource = recording.audioSource
                            )
                            AppDatabase.getDatabase(context).recordingDao().insertRecording(newEntity)
                            onTrimSuccess(newEntity)
                        } else {
                            errorMessage = result.errorMessage ?: "Trimming failed"
                        }
                    }
                },
                enabled = !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("apply_trim_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FoxyPrimary,
                    contentColor = Color.White
                )
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trim & Save New Video", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
