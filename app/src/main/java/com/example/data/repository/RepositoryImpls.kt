package com.example.data.repository

import com.example.data.extractor.EgyDeadScraper
import com.example.data.extractor.QualityInspector
import com.example.data.local.DownloadDao
import com.example.data.local.DownloadEntity
import com.example.data.local.HistoryDao
import com.example.data.local.ResolvedHistoryEntity
import com.example.data.local.SettingsDataStore
import com.example.domain.model.DownloadStatus
import com.example.domain.model.DownloadTask
import com.example.domain.model.ExtractedVideo
import com.example.domain.model.QualityPreferencePolicy
import com.example.domain.model.ResolutionBadge
import com.example.domain.model.ResolvedHistoryItem
import com.example.domain.model.ScrapeInspectionReport
import com.example.domain.model.ScraperSettings
import com.example.domain.model.ServerHost
import com.example.domain.repository.DownloadRepository
import com.example.domain.repository.HistoryRepository
import com.example.domain.repository.ScraperRepository
import com.example.domain.repository.SettingsRepository
import com.example.worker.DownloadOrchestrator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScraperRepositoryImpl @Inject constructor(
    private val scraper: EgyDeadScraper,
    private val qualityInspector: QualityInspector,
    private val settingsRepository: SettingsRepository
) : ScraperRepository {

    override suspend fun inspectAndResolve(
        inputUrl: String,
        policy: QualityPreferencePolicy
    ): ScrapeInspectionReport {
        val ua = settingsRepository.settings.first().customUserAgent
        return scraper.inspectAndResolve(inputUrl = inputUrl, policy = policy, userAgent = ua)
    }

    override suspend fun runOfflineLabInspection(
        policy: QualityPreferencePolicy
    ): ScrapeInspectionReport {
        return scraper.runOfflineLabInspection(policy)
    }

    override suspend fun resolveFreshForDownload(
        sourcePageUrl: String,
        preferredResolutionLabel: String,
        policy: QualityPreferencePolicy
    ): ExtractedVideo? {
        val ua = settingsRepository.settings.first().customUserAgent
        val report = scraper.inspectAndResolve(sourcePageUrl, policy, ua)
        val exactMatch = report.resolvedVideos.firstOrNull {
            it.quality.resolution.label.equals(preferredResolutionLabel, ignoreCase = true)
        }
        return exactMatch ?: qualityInspector.selectBestByPolicy(report.resolvedVideos, policy)
    }
}

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val downloadDao: DownloadDao,
    private val orchestrator: DownloadOrchestrator
) : DownloadRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override val allTasks: Flow<List<DownloadTask>> = downloadDao.observeAll().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun enqueueVideo(video: ExtractedVideo, delaySeconds: Int): Long {
        val now = System.currentTimeMillis()
        val ext = if (video.host == ServerHost.DIRECT_HLS) "ts" else "mp4"
        val cleanTitle = video.title.replace(Regex("""[\\/:*?"<>|]"""), "_").take(50).ifBlank { "video" }
        val fileName = "${cleanTitle}_${video.quality.resolution.label}.$ext"

        val entity = DownloadEntity(
            title = video.title,
            sourcePageUrl = video.sourcePageUrl,
            directUrl = video.directUrl,
            hostName = video.host.name,
            resolutionLabel = video.quality.resolution.label,
            is360p = video.quality.resolution == ResolutionBadge.P360,
            expectedSizeBytes = video.quality.contentLengthBytes ?: 0L,
            downloadedBytes = 0L,
            speedBytesPerSec = 0L,
            status = DownloadStatus.QUEUED.name,
            headersJson = json.encodeToString(video.headers),
            subtitleUrl = video.subtitleUrl,
            localUri = null,
            fileName = fileName,
            supportsRange = video.quality.supportsRange,
            errorMessage = null,
            retryCount = 0,
            delaySecondsBeforeStart = delaySeconds,
            createdAt = now,
            updatedAt = now
        )
        val id = downloadDao.insert(entity)
        orchestrator.dispatchNextFromQueue()
        return id
    }

    override suspend fun pauseTask(taskId: Long) {
        downloadDao.updateStatus(taskId, DownloadStatus.PAUSED.name, null, System.currentTimeMillis())
        orchestrator.cancelWorker(taskId)
        orchestrator.dispatchNextFromQueue()
    }

    override suspend fun resumeTask(taskId: Long) {
        downloadDao.updateStatus(taskId, DownloadStatus.QUEUED.name, null, System.currentTimeMillis())
        orchestrator.dispatchNextFromQueue()
    }

    override suspend fun retryTask(taskId: Long) {
        downloadDao.resetForRetry(taskId, System.currentTimeMillis())
        orchestrator.dispatchNextFromQueue()
    }

    override suspend fun cancelTask(taskId: Long) {
        downloadDao.updateStatus(taskId, DownloadStatus.CANCELLED.name, null, System.currentTimeMillis())
        orchestrator.cancelWorker(taskId)
        orchestrator.dispatchNextFromQueue()
    }

    override suspend fun pauseAllActive() {
        val pending = downloadDao.getPendingOrActive()
        downloadDao.pauseAllActive(System.currentTimeMillis())
        pending.forEach { orchestrator.cancelWorker(it.id) }
    }

    override suspend fun resumeAllPaused() {
        downloadDao.resumeAllPaused(System.currentTimeMillis())
        orchestrator.dispatchNextFromQueue()
    }

    override suspend fun clearCompletedAndCancelled() {
        downloadDao.clearFinished()
    }

    override suspend fun deleteTask(taskId: Long) {
        orchestrator.cancelWorker(taskId)
        downloadDao.deleteById(taskId)
    }

    private fun DownloadEntity.toDomain(): DownloadTask {
        val parsedHeaders = runCatching {
            json.decodeFromString<Map<String, String>>(headersJson)
        }.getOrDefault(emptyMap())
        val parsedHost = ServerHost.entries.firstOrNull { it.name == hostName } ?: ServerHost.UNKNOWN
        val parsedStatus = DownloadStatus.entries.firstOrNull { it.name == status } ?: DownloadStatus.QUEUED

        return DownloadTask(
            id = id,
            title = title,
            sourcePageUrl = sourcePageUrl,
            directUrl = directUrl,
            host = parsedHost,
            resolutionLabel = resolutionLabel,
            is360p = is360p,
            expectedSizeBytes = expectedSizeBytes,
            downloadedBytes = downloadedBytes,
            speedBytesPerSec = speedBytesPerSec,
            status = parsedStatus,
            headers = parsedHeaders,
            subtitleUrl = subtitleUrl,
            localUri = localUri,
            fileName = fileName,
            supportsRange = supportsRange,
            errorMessage = errorMessage,
            retryCount = retryCount,
            delaySecondsBeforeStart = delaySecondsBeforeStart,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao
) : HistoryRepository {

    override val historyItems: Flow<List<ResolvedHistoryItem>> =
        historyDao.observeHistory().map { list ->
            list.map {
                ResolvedHistoryItem(
                    id = it.id,
                    inputUrl = it.inputUrl,
                    pageTitle = it.pageTitle,
                    serverCount = it.serverCount,
                    has360p = it.has360p,
                    bestQualityLabel = it.bestQualityLabel,
                    hostSummary = it.hostSummary,
                    createdAt = it.createdAt
                )
            }
        }

    override suspend fun saveHistory(report: ScrapeInspectionReport) {
        val hostSummary = report.resolvedVideos
            .map { it.host.displayName }
            .distinct()
            .joinToString(" • ")
            .ifBlank { "بدون روابط" }

        historyDao.insert(
            ResolvedHistoryEntity(
                inputUrl = report.inputUrl,
                pageTitle = report.pageTitle,
                serverCount = report.resolvedVideos.size,
                has360p = report.resolvedVideos.any { it.is360pPreferred },
                bestQualityLabel = report.recommendedVideo?.quality?.resolution?.label ?: "غير محدد",
                hostSummary = hostSummary,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteHistoryItem(id: Long) = historyDao.deleteById(id)

    override suspend fun clearHistory() = historyDao.clearAll()
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore
) : SettingsRepository {
    override val settings: Flow<ScraperSettings> = dataStore.settings
    override suspend fun updateQualityPolicy(policy: QualityPreferencePolicy) = dataStore.updateQualityPolicy(policy)
    override suspend fun updateMaxConcurrent(count: Int) = dataStore.updateMaxConcurrent(count)
    override suspend fun updateInterDownloadDelay(seconds: Int) = dataStore.updateInterDelay(seconds)
    override suspend fun updateAutoRetryCount(count: Int) = dataStore.updateAutoRetry(count)
    override suspend fun updatePreferSubtitles(enabled: Boolean) = dataStore.updatePreferSubtitles(enabled)
    override suspend fun updateUserAgent(userAgent: String) = dataStore.updateUserAgent(userAgent)
    override suspend fun updateDarkTheme(dark: Boolean) = dataStore.updateDarkTheme(dark)
}
