package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_tasks")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val sourcePageUrl: String,
    val directUrl: String,
    val hostName: String,
    val resolutionLabel: String,
    val is360p: Boolean,
    val expectedSizeBytes: Long,
    val downloadedBytes: Long,
    val speedBytesPerSec: Long,
    val status: String,
    val headersJson: String,
    val subtitleUrl: String?,
    val localUri: String?,
    val fileName: String,
    val supportsRange: Boolean,
    val errorMessage: String?,
    val retryCount: Int,
    val delaySecondsBeforeStart: Int,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "resolved_history")
data class ResolvedHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val inputUrl: String,
    val pageTitle: String,
    val serverCount: Int,
    val has360p: Boolean,
    val bestQualityLabel: String,
    val hostSummary: String,
    val createdAt: Long
)
