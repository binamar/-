package com.example.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.domain.repository.RoutineRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltWorker
class GroupedRoutineReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val routineRepository: RoutineRepository,
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = settingsRepository.preferencesFlow.first()
        if (prefs.groupedRoutineReminders) {
            val routines = routineRepository.observeAllRoutines().first()
            reminderScheduler.scheduleGroupedRoutineCheck(routines, prefs.reminderGroupWindowMinutes)
        }
        return Result.success()
    }
}

@AndroidEntryPoint
class ReminderBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var routineRepository: RoutineRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    companion object {
        const val ACTION_TASK_REMINDER = "com.example.flowday.ACTION_TASK_REMINDER"
        const val ACTION_ROUTINE_GROUP_TRIGGER = "com.example.flowday.ACTION_ROUTINE_GROUP_TRIGGER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val prefs = settingsRepository.preferencesFlow.first()
                when (intent.action) {
                    ACTION_TASK_REMINDER -> {
                        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
                        val title = intent.getStringExtra(NotificationHelper.EXTRA_TITLE).orEmpty()
                        if (taskId > 0L && title.isNotBlank()) {
                            notificationHelper.showTaskReminder(taskId, title, prefs.smartSnoozeMinutes)
                        }
                    }
                    ACTION_ROUTINE_GROUP_TRIGGER -> {
                        val ids = intent.getLongArrayExtra(NotificationHelper.EXTRA_ROUTINE_IDS)?.toSet().orEmpty()
                        val routines = routineRepository.observeAllRoutines().first()
                            .filter { ids.contains(it.id) && !it.isPaused }
                        if (routines.isNotEmpty()) {
                            notificationHelper.showGroupedRoutineReminder(routines, prefs.smartSnoozeMinutes)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1)
        if (notifId != -1) {
            NotificationManagerCompat.from(context).cancel(notifId)
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    NotificationHelper.ACTION_COMPLETE_TASK -> {
                        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
                        if (taskId > 0L) {
                            taskRepository.toggleTaskCompletion(taskId, true)
                        }
                    }
                    NotificationHelper.ACTION_SMART_SNOOZE -> {
                        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
                        val snoozeMin = intent.getIntExtra(NotificationHelper.EXTRA_SNOOZE_MINUTES, 15)
                        val task = if (taskId > 0L) taskRepository.getTaskById(taskId) else null
                        if (task != null) {
                            val snoozeUntil = System.currentTimeMillis() + snoozeMin * 60_000L
                            reminderScheduler.scheduleTaskReminder(task, snoozeOverrideMillis = snoozeUntil)
                        }
                    }
                    NotificationHelper.ACTION_SNOOZE_ROUTINE_GROUP -> {
                        val ids = intent.getLongArrayExtra(NotificationHelper.EXTRA_ROUTINE_IDS) ?: longArrayOf()
                        val snoozeMin = intent.getIntExtra(NotificationHelper.EXTRA_SNOOZE_MINUTES, 15)
                        if (ids.isNotEmpty()) {
                            reminderScheduler.scheduleRoutineGroupSnooze(ids, snoozeMin)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var routineRepository: RoutineRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val prefs = settingsRepository.preferencesFlow.first()
                taskRepository.observeAllTasks().first()
                    .filter { !it.isCompleted && it.dueDate != null && it.dueTime != null }
                    .forEach { reminderScheduler.scheduleTaskReminder(it) }

                if (prefs.groupedRoutineReminders) {
                    val routines = routineRepository.observeAllRoutines().first()
                    reminderScheduler.scheduleGroupedRoutineCheck(routines, prefs.reminderGroupWindowMinutes)
                }
                reminderScheduler.ensurePeriodicWorkers()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
