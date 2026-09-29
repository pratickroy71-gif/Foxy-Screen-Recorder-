package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object ShareHelper {

    fun shareVideo(context: Context, filePath: String, title: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Recorded with Foxy Screen Recorder: $title")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Recording"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing video: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWithExternalPlayer(context: Context, filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val playIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(playIntent, "Play with"))
        } catch (e: Exception) {
            Toast.makeText(context, "No app available to play this video", Toast.LENGTH_SHORT).show()
        }
    }

    fun renameFile(oldPath: String, newNameWithoutExt: String): String? {
        val oldFile = File(oldPath)
        if (!oldFile.exists()) return null

        val extension = oldFile.extension.ifEmpty { "mp4" }
        val sanitized = newNameWithoutExt.replace(Regex("[^a-zA-Z0-9._ -]"), "_")
        val newFile = File(oldFile.parentFile, "$sanitized.$extension")

        return if (oldFile.renameTo(newFile)) {
            newFile.absolutePath
        } else {
            null
        }
    }

    fun deleteFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) file.delete() else true
        } catch (_: Exception) {
            false
        }
    }
}
