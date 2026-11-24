package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.DownloadStatus
import com.example.domain.model.DownloadTask
import com.example.domain.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QueueUiSummary(
    val tasks: List<DownloadTask> = emptyList(),
    val activeCount: Int = 0,
    val queuedCount: Int = 0,
    val completedCount: Int = 0,
    val failedCount: Int = 0
)

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val queueState: StateFlow<QueueUiSummary> = downloadRepository.allTasks
        .map { list ->
            QueueUiSummary(
                tasks = list,
                activeCount = list.count {
                    it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.RESOLVING
                },
                queuedCount = list.count { it.status == DownloadStatus.QUEUED },
                completedCount = list.count { it.status == DownloadStatus.COMPLETED },
                failedCount = list.count { it.status == DownloadStatus.FAILED }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = QueueUiSummary()
        )

    fun pause(taskId: Long) = viewModelScope.launch { downloadRepository.pauseTask(taskId) }
    fun resume(taskId: Long) = viewModelScope.launch { downloadRepository.resumeTask(taskId) }
    fun retry(taskId: Long) = viewModelScope.launch { downloadRepository.retryTask(taskId) }
    fun cancel(taskId: Long) = viewModelScope.launch { downloadRepository.cancelTask(taskId) }
    fun delete(taskId: Long) = viewModelScope.launch { downloadRepository.deleteTask(taskId) }
    fun pauseAll() = viewModelScope.launch { downloadRepository.pauseAllActive() }
    fun resumeAll() = viewModelScope.launch { downloadRepository.resumeAllPaused() }
    fun clearFinished() = viewModelScope.launch { downloadRepository.clearCompletedAndCancelled() }
}
