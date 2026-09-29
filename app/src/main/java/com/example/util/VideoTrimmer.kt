package com.example.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

object VideoTrimmer {
    private const val TAG = "VideoTrimmer"
    private const val BUFFER_SIZE = 1024 * 1024

    data class TrimResult(
        val success: Boolean,
        val outputPath: String,
        val durationMs: Long,
        val fileSizeBytes: Long,
        val errorMessage: String? = null
    )

    suspend fun trimVideo(
        inputPath: String,
        outputPath: String,
        startMs: Long,
        endMs: Long,
        onProgress: (Float) -> Unit = {}
    ): TrimResult = withContext(Dispatchers.IO) {
        val inputFile = File(inputPath)
        if (!inputFile.exists()) {
            return@withContext TrimResult(false, "", 0, 0, "Source file does not exist")
        }

        val startUs = startMs * 1000L
        val endUs = endMs * 1000L
        val targetDurationUs = endUs - startUs

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            extractor.setDataSource(inputPath)
            val trackCount = extractor.trackCount

            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val indexMap = HashMap<Int, Int>(trackCount)

            var videoTrackIndex = -1
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    val muxerTrackIndex = muxer.addTrack(format)
                    indexMap[i] = muxerTrackIndex
                    if (mime.startsWith("video/")) {
                        videoTrackIndex = i
                    }
                }
            }

            if (indexMap.isEmpty()) {
                return@withContext TrimResult(false, "", 0, 0, "No compatible audio/video tracks found")
            }

            muxer.start()

            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()

            for ((extractorTrackIndex, muxerTrackIndex) in indexMap) {
                extractor.unselectTrack(extractorTrackIndex)
            }

            // Mux each track independently
            for ((extractorTrackIndex, muxerTrackIndex) in indexMap) {
                for (i in 0 until trackCount) {
                    extractor.unselectTrack(i)
                }
                extractor.selectTrack(extractorTrackIndex)
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

                var firstSampleUs: Long = -1

                while (true) {
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) {
                        bufferInfo.size = 0
                        break
                    }

                    val sampleTime = extractor.sampleTime
                    if (sampleTime > endUs) {
                        break
                    }

                    if (sampleTime >= startUs) {
                        if (firstSampleUs < 0) {
                            firstSampleUs = sampleTime
                        }
                        bufferInfo.presentationTimeUs = sampleTime - firstSampleUs
                        bufferInfo.flags = extractor.sampleFlags
                        bufferInfo.offset = 0

                        muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)

                        if (extractorTrackIndex == videoTrackIndex && targetDurationUs > 0) {
                            val progress = ((sampleTime - startUs).toFloat() / targetDurationUs.toFloat()).coerceIn(0f, 1f)
                            onProgress(progress)
                        }
                    }

                    extractor.advance()
                }
            }

            muxer.stop()
            muxer.release()
            extractor.release()

            val outputFile = File(outputPath)
            val trimmedDurationMs = (endMs - startMs).coerceAtLeast(0)

            TrimResult(
                success = true,
                outputPath = outputPath,
                durationMs = trimmedDurationMs,
                fileSizeBytes = outputFile.length()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error trimming video", e)
            try {
                muxer?.release()
                extractor.release()
            } catch (_: Exception) {}

            TrimResult(
                success = false,
                outputPath = "",
                durationMs = 0,
                fileSizeBytes = 0,
                errorMessage = e.localizedMessage ?: "Unknown trim error"
            )
        }
    }

    fun getVideoDurationMs(filePath: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            time?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }
}
