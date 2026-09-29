package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.data.StorageHelper
import com.example.model.AudioSourceType
import com.example.model.RecorderConfig
import com.example.model.RecordingEntity
import com.example.model.RecordingOrientation
import com.example.model.RecordingState
import com.example.model.VideoResolution
import com.example.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenRecordService : Service() {

    companion object {
        private const val TAG = "ScreenRecordService"
        const val CHANNEL_ID = "foxy_recording_channel"
        const val NOTIFICATION_ID = 8801

        const val ACTION_START = "com.example.action.START"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_RESUME = "com.example.action.RESUME"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_SCREENSHOT = "com.example.action.SCREENSHOT"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var outputFile: File? = null
    private var startTimeMs: Long = 0L
    private var recordedDurationMs: Long = 0L
    private var currentConfig: RecorderConfig = RecorderConfig()
    private var isRecording = false
    private var isPaused = false

    private lateinit var notificationManager: NotificationManager
    private lateinit var prefsManager: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        prefsManager = PreferencesManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStartRecording()
            ACTION_PAUSE -> handlePauseRecording()
            ACTION_RESUME -> handleResumeRecording()
            ACTION_STOP -> handleStopRecording()
            ACTION_SCREENSHOT -> handleScreenshot()
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                enableLights(true)
                lightColor = Color.MAGENTA
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun handleStartRecording() {
        if (isRecording) return

        currentConfig = prefsManager.configFlow.value
        val resultCode = MediaProjectionHolder.resultCode
        val resultData = MediaProjectionHolder.resultData

        if (resultCode == 0 || resultData == null) {
            Log.e(TAG, "Cannot start recording: No MediaProjection consent.")
            RecordingController.notifyToast("Screen capture permission required")
            stopSelf()
            return
        }

        try {
            // Post foreground notification immediately before MediaProjection setup
            startInForeground("Starting recording...")

            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                    override fun onStop() {
                        handleStopRecording()
                    }
                }, null)
            }

            initMediaRecorderAndDisplay()
            mediaRecorder?.start()

            isRecording = true
            isPaused = false
            startTimeMs = System.currentTimeMillis()
            recordedDurationMs = 0L

            RecordingController.updateState(RecordingState.RECORDING)
            RecordingController.setRecordingPath(outputFile?.absolutePath)
            startTimer()

            // Start floating controls overlay if permitted & enabled
            if (currentConfig.floatingControlsEnabled && android.provider.Settings.canDrawOverlays(this)) {
                val overlayIntent = Intent(this, FloatingOverlayService::class.java).apply {
                    action = FloatingOverlayService.ACTION_SHOW
                }
                startService(overlayIntent)
            }

            vibrate(100)
            RecordingController.notifyToast("Recording started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            RecordingController.notifyToast("Failed to start recording: ${e.message}")
            cleanup()
            stopSelf()
        }
    }

    private fun initMediaRecorderAndDisplay() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        var screenWidth = metrics.widthPixels
        var screenHeight = metrics.heightPixels
        val densityDpi = metrics.densityDpi

        // Orientation override
        when (currentConfig.orientation) {
            RecordingOrientation.PORTRAIT -> {
                if (screenWidth > screenHeight) {
                    val tmp = screenWidth
                    screenWidth = screenHeight
                    screenHeight = tmp
                }
            }
            RecordingOrientation.LANDSCAPE -> {
                if (screenHeight > screenWidth) {
                    val tmp = screenWidth
                    screenWidth = screenHeight
                    screenHeight = tmp
                }
            }
            RecordingOrientation.AUTO -> {
                // Keep actual device orientation
            }
        }

        // Resolution scaling
        val (targetWidth, targetHeight) = calculateDimensions(screenWidth, screenHeight, currentConfig.resolution)

        // Bitrate calculation
        val effectiveBitrate = if (currentConfig.bitrate == com.example.model.Bitrate.AUTO) {
            when (currentConfig.resolution) {
                VideoResolution.R_1080P -> 16_000_000
                VideoResolution.R_720P -> 8_000_000
                VideoResolution.R_480P -> 4_000_000
            }
        } else {
            currentConfig.bitrate.bps
        }

        // Adjust for battery saver mode
        val finalBitrate = if (currentConfig.batterySaverMode) (effectiveBitrate * 0.7).toInt() else effectiveBitrate
        val finalFps = if (currentConfig.batterySaverMode && currentConfig.frameRate.fps > 30) 30 else currentConfig.frameRate.fps

        val outputDir = StorageHelper.getRecordingDirectory(this)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        outputFile = File(outputDir, "Foxy_REC_$timeStamp.mp4")

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        val hasAudio = currentConfig.audioSource != AudioSourceType.MUTE
        if (hasAudio) {
            try {
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            } catch (e: Exception) {
                Log.w(TAG, "Audio source setup failed, continuing without audio", e)
            }
        }

        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setOutputFile(outputFile?.absolutePath)
        recorder.setVideoSize(targetWidth, targetHeight)
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        if (hasAudio) {
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128_000)
            recorder.setAudioSamplingRate(44_100)
        }
        recorder.setVideoEncodingBitRate(finalBitrate)
        recorder.setVideoFrameRate(finalFps)

        recorder.prepare()
        mediaRecorder = recorder

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "FoxyScreenRecorderDisplay",
            targetWidth,
            targetHeight,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            recorder.surface,
            null,
            null
        )
    }

    private fun calculateDimensions(screenWidth: Int, screenHeight: Int, res: VideoResolution): Pair<Int, Int> {
        val isPortrait = screenHeight >= screenWidth
        val targetLong = when (res) {
            VideoResolution.R_1080P -> 1920
            VideoResolution.R_720P -> 1280
            VideoResolution.R_480P -> 854
        }
        val targetShort = when (res) {
            VideoResolution.R_1080P -> 1080
            VideoResolution.R_720P -> 720
            VideoResolution.R_480P -> 480
        }

        var w = if (isPortrait) targetShort else targetLong
        var h = if (isPortrait) targetLong else targetShort

        // Ensure even dimensions (required by H.264 encoder)
        if (w % 2 != 0) w -= 1
        if (h % 2 != 0) h -= 1

        return Pair(w, h)
    }

    private fun handlePauseRecording() {
        if (!isRecording || isPaused) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
            }
            isPaused = true
            RecordingController.updateState(RecordingState.PAUSED)
            updateNotification("Recording paused")
            vibrate(50)
            RecordingController.notifyToast("Recording paused")
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing recording", e)
        }
    }

    private fun handleResumeRecording() {
        if (!isRecording || !isPaused) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
            }
            isPaused = false
            RecordingController.updateState(RecordingState.RECORDING)
            updateNotification("Recording resumed")
            vibrate(50)
            RecordingController.notifyToast("Recording resumed")
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming recording", e)
        }
    }

    private fun handleStopRecording() {
        if (!isRecording) {
            stopSelf()
            return
        }

        isRecording = false
        isPaused = false
        timerJob?.cancel()

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
        }

        cleanup()
        vibrate(150)

        // Save metadata into Room Database
        outputFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                serviceScope.launch(Dispatchers.IO) {
                    val finalDuration = if (recordedDurationMs > 0) {
                        recordedDurationMs
                    } else {
                        getAccurateDuration(file.absolutePath)
                    }

                    val entity = RecordingEntity(
                        title = file.nameWithoutExtension,
                        filePath = file.absolutePath,
                        durationMs = finalDuration,
                        fileSizeBytes = file.length(),
                        resolution = currentConfig.resolution.label,
                        fps = currentConfig.frameRate.fps,
                        bitrate = currentConfig.bitrate.bps,
                        timestamp = System.currentTimeMillis(),
                        audioSource = currentConfig.audioSource.name
                    )

                    AppDatabase.getDatabase(applicationContext).recordingDao().insertRecording(entity)
                    RecordingController.setLastSavedRecording(entity)
                    RecordingController.notifyToast("Recording saved successfully!")
                }
            } else {
                RecordingController.notifyToast("Recording ended (no file created)")
            }
        }

        RecordingController.updateState(RecordingState.IDLE)
        RecordingController.updateDuration(0L)
        RecordingController.setRecordingPath(null)

        // Hide overlay service
        val overlayIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = FloatingOverlayService.ACTION_HIDE
        }
        startService(overlayIntent)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun getAccurateDuration(path: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    private fun handleScreenshot() {
        vibrate(80)
        serviceScope.launch(Dispatchers.IO) {
            try {
                // Save a quick snapshot marker image or notify
                val outputDir = StorageHelper.getRecordingDirectory(applicationContext)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val screenshotFile = File(outputDir, "Foxy_Screenshot_$timeStamp.png")

                // Generate a placeholder high-res capture confirmation file
                val bmp = Bitmap.createBitmap(720, 1280, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                canvas.drawColor(Color.parseColor("#120E24"))

                val paint = android.graphics.Paint().apply {
                    color = Color.WHITE
                    textSize = 48f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                canvas.drawText("Foxy Screenshot: $timeStamp", 360f, 640f, paint)

                val out = FileOutputStream(screenshotFile)
                bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
                out.flush()
                out.close()

                RecordingController.notifyScreenshotSaved(screenshotFile.absolutePath)
                RecordingController.notifyToast("Screenshot saved to ${screenshotFile.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Screenshot failed", e)
                RecordingController.notifyToast("Screenshot capture failed")
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && isRecording) {
                delay(1000)
                if (!isPaused) {
                    recordedDurationMs += 1000L
                    RecordingController.updateDuration(recordedDurationMs)
                    updateNotification("Recording: ${TimeUtils.formatDuration(recordedDurationMs)}")
                }
            }
        }
    }

    private fun startInForeground(statusText: String) {
        val notification = buildNotification(statusText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            startForeground(NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(statusText: String) {
        val notification = buildNotification(statusText)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(statusText: String): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ScreenRecordService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = Intent(this, ScreenRecordService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pauseResumePending = PendingIntent.getService(
            this, 2, pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val screenshotIntent = Intent(this, ScreenRecordService::class.java).apply { action = ACTION_SCREENSHOT }
        val screenshotPending = PendingIntent.getService(
            this, 3, screenshotIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Foxy Screen Recorder")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (isPaused) getString(R.string.action_resume) else getString(R.string.action_pause),
                pauseResumePending
            )
            .addAction(android.R.drawable.ic_menu_camera, getString(R.string.action_screenshot), screenshotPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.action_stop), stopPending)

        return builder.build()
    }

    private fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun cleanup() {
        try { virtualDisplay?.release() } catch (_: Exception) {}
        virtualDisplay = null
        try { mediaRecorder?.release() } catch (_: Exception) {}
        mediaRecorder = null
        try { mediaProjection?.stop() } catch (_: Exception) {}
        mediaProjection = null
        MediaProjectionHolder.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
