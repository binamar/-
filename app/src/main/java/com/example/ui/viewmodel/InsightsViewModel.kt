package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.FocusSession
import com.example.domain.model.InsightPeriod
import com.example.domain.model.InsightSummary
import com.example.domain.model.InsightTypeFilter
import com.example.domain.model.PlannerTask
import com.example.domain.model.Routine
import com.example.domain.model.TimeOfDayBucket
import com.example.domain.repository.FocusRepository
import com.example.domain.repository.RoutineRepository
import com.example.domain.repository.TaskRepository
import com.example.domain.usecase.BuildInsightsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class InsightsUiState(
    val period: InsightPeriod = InsightPeriod.WEEKLY,
    val typeFilter: InsightTypeFilter = InsightTypeFilter.ALL,
    val selectedTag: String? = null,
    val summary: InsightSummary = InsightSummary(
        completionRate = 0f,
        previousPeriodRate = 0f,
        deltaPercent = 0,
        completedCount = 0,
        totalCount = 0,
        focusMinutesTotal = 0,
        bestActiveStreak = 0,
        dailyPoints = emptyList(),
        timeOfDayDistribution = TimeOfDayBucket.entries.associateWith { 0 },
        repeatedlyPostponedTasks = emptyList(),
        availableTags = emptyList()
    )
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    routineRepository: RoutineRepository,
    focusRepository: FocusRepository,
    private val buildInsightsUseCase: BuildInsightsUseCase
) : ViewModel() {

    private val _period = MutableStateFlow(InsightPeriod.WEEKLY)
    private val _typeFilter = MutableStateFlow(InsightTypeFilter.ALL)
    private val _selectedTag = MutableStateFlow<String?>(null)

    private val filterFlow = combine(_period, _typeFilter, _selectedTag) { p, t, tag ->
        Triple(p, t, tag)
    }

    val uiState: StateFlow<InsightsUiState> = combine(
        filterFlow,
        taskRepository.observeAllTasks(),
        routineRepository.observeAllRoutines(),
        routineRepository.observeAllCompletionDates(),
        focusRepository.observeAllSessions()
    ) { (period, typeFilter, tag), tasks, routines, logs, sessions ->
        @Suppress("UNCHECKED_CAST")
        val summary = buildInsightsUseCase.build(
            tasks = tasks as List<PlannerTask>,
            routines = routines as List<Routine>,
            routineLogs = logs as Map<Long, Set<LocalDate>>,
            focusSessions = sessions as List<FocusSession>,
            period = period,
            typeFilter = typeFilter,
            selectedTag = tag,
            today = LocalDate.now()
        )
        InsightsUiState(
            period = period,
            typeFilter = typeFilter,
            selectedTag = tag,
            summary = summary
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsUiState()
    )

    fun setPeriod(period: InsightPeriod) {
        _period.value = period
    }

    fun setTypeFilter(filter: InsightTypeFilter) {
        _typeFilter.value = filter
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
    }

    fun addBreakdownSubtask(taskId: Long, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            taskRepository.addSubtask(taskId, title)
        }
    }
}
