package com.example.model

enum class VideoResolution(val width: Int, val height: Int, val label: String) {
    R_1080P(1920, 1080, "1080p Full HD"),
    R_720P(1280, 720, "720p HD"),
    R_480P(854, 480, "480p SD");

    companion object {
        fun fromLabel(label: String): VideoResolution =
            entries.find { it.name == label || it.label == label } ?: R_1080P
    }
}

enum class FrameRate(val fps: Int, val label: String) {
    FPS_60(60, "60 FPS (Smooth)"),
    FPS_30(30, "30 FPS (Standard)");

    companion object {
        fun fromFps(fps: Int): FrameRate =
            entries.find { it.fps == fps } ?: FPS_60
    }
}

enum class Bitrate(val bps: Int, val label: String) {
    AUTO(0, "Auto (Adaptive)"),
    B_24M(24_000_000, "24 Mbps (Ultra)"),
    B_16M(16_000_000, "16 Mbps (High)"),
    B_12M(12_000_000, "12 Mbps (Standard)"),
    B_8M(8_000_000, "8 Mbps (Eco)"),
    B_4M(4_000_000, "4 Mbps (Low)");

    companion object {
        fun fromBps(bps: Int): Bitrate =
            entries.find { it.bps == bps } ?: B_16M
    }
}

enum class AudioSourceType(val label: String, val description: String) {
    MUTE("Mute", "Record screen without audio"),
    MIC("Microphone", "Record voice and external sounds"),
    INTERNAL("Internal Audio", "Record game & app sounds (Android 10+)"),
    INTERNAL_AND_MIC("Internal + Mic", "Record both app audio and voice commentary");

    companion object {
        fun fromName(name: String): AudioSourceType =
            entries.find { it.name == name } ?: MIC
    }
}

enum class CountdownDuration(val seconds: Int, val label: String) {
    OFF(0, "No Countdown"),
    SEC_3(3, "3 Seconds"),
    SEC_5(5, "5 Seconds"),
    SEC_10(10, "10 Seconds");

    companion object {
        fun fromSeconds(sec: Int): CountdownDuration =
            entries.find { it.seconds == sec } ?: SEC_3
    }
}

enum class RecordingOrientation(val label: String) {
    AUTO("Auto (Follow Screen)"),
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape");

    companion object {
        fun fromName(name: String): RecordingOrientation =
            entries.find { it.name == name } ?: AUTO
    }
}

enum class FacecamSize(val dpSize: Int, val label: String) {
    SMALL(120, "Small (120dp)"),
    MEDIUM(160, "Medium (160dp)"),
    LARGE(200, "Large (200dp)")
}

enum class FacecamShape(val label: String) {
    CIRCLE("Circular"),
    ROUNDED_SQUARE("Rounded Square")
}

enum class RecordingState {
    IDLE,
    COUNTDOWN,
    RECORDING,
    PAUSED,
    STOPPED
}
