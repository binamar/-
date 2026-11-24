package com.example.data.repository

import com.example.data.local.FocusDao
import com.example.data.local.FocusSessionEntity
import com.example.data.local.RoomTypeConverters
import com.example.data.local.RoutineDao
import com.example.data.local.RoutineEntity
import com.example.data.local.RoutineLogEntity
import com.example.data.local.SubtaskEntity
import com.example.data.local.TaskDao
import com.example.data.local.TaskEntity
import com.example.data.local.TaskTemplateEntity
import com.example.data.local.TaskWithSubtasks
import com.example.data.local.TemplateDao
import com.example.data.local.UserPreferencesDataStore
import com.example.domain.model.AppLanguage
import com.example.domain.model.DarkContrastMode
import com.example.domain.model.FocusSession
import com.example.domain.model.PlannerTask
import com.example.domain.model.Priority
import com.example.domain.model.Routine
import com.example.domain.model.Subtask
import com.example.domain.model.TaskTemplate
import com.example.domain.model.ThemeMode
import com.example.domain.model.TimeBlock
import com.example.domain.model.UserPreferences
import com.example.domain.repository.FocusRepository
import com.example.domain.repository.RoutineRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

private fun TaskWithSubtasks.toDomain(): PlannerTask = PlannerTask(
    id = task.id,
    title = task.title,
    notes = task.notes,
    priority = task.priority,
    dueDate = task.dueDateEpochDay?.let { LocalDate.ofEpochDay(it) },
    dueTime = task.dueTimeMinuteOfDay?.let { LocalTime.of(it / 60, it % 60) },
    timeBlock = task.timeBlock,
    tags = RoomTypeConverters.decodeStringList(task.tagsJson),
    isCompleted = task.isCompleted,
    completedAt = task.completedAt,
    postponeCount = task.postponeCount,
    reminderMinutesBefore = task.reminderMinutesBefore,
    createdAt = task.createdAt,
    subtasks = subtasks.sortedBy { it.sortOrder }.map {
        Subtask(
            id = it.id,
            taskId = it.taskId,
            title = it.title,
            isCompleted = it.isCompleted,
            sortOrder = it.sortOrder
        )
    }
)

private fun PlannerTask.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    notes = notes,
    priority = priority,
    dueDateEpochDay = dueDate?.toEpochDay(),
    dueTimeMinuteOfDay = dueTime?.let { it.hour * 60 + it.minute },
    timeBlock = timeBlock,
    tagsJson = RoomTypeConverters.encodeStringList(tags),
    isCompleted = isCompleted,
    completedAt = completedAt,
    postponeCount = postponeCount,
    reminderMinutesBefore = reminderMinutesBefore,
    createdAt = createdAt
)

private fun TaskTemplateEntity.toDomain(): TaskTemplate = TaskTemplate(
    id = id,
    name = name,
    nameAr = nameAr,
    defaultTitle = defaultTitle,
    defaultTitleAr = defaultTitleAr,
    priority = priority,
    timeBlock = timeBlock,
    tags = RoomTypeConverters.decodeStringList(tagsJson),
    defaultSubtasks = RoomTypeConverters.decodeStringList(defaultSubtasksJson),
    defaultSubtasksAr = RoomTypeConverters.decodeStringList(defaultSubtasksArJson),
    isBuiltIn = isBuiltIn
)

private fun RoutineEntity.toDomain(): Routine = Routine(
    id = id,
    title = title,
    description = description,
    category = category,
    activeDays = RoomTypeConverters.decodeIntSet(activeDaysJson),
    reminderTime = reminderMinuteOfDay?.let { LocalTime.of(it / 60, it % 60) },
    isPaused = isPaused,
    pausedUntil = pausedUntilEpochDay?.let { LocalDate.ofEpochDay(it) },
    defaultDurationMinutes = defaultDurationMinutes,
    variations = RoomTypeConverters.decodeVariations(variationsJson),
    createdAt = createdAt
)

