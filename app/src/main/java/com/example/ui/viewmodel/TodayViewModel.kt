package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.PlannerTask
import com.example.domain.model.Priority
import com.example.domain.model.RoutineWithStats
import com.example.domain.model.Subtask
import com.example.domain.model.TaskTemplate
import com.example.domain.model.TimeBlock
import com.example.domain.parser.NaturalLanguageTaskParser
import com.example.domain.parser.ParsedTaskInput
import com.example.domain.repository.RoutineRepository
import com.example.domain.repository.TaskRepository
import com.example.domain.usecase.CalculateRoutineStatsUseCase
import com.example.scheduler.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class TodayUiState(
    val today: LocalDate = LocalDate.now(),
    val todayTasks: List<PlannerTask> = emptyList(),
    val overdueTasks: List<PlannerTask> = emptyList(),
    val todayRoutines: List<RoutineWithStats> = emptyList(),
    val templates: List<TaskTemplate> = emptyList(),
    val quickInput: String = "",
    val parsedPreview: ParsedTaskInput? = null
) {
    val activeTasks: List<PlannerTask> get() = todayTasks.filter { !it.isCompleted }
    val completedTasks: List<PlannerTask> get() = todayTasks.filter { it.isCompleted }
    val completionRatio: Float
        get() {
            val total = todayTasks.size + todayRoutines.size
            if (total == 0) return 0f
            val done = completedTasks.size + todayRoutines.count { it.isCompletedToday }
            return done.toFloat() / total.toFloat()
        }
}

sealed interface TodayEvent {
    data class ShowUndoComplete(val taskId: Long, val taskTitle: String) : TodayEvent
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val routineRepository: RoutineRepository,
    private val routineStatsUseCase: CalculateRoutineStatsUseCase,
    private val nlpParser: NaturalLanguageTaskParser,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val todayDate = LocalDate.now()
    private val _quickInput = MutableStateFlow("")
    private val _events = MutableSharedFlow<TodayEvent>()
    val events: SharedFlow<TodayEvent> = _events.asSharedFlow()

    private val _expandedTaskIds = MutableStateFlow<Set<Long>>(emptySet())
    val expandedTaskIds: StateFlow<Set<Long>> = _expandedTaskIds.asStateFlow()

