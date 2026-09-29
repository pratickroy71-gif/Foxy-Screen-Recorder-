package com.example.data

import android.content.Context
import android.content.SharedPreferences
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("foxy_screen_recorder_prefs", Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<RecorderConfig> = _configFlow.asStateFlow()

    private fun loadConfig(): RecorderConfig {
        val resName = prefs.getString("resolution", VideoResolution.R_1080P.name) ?: VideoResolution.R_1080P.name
        val fpsVal = prefs.getInt("fps", 60)
        val bitrateVal = prefs.getInt("bitrate", Bitrate.B_16M.bps)
        val audioSrc = prefs.getString("audio_source", AudioSourceType.MIC.name) ?: AudioSourceType.MIC.name
        val countdownSec = prefs.getInt("countdown", 3)
        val orient = prefs.getString("orientation", RecordingOrientation.AUTO.name) ?: RecordingOrientation.AUTO.name
        val facecam = prefs.getBoolean("facecam_enabled", false)
        val facecamSizeName = prefs.getString("facecam_size", FacecamSize.MEDIUM.name) ?: FacecamSize.MEDIUM.name
        val facecamShapeName = prefs.getString("facecam_shape", FacecamShape.CIRCLE.name) ?: FacecamShape.CIRCLE.name
        val touch = prefs.getBoolean("show_touch", true)
        val watermark = prefs.getBoolean("watermark", false)
        val batterySaver = prefs.getBoolean("battery_saver", false)
        val floating = prefs.getBoolean("floating_controls", true)
        val hideOverlay = prefs.getBoolean("hide_overlay_during_recording", true)
        val shake = prefs.getBoolean("shake_to_stop", true)
        val themeName = prefs.getString("theme_mode", ThemeMode.DARK.name) ?: ThemeMode.DARK.name

        return RecorderConfig(
            resolution = try { VideoResolution.valueOf(resName) } catch (_: Exception) { VideoResolution.R_1080P },
            frameRate = FrameRate.fromFps(fpsVal),
            bitrate = Bitrate.fromBps(bitrateVal),
            audioSource = AudioSourceType.fromName(audioSrc),
            countdown = CountdownDuration.fromSeconds(countdownSec),
            orientation = RecordingOrientation.fromName(orient),
            facecamEnabled = facecam,
            facecamSize = try { FacecamSize.valueOf(facecamSizeName) } catch (_: Exception) { FacecamSize.MEDIUM },
            facecamShape = try { FacecamShape.valueOf(facecamShapeName) } catch (_: Exception) { FacecamShape.CIRCLE },
            showTouchIndicators = touch,
            watermarkEnabled = watermark,
            batterySaverMode = batterySaver,
            floatingControlsEnabled = floating,
            hideOverlayDuringRecording = hideOverlay,
            shakeToStop = shake,
            themeMode = try { ThemeMode.valueOf(themeName) } catch (_: Exception) { ThemeMode.DARK }
        )
    }

    fun updateConfig(update: (RecorderConfig) -> RecorderConfig) {
        val newConfig = update(_configFlow.value)
        _configFlow.value = newConfig
        prefs.edit()
            .putString("resolution", newConfig.resolution.name)
            .putInt("fps", newConfig.frameRate.fps)
            .putInt("bitrate", newConfig.bitrate.bps)
            .putString("audio_source", newConfig.audioSource.name)
            .putInt("countdown", newConfig.countdown.seconds)
            .putString("orientation", newConfig.orientation.name)
            .putBoolean("facecam_enabled", newConfig.facecamEnabled)
            .putString("facecam_size", newConfig.facecamSize.name)
            .putString("facecam_shape", newConfig.facecamShape.name)
            .putBoolean("show_touch", newConfig.showTouchIndicators)
            .putBoolean("watermark", newConfig.watermarkEnabled)
            .putBoolean("battery_saver", newConfig.batterySaverMode)
            .putBoolean("floating_controls", newConfig.floatingControlsEnabled)
            .putBoolean("hide_overlay_during_recording", newConfig.hideOverlayDuringRecording)
            .putBoolean("shake_to_stop", newConfig.shakeToStop)
            .putString("theme_mode", newConfig.themeMode.name)
            .apply()
    }
}
