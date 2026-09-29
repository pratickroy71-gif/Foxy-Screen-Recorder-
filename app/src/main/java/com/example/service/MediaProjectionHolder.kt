package com.example.service

import android.content.Intent
import android.media.projection.MediaProjection

object MediaProjectionHolder {
    var resultCode: Int = 0
    var resultData: Intent? = null
    var mediaProjection: MediaProjection? = null

    fun set(code: Int, data: Intent?) {
        resultCode = code
        resultData = data
    }

    fun clear() {
        resultCode = 0
        resultData = null
        try {
            mediaProjection?.stop()
        } catch (_: Exception) {}
        mediaProjection = null
    }

    val hasConsent: Boolean
        get() = resultCode != 0 && resultData != null
}