private fun Routine.toEntity(): RoutineEntity = RoutineEntity(
    id = id,
    title = title,
    description = description,
    category = category,
    activeDaysJson = RoomTypeConverters.encodeIntSet(activeDays),
    reminderMinuteOfDay = reminderTime?.let { it.hour * 60 + it.minute },
    isPaused = isPaused,
    pausedUntilEpochDay = pausedUntil?.toEpochDay(),
    defaultDurationMinutes = defaultDurationMinutes,
    variationsJson = RoomTypeConverters.encodeVariations(variations),
    createdAt = createdAt
)

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val templateDao: TemplateDao
) : TaskRepository {

    override fun observeAllTasks(): Flow<List<PlannerTask>> =
        taskDao.observeAllTasks().map { list -> list.map { it.toDomain() } }

    override fun observeTasksForDate(date: LocalDate): Flow<List<PlannerTask>> =
        taskDao.observeTasksForDate(date.toEpochDay()).map { list -> list.map { it.toDomain() } }

    override fun observeOverdueIncompleteTasks(today: LocalDate): Flow<List<PlannerTask>> =
        taskDao.observeOverdueIncompleteTasks(today.toEpochDay()).map { list -> list.map { it.toDomain() } }

    override fun observeUnscheduledTasks(): Flow<List<PlannerTask>> =
        taskDao.observeUnscheduledTasks().map { list -> list.map { it.toDomain() } }

    override suspend fun getTaskById(id: Long): PlannerTask? =
        taskDao.getTaskById(id)?.toDomain()

    override suspend fun upsertTask(task: PlannerTask): Long {
        val insertedId = if (task.id == 0L) {
            taskDao.insertTask(task.toEntity())
        } else {
            taskDao.updateTask(task.toEntity())
            task.id
        }
        if (task.id == 0L && task.subtasks.isNotEmpty()) {
            val entities = task.subtasks.mapIndexed { idx, sub ->
                SubtaskEntity(
                    taskId = insertedId,
                    title = sub.title,
                    isCompleted = sub.isCompleted,
                    sortOrder = idx
                )
            }
            taskDao.insertSubtasks(entities)
        }
        return insertedId
    }

    override suspend fun toggleTaskCompletion(taskId: Long, completed: Boolean) {
        taskDao.updateCompletion(
            taskId = taskId,
            completed = completed,
            completedAt = if (completed) System.currentTimeMillis() else null
        )
    }

    override suspend fun deleteTask(taskId: Long) = taskDao.deleteTask(taskId)

    override suspend fun rolloverTasks(taskIds: List<Long>, targetDate: LocalDate) {
        if (taskIds.isNotEmpty()) {
            taskDao.rolloverTasks(taskIds, targetDate.toEpochDay())
        }
    }

    override suspend fun rescheduleTask(
        taskId: Long,
        targetDate: LocalDate?,
        timeBlock: TimeBlock,
        dueTime: LocalTime?
    ) {
        taskDao.rescheduleTask(
            taskId = taskId,
            targetEpochDay = targetDate?.toEpochDay(),
            timeBlock = timeBlock.name,
            minuteOfDay = dueTime?.let { it.hour * 60 + it.minute }
        )
    }

    override suspend fun addSubtask(taskId: Long, title: String) {
        val order = taskDao.nextSubtaskOrder(taskId)
        taskDao.insertSubtask(
            SubtaskEntity(
                taskId = taskId,
                title = title.trim(),
                isCompleted = false,
                sortOrder = order
            )
        )
    }

    override suspend fun toggleSubtask(subtaskId: Long, completed: Boolean) {
        taskDao.updateSubtaskCompletion(subtaskId, completed)
    }

    override suspend fun deleteSubtask(subtaskId: Long) {
        taskDao.deleteSubtask(subtaskId)
    }

    override suspend fun reorderSubtasks(taskId: Long, orderedSubtasks: List<Subtask>) {
        orderedSubtasks.forEachIndexed { index, subtask ->
            taskDao.updateSubtaskOrder(subtask.id, index)
        }
    }

    override fun observeTemplates(): Flow<List<TaskTemplate>> =
        templateDao.observeTemplates().map { list ->
            val mapped = list.map { it.toDomain() }
            if (mapped.isEmpty()) builtInDefaultTemplates() else mapped
        }

    override suspend fun saveTemplate(template: TaskTemplate): Long {
        return templateDao.insertTemplate(
            TaskTemplateEntity(
                id = template.id,
                name = template.name,
                nameAr = template.nameAr.ifBlank { template.name },
                defaultTitle = template.defaultTitle,
                defaultTitleAr = template.defaultTitleAr.ifBlank { template.defaultTitle },
                priority = template.priority,
                timeBlock = template.timeBlock,
                tagsJson = RoomTypeConverters.encodeStringList(template.tags),
                defaultSubtasksJson = RoomTypeConverters.encodeStringList(template.defaultSubtasks),
                defaultSubtasksArJson = RoomTypeConverters.encodeStringList(template.defaultSubtasksAr),
                isBuiltIn = template.isBuiltIn
            )
        )
    }

    override suspend fun deleteTemplate(id: Long) = templateDao.deleteTemplate(id)

    companion object {
        fun builtInDefaultTemplates(): List<TaskTemplate> = listOf(
            TaskTemplate(
                id = -1L,
                name = "Deep Work Sprint",
                nameAr = "جلسة عمل عميق",
                defaultTitle = "Deep work session",
                defaultTitleAr = "جلسة عمل مركّز",
                priority = Priority.HIGH,
                timeBlock = TimeBlock.MORNING,
                tags = listOf("focus", "work"),
                defaultSubtasks = listOf("Define single outcome", "Silence notifications", "25m focus sprint"),
                defaultSubtasksArJson(listOf("تحديد هدف واحد", "إيقاف التنبيهات", "جولة تركيز ٢٥ دقيقة")),
                isBuiltIn = true
            ),
            TaskTemplate(
                id = -2L,
                name = "Study Session",
                nameAr = "جلسة مذاكرة",
                defaultTitle = "Study & summarize chapter",
                defaultTitleAr = "مذاكرة وتلخيص الفصل",
                priority = Priority.MEDIUM,
                timeBlock = TimeBlock.AFTERNOON,
                tags = listOf("study"),
                defaultSubtasks = listOf("Skim headings", "Active recall notes", "Practice questions"),
                defaultSubtasksAr = listOf("قراءة العناوين", "تدوين ملاحظات الفهم", "حل أسئلة تطبيقية"),
                isBuiltIn = true
            ),
            TaskTemplate(
                id = -3L,
                name = "Evening Reset",
                nameAr = "ترتيب المساء",
                defaultTitle = "Daily wrap-up & tomorrow plan",
                defaultTitleAr = "مراجعة اليوم وخطة الغد",
                priority = Priority.LOW,
                timeBlock = TimeBlock.EVENING,
                tags = listOf("planning"),
                defaultSubtasks = listOf("Clear inbox", "Pick top 3 for tomorrow"),
                defaultSubtasksAr = listOf("ترتيب الملاحظات", "اختيار أهم ٣ مهام للغد"),
                isBuiltIn = true
            )
        )

        private fun defaultSubtasksArJson(items: List<String>) = items
    }
}

