package com.example.scheduler

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.model.Routine
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_TASKS = "flowday_tasks"
        const val CHANNEL_ROUTINES = "flowday_routines"
        const val GROUP_ROUTINES = "flowday_routine_group"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_ROUTINE_IDS = "extra_routine_ids"
        const val ACTION_COMPLETE_TASK = "com.example.flowday.ACTION_COMPLETE_TASK"
        const val ACTION_SMART_SNOOZE = "com.example.flowday.ACTION_SMART_SNOOZE"
        const val ACTION_SNOOZE_ROUTINE_GROUP = "com.example.flowday.ACTION_SNOOZE_ROUTINE_GROUP"
    }

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val taskChannel = NotificationChannel(
                CHANNEL_TASKS,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled reminders and smart snooze for daily tasks"
            }
            val routineChannel = NotificationChannel(
                CHANNEL_ROUTINES,
                "Routine Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Grouped routine reminders and daily habit prompts"
            }
            manager.createNotificationChannel(taskChannel)
            manager.createNotificationChannel(routineChannel)
        }
    }

    private fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showTaskReminder(taskId: Long, title: String, snoozeMinutes: Int) {
        if (!canPostNotifications()) return
        ensureChannels()

        val notifId = (10_000 + (taskId % 50_000)).toInt()
        val openIntent = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = PendingIntent.getBroadcast(
            context,
            notifId + 1,
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_COMPLETE_TASK
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_NOTIFICATION_ID, notifId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smartSnoozeIntent = PendingIntent.getBroadcast(
            context,
            notifId + 2,
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_SMART_SNOOZE
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                putExtra(EXTRA_NOTIFICATION_ID, notifId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TASKS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("FlowDay Task Reminder")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .addAction(0, "Done", completeIntent)
            .addAction(0, "Snooze +${snoozeMinutes}m", smartSnoozeIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notification)
    }

    fun showGroupedRoutineReminder(routines: List<Routine>, snoozeMinutes: Int) {
        if (routines.isEmpty() || !canPostNotifications()) return
        ensureChannels()

        val notifId = 88_001
        val openIntent = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
        val title = if (routines.size == 1) {
            routines.first().title
        } else {
            "${routines.size} Routines Ready"
        }

        val inbox = NotificationCompat.InboxStyle().setBigContentTitle(title)
        routines.forEach { routine ->
            val timeStr = routine.reminderTime?.format(timeFmt) ?: ""
            inbox.addLine(if (timeStr.isNotBlank()) "$timeStr • ${routine.title}" else routine.title)
        }

        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            notifId + 3,
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_SNOOZE_ROUTINE_GROUP
                putExtra(EXTRA_ROUTINE_IDS, routines.map { it.id }.toLongArray())
                putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                putExtra(EXTRA_NOTIFICATION_ID, notifId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ROUTINES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(routines.joinToString(" • ") { it.title })
            .setStyle(inbox)
            .setGroup(GROUP_ROUTINES)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .addAction(0, "Smart Snooze +${snoozeMinutes}m", snoozeIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notification)
    }
}
