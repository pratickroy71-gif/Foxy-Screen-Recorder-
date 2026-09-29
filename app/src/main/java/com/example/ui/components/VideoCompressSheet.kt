package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.FoxyAccentPink
import com.example.ui.theme.FoxyCardElevated
import com.example.ui.theme.FoxyPrimary
import com.example.ui.theme.FoxyPrimaryGlow
import com.example.ui.theme.FoxySurface
import com.example.ui.theme.FoxySurfaceVariant
import com.example.ui.theme.FoxyTextPrimary
import com.example.ui.theme.FoxyTextSecondary
import com.example.ui.theme.FoxyTextTertiary
import com.example.util.ShareHelper
import com.example.util.TimeUtils
import com.example.util.VideoCompressor
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCompressSheet(
    recording: RecordingEntity,
    onDismiss: () -> Unit,
    onCompressionSuccess: (RecordingEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedQuality by remember { mutableStateOf(VideoCompressor.CompressionQuality.BALANCED) }
    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var resultState by remember { mutableStateOf<VideoCompressor.CompressionResult?>(null) }
    var createdEntity by remember { mutableStateOf<RecordingEntity?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(FoxyPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compress,
                            contentDescription = "Compress",
                            tint = FoxyPrimaryGlow,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Compress & Export",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = FoxyTextPrimary
                        )
                        Text(
                            text = "Hardware-accelerated MediaCodec encoding",
                            style = MaterialTheme.typography.bodySmall,
                            color = FoxyTextTertiary
                        )
                    }
                }

                if (!isProcessing) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = FoxyTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Source File Summary Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = FoxyCardElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recording.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = FoxyTextPrimary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${recording.resolution} • ${TimeUtils.formatDuration(recording.durationMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = FoxyTextTertiary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Current Size",
                            style = MaterialTheme.typography.labelSmall,
                            color = FoxyTextTertiary
                        )
                        Text(
                            text = StorageHelper.formatBytes(recording.fileSizeBytes),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = FoxyAccentCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // State 1: Success Results Card
            if (resultState != null && resultState!!.success && createdEntity != null) {
                val res = resultState!!
                val newRec = createdEntity!!

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF1B2A24),
                    border = BorderStroke(1.dp, FoxyAccentCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(FoxyAccentCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = FoxyAccentCyan,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Compression Complete!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = FoxyTextPrimary
                        )

                        Text(
                            text = "Saved ${StorageHelper.formatBytes(res.savedBytes)} (${res.reductionPercentage}% smaller)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = FoxyAccentCyan
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Original", style = MaterialTheme.typography.labelSmall, color = FoxyTextTertiary)
                                Text(
                                    StorageHelper.formatBytes(res.originalSizeBytes),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                            Text("➔", color = FoxyAccentCyan, style = MaterialTheme.typography.titleMedium)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Compressed", style = MaterialTheme.typography.labelSmall, color = FoxyTextTertiary)
                                Text(
                                    StorageHelper.formatBytes(res.compressedSizeBytes),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = FoxyAccentCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    onCompressionSuccess(newRec)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FoxyPrimary)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play")
                            }

                            OutlinedButton(
                                onClick = {
                                    ShareHelper.shareVideo(context, newRec.filePath, newRec.title)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            } else if (isProcessing) {
                // State 2: Compression in progress
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = FoxyCardElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Compressing Video...",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = FoxyTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Transcoding with MediaCodec ${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = FoxyPrimaryGlow
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = FoxyPrimary,
                            trackColor = FoxySurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Optimizing video bitrate and frame sizing",
                            style = MaterialTheme.typography.labelSmall,
                            color = FoxyTextTertiary
                        )
                    }
                }
            } else {
                // State 3: Selection list
                Text(
                    text = "Select Compression Preset",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = FoxyTextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoCompressor.CompressionQuality.entries.forEach { quality ->
                        val isSelected = selectedQuality == quality
                        val estimatedSize = (recording.fileSizeBytes * (1f - (quality.estimatedReductionPercent / 100f))).toLong()

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedQuality = quality },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Color(0xFF261D48) else FoxySurfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) FoxyPrimary else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = quality.title,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (isSelected) FoxyPrimaryGlow else FoxyTextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = FoxyAccentCyan.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "-${quality.estimatedReductionPercent}%",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = FoxyAccentCyan,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = quality.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FoxyTextTertiary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Est. Size", style = MaterialTheme.typography.labelSmall, color = FoxyTextTertiary)
                                    Text(
                                        text = "~${StorageHelper.formatBytes(estimatedSize)}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = FoxyTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        isProcessing = true
                        progress = 0f
                        errorMessage = null
                        resultState = null

                        coroutineScope.launch {
                            val outputDir = StorageHelper.getRecordingDirectory(context)
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            val qualityTag = when (selectedQuality) {
                                VideoCompressor.CompressionQuality.COMPACT -> "480p"
                                VideoCompressor.CompressionQuality.BALANCED -> "720p"
                                VideoCompressor.CompressionQuality.HIGH -> "HD"
                            }
                            val outputPath = File(outputDir, "${recording.title}_compressed_${qualityTag}_$timeStamp.mp4").absolutePath

                            val result = VideoCompressor.compressVideo(
                                inputPath = recording.filePath,
                                outputPath = outputPath,
                                quality = selectedQuality,
                                onProgress = { p -> progress = p }
                            )

                            isProcessing = false
                            resultState = result

                            if (result.success) {
                                val entity = RecordingEntity(
                                    title = "${recording.title} ($qualityTag)",
                                    filePath = result.outputPath,
                                    durationMs = result.durationMs,
                                    fileSizeBytes = result.compressedSizeBytes,
                                    resolution = "${selectedQuality.targetWidth}x${selectedQuality.targetHeight}",
                                    fps = selectedQuality.targetFps,
                                    bitrate = selectedQuality.targetBitrateBps,
                                    timestamp = System.currentTimeMillis(),
                                    audioSource = recording.audioSource
                                )
                                AppDatabase.getDatabase(context).recordingDao().insertRecording(entity)
                                createdEntity = entity
                            } else {
                                errorMessage = result.errorMessage ?: "Video compression failed"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("start_compress_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FoxyPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Compress, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compress & Save Video", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
