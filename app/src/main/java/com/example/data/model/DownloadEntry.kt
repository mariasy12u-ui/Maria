package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val url: String,
    val fileUri: String? = null,
    val sizeBytes: Long = 0,
    val mimeType: String = "application/octet-stream",
    val status: String = "Completed", // "Downloading", "Completed", "Failed"
    val timestamp: Long = System.currentTimeMillis()
)
