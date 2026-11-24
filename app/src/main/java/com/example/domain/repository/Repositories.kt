package com.example.domain.repository

import com.example.domain.model.FocusSession
import com.example.domain.model.PlannerTask
import com.example.domain.model.Routine
import com.example.domain.model.Subtask
import com.example.domain.model.TaskTemplate
import com.example.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskRepository {
    fun observeAllTasks(): Flow<List<PlannerTask>>
    fun observeTasksForDate(date: LocalDate): Flow<List<PlannerTask>>
    fun observeOverdueIncompleteTasks(today: LocalDate): Flow<List<PlannerTask>>
    fun observeUnscheduledTasks(): Flow<List<PlannerTask>>
    suspend fun getTaskById(id: Long): PlannerTask?
    suspend fun upsertTask(task: PlannerTask): Long
    suspend fun toggleTaskCompletion(taskId: Long, completed: Boolean)
    suspend fun deleteTask(taskId: Long)
    suspend fun rolloverTasks(taskIds: List<Long>, targetDate: LocalDate)
    suspend fun rescheduleTask(taskId: Long, targetDate: LocalDate?, timeBlock: com.example.domain.model.TimeBlock, dueTime: java.time.LocalTime?)
    suspend fun addSubtask(taskId: Long, title: String)
    suspend fun toggleSubtask(subtaskId: Long, completed: Boolean)
    suspend fun deleteSubtask(subtaskId: Long)
    suspend fun reorderSubtasks(taskId: Long, orderedSubtasks: List<Subtask>)

    fun observeTemplates(): Flow<List<TaskTemplate>>
    suspend fun saveTemplate(template: TaskTemplate): Long
    suspend fun deleteTemplate(id: Long)
}

interface RoutineRepository {
    fun observeAllRoutines(): Flow<List<Routine>>
    fun observeAllCompletionDates(): Flow<Map<Long, Set<LocalDate>>>
    suspend fun getRoutineById(id: Long): Routine?
    suspend fun upsertRoutine(routine: Routine): Long
    suspend fun setRoutinePaused(routineId: Long, paused: Boolean, pausedUntil: LocalDate? = null)
    suspend fun deleteRoutine(routineId: Long)
    suspend fun toggleRoutineCompletion(routineId: Long, date: LocalDate)
}

interface FocusRepository {
    fun observeAllSessions(): Flow<List<FocusSession>>
    fun observeSessionsForTask(taskId: Long): Flow<List<FocusSession>>
    suspend fun recordSession(session: FocusSession): Long
}

interface SettingsRepository {
    val preferencesFlow: Flow<UserPreferences>
    suspend fun updateThemeMode(mode: com.example.domain.model.ThemeMode)
    suspend fun updateDarkContrast(contrast: com.example.domain.model.DarkContrastMode)
    suspend fun updateFontScale(scale: Float)
    suspend fun updateMinimalistMode(enabled: Boolean)
    suspend fun updateLanguage(language: com.example.domain.model.AppLanguage)
    suspend fun updateGroupedRoutineReminders(enabled: Boolean)
    suspend fun updateSmartSnoozeMinutes(minutes: Int)
}
