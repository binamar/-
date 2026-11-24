package com.example.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.domain.model.PlannerTask
import com.example.domain.model.Routine
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleTaskReminder(task: PlannerTask, snoozeOverrideMillis: Long? = null) {
        if (task.isCompleted) {
            cancelTaskReminder(task.id)
            return
        }

        val triggerAtMillis = snoozeOverrideMillis ?: run {
            val date = task.dueDate ?: return
            val time = task.dueTime ?: return
            val offsetMin = task.reminderMinutesBefore ?: 0
            val targetDateTime = LocalDateTime.of(date, time).minusMinutes(offsetMin.toLong())
            val millis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (millis <= System.currentTimeMillis()) return
            millis
        }

        val pendingIntent = buildTaskPendingIntent(task.id, task.title)
        scheduleAlarmAt(triggerAtMillis, pendingIntent)
    }

    fun cancelTaskReminder(taskId: Long) {
        val pendingIntent = buildTaskPendingIntent(taskId, "")
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleRoutineGroupSnooze(routineIds: LongArray, delayMinutes: Int) {
        val triggerAtMillis = System.currentTimeMillis() + delayMinutes * 60_000L
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_ROUTINE_GROUP_TRIGGER
            putExtra(NotificationHelper.EXTRA_ROUTINE_IDS, routineIds)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            90_100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleAlarmAt(triggerAtMillis, pi)
    }

    fun scheduleGroupedRoutineCheck(routines: List<Routine>, windowMinutes: Int) {
        val today = LocalDate.now()
        val now = LocalDateTime.now()
        val activeToday = routines
            .filter { !it.isPaused && it.activeDays.contains(today.dayOfWeek.value) && it.reminderTime != null }
            .sortedBy { it.reminderTime }

        if (activeToday.isEmpty()) return

        // Cluster routines within windowMinutes into groups and schedule next upcoming cluster
        val clusters = mutableListOf<MutableList<Routine>>()
        for (routine in activeToday) {
            val lastCluster = clusters.lastOrNull()
            val lastTime = lastCluster?.firstOrNull()?.reminderTime
            val rTime = routine.reminderTime!!
            if (lastCluster != null && lastTime != null &&
                java.time.Duration.between(lastTime, rTime).toMinutes() <= windowMinutes
            ) {
                lastCluster.add(routine)
            } else {
                clusters.add(mutableListOf(routine))
            }
        }

        clusters.firstOrNull { cluster ->
            val firstTime = cluster.first().reminderTime!!
            LocalDateTime.of(today, firstTime).isAfter(now)
        }?.let { upcoming ->
            val firstTime = upcoming.first().reminderTime!!
            val millis = LocalDateTime.of(today, firstTime)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                action = ReminderBroadcastReceiver.ACTION_ROUTINE_GROUP_TRIGGER
                putExtra(NotificationHelper.EXTRA_ROUTINE_IDS, upcoming.map { it.id }.toLongArray())
            }
            val pi = PendingIntent.getBroadcast(
                context,
                90_001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleAlarmAt(millis, pi)
        }
    }

    fun ensurePeriodicWorkers() {
        val request = PeriodicWorkRequestBuilder<GroupedRoutineReminderWorker>(
            12, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "flowday_routine_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun buildTaskPendingIntent(taskId: Long, title: String): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TASK_REMINDER
            putExtra(NotificationHelper.EXTRA_TASK_ID, taskId)
            putExtra(NotificationHelper.EXTRA_TITLE, title)
        }
        return PendingIntent.getBroadcast(
            context,
            (20_000 + (taskId % 50_000)).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleAlarmAt(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}
