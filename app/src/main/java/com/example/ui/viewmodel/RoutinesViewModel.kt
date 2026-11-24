package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Routine
import com.example.domain.model.RoutineCategory
import com.example.domain.model.RoutineDayVariation
import com.example.domain.model.RoutineWithStats
import com.example.domain.repository.RoutineRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.usecase.CalculateRoutineStatsUseCase
import com.example.scheduler.NotificationHelper
import com.example.scheduler.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class RoutinesUiState(
    val routines: List<RoutineWithStats> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val groupedRemindersEnabled: Boolean = true
)

@HiltViewModel
class RoutinesViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val settingsRepository: SettingsRepository,
    private val statsUseCase: CalculateRoutineStatsUseCase,
    private val reminderScheduler: ReminderScheduler,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val today = LocalDate.now()

    val uiState: StateFlow<RoutinesUiState> = combine(
        routineRepository.observeAllRoutines(),
        routineRepository.observeAllCompletionDates(),
        settingsRepository.preferencesFlow
    ) { routines, logs, prefs ->
        val statsList = routines.map { r ->
            statsUseCase.calculate(r, logs[r.id].orEmpty(), today)
        }
        RoutinesUiState(
            routines = statsList,
            today = today,
            groupedRemindersEnabled = prefs.groupedRoutineReminders
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RoutinesUiState(today = today)
    )

    fun saveRoutine(
        existing: Routine?,
        title: String,
        description: String,
        category: RoutineCategory,
        activeDays: Set<Int>,
        reminderTime: LocalTime?,
        defaultDurationMinutes: Int,
        variations: Map<Int, RoutineDayVariation>
    ) {
        if (title.isBlank() || activeDays.isEmpty()) return
        viewModelScope.launch {
            val routine = (existing ?: Routine(title = title)).copy(
                title = title.trim(),
                description = description.trim(),
                category = category,
                activeDays = activeDays,
                reminderTime = reminderTime,
                defaultDurationMinutes = defaultDurationMinutes.coerceIn(1, 240),
                variations = variations
            )
            routineRepository.upsertRoutine(routine)
            syncReminders()
        }
    }

    fun toggleCompletionToday(routineId: Long) {
        viewModelScope.launch {
            routineRepository.toggleRoutineCompletion(routineId, today)
        }
    }

    fun togglePause(routine: Routine) {
        viewModelScope.launch {
            routineRepository.setRoutinePaused(
                routineId = routine.id,
                paused = !routine.isPaused,
                pausedUntil = null
            )
            syncReminders()
        }
    }

    fun deleteRoutine(routineId: Long) {
        viewModelScope.launch {
            routineRepository.deleteRoutine(routineId)
            syncReminders()
        }
    }

    fun triggerGroupedNotificationPreview() {
        viewModelScope.launch {
            val prefs = settingsRepository.preferencesFlow.first()
            val active = uiState.value.routines
                .map { it.routine }
                .filter { !it.isPaused }
            if (active.isNotEmpty()) {
                notificationHelper.showGroupedRoutineReminder(active, prefs.smartSnoozeMinutes)
            }
        }
    }

    private suspend fun syncReminders() {
        val prefs = settingsRepository.preferencesFlow.first()
        if (prefs.groupedRoutineReminders) {
            val all = routineRepository.observeAllRoutines().first()
            reminderScheduler.scheduleGroupedRoutineCheck(all, prefs.reminderGroupWindowMinutes)
        }
    }
}
