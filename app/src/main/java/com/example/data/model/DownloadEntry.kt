package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntry(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileName: String,
    val url: String,
    val mimeType: String? = null,
    val fileSizeBytes: Long = 0,
    val localFilePath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