@Singleton
class RoutineRepositoryImpl @Inject constructor(
    private val routineDao: RoutineDao
) : RoutineRepository {

    override fun observeAllRoutines(): Flow<List<Routine>> =
        routineDao.observeAllRoutines().map { list -> list.map { it.toDomain() } }

    override fun observeAllCompletionDates(): Flow<Map<Long, Set<LocalDate>>> =
        routineDao.observeAllLogs().map { logs ->
            logs.groupBy { it.routineId }
                .mapValues { (_, entries) -> entries.map { LocalDate.ofEpochDay(it.dateEpochDay) }.toSet() }
        }

    override suspend fun getRoutineById(id: Long): Routine? =
        routineDao.getRoutineById(id)?.toDomain()

    override suspend fun upsertRoutine(routine: Routine): Long =
        routineDao.upsertRoutine(routine.toEntity())

    override suspend fun setRoutinePaused(routineId: Long, paused: Boolean, pausedUntil: LocalDate?) {
        routineDao.setPaused(routineId, paused, pausedUntil?.toEpochDay())
    }

    override suspend fun deleteRoutine(routineId: Long) = routineDao.deleteRoutine(routineId)

    override suspend fun toggleRoutineCompletion(routineId: Long, date: LocalDate) {
        val epochDay = date.toEpochDay()
        val existing = routineDao.findLog(routineId, epochDay)
        if (existing != null) {
            routineDao.deleteLog(routineId, epochDay)
        } else {
            routineDao.insertLog(RoutineLogEntity(routineId = routineId, dateEpochDay = epochDay))
        }
    }
}

@Singleton
class FocusRepositoryImpl @Inject constructor(
    private val focusDao: FocusDao
) : FocusRepository {

    override fun observeAllSessions(): Flow<List<FocusSession>> =
        focusDao.observeAllSessions().map { list ->
            list.map {
                FocusSession(
                    id = it.id,
                    taskId = it.taskId,
                    taskTitleSnapshot = it.taskTitleSnapshot,
                    durationMinutes = it.durationMinutes,
                    isBreak = it.isBreak,
                    startedAt = it.startedAt,
                    completedAt = it.completedAt
                )
            }
        }

    override fun observeSessionsForTask(taskId: Long): Flow<List<FocusSession>> =
        focusDao.observeSessionsForTask(taskId).map { list ->
            list.map {
                FocusSession(
                    id = it.id,
                    taskId = it.taskId,
                    taskTitleSnapshot = it.taskTitleSnapshot,
                    durationMinutes = it.durationMinutes,
                    isBreak = it.isBreak,
                    startedAt = it.startedAt,
                    completedAt = it.completedAt
                )
            }
        }

    override suspend fun recordSession(session: FocusSession): Long =
        focusDao.insertSession(
            FocusSessionEntity(
                id = session.id,
                taskId = session.taskId,
                taskTitleSnapshot = session.taskTitleSnapshot,
                durationMinutes = session.durationMinutes,
                isBreak = session.isBreak,
                startedAt = session.startedAt,
                completedAt = session.completedAt
            )
        )
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) : SettingsRepository {
    override val preferencesFlow: Flow<UserPreferences> = dataStore.preferencesFlow
    override suspend fun updateThemeMode(mode: ThemeMode) = dataStore.updateThemeMode(mode)
    override suspend fun updateDarkContrast(contrast: DarkContrastMode) = dataStore.updateDarkContrast(contrast)
    override suspend fun updateFontScale(scale: Float) = dataStore.updateFontScale(scale)
    override suspend fun updateMinimalistMode(enabled: Boolean) = dataStore.updateMinimalistMode(enabled)
    override suspend fun updateLanguage(language: AppLanguage) = dataStore.updateLanguage(language)
    override suspend fun updateGroupedRoutineReminders(enabled: Boolean) = dataStore.updateGroupedRoutineReminders(enabled)
    override suspend fun updateSmartSnoozeMinutes(minutes: Int) = dataStore.updateSmartSnoozeMinutes(minutes)
}
