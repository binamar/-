package com.example.domain.repository

import com.example.domain.model.DownloadTask
import com.example.domain.model.ExtractedVideo
import com.example.domain.model.QualityPreferencePolicy
import com.example.domain.model.ResolvedHistoryItem
import com.example.domain.model.ScrapeInspectionReport
import com.example.domain.model.ScraperSettings
import kotlinx.coroutines.flow.Flow

interface ScraperRepository {
    suspend fun inspectAndResolve(
        inputUrl: String,
        policy: QualityPreferencePolicy
    ): ScrapeInspectionReport

    suspend fun runOfflineLabInspection(
        policy: QualityPreferencePolicy
    ): ScrapeInspectionReport

    suspend fun resolveFreshForDownload(
        sourcePageUrl: String,
        preferredResolutionLabel: String,
        policy: QualityPreferencePolicy
    ): ExtractedVideo?
}

interface DownloadRepository {
    val allTasks: Flow<List<DownloadTask>>
    suspend fun enqueueVideo(video: ExtractedVideo, delaySeconds: Int = 0): Long
    suspend fun pauseTask(taskId: Long)
    suspend fun resumeTask(taskId: Long)
    suspend fun retryTask(taskId: Long)
    suspend fun cancelTask(taskId: Long)
    suspend fun pauseAllActive()
    suspend fun resumeAllPaused()
    suspend fun clearCompletedAndCancelled()
    suspend fun deleteTask(taskId: Long)
}

interface HistoryRepository {
    val historyItems: Flow<List<ResolvedHistoryItem>>
    suspend fun saveHistory(report: ScrapeInspectionReport)
    suspend fun deleteHistoryItem(id: Long)
    suspend fun clearHistory()
}

interface SettingsRepository {
    val settings: Flow<ScraperSettings>
    suspend fun updateQualityPolicy(policy: QualityPreferencePolicy)
    suspend fun updateMaxConcurrent(count: Int)
    suspend fun updateInterDownloadDelay(seconds: Int)
    suspend fun updateAutoRetryCount(count: Int)
    suspend fun updatePreferSubtitles(enabled: Boolean)
    suspend fun updateUserAgent(userAgent: String)
    suspend fun updateDarkTheme(dark: Boolean)
}
