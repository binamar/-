package com.example.domain.usecase

import com.example.domain.model.DailyCompletionPoint
import com.example.domain.model.FocusSession
import com.example.domain.model.InsightPeriod
import com.example.domain.model.InsightSummary
import com.example.domain.model.InsightTypeFilter
import com.example.domain.model.PlannerTask
import com.example.domain.model.Routine
import com.example.domain.model.RoutineWithStats
import com.example.domain.model.TimeBlock
import com.example.domain.model.TimeOfDayBucket
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class CalculateRoutineStatsUseCase @Inject constructor() {

    fun calculate(
        routine: Routine,
        completedDates: Set<LocalDate>,
        today: LocalDate = LocalDate.now()
    ): RoutineWithStats {
        val isCompletedToday = completedDates.contains(today)

        // Gentle Streak (Grace Day): allows 1 missed scheduled day per rolling 7 scheduled days.
        var streak = 0
        var graceUsedRecent = false
        var missedInCurrentWindow = 0
        var scheduledChecked = 0
        var cursor = today

        // If today is scheduled and not yet completed, don't penalize streak until day ends
        if (routine.activeDays.contains(cursor.dayOfWeek.value) && isCompletedToday) {
            streak++
            scheduledChecked++
        }
        cursor = cursor.minusDays(1)

        var safetyCounter = 0
        while (safetyCounter < 365) {
            safetyCounter++
            if (routine.activeDays.contains(cursor.dayOfWeek.value)) {
                scheduledChecked++
                if (completedDates.contains(cursor)) {
                    streak++
                } else {
                    missedInCurrentWindow++
                    if (missedInCurrentWindow <= 1 && scheduledChecked <= 7) {
                        // Grace day absorbs 1 miss without breaking streak
                        graceUsedRecent = true
                    } else if (missedInCurrentWindow <= 1) {
                        graceUsedRecent = true
                        missedInCurrentWindow = 1
                    } else {
                        break
                    }
                }
                if (scheduledChecked % 7 == 0) {
                    missedInCurrentWindow = 0
                }
            }
            cursor = cursor.minusDays(1)
        }

        // Overall completion rate over last 30 days (or since creation)
        val createdDate = Instant.ofEpochMilli(routine.createdAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val windowStart = maxOf(createdDate, today.minusDays(29))
        var scheduledTotal = 0
        var completedInWindow = 0
        var d = windowStart
        while (!d.isAfter(today)) {
            if (routine.activeDays.contains(d.dayOfWeek.value)) {
                scheduledTotal++
                if (completedDates.contains(d)) completedInWindow++
            }
            d = d.plusDays(1)
        }
        val totalRate = if (scheduledTotal == 0) {
            if (isCompletedToday) 1f else 0f
        } else {
            completedInWindow.toFloat() / scheduledTotal.toFloat()
        }

        return RoutineWithStats(
            routine = routine,
            isCompletedToday = isCompletedToday,
            currentStreak = streak,
            graceUsedThisWeek = graceUsedRecent,
            totalCompletionRate = totalRate.coerceIn(0f, 1f),
            completedDates = completedDates
        )
    }
}

@Singleton
class BuildInsightsUseCase @Inject constructor(
    private val routineStatsUseCase: CalculateRoutineStatsUseCase
) {

    fun build(
        tasks: List<PlannerTask>,
        routines: List<Routine>,
        routineLogs: Map<Long, Set<LocalDate>>,
        focusSessions: List<FocusSession>,
        period: InsightPeriod,
        typeFilter: InsightTypeFilter,
        selectedTag: String?,
        today: LocalDate = LocalDate.now()
    ): InsightSummary {
        val windowDays = if (period == InsightPeriod.WEEKLY) 7 else 30
        val currentStart = today.minusDays((windowDays - 1).toLong())
        val prevStart = currentStart.minusDays(windowDays.toLong())
        val prevEnd = currentStart.minusDays(1)

        val availableTags = tasks.flatMap { it.tags }.distinct().sorted()

        val filteredTasks = tasks.filter { task ->
            selectedTag == null || task.tags.any { it.equals(selectedTag, ignoreCase = true) }
        }

        val includeTasks = typeFilter == InsightTypeFilter.ALL || typeFilter == InsightTypeFilter.TASKS
        val includeRoutines = (typeFilter == InsightTypeFilter.ALL || typeFilter == InsightTypeFilter.ROUTINES) && selectedTag == null

        fun computePeriodStats(start: LocalDate, end: LocalDate): Pair<Int, Int> {
            var completed = 0
            var total = 0
            if (includeTasks) {
                val periodTasks = filteredTasks.filter { t ->
                    val d = t.dueDate ?: return@filter false
                    !d.isBefore(start) && !d.isAfter(end)
                }
                total += periodTasks.size
                completed += periodTasks.count { it.isCompleted }
            }
            if (includeRoutines) {
                var d = start
                while (!d.isAfter(end)) {
                    val dow = d.dayOfWeek.value
                    routines.filter { !it.isPaused && it.activeDays.contains(dow) }.forEach { r ->
                        total++
                        if (routineLogs[r.id]?.contains(d) == true) completed++
                    }
                    d = d.plusDays(1)
                }
            }
            return completed to total
        }

        val (currentCompleted, currentTotal) = computePeriodStats(currentStart, today)
        val (prevCompleted, prevTotal) = computePeriodStats(prevStart, prevEnd)

        val currentRate = if (currentTotal == 0) 0f else currentCompleted.toFloat() / currentTotal.toFloat()
        val prevRate = if (prevTotal == 0) 0f else prevCompleted.toFloat() / prevTotal.toFloat()
        val deltaPercent = ((currentRate - prevRate) * 100f).roundToInt()

        // Daily points for chart
        val points = mutableListOf<DailyCompletionPoint>()
        var dayCursor = currentStart
        while (!dayCursor.isAfter(today)) {
            val (c, t) = computePeriodStats(dayCursor, dayCursor)
            points.add(DailyCompletionPoint(dayCursor, c, t))
            dayCursor = dayCursor.plusDays(1)
        }

        // Time of day distribution for completed tasks & focus sessions
        val bucketCounts = mutableMapOf(
            TimeOfDayBucket.MORNING to 0,
            TimeOfDayBucket.AFTERNOON to 0,
            TimeOfDayBucket.EVENING to 0,
            TimeOfDayBucket.NIGHT to 0
        )

        if (includeTasks) {
            filteredTasks.filter { it.isCompleted }.forEach { task ->
                val bucket = when {
                    task.completedAt != null -> {
                        val hour = Instant.ofEpochMilli(task.completedAt)
                            .atZone(ZoneId.systemDefault())
                            .hour
                        hourToBucket(hour)
                    }
                    task.dueTime != null -> hourToBucket(task.dueTime.hour)
                    task.timeBlock == TimeBlock.MORNING -> TimeOfDayBucket.MORNING
                    task.timeBlock == TimeBlock.AFTERNOON -> TimeOfDayBucket.AFTERNOON
                    task.timeBlock == TimeBlock.EVENING -> TimeOfDayBucket.EVENING
                    else -> TimeOfDayBucket.MORNING
                }
                bucketCounts[bucket] = (bucketCounts[bucket] ?: 0) + 1
            }
        }
        if (includeRoutines) {
            routines.forEach { routine ->
                val count = routineLogs[routine.id]?.count { !it.isBefore(currentStart) && !it.isAfter(today) } ?: 0
                if (count > 0) {
                    val bucket = routine.reminderTime?.let { hourToBucket(it.hour) } ?: when (routine.category) {
                        com.example.domain.model.RoutineCategory.MORNING -> TimeOfDayBucket.MORNING
                        com.example.domain.model.RoutineCategory.EVENING -> TimeOfDayBucket.EVENING
                        else -> TimeOfDayBucket.AFTERNOON
                    }
                    bucketCounts[bucket] = (bucketCounts[bucket] ?: 0) + count
                }
            }
        }

        val focusMinutesTotal = focusSessions
            .filter { !it.isBreak }
            .filter {
                val d = Instant.ofEpochMilli(it.completedAt).atZone(ZoneId.systemDefault()).toLocalDate()
                !d.isBefore(currentStart) && !d.isAfter(today)
            }
            .sumOf { it.durationMinutes }

        val bestStreak = routines.maxOfOrNull { r ->
            routineStatsUseCase.calculate(r, routineLogs[r.id].orEmpty(), today).currentStreak
        } ?: 0

        val repeatedlyPostponed = filteredTasks
            .filter { it.isRepeatedlyPostponed }
            .sortedByDescending { it.postponeCount }

        return InsightSummary(
            completionRate = currentRate,
            previousPeriodRate = prevRate,
            deltaPercent = deltaPercent,
            completedCount = currentCompleted,
            totalCount = currentTotal,
            focusMinutesTotal = focusMinutesTotal,
            bestActiveStreak = bestStreak,
            dailyPoints = points,
            timeOfDayDistribution = bucketCounts,
            repeatedlyPostponedTasks = repeatedlyPostponed,
            availableTags = availableTags
        )
    }

    private fun hourToBucket(hour: Int): TimeOfDayBucket = when (hour) {
        in 5..11 -> TimeOfDayBucket.MORNING
        in 12..16 -> TimeOfDayBucket.AFTERNOON
        in 17..21 -> TimeOfDayBucket.EVENING
        else -> TimeOfDayBucket.NIGHT
    }
}