    val uiState: StateFlow<TodayUiState> = combine(
        taskRepository.observeTasksForDate(todayDate),
        taskRepository.observeOverdueIncompleteTasks(todayDate),
        routineRepository.observeAllRoutines(),
        routineRepository.observeAllCompletionDates(),
        taskRepository.observeTemplates(),
        _quickInput
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val tasks = values[0] as List<PlannerTask>
        @Suppress("UNCHECKED_CAST")
        val overdue = values[1] as List<PlannerTask>
        @Suppress("UNCHECKED_CAST")
        val routines = values[2] as List<com.example.domain.model.Routine>
        @Suppress("UNCHECKED_CAST")
        val logs = values[3] as Map<Long, Set<LocalDate>>
        @Suppress("UNCHECKED_CAST")
        val templates = values[4] as List<TaskTemplate>
        val rawInput = values[5] as String

        val todayDow = todayDate.dayOfWeek.value
        val activeRoutines = routines
            .filter { !it.isPaused && it.activeDays.contains(todayDow) }
            .map { routineStatsUseCase.calculate(it, logs[it.id].orEmpty(), todayDate) }

        val preview = if (rawInput.isNotBlank()) nlpParser.parse(rawInput, todayDate) else null

        TodayUiState(
            today = todayDate,
            todayTasks = tasks,
            overdueTasks = overdue,
            todayRoutines = activeRoutines,
            templates = templates,
            quickInput = rawInput,
            parsedPreview = preview
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayUiState(today = todayDate)
    )

    fun onQuickInputChange(text: String) {
        _quickInput.value = text
    }

    fun submitQuickAdd() {
        val raw = _quickInput.value.trim()
        if (raw.isBlank()) return
        val parsed = nlpParser.parse(raw, todayDate)
        viewModelScope.launch {
            val newTask = PlannerTask(
                title = parsed.cleanTitle,
                priority = parsed.priority,
                dueDate = parsed.dueDate ?: todayDate,
                dueTime = parsed.dueTime,
                timeBlock = parsed.timeBlock,
                tags = parsed.tags,
                reminderMinutesBefore = if (parsed.dueTime != null) 10 else null
            )
            val id = taskRepository.upsertTask(newTask)
            if (newTask.dueTime != null) {
                reminderScheduler.scheduleTaskReminder(newTask.copy(id = id))
            }
            _quickInput.value = ""
        }
    }

    fun createTaskFromTemplate(template: TaskTemplate, isArabic: Boolean) {
        viewModelScope.launch {
            val title = if (isArabic && template.defaultTitleAr.isNotBlank()) {
                template.defaultTitleAr
            } else {
                template.defaultTitle
            }
            val subTitles = if (isArabic && template.defaultSubtasksAr.isNotEmpty()) {
                template.defaultSubtasksAr
            } else {
                template.defaultSubtasks
            }
            val task = PlannerTask(
                title = title,
                priority = template.priority,
                dueDate = todayDate,
                timeBlock = template.timeBlock,
                tags = template.tags,
                subtasks = subTitles.mapIndexed { idx, s -> Subtask(title = s, sortOrder = idx) }
            )
            val id = taskRepository.upsertTask(task)
            _expandedTaskIds.value = _expandedTaskIds.value + id
        }
    }

    fun saveTaskAsTemplate(task: PlannerTask) {
        viewModelScope.launch {
            taskRepository.saveTemplate(
                TaskTemplate(
                    name = task.title,
                    nameAr = task.title,
                    defaultTitle = task.title,
                    defaultTitleAr = task.title,
                    priority = task.priority,
                    timeBlock = task.timeBlock,
                    tags = task.tags,
                    defaultSubtasks = task.subtasks.map { it.title },
                    defaultSubtasksAr = task.subtasks.map { it.title },
                    isBuiltIn = false
                )
            )
        }
    }

    fun saveTask(
        existingTask: PlannerTask?,
        title: String,
        notes: String,
        priority: Priority,
        dueDate: LocalDate?,
        dueTime: LocalTime?,
        timeBlock: TimeBlock,
        tags: List<String>,
        reminderMinutesBefore: Int?
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val task = (existingTask ?: PlannerTask(title = title)).copy(
                title = title.trim(),
                notes = notes.trim(),
                priority = priority,
                dueDate = dueDate,
                dueTime = dueTime,
                timeBlock = timeBlock,
                tags = tags,
                reminderMinutesBefore = reminderMinutesBefore
            )
            val id = taskRepository.upsertTask(task)
            val saved = task.copy(id = id)
            if (saved.dueDate != null && saved.dueTime != null && saved.reminderMinutesBefore != null) {
                reminderScheduler.scheduleTaskReminder(saved)
            } else {
                reminderScheduler.cancelTaskReminder(saved.id)
            }
        }
    }

    fun toggleTaskCompletion(task: PlannerTask) {
        viewModelScope.launch {
            val newCompleted = !task.isCompleted
            taskRepository.toggleTaskCompletion(task.id, newCompleted)
            if (newCompleted) {
                reminderScheduler.cancelTaskReminder(task.id)
                _events.emit(TodayEvent.ShowUndoComplete(task.id, task.title))
            } else if (task.dueTime != null && task.reminderMinutesBefore != null) {
                reminderScheduler.scheduleTaskReminder(task.copy(isCompleted = false))
            }
        }
    }

    fun undoCompletion(taskId: Long) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(taskId, false)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            reminderScheduler.cancelTaskReminder(taskId)
            taskRepository.deleteTask(taskId)
        }
    }

    fun rolloverAllToToday() {
        val ids = uiState.value.overdueTasks.map { it.id }
        if (ids.isEmpty()) return
        viewModelScope.launch {
            taskRepository.rolloverTasks(ids, todayDate)
        }
    }

    fun rolloverSelectedToToday(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            taskRepository.rolloverTasks(ids, todayDate)
        }
    }

    fun toggleExpanded(taskId: Long) {
        val current = _expandedTaskIds.value
        _expandedTaskIds.value = if (current.contains(taskId)) current - taskId else current + taskId
    }

    fun addSubtask(taskId: Long, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            taskRepository.addSubtask(taskId, title)
        }
    }

    fun toggleSubtask(subtask: Subtask) {
        viewModelScope.launch {
            taskRepository.toggleSubtask(subtask.id, !subtask.isCompleted)
        }
    }

    fun deleteSubtask(subtaskId: Long) {
        viewModelScope.launch {
            taskRepository.deleteSubtask(subtaskId)
        }
    }

    fun reorderSubtasks(taskId: Long, reordered: List<Subtask>) {
        viewModelScope.launch {
            taskRepository.reorderSubtasks(taskId, reordered)
        }
    }

    fun toggleRoutineToday(routineId: Long) {
        viewModelScope.launch {
            routineRepository.toggleRoutineCompletion(routineId, todayDate)
        }
    }
}
