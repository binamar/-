package com.example.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager: NotificationManager?
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تحميلات StreamGrab",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات متابعة الطابور الآلي والتحميل الثقيل"
            }
            manager?.createNotificationChannel(channel)
        }
    }

    fun buildProgressNotification(
        title: String,
        subtitle: String,
        progressPercent: Int,
        indeterminate: Boolean
    ): Notification {
        ensureChannels()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, progressPercent.coerceIn(0, 100), indeterminate)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun showCompletedNotification(taskId: Long, title: String, isError: Boolean, detail: String) {
        ensureChannels()
        val icon = if (isError) android.R.drawable.stat_notify_error else android.R.drawable.stat_sys_download_done
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(detail)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager?.notify((2000 + (taskId % 1000)).toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "streamgrab_downloads"
    }
}
