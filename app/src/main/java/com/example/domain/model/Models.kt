package com.example.domain.model

import java.time.LocalDate
import java.time.LocalTime

enum class Priority(val level: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    URGENT(4)
}

enum class TimeBlock {
    ANYTIME,
    MORNING,
    AFTERNOON,
    EVENING
}

enum class RoutineCategory {
    MORNING,
    EVENING,
    STUDY_WORK,
    CUSTOM
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class DarkContrastMode {
    BALANCED,
    HIGH_CONTRAST,
    OLED_BLACK
}

enum class AppLanguage {
    SYSTEM,
    ENGLISH,
    ARABIC
}

enum class InsightPeriod {
    WEEKLY,
    MONTHLY
}

enum class InsightTypeFilter {
    ALL,
    TASKS,
    ROUTINES
}

enum class CalendarViewMode {
    DAY,
    WEEK,
    MONTH
}

enum class TimeOfDayBucket {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT
}

data class Subtask(
    val id: Long = 0L,
    val taskId: Long = 0L,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0
)

data class PlannerTask(
    val id: Long = 0L,
    val title: String,
    val notes: String = "",
    val priority: Priority = Priority.MEDIUM,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val timeBlock: TimeBlock = TimeBlock.ANYTIME,
    val tags: List<String> = emptyList(),
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val postponeCount: Int = 0,
    val reminderMinutesBefore: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val subtasks: List<Subtask> = emptyList()
) {
    val completionProgress: Float
        get() = if (subtasks.isEmpty()) {
            if (isCompleted) 1f else 0f
        } else {
            subtasks.count { it.isCompleted }.toFloat() / subtasks.size.toFloat()
        }

    val isRepeatedlyPostponed: Boolean
        get() = !isCompleted && postponeCount >= 3

    val isUnscheduled: Boolean
        get() = dueDate == null || (dueTime == null && timeBlock == TimeBlock.ANYTIME)
}

data class TaskTemplate(
    val id: Long = 0L,
    val name: String,
    val nameAr: String = "",
    val defaultTitle: String,
    val defaultTitleAr: String = "",
    val priority: Priority = Priority.MEDIUM,
    val timeBlock: TimeBlock = TimeBlock.ANYTIME,
    val tags: List<String> = emptyList(),
    val defaultSubtasks: List<String> = emptyList(),
    val defaultSubtasksAr: List<String> = emptyList(),
    val isBuiltIn: Boolean = false
)

data class RoutineDayVariation(
    val dayOfWeek: Int, // 1 (Monday) .. 7 (Sunday)
    val note: String,
    val durationMinutes: Int
)

data class Routine(
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val category: RoutineCategory = RoutineCategory.MORNING,
    val activeDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val reminderTime: LocalTime? = null,
    val isPaused: Boolean = false,
    val pausedUntil: LocalDate? = null,
    val defaultDurationMinutes: Int = 15,
    val variations: Map<Int, RoutineDayVariation> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun variationFor(dayOfWeek: Int): RoutineDayVariation? = variations[dayOfWeek]

    fun effectiveDurationFor(dayOfWeek: Int): Int =
        variations[dayOfWeek]?.durationMinutes ?: defaultDurationMinutes
}

data class RoutineWithStats(
    val routine: Routine,
    val isCompletedToday: Boolean,
    val currentStreak: Int,
    val graceUsedThisWeek: Boolean,
    val totalCompletionRate: Float,
    val completedDates: Set<LocalDate>
)

data class FocusSession(
    val id: Long = 0L,
    val taskId: Long? = null,
    val taskTitleSnapshot: String = "",
    val durationMinutes: Int,
    val isBreak: Boolean = false,
    val startedAt: Long,
    val completedAt: Long
)

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val darkContrastMode: DarkContrastMode = DarkContrastMode.BALANCED,
    val fontScale: Float = 1.0f,
    val minimalistMode: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val groupedRoutineReminders: Boolean = true,
    val smartSnoozeMinutes: Int = 15,
    val reminderGroupWindowMinutes: Int = 30
)

data class DailyCompletionPoint(
    val date: LocalDate,
    val completed: Int,
    val total: Int
) {
    val rate: Float get() = if (total == 0) 0f else completed.toFloat() / total.toFloat()
}

data class InsightSummary(
    val completionRate: Float,
    val previousPeriodRate: Float,
    val deltaPercent: Int,
    val completedCount: Int,
    val totalCount: Int,
    val focusMinutesTotal: Int,
    val bestActiveStreak: Int,
    val dailyPoints: List<DailyCompletionPoint>,
    val timeOfDayDistribution: Map<TimeOfDayBucket, Int>,
    val repeatedlyPostponedTasks: List<PlannerTask>,
    val availableTags: List<String>
)
