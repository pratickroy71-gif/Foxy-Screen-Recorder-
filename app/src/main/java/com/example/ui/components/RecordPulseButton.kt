package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RecordingState
import com.example.ui.theme.FoxyAccentPink
import com.example.ui.theme.FoxyPrimary
import com.example.ui.theme.FoxyPrimaryGlow
import com.example.ui.theme.FoxyRecordRed
import com.example.ui.theme.FoxyTextPrimary
import com.example.ui.theme.FoxyTextSecondary
import com.example.ui.theme.FoxyTextTertiary
import com.example.ui.theme.FoxyWarning
import com.example.util.TimeUtils

@Composable
fun RecordPulseButton(
    recordingState: RecordingState,
    elapsedDurationMs: Long,
    onRecordClick: () -> Unit,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecording = recordingState == RecordingState.RECORDING
    val isPaused = recordingState == RecordingState.PAUSED

    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.14f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isRecording) 0.6f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(170.dp)
        ) {
            // Outer glowing ring
            Box(
                modifier = Modifier
                    .size(156.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        when {
                            isRecording -> FoxyRecordRed.copy(alpha = glowAlpha)
                            isPaused -> FoxyWarning.copy(alpha = glowAlpha)
                            else -> FoxyPrimary.copy(alpha = glowAlpha * 0.5f)
                        }
                    )
            )

            // Mid border ring
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = when {
                            isRecording -> FoxyRecordRed.copy(alpha = 0.8f)
                            isPaused -> FoxyWarning.copy(alpha = 0.8f)
                            else -> FoxyPrimaryGlow.copy(alpha = 0.4f)
                        },
                        shape = CircleShape
                    )
            )

            // Inner button
            val gradient = when {
                isRecording -> listOf(FoxyRecordRed, Color(0xFFC9184A))
                isPaused -> listOf(FoxyWarning, Color(0xFFE85D04))
                else -> listOf(FoxyPrimary, FoxyAccentPink)
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(114.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradient))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = Color.White),
                        onClick = onRecordClick
                    )
                    .testTag("record_button")
            ) {
                Icon(
                    imageVector = when {
                        isRecording -> Icons.Filled.Stop
                        isPaused -> Icons.Filled.PlayArrow
                        else -> Icons.Filled.FiberManualRecord
                    },
                    contentDescription = when {
                        isRecording -> "Stop Recording"
                        isPaused -> "Resume Recording"
                        else -> "Start Recording"
                    },
                    tint = Color.White,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Status text and timer readout
        when {
            isRecording -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = TimeUtils.formatDuration(elapsedDurationMs),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        ),
                        color = FoxyRecordRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "REC • Tap to Stop",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = FoxyTextSecondary
                    )
                }
            }
            isPaused -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = TimeUtils.formatDuration(elapsedDurationMs),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        ),
                        color = FoxyWarning
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "PAUSED • Tap to Resume",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = FoxyTextSecondary
                    )
                }
            }
            else -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "READY TO RECORD",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = FoxyTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tap circle to capture screen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FoxyTextTertiary
                    )
                }
            }
        }
    }
}
