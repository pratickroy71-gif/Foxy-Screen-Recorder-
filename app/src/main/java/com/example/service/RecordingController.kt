package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.model.RecordingEntity
import com.example.model.RecordingState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object RecordingController {

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _elapsedDurationMs = MutableStateFlow(0L)
    val elapsedDurationMs: StateFlow<Long> = _elapsedDurationMs.asStateFlow()

    private val _countdownRemaining = MutableStateFlow(0)
    val countdownRemaining: StateFlow<Int> = _countdownRemaining.asStateFlow()

    private val _currentRecordingPath = MutableStateFlow<String?>(null)
    val currentRecordingPath: StateFlow<String?> = _currentRecordingPath.asStateFlow()

    private val _lastSavedRecording = MutableStateFlow<RecordingEntity?>(null)
    val lastSavedRecording: StateFlow<RecordingEntity?> = _lastSavedRecording.asStateFlow()

    private val _facecamVisible = MutableStateFlow(false)
    val facecamVisible: StateFlow<Boolean> = _facecamVisible.asStateFlow()

    private val _screenshotEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val screenshotEvent: SharedFlow<String> = _screenshotEvent.asSharedFlow()

    private val _toastEvent = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    fun updateState(state: RecordingState) {
        _recordingState.value = state
    }

    fun updateDuration(durationMs: Long) {
        _elapsedDurationMs.value = durationMs
    }

    fun updateCountdown(seconds: Int) {
        _countdownRemaining.value = seconds
    }

    fun setRecordingPath(path: String?) {
        _currentRecordingPath.value = path
    }

    fun setLastSavedRecording(entity: RecordingEntity?) {
        _lastSavedRecording.value = entity
    }

    fun setFacecamVisible(visible: Boolean) {
        _facecamVisible.value = visible
    }

    fun notifyScreenshotSaved(path: String) {
        _screenshotEvent.tryEmit(path)
    }

    fun notifyToast(message: String) {
        _toastEvent.tryEmit(message)
    }

    fun requestPause(context: Context) {
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    fun requestResume(context: Context) {
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_RESUME
        }
        context.startService(intent)
    }

    fun requestStop(context: Context) {
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun requestScreenshot(context: Context) {
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_SCREENSHOT
        }
        context.startService(intent)
    }

    fun startService(context: Context) {
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
