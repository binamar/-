package com.example.worker

import android.content.ContentValues
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.data.local.DownloadDao
import com.example.data.local.DownloadEntity
import com.example.domain.model.DownloadStatus
import com.example.domain.model.ServerHost
import com.example.domain.repository.ScraperRepository
import com.example.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val downloadDao: DownloadDao,
    private val scraperRepository: ScraperRepository,
    private val settingsRepository: SettingsRepository,
    private val okHttpClient: OkHttpClient,
    private val orchestrator: DownloadOrchestrator,
    private val notificationHelper: DownloadNotificationHelper
) : CoroutineWorker(appContext, params) {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        if (taskId <= 0L) return Result.failure()

        var task = downloadDao.getById(taskId) ?: return Result.failure()
        if (task.status == DownloadStatus.CANCELLED.name || task.status == DownloadStatus.PAUSED.name) {
            return Result.success()
        }

        val settings = settingsRepository.settings.first()

        return try {
            // Optional rate-limit delay before opening connection
            if (task.delaySecondsBeforeStart > 0) {
                setForeground(
                    createForegroundInfo(
                        taskId = taskId,
                        title = task.title,
                        subtitle = "انتظار ${task.delaySecondsBeforeStart} ث لتقليل الحظر...",
                        progressPercent = 0,
                        indeterminate = true
                    )
                )
                delay(task.delaySecondsBeforeStart * 1000L)
            }

            // Re-check pause/cancel after delay
            task = downloadDao.getById(taskId) ?: return Result.success()
            if (task.status == DownloadStatus.PAUSED.name || task.status == DownloadStatus.CANCELLED.name) {
                return Result.success()
            }

            // Just-In-Time (JIT) Re-Extraction for expiring Doodstream / MixDrop tokens
            val needsJitResolve = task.directUrl.isBlank() ||
                (task.hostName in listOf(ServerHost.DOODSTREAM.name, ServerHost.MIXDROP.name) &&
                    (task.retryCount > 0 || (System.currentTimeMillis() - task.updatedAt) > JIT_STALE_THRESHOLD_MS))

            if (needsJitResolve && task.sourcePageUrl.startsWith("http")) {
                downloadDao.updateStatus(taskId, DownloadStatus.RESOLVING.name, null, System.currentTimeMillis())
                setForeground(
                    createForegroundInfo(
                        taskId = taskId,
                        title = task.title,
                        subtitle = "استخراج رابط جديد (JIT) قبل التحميل...",
                        progressPercent = 0,
                        indeterminate = true
                    )
                )

                val freshVideo = scraperRepository.resolveFreshForDownload(
                    sourcePageUrl = task.sourcePageUrl,
                    preferredResolutionLabel = task.resolutionLabel,
                    policy = settings.qualityPolicy
                )
                if (freshVideo != null && freshVideo.directUrl.isNotBlank()) {
                    val freshHeadersJson = json.encodeToString(freshVideo.headers)
                    downloadDao.updateDirectUrl(
                        id = taskId,
                        directUrl = freshVideo.directUrl,
                        headersJson = freshHeadersJson,
                        updatedAt = System.currentTimeMillis()
                    )
                    task = downloadDao.getById(taskId) ?: task
                }
            }

            downloadDao.updateStatus(taskId, DownloadStatus.DOWNLOADING.name, null, System.currentTimeMillis())
            val savedUri = executeResumableDownload(task)

            downloadDao.markCompleted(
                id = taskId,
                localUri = savedUri,
                downloadedBytes = downloadDao.getById(taskId)?.downloadedBytes ?: task.downloadedBytes,
                updatedAt = System.currentTimeMillis()
            )
            notificationHelper.showCompletedNotification(
                taskId = taskId,
                title = "اكتمل التحميل: ${task.title}",
                isError = false,
                detail = "${task.resolutionLabel} • ${task.fileName}"
            )
            orchestrator.dispatchNextFromQueue()
            Result.success()
        } catch (_: PausedOrCancelledException) {
            orchestrator.dispatchNextFromQueue()
            Result.success()
        } catch (e: Exception) {
            val latest = downloadDao.getById(taskId)
            if (latest != null && latest.retryCount < settings.autoRetryCount) {
                downloadDao.resetForRetry(taskId, System.currentTimeMillis())
                orchestrator.scheduleWorker(taskId, delaySeconds = (settings.interDownloadDelaySec + 3).toLong())
                Result.failure()
            } else {
                downloadDao.updateStatus(
                    id = taskId,
                    status = DownloadStatus.FAILED.name,
                    errorMessage = e.message ?: "فشل التحميل",
                    updatedAt = System.currentTimeMillis()
                )
                notificationHelper.showCompletedNotification(
                    taskId = taskId,
                    title = "تعذر التحميل: ${task.title}",
                    isError = true,
                    detail = e.message ?: "خطأ في الشبكة أو الرابط"
                )
                orchestrator.dispatchNextFromQueue()
                Result.failure()
            }
        }
    }

    private suspend fun executeResumableDownload(task: DownloadEntity): String = withContext(Dispatchers.IO) {
        val partialDir = File(applicationContext.filesDir, "partial_downloads").apply { mkdirs() }
        val safeName = sanitizeFileName(task.fileName)
        val partialFile = File(partialDir, "${task.id}_$safeName")
        val existingBytes = if (partialFile.exists()) partialFile.length() else 0L

        val headersMap: MutableMap<String, String> = runCatching {
            json.decodeFromString<Map<String, String>>(task.headersJson)
        }.getOrDefault(emptyMap()).toMutableMap()

        if (existingBytes > 0L) {
            headersMap["Range"] = "bytes=$existingBytes-"
        }

        val request = Request.Builder()
            .url(task.directUrl)
            .headers(headersMap.toHeaders())
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 206) {
                throw IOException("HTTP ${response.code} أثناء التحميل")
            }

            val isPartial = response.code == 206
            val body = response.body ?: throw IOException("استجابة فارغة من الخادم")
            val contentLen = body.contentLength()
            val totalExpected = when {
                isPartial && contentLen > 0L -> existingBytes + contentLen
                contentLen > 0L -> contentLen
                else -> task.expectedSizeBytes
            }
            val startOffset = if (isPartial) existingBytes else 0L
            val supportsRange = isPartial ||
                response.header("Accept-Ranges")?.contains("bytes", ignoreCase = true) == true

            body.byteStream().use { input ->
                FileOutputStream(partialFile, isPartial).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var totalRead = startOffset
                    var bytesSinceTick = 0L
                    var lastTickMs = System.currentTimeMillis()

                    while (true) {
                        if (isStopped) throw PausedOrCancelledException()
                        val currentDbState = downloadDao.getById(task.id)?.status
                        if (currentDbState == DownloadStatus.PAUSED.name ||
                            currentDbState == DownloadStatus.CANCELLED.name
                        ) {
                            throw PausedOrCancelledException()
                        }

                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        totalRead += read
                        bytesSinceTick += read

                        val now = System.currentTimeMillis()
                        val elapsedMs = now - lastTickMs
                        if (elapsedMs >= 350L) {
                            val speed = (bytesSinceTick * 1000L) / elapsedMs.coerceAtLeast(1L)
                            downloadDao.updateProgress(
                                id = task.id,
                                downloaded = totalRead,
                                expectedSize = totalExpected,
                                speed = speed,
                                supportsRange = supportsRange,
                                updatedAt = now
                            )
                            val pct = if (totalExpected > 0L) {
                                ((totalRead * 100L) / totalExpected).toInt()
                            } else 0
                            setForeground(
                                createForegroundInfo(
                                    taskId = task.id,
                                    title = task.title,
                                    subtitle = "${task.resolutionLabel} • $pct% (${formatSpeed(speed)})",
                                    progressPercent = pct,
                                    indeterminate = totalExpected <= 0L
                                )
                            )
                            bytesSinceTick = 0L
                            lastTickMs = now
                        }
                    }
                    output.flush()
                    downloadDao.updateProgress(
                        id = task.id,
                        downloaded = totalRead,
                        expectedSize = totalRead,
                        speed = 0L,
                        supportsRange = supportsRange,
                        updatedAt = System.currentTimeMillis()
                    )
                }
            }
        }

        // Export completed file to MediaStore.Downloads (API 29+) or app Downloads dir
        val exportedUri = exportToDownloads(partialFile, safeName, "video/mp4")
        partialFile.delete()

        // Also fetch MixDrop subtitle if available and enabled
        val settings = settingsRepository.settings.first()
        if (settings.preferMixDropSubtitles && !task.subtitleUrl.isNullOrBlank()) {
            runCatching {
                val subReq = Request.Builder().url(task.subtitleUrl).get().build()
                okHttpClient.newCall(subReq).execute().use { subResp ->
                    if (subResp.isSuccessful) {
                        val subBytes = subResp.body?.bytes()
                        if (subBytes != null && subBytes.isNotEmpty()) {
                            val subName = safeName.substringBeforeLast(".") + ".srt"
                            val tempSub = File(partialDir, "${task.id}_$subName")
                            tempSub.writeBytes(subBytes)
                            exportToDownloads(tempSub, subName, "application/x-subrip")
                            tempSub.delete()
                        }
                    }
                }
            }
        }

        exportedUri
    }

    private fun exportToDownloads(sourceFile: File, displayName: String, mimeType: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = applicationContext.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/StreamGrab")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    sourceFile.inputStream().use { inp -> inp.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                return uri.toString()
            }
        }
        val fallbackDir = File(
            applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: applicationContext.filesDir,
            "StreamGrab"
        ).apply { mkdirs() }
        val target = File(fallbackDir, displayName)
        sourceFile.copyTo(target, overwrite = true)
        return target.absolutePath
    }

    private fun createForegroundInfo(
        taskId: Long,
        title: String,
        subtitle: String,
        progressPercent: Int,
        indeterminate: Boolean
    ): ForegroundInfo {
        val notification = notificationHelper.buildProgressNotification(
            title = title,
            subtitle = subtitle,
            progressPercent = progressPercent,
            indeterminate = indeterminate
        )
        val notificationId = (1000 + (taskId % 1000)).toInt()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun sanitizeFileName(raw: String): String {
        val cleaned = raw.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
        return cleaned.ifBlank { "streamgrab_video.mp4" }
    }

    private fun formatSpeed(bytesPerSec: Long): String = when {
        bytesPerSec >= 1024L * 1024L -> String.format("%.1f MB/s", bytesPerSec / (1024f * 1024f))
        bytesPerSec >= 1024L -> String.format("%.0f KB/s", bytesPerSec / 1024f)
        else -> "$bytesPerSec B/s"
    }

    private class PausedOrCancelledException : Exception()

    companion object {
        const val KEY_TASK_ID = "task_id"
        private const val JIT_STALE_THRESHOLD_MS = 120_000L
    }
}
