package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.data.StorageHelper
import com.example.model.AudioSourceType
import com.example.model.CountdownDuration
import com.example.model.RecordingEntity
import com.example.model.RecordingState
import com.example.service.FloatingOverlayService
import com.example.service.MediaProjectionHolder
import com.example.service.RecordingController
import com.example.service.ScreenRecordService
import com.example.ui.components.CountdownDialog
import com.example.ui.components.PermissionDialog
import com.example.ui.components.VideoCompressSheet
import com.example.ui.components.VideoPlayerModal
import com.example.ui.components.VideoTrimSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MainBottomBar
import com.example.ui.screens.NavigationTab
import com.example.ui.screens.RecordingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.FoxyScreenRecorderTheme
import com.example.util.ShareHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var prefsManager: PreferencesManager
    private lateinit var appDatabase: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefsManager = PreferencesManager(this)
        appDatabase = AppDatabase.getDatabase(this)

        setContent {
            FoxyScreenRecorderTheme {
                MainAppContent(
                    prefsManager = prefsManager,
                    appDatabase = appDatabase
                )
            }
        }
    }
}

@Composable
private fun MainAppContent(
    prefsManager: PreferencesManager,
    appDatabase: AppDatabase
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }

    // State from RecorderConfig
    val config by prefsManager.configFlow.collectAsState()

    // State from RecordingController
    val recordingState by RecordingController.recordingState.collectAsState()
    val elapsedDurationMs by RecordingController.elapsedDurationMs.collectAsState()

    // Database states
    val allRecordings by appDatabase.recordingDao().getAllRecordings().collectAsState(initial = emptyList())
    val recentRecordings by appDatabase.recordingDao().getRecentRecordings().collectAsState(initial = emptyList())
    val totalClipsCount by appDatabase.recordingDao().getRecordingCount().collectAsState(initial = 0)
    val totalStorageUsed by appDatabase.recordingDao().getTotalStorageUsed().collectAsState(initial = 0L)
    val totalDurationMs by appDatabase.recordingDao().getTotalDurationMs().collectAsState(initial = 0L)

    val storageStats = remember(totalStorageUsed, totalClipsCount) {
        StorageHelper.getDeviceStorageStats(context, totalStorageUsed, totalClipsCount)
    }

    // Modal dialogs state
    var selectedVideoForPlayback by remember { mutableStateOf<RecordingEntity?>(null) }
    var selectedVideoForTrimming by remember { mutableStateOf<RecordingEntity?>(null) }
    var selectedVideoForCompression by remember { mutableStateOf<RecordingEntity?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Countdown state
    var isCountingDown by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableIntStateOf(0) }
    var countdownJob by remember { mutableStateOf<Job?>(null) }

    // Toast listener
    LaunchedEffect(Unit) {
        RecordingController.toastEvent.collectLatest { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Projection Launcher
    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            MediaProjectionHolder.set(result.resultCode, result.data)
            RecordingController.startService(context)
        } else {
            Toast.makeText(context, "Screen capture permission was not granted", Toast.LENGTH_SHORT).show()
            RecordingController.updateState(RecordingState.IDLE)
        }
    }

    // Permission Launcher for Audio & Camera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: true
        if (!audioGranted && config.audioSource != AudioSourceType.MUTE) {
            Toast.makeText(context, "Microphone permission is required for audio recording", Toast.LENGTH_LONG).show()
        }
    }

    fun initiateRecordingFlow() {
        // Step 1: Check permissions
        val needsAudio = config.audioSource != AudioSourceType.MUTE
        val audioGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        val permissionsToRequest = mutableListOf<String>()
        if (needsAudio && !audioGranted) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (config.facecamEnabled && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        if (!hasNotifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
            return
        }

        // Step 2: Handle countdown if configured
        val countSeconds = config.countdown.seconds
        if (countSeconds > 0) {
            isCountingDown = true
            countdownSeconds = countSeconds
            countdownJob?.cancel()
            countdownJob = coroutineScope.launch {
                for (s in countSeconds downTo 1) {
                    countdownSeconds = s
                    delay(1000)
                }
                isCountingDown = false
                // Launch MediaProjection consent
                val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
            }
        } else {
            // Immediate capture consent
            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        }
    }

    // Back handler for sub-tabs or open player
    BackHandler(enabled = selectedVideoForPlayback != null || selectedVideoForTrimming != null || selectedVideoForCompression != null || currentTab != NavigationTab.HOME) {
        when {
            selectedVideoForPlayback != null -> selectedVideoForPlayback = null
            selectedVideoForTrimming != null -> selectedVideoForTrimming = null
            selectedVideoForCompression != null -> selectedVideoForCompression = null
            currentTab != NavigationTab.HOME -> currentTab = NavigationTab.HOME
        }
    }

    Scaffold(
        bottomBar = {
            MainBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                recordingState = recordingState,
                elapsedDurationMs = elapsedDurationMs,
                onPauseResumeClick = {
                    if (recordingState == RecordingState.PAUSED) {
                        RecordingController.requestResume(context)
                    } else {
                        RecordingController.requestPause(context)
                    }
                },
                onStopClick = {
                    RecordingController.requestStop(context)
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (currentTab) {
                NavigationTab.HOME -> {
                    HomeScreen(
                        recordingState = recordingState,
                        elapsedDurationMs = elapsedDurationMs,
                        config = config,
                        storageStats = storageStats,
                        totalDurationMs = totalDurationMs,
                        recentRecordings = recentRecordings,
                        onRecordClick = {
                            when (recordingState) {
                                RecordingState.IDLE, RecordingState.STOPPED -> initiateRecordingFlow()
                                RecordingState.RECORDING, RecordingState.PAUSED -> RecordingController.requestStop(context)
                                RecordingState.COUNTDOWN -> {
                                    countdownJob?.cancel()
                                    isCountingDown = false
                                }
                            }
                        },
                        onPauseClick = {
                            if (recordingState == RecordingState.PAUSED) {
                                RecordingController.requestResume(context)
                            } else {
                                RecordingController.requestPause(context)
                            }
                        },
                        onScreenshotClick = {
                            RecordingController.requestScreenshot(context)
                        },
                        onConfigChange = { newConfig ->
                            prefsManager.updateConfig { newConfig }
                        },
                        onFacecamToggle = {
                            if (!config.facecamEnabled) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                                }
                                if (!Settings.canDrawOverlays(context)) {
                                    Toast.makeText(context, "Facecam requires 'Display over other apps' permission", Toast.LENGTH_SHORT).show()
                                    showPermissionDialog = true
                                }
                            }
                            prefsManager.updateConfig { it.copy(facecamEnabled = !it.facecamEnabled) }
                        },
                        onOpenPermissions = { showPermissionDialog = true },
                        onPlayRecording = { rec -> selectedVideoForPlayback = rec },
                        onViewAllRecordings = { currentTab = NavigationTab.RECORDINGS },
                        onShareRecording = { rec -> ShareHelper.shareVideo(context, rec.filePath, rec.title) }
                    )
                }

                NavigationTab.RECORDINGS -> {
                    RecordingsScreen(
                        recordings = allRecordings,
                        onPlayRecording = { rec -> selectedVideoForPlayback = rec },
                        onShareRecording = { rec -> ShareHelper.shareVideo(context, rec.filePath, rec.title) },
                        onDeleteRecording = { rec ->
                            coroutineScope.launch {
                                ShareHelper.deleteFile(rec.filePath)
                                appDatabase.recordingDao().deleteRecording(rec)
                                Toast.makeText(context, "Recording deleted", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRenameRecording = { rec, newName ->
                            coroutineScope.launch {
                                val newPath = ShareHelper.renameFile(rec.filePath, newName)
                                if (newPath != null) {
                                    appDatabase.recordingDao().updateRecording(
                                        rec.copy(title = newName, filePath = newPath)
                                    )
                                    Toast.makeText(context, "Recording renamed", Toast.LENGTH_SHORT).show()
                                } else {
                                    appDatabase.recordingDao().renameRecording(rec.id, newName)
                                }
                            }
                        },
                        onToggleFavorite = { rec ->
                            coroutineScope.launch {
                                appDatabase.recordingDao().toggleFavorite(rec.id, !rec.isFavorite)
                            }
                        },
                        onTrimRecording = { rec ->
                            selectedVideoForTrimming = rec
                        },
                        onCompressRecording = { rec ->
                            selectedVideoForCompression = rec
                        }
                    )
                }

                NavigationTab.SETTINGS -> {
                    SettingsScreen(
                        config = config,
                        onConfigChange = { newConfig ->
                            prefsManager.updateConfig { newConfig }
                        },
                        onOpenPermissions = { showPermissionDialog = true }
                    )
                }
            }
        }
    }

    // Modal Video Player
    selectedVideoForPlayback?.let { video ->
        VideoPlayerModal(
            recording = video,
            onDismiss = { selectedVideoForPlayback = null },
            onShare = { ShareHelper.shareVideo(context, video.filePath, video.title) },
            onDelete = {
                coroutineScope.launch {
                    ShareHelper.deleteFile(video.filePath)
                    appDatabase.recordingDao().deleteRecording(video)
                    selectedVideoForPlayback = null
                    Toast.makeText(context, "Recording deleted", Toast.LENGTH_SHORT).show()
                }
            },
            onRename = {
                // Handled in recordings screen or quick dialog
                Toast.makeText(context, "Open Recordings tab to rename file", Toast.LENGTH_SHORT).show()
            },
            onTrim = {
                selectedVideoForTrimming = video
            },
            onCompress = {
                selectedVideoForCompression = video
            }
        )
    }

    // Modal Video Trimmer Sheet
    selectedVideoForTrimming?.let { video ->
        VideoTrimSheet(
            recording = video,
            onDismiss = { selectedVideoForTrimming = null },
            onTrimSuccess = { newTrimmedRecording ->
                selectedVideoForTrimming = null
                selectedVideoForPlayback = newTrimmedRecording
                Toast.makeText(context, "Trimmed video saved successfully!", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Modal Video Compression Sheet
    selectedVideoForCompression?.let { video ->
        VideoCompressSheet(
            recording = video,
            onDismiss = { selectedVideoForCompression = null },
            onCompressionSuccess = { newCompressedRecording ->
                selectedVideoForCompression = null
                selectedVideoForPlayback = newCompressedRecording
                Toast.makeText(context, "Compressed video saved successfully!", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Modal Countdown Overlay
    if (isCountingDown) {
        CountdownDialog(
            remainingSeconds = countdownSeconds,
            onCancel = {
                countdownJob?.cancel()
                isCountingDown = false
                RecordingController.updateState(RecordingState.IDLE)
            }
        )
    }

    // Modal Permission Manager
    if (showPermissionDialog) {
        PermissionDialog(onDismiss = { showPermissionDialog = false })
    }
}
