package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val resolution: String,
    val fps: Int,
    val bitrate: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val audioSource: String = "MIC"
)
