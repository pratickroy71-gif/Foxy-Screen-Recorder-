package com.example.model

data class RecorderConfig(
    val resolution: VideoResolution = VideoResolution.R_1080P,
    val frameRate: FrameRate = FrameRate.FPS_60,
    val bitrate: Bitrate = Bitrate.B_16M,
    val audioSource: AudioSourceType = AudioSourceType.MIC,
    val countdown: CountdownDuration = CountdownDuration.SEC_3,
    val orientation: RecordingOrientation = RecordingOrientation.AUTO,
    val facecamEnabled: Boolean = false,
    val facecamSize: FacecamSize = FacecamSize.MEDIUM,
    val facecamShape: FacecamShape = FacecamShape.CIRCLE,
    val showTouchIndicators: Boolean = true,
    val watermarkEnabled: Boolean = false,
    val batterySaverMode: Boolean = false,
    val floatingControlsEnabled: Boolean = true,
    val dedicatedFolderName: String = "FoxyRecordings"
)
