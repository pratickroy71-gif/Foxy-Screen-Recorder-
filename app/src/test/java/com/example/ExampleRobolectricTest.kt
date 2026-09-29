package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AudioSourceType
import com.example.model.Bitrate
import com.example.model.CountdownDuration
import com.example.model.FrameRate
import com.example.model.RecorderConfig
import com.example.model.VideoResolution
import com.example.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Foxy Screen Recorder", appName)
  }

  @Test
  fun `verify time formatting`() {
    assertEquals("00:00", TimeUtils.formatDuration(0L))
    assertEquals("00:45", TimeUtils.formatDuration(45000L))
    assertEquals("01:30", TimeUtils.formatDuration(90000L))
    assertEquals("01:05:20", TimeUtils.formatDuration(3920000L))
  }

  @Test
  fun `verify video compression quality presets`() {
    val compact = com.example.util.VideoCompressor.CompressionQuality.COMPACT
    assertEquals(854, compact.targetWidth)
    assertEquals(480, compact.targetHeight)
    assertEquals(1_500_000, compact.targetBitrateBps)
    assertEquals(65, compact.estimatedReductionPercent)

    val balanced = com.example.util.VideoCompressor.CompressionQuality.BALANCED
    assertEquals(1280, balanced.targetWidth)
    assertEquals(720, balanced.targetHeight)
    assertEquals(3_000_000, balanced.targetBitrateBps)

    val testResult = com.example.util.VideoCompressor.CompressionResult(
      success = true,
      outputPath = "/dummy/path.mp4",
      originalSizeBytes = 100_000_000L,
      compressedSizeBytes = 35_000_000L,
      durationMs = 60000L
    )
    assertEquals(65_000_000L, testResult.savedBytes)
    assertEquals(65, testResult.reductionPercentage)
  }
}

