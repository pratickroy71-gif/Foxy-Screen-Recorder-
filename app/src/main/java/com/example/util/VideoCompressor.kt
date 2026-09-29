package com.example.util

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

object VideoCompressor {
    private const val TAG = "VideoCompressor"
    private const val TIMEOUT_USEC = 10000L

    enum class CompressionQuality(
        val title: String,
        val description: String,
        val targetWidth: Int,
        val targetHeight: Int,
        val targetBitrateBps: Int,
        val targetFps: Int,
        val estimatedReductionPercent: Int
    ) {
        COMPACT(
            title = "Compact (480p)",
            description = "Maximum space savings, ideal for messaging & quick sharing",
            targetWidth = 854,
            targetHeight = 480,
            targetBitrateBps = 1_500_000,
            targetFps = 30,
            estimatedReductionPercent = 65
        ),
        BALANCED(
            title = "Balanced (720p HD)",
            description = "Great clarity with significant file size reduction",
            targetWidth = 1280,
            targetHeight = 720,
            targetBitrateBps = 3_000_000,
            targetFps = 30,
            estimatedReductionPercent = 45
        ),
        HIGH(
            title = "High Quality (Reduced Bitrate)",
            description = "Maintains sharp resolution with efficient H.264 compression",
            targetWidth = 1280,
            targetHeight = 720,
            targetBitrateBps = 5_000_000,
            targetFps = 30,
            estimatedReductionPercent = 25
        )
    }

    data class CompressionResult(
        val success: Boolean,
        val outputPath: String,
        val originalSizeBytes: Long,
        val compressedSizeBytes: Long,
        val durationMs: Long,
        val errorMessage: String? = null
    ) {
        val savedBytes: Long get() = (originalSizeBytes - compressedSizeBytes).coerceAtLeast(0L)
        val reductionPercentage: Int
            get() = if (originalSizeBytes > 0) {
                ((savedBytes.toDouble() / originalSizeBytes.toDouble()) * 100).toInt()
            } else 0
    }

    suspend fun compressVideo(
        inputPath: String,
        outputPath: String,
        quality: CompressionQuality,
        onProgress: (Float) -> Unit = {}
    ): CompressionResult = withContext(Dispatchers.IO) {
        val inputFile = File(inputPath)
        if (!inputFile.exists()) {
            return@withContext CompressionResult(
                success = false,
                outputPath = "",
                originalSizeBytes = 0,
                compressedSizeBytes = 0,
                durationMs = 0,
                errorMessage = "Source file does not exist"
            )
        }

        val originalSize = inputFile.length()
        val durationMs = getDurationMs(inputPath)

        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var inputSurfaceWrapper: CodecInputSurface? = null
        var outputSurfaceWrapper: CodecOutputSurface? = null

        try {
            extractor = MediaExtractor().apply { setDataSource(inputPath) }
            val trackCount = extractor.trackCount

            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var inputVideoFormat: MediaFormat? = null
            var inputAudioFormat: MediaFormat? = null

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") && videoTrackIndex == -1) {
                    videoTrackIndex = i
                    inputVideoFormat = format
                } else if (mime.startsWith("audio/") && audioTrackIndex == -1) {
                    audioTrackIndex = i
                    inputAudioFormat = format
                }
            }

            if (videoTrackIndex == -1 || inputVideoFormat == null) {
                return@withContext CompressionResult(
                    success = false,
                    outputPath = "",
                    originalSizeBytes = originalSize,
                    compressedSizeBytes = 0,
                    durationMs = durationMs,
                    errorMessage = "No video track found in input file"
                )
            }

            val origWidth = inputVideoFormat.getInteger(MediaFormat.KEY_WIDTH)
            val origHeight = inputVideoFormat.getInteger(MediaFormat.KEY_HEIGHT)
            val isPortrait = origHeight > origWidth

            val (outWidth, outHeight) = calculateTargetDimensions(
                origWidth = origWidth,
                origHeight = origHeight,
                targetLong = if (isPortrait) quality.targetHeight else quality.targetWidth,
                targetShort = if (isPortrait) quality.targetWidth else quality.targetHeight
            )

