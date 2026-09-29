package com.example.data

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.util.Locale

object StorageHelper {

    data class StorageStats(
        val totalBytes: Long,
        val freeBytes: Long,
        val appRecordingsBytes: Long,
        val totalRecordingsCount: Int
    ) {
        val usedPercentage: Float
            get() = if (totalBytes > 0) ((totalBytes - freeBytes).toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

        val appPercentage: Float
            get() = if (totalBytes > 0) (appRecordingsBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
    }

    fun getDeviceStorageStats(context: Context, appRecordingBytes: Long, count: Int): StorageStats {
        return try {
            val path = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize

            StorageStats(
                totalBytes = totalBytes,
                freeBytes = freeBytes,
                appRecordingsBytes = appRecordingBytes,
                totalRecordingsCount = count
            )
        } catch (_: Exception) {
            StorageStats(
                totalBytes = 64L * 1024 * 1024 * 1024,
                freeBytes = 32L * 1024 * 1024 * 1024,
                appRecordingsBytes = appRecordingBytes,
                totalRecordingsCount = count
            )
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            else -> String.format(Locale.US, "%.1f KB", kb)
        }
    }

    fun getRecordingDirectory(context: Context): File {
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val dir = File(moviesDir, "FoxyRecordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
}
