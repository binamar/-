package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.CalendarViewMode
import com.example.domain.model.PlannerTask
import com.example.domain.model.Routine
import com.example.domain.model.TimeBlock
import com.example.domain.repository.RoutineRepository
import com.example.domain.repository.TaskRepository
import com.example.scheduler.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val viewMode: CalendarViewMode = CalendarViewMode.WEEK,
    val tasksForSelectedDate: List<PlannerTask> = emptyList(),
    val routinesForSelectedDate: List<Routine> = emptyList(),
    val unscheduledTasks: List<PlannerTask> = emptyList(),
    val workloadByDate: Map<LocalDate, Int> = emptyMap()
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val routineRepository: RoutineRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _viewMode = MutableStateFlow(CalendarViewMode.WEEK)

    val uiState: StateFlow<CalendarUiState> = combine(
        _selectedDate,
        _viewMode,
        taskRepository.observeAllTasks(),
        taskRepository.observeUnscheduledTasks(),
        routineRepository.observeAllRoutines()
    ) { selectedDate, viewMode, allTasks, unscheduled, routines ->
        val dayTasks = allTasks.filter { it.dueDate == selectedDate }
        val dayRoutines = routines.filter { !it.isPaused && it.activeDays.contains(selectedDate.dayOfWeek.value) }

        // Build workload density across a +/- 35 day range around selectedDate
        val workload = mutableMapOf<LocalDate, Int>()
        allTasks.filter { !it.isCompleted && it.dueDate != null }.forEach { task ->
            val d = task.dueDate!!
            workload[d] = (workload[d] ?: 0) + 1
        }
        val start = selectedDate.minusDays(35)
        val end = selectedDate.plusDays(35)
        var cursor = start
        while (!cursor.isAfter(end)) {
            val dow = cursor.dayOfWeek.value
            val routineLoad = routines.count { !it.isPaused && it.activeDays.contains(dow) }
            if (routineLoad > 0) {
                workload[cursor] = (workload[cursor] ?: 0) + routineLoad
            }
            cursor = cursor.plusDays(1)
        }

        CalendarUiState(
            selectedDate = selectedDate,
            viewMode = viewMode,
            tasksForSelectedDate = dayTasks,
            routinesForSelectedDate = dayRoutines,
            unscheduledTasks = unscheduled,
            workloadByDate = workload
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun switchViewMode(mode: CalendarViewMode) {
        // Preserves _selectedDate automatically
        _viewMode.value = mode
    }

    fun quickReschedule(
        taskId: Long,
        targetDate: LocalDate?,
        timeBlock: TimeBlock,
        dueTime: LocalTime?
    ) {
        viewModelScope.launch {
            taskRepository.rescheduleTask(taskId, targetDate, timeBlock, dueTime)
            taskRepository.getTaskById(taskId)?.let { updated ->
                if (updated.dueDate != null && updated.dueTime != null) {
                    reminderScheduler.scheduleTaskReminder(updated)
                }
            }
        }
    }

    fun toggleTaskCompletion(task: PlannerTask) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(task.id, !task.isCompleted)
        }
    }
}