            // Setup Encoder
            val outputVideoFormat = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC,
                outWidth,
                outHeight
            ).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, quality.targetBitrateBps)
                setInteger(MediaFormat.KEY_FRAME_RATE, quality.targetFps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
                configure(outputVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                inputSurfaceWrapper = CodecInputSurface(createInputSurface())
                inputSurfaceWrapper?.makeCurrent()
                start()
            }

            // Setup Decoder with OutputSurface connected to EGL
            outputSurfaceWrapper = CodecOutputSurface()
            val videoMime = inputVideoFormat.getString(MediaFormat.KEY_MIME) ?: MediaFormat.MIMETYPE_VIDEO_AVC
            decoder = MediaCodec.createDecoderByType(videoMime).apply {
                configure(inputVideoFormat, outputSurfaceWrapper?.surface, null, 0)
                start()
            }

            // Setup Muxer
            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerVideoTrack = -1
            var muxerAudioTrack = -1

            if (inputAudioFormat != null) {
                muxerAudioTrack = muxer.addTrack(inputAudioFormat)
            }

            var muxerStarted = false
            extractor.selectTrack(videoTrackIndex)

            // Video transcode loop
            val decoderBufferInfo = MediaCodec.BufferInfo()
            val encoderBufferInfo = MediaCodec.BufferInfo()
            var extractorDone = false
            var decoderDone = false
            var encoderDone = false

            val totalDurationUs = durationMs * 1000L

            while (!encoderDone) {
                // 1. Feed Extractor to Decoder
                if (!extractorDone) {
                    val inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC)
                    if (inputBufIndex >= 0) {
                        val inputBuf = decoder.getInputBuffer(inputBufIndex)
                        if (inputBuf != null) {
                            val sampleSize = extractor.readSampleData(inputBuf, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(
                                    inputBufIndex, 0, 0, 0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                extractorDone = true
                            } else {
                                val sampleTime = extractor.sampleTime
                                decoder.queueInputBuffer(inputBufIndex, 0, sampleSize, sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                // 2. Dequeue from Decoder and render to Encoder surface
                if (!decoderDone) {
                    val decoderStatus = decoder.dequeueOutputBuffer(decoderBufferInfo, TIMEOUT_USEC)
                    if (decoderStatus >= 0) {
                        val doRender = (decoderBufferInfo.size != 0)
                        decoder.releaseOutputBuffer(decoderStatus, doRender)
                        if (doRender) {
                            outputSurfaceWrapper?.awaitNewImage()
                            outputSurfaceWrapper?.drawImage()
                            inputSurfaceWrapper?.setPresentationTime(decoderBufferInfo.presentationTimeUs * 1000L)
                            inputSurfaceWrapper?.swapBuffers()

                            if (totalDurationUs > 0) {
                                val progress = (decoderBufferInfo.presentationTimeUs.toFloat() / totalDurationUs.toFloat())
                                    .coerceIn(0f, 0.9f)
                                onProgress(progress)
                            }
                        }
                        if ((decoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            decoderDone = true
                            encoder.signalEndOfInputStream()
                        }
                    }
                }

                // 3. Dequeue from Encoder and write to Muxer
                var encoderStatus = encoder.dequeueOutputBuffer(encoderBufferInfo, TIMEOUT_USEC)
                while (encoderStatus >= 0) {
                    val encodedData = encoder.getOutputBuffer(encoderStatus)
                    if (encodedData != null) {
                        if ((encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            encoderBufferInfo.size = 0
                        }

                        if (encoderBufferInfo.size != 0) {
                            if (!muxerStarted) {
                                val newFormat = encoder.outputFormat
                                muxerVideoTrack = muxer.addTrack(newFormat)
                                muxer.start()
                                muxerStarted = true
                            }
                            encodedData.position(encoderBufferInfo.offset)
                            encodedData.limit(encoderBufferInfo.offset + encoderBufferInfo.size)
                            muxer.writeSampleData(muxerVideoTrack, encodedData, encoderBufferInfo)
                        }

                        encoder.releaseOutputBuffer(encoderStatus, false)

                        if ((encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            encoderDone = true
                            break
                        }
                    }
                    encoderStatus = encoder.dequeueOutputBuffer(encoderBufferInfo, 0)
                }

                if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && !muxerStarted) {
                    val newFormat = encoder.outputFormat
                    muxerVideoTrack = muxer.addTrack(newFormat)
                    muxer.start()
                    muxerStarted = true
                }
            }

            // Copy audio track samples directly (lossless, instant audio transfer)
            if (audioTrackIndex != -1 && muxerAudioTrack != -1) {
                if (!muxerStarted) {
                    muxer.start()
                    muxerStarted = true
                }
                copyAudioTrack(extractor, audioTrackIndex, muxer, muxerAudioTrack)
            }

            onProgress(1.0f)

            // Safe release
            try { decoder.stop() } catch (_: Exception) {}
            try { encoder.stop() } catch (_: Exception) {}
            try { muxer.stop() } catch (_: Exception) {}

            val outputFile = File(outputPath)
            val compressedSize = if (outputFile.exists()) outputFile.length() else 0L

            CompressionResult(
                success = compressedSize > 0,
                outputPath = outputPath,
                originalSizeBytes = originalSize,
                compressedSizeBytes = compressedSize,
                durationMs = durationMs
            )
        } catch (e: Exception) {
            Log.e(TAG, "Video compression failed", e)
            CompressionResult(
                success = false,
                outputPath = "",
                originalSizeBytes = originalSize,
                compressedSizeBytes = 0,
                durationMs = durationMs,
                errorMessage = e.localizedMessage ?: "Compression failed"
            )
        } finally {
            try { decoder?.release() } catch (_: Exception) {}
            try { encoder?.release() } catch (_: Exception) {}
            try { inputSurfaceWrapper?.release() } catch (_: Exception) {}
            try { outputSurfaceWrapper?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }
    }

    private fun copyAudioTrack(
        extractor: MediaExtractor,
        audioTrackIndex: Int,
        muxer: MediaMuxer,
        muxerAudioTrack: Int
    ) {
        extractor.unselectTrack(0)
        extractor.selectTrack(audioTrackIndex)
        extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        val buffer = ByteBuffer.allocate(256 * 1024)
        val bufferInfo = MediaCodec.BufferInfo()

        while (true) {
            bufferInfo.size = extractor.readSampleData(buffer, 0)
            if (bufferInfo.size < 0) {
                break
            }
            bufferInfo.presentationTimeUs = extractor.sampleTime
            bufferInfo.flags = extractor.sampleFlags
            bufferInfo.offset = 0

            muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
            extractor.advance()
        }
    }

    private fun calculateTargetDimensions(
        origWidth: Int,
        origHeight: Int,
        targetLong: Int,
        targetShort: Int
    ): Pair<Int, Int> {
        val isPortrait = origHeight > origWidth
        var targetW = if (isPortrait) targetShort else targetLong
        var targetH = if (isPortrait) targetLong else targetShort

        // Keep original dimensions if source is already smaller than target
        if (origWidth < targetW && origHeight < targetH) {
            targetW = origWidth
            targetH = origHeight
        }

        // Must be even for H.264
        if (targetW % 2 != 0) targetW -= 1
        if (targetH % 2 != 0) targetH -= 1

        return Pair(targetW, targetH)
    }

    fun getDurationMs(filePath: String): Long {
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

    // Helper EGL context for Encoder's input surface
    private class CodecInputSurface(private val surface: Surface) {
        private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

        init {
            eglSetup()
        }

        private fun eglSetup() {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val version = IntArray(2)
            EGL14.eglInitialize(eglDisplay, version, 0, version, 1)

            val attribList = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGLExt.EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE
            )

            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0)

            val attribContext = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )
            eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, attribContext, 0)

            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surface, surfaceAttribs, 0)
        }

        fun makeCurrent() {
            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        }

        fun swapBuffers(): Boolean {
            return EGL14.eglSwapBuffers(eglDisplay, eglSurface)
        }

        fun setPresentationTime(nsecs: Long) {
            EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nsecs)
        }

        fun release() {
            if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                EGL14.eglReleaseThread()
                EGL14.eglTerminate(eglDisplay)
            }
            surface.release()
            eglDisplay = EGL14.EGL_NO_DISPLAY
            eglContext = EGL14.EGL_NO_CONTEXT
            eglSurface = EGL14.EGL_NO_SURFACE
        }
    }

    // Helper Surface + Texture render for Decoder output
    private class CodecOutputSurface : SurfaceTexture.OnFrameAvailableListener {
        private var surfaceTexture: SurfaceTexture? = null
        var surface: Surface? = null
            private set

        private val frameSyncObject = Object()
        private var frameAvailable = false
        private var textureRender: TextureRender? = null

        init {
            setup()
        }

        private fun setup() {
            textureRender = TextureRender()
            textureRender?.surfaceCreated()

            surfaceTexture = SurfaceTexture(textureRender!!.textureId).apply {
                setOnFrameAvailableListener(this@CodecOutputSurface)
            }
            surface = Surface(surfaceTexture)
        }

        fun awaitNewImage() {
            synchronized(frameSyncObject) {
                while (!frameAvailable) {
                    try {
                        frameSyncObject.wait(500)
                        if (!frameAvailable) {
                            break
                        }
                    } catch (_: InterruptedException) {
                        break
                    }
                }
                frameAvailable = false
            }
            surfaceTexture?.updateTexImage()
        }

        fun drawImage() {
            textureRender?.drawFrame(surfaceTexture!!)
        }

        override fun onFrameAvailable(st: SurfaceTexture?) {
            synchronized(frameSyncObject) {
                frameAvailable = true
                frameSyncObject.notifyAll()
            }
        }

        fun release() {
            surface?.release()
            surface = null
            surfaceTexture = null
        }
    }

    // Minimal GLES Quad Renderer for copying SurfaceTexture to EGL surface
    private class TextureRender {
        var textureId = -12345
            private set
        private var program = 0
        private var uMVPMatrixHandle = 0
        private var uSTMatrixHandle = 0
        private var aPositionHandle = 0
        private var aTextureHandle = 0

        private val triangleVerticesData = floatArrayOf(
            -1.0f, -1.0f, 0f, 0f, 0f,
             1.0f, -1.0f, 0f, 1f, 0f,
            -1.0f,  1.0f, 0f, 0f, 1f,
             1.0f,  1.0f, 0f, 1f, 1f
        )
        private val triangleVertices: FloatBuffer = ByteBuffer.allocateDirect(
            triangleVerticesData.size * 4
        ).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(triangleVerticesData)
            position(0)
        }

        private val mvpMatrix = FloatArray(16).apply { android.opengl.Matrix.setIdentityM(this, 0) }
        private val stMatrix = FloatArray(16)

        fun drawFrame(st: SurfaceTexture) {
            st.getTransformMatrix(stMatrix)

            GLES20.glUseProgram(program)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

            triangleVertices.position(0)
            GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 20, triangleVertices)
            GLES20.glEnableVertexAttribArray(aPositionHandle)

            triangleVertices.position(3)
            GLES20.glVertexAttribPointer(aTextureHandle, 2, GLES20.GL_FLOAT, false, 20, triangleVertices)
            GLES20.glEnableVertexAttribArray(aTextureHandle)

            GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)
            GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, stMatrix, 0)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        }

        fun surfaceCreated() {
            val vertexShader = """
                uniform mat4 uMVPMatrix;
                uniform mat4 uSTMatrix;
                attribute vec4 aPosition;
                attribute vec4 aTextureCoord;
                varying vec2 vTextureCoord;
                void main() {
                    gl_Position = uMVPMatrix * aPosition;
                    vTextureCoord = (uSTMatrix * aTextureCoord).xy;
                }
            """.trimIndent()

            val fragmentShader = """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;
                varying vec2 vTextureCoord;
                uniform samplerExternalOES sTexture;
                void main() {
                    gl_FragColor = texture2D(sTexture, vTextureCoord);
                }
            """.trimIndent()

            program = createProgram(vertexShader, fragmentShader)
            aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
            aTextureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")
            uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
            uSTMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix")

            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            textureId = textures[0]
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        }

        private fun createProgram(vertexSource: String, fragmentSource: String): Int {
            val vShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
            val fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
            val p = GLES20.glCreateProgram()
            GLES20.glAttachShader(p, vShader)
            GLES20.glAttachShader(p, fShader)
            GLES20.glLinkProgram(p)
            return p
        }

        private fun loadShader(shaderType: Int, source: String): Int {
            val shader = GLES20.glCreateShader(shaderType)
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            return shader
        }
    }
}
