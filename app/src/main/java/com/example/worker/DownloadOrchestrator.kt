package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.local.DownloadDao
import com.example.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val settingsRepository: SettingsRepository
) {
    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    suspend fun dispatchNextFromQueue() {
        val settings = settingsRepository.settings.first()
        val activeCount = downloadDao.countActive()
        val availableSlots = (settings.maxConcurrentDownloads - activeCount).coerceAtLeast(0)
        if (availableSlots == 0) return

        val nextTasks = downloadDao.getNextQueued(availableSlots)
        nextTasks.forEachIndexed { index, entity ->
            val delaySec = if (entity.delaySecondsBeforeStart > 0) {
                entity.delaySecondsBeforeStart.toLong()
            } else if (index > 0 || activeCount > 0) {
                settings.interDownloadDelaySec.toLong()
            } else {
                0L
            }
            scheduleWorker(entity.id, delaySec)
        }
    }

    fun scheduleWorker(taskId: Long, delaySeconds: Long = 0L) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val requestBuilder = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(DownloadWorker.KEY_TASK_ID to taskId))
            .addTag(workTag(taskId))

        if (delaySeconds > 0L) {
            requestBuilder.setInitialDelay(delaySeconds, TimeUnit.SECONDS)
        }

        workManager.enqueueUniqueWork(
            uniqueWorkName(taskId),
            ExistingWorkPolicy.KEEP,
            requestBuilder.build()
        )
    }

    fun cancelWorker(taskId: Long) {
        workManager.cancelUniqueWork(uniqueWorkName(taskId))
    }

    companion object {
        fun uniqueWorkName(taskId: Long) = "streamgrab_dl_$taskId"
        fun workTag(taskId: Long) = "streamgrab_tag_$taskId"
    }
}
