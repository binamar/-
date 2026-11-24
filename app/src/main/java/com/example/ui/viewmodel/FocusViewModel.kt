package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.FocusSession
import com.example.domain.model.PlannerTask
import com.example.domain.repository.FocusRepository
import com.example.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FocusUiState(
    val linkedTask: PlannerTask? = null,
    val availableTasks: List<PlannerTask> = emptyList(),
    val targetMinutes: Int = 25,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val isBreakMode: Boolean = false,
    val sessionLogs: List<FocusSession> = emptyList(),
    val taskCompletedFeedback: Boolean = false
) {
    val totalSeconds: Int get() = (targetMinutes * 60).coerceAtLeast(60)
    val progress: Float
        get() = 1f - (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
}

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val focusRepository: FocusRepository
) : ViewModel() {

    private val _selectedTaskId = MutableStateFlow<Long?>(null)
    private val _targetMinutes = MutableStateFlow(25)
    private val _remainingSeconds = MutableStateFlow(25 * 60)
    private val _isRunning = MutableStateFlow(false)
    private val _isBreakMode = MutableStateFlow(false)
    private val _taskCompletedFeedback = MutableStateFlow(false)

    private var sessionStartedAtMillis: Long = System.currentTimeMillis()
    private var timerJob: Job? = null

    private data class TimerMeta(
        val selectedTaskId: Long?,
        val targetMinutes: Int,
        val remainingSeconds: Int,
        val isRunning: Boolean,
        val isBreakMode: Boolean,
        val completedFeedback: Boolean
    )

    private val timerMetaFlow = combine<Any?, TimerMeta>(
        _selectedTaskId,
        _targetMinutes,
        _remainingSeconds,
        _isRunning,
        _isBreakMode,
        _taskCompletedFeedback
    ) { values ->
        TimerMeta(
            selectedTaskId = values[0] as Long?,
            targetMinutes = values[1] as Int,
            remainingSeconds = values[2] as Int,
            isRunning = values[3] as Boolean,
            isBreakMode = values[4] as Boolean,
            completedFeedback = values[5] as Boolean
        )
    }

    val uiState: StateFlow<FocusUiState> = combine(
        timerMetaFlow,
        taskRepository.observeAllTasks(),
        focusRepository.observeAllSessions()
    ) { meta, allTasks, allSessions ->
        val linked = meta.selectedTaskId?.let { id -> allTasks.firstOrNull { it.id == id } }
        val activeTasks = allTasks.filter { !it.isCompleted }
        val logs = if (linked != null) {
            allSessions.filter { it.taskId == linked.id }
        } else {
            allSessions.take(20)
        }
        FocusUiState(
            linkedTask = linked,
            availableTasks = activeTasks,
            targetMinutes = meta.targetMinutes,
            remainingSeconds = meta.remainingSeconds,
            isRunning = meta.isRunning,
            isBreakMode = meta.isBreakMode,
            sessionLogs = logs,
            taskCompletedFeedback = meta.completedFeedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FocusUiState()
    )

    fun linkTask(taskId: Long?) {
        _selectedTaskId.value = taskId
        _taskCompletedFeedback.value = false
    }

    fun selectPreset(minutes: Int, isBreak: Boolean = false) {
        pauseTimer()
        _isBreakMode.value = isBreak
        _targetMinutes.value = minutes.coerceIn(1, 120)
        _remainingSeconds.value = _targetMinutes.value * 60
        _taskCompletedFeedback.value = false
    }

    fun startOrPause() {
        if (_isRunning.value) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    fun resetTimer() {
        pauseTimer()
        _remainingSeconds.value = _targetMinutes.value * 60
    }

    fun startFlexibleBreak(minutes: Int = 5) {
        selectPreset(minutes, isBreak = true)
        startTimer()
    }

    fun extendFiveMinutes() {
        _targetMinutes.value = (_targetMinutes.value + 5).coerceAtMost(180)
        _remainingSeconds.value += 5 * 60
    }

    fun completeLinkedTaskFromFocus() {
        val current = uiState.value
        val task = current.linkedTask ?: return
        viewModelScope.launch {
            val elapsedMinutes = ((current.totalSeconds - current.remainingSeconds) / 60).coerceAtLeast(1)
            if (!current.isBreakMode && current.remainingSeconds < current.totalSeconds) {
                focusRepository.recordSession(
                    FocusSession(
                        taskId = task.id,
                        taskTitleSnapshot = task.title,
                        durationMinutes = elapsedMinutes,
                        isBreak = false,
                        startedAt = sessionStartedAtMillis,
                        completedAt = System.currentTimeMillis()
                    )
                )
            }
            taskRepository.toggleTaskCompletion(task.id, true)
            pauseTimer()
            _taskCompletedFeedback.value = true
        }
    }

    private fun startTimer() {
        if (_remainingSeconds.value <= 0) {
            _remainingSeconds.value = _targetMinutes.value * 60
        }
        sessionStartedAtMillis = System.currentTimeMillis()
        _isRunning.value = true
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isRunning.value && _remainingSeconds.value > 0) {
                delay(1000L)
                _remainingSeconds.value = (_remainingSeconds.value - 1).coerceAtLeast(0)
            }
            if (_remainingSeconds.value == 0 && _isRunning.value) {
                _isRunning.value = false
                recordCompletedTimer()
            }
        }
    }

    private fun pauseTimer() {
        _isRunning.value = false
        timerJob?.cancel()
    }

    private suspend fun recordCompletedTimer() {
        val current = uiState.value
        focusRepository.recordSession(
            FocusSession(
                taskId = current.linkedTask?.id,
                taskTitleSnapshot = current.linkedTask?.title ?: if (current.isBreakMode) "Break" else "Free Focus",
                durationMinutes = current.targetMinutes,
                isBreak = current.isBreakMode,
                startedAt = sessionStartedAtMillis,
                completedAt = System.currentTimeMillis()
            )
        )
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
