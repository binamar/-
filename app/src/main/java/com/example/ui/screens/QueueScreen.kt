package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.DownloadStatus
import com.example.domain.model.DownloadTask
import com.example.ui.components.HostBadge
import com.example.ui.components.MetaChip
import com.example.ui.components.ResolutionPill
import com.example.ui.components.formatBytes
import com.example.ui.components.formatSpeed
import com.example.ui.theme.LocalStreamGrabSemantics
import com.example.ui.viewmodel.QueueUiSummary

@Composable
fun QueueScreen(
    summary: QueueUiSummary,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onClearFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            QueueDashboardCard(
                summary = summary,
                onPauseAll = onPauseAll,
                onResumeAll = onResumeAll,
                onClearFinished = onClearFinished
            )
        }

        if (summary.tasks.isEmpty()) {
            item {
                EmptyQueueCard()
            }
        } else {
            items(summary.tasks, key = { it.id }) { task ->
                DownloadTaskCard(
                    task = task,
                    onPause = { onPause(task.id) },
                    onResume = { onResume(task.id) },
                    onRetry = { onRetry(task.id) },
                    onCancel = { onCancel(task.id) },
                    onDelete = { onDelete(task.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun QueueDashboardCard(
    summary: QueueUiSummary,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onClearFinished: () -> Unit
) {
    val semantics = LocalStreamGrabSemantics.current
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "محرك التحميل الآلي الثقيل (WorkManager + Room)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QueueStatTile(
                    label = "نشط",
                    value = summary.activeCount.toString(),
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                QueueStatTile(
                    label = "بالانتظار",
                    value = summary.queuedCount.toString(),
                    accent = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                QueueStatTile(
                    label = "مكتمل",
                    value = summary.completedCount.toString(),
                    accent = semantics.success,
                    modifier = Modifier.weight(1f)
                )
                QueueStatTile(
                    label = "فشل",
                    value = summary.failedCount.toString(),
                    accent = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPauseAll,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("pause_all_btn")
                ) {
                    Icon(Icons.Rounded.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إيقاف الكل", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onResumeAll,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("resume_all_btn")
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("استكمال", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onClearFinished,
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("clear_completed_btn")
                ) {
                    Icon(Icons.Rounded.CleaningServices, contentDescription = "مسح المكتمل", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun QueueStatTile(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DownloadTaskCard(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val semantics = LocalStreamGrabSemantics.current
    val statusColor = when (task.status) {
        DownloadStatus.DOWNLOADING, DownloadStatus.RESOLVING -> MaterialTheme.colorScheme.primary
        DownloadStatus.QUEUED -> MaterialTheme.colorScheme.secondary
        DownloadStatus.COMPLETED -> semantics.success
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
        DownloadStatus.PAUSED, DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (task.is360p) 1.3.dp else 1.dp,
            color = if (task.is360p) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HostBadge(host = task.host)
                    ResolutionPill(label = task.resolutionLabel, is360p = task.is360p)
                }
                MetaChip(text = task.status.arabicLabel, tint = statusColor)
            }

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (task.status == DownloadStatus.DOWNLOADING ||
                task.status == DownloadStatus.PAUSED ||
                task.status == DownloadStatus.COMPLETED
            ) {
                LinearProgressIndicator(
                    progress = { task.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            } else if (task.status == DownloadStatus.RESOLVING) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sizeInfo = if (task.expectedSizeBytes > 0L) {
                    "${formatBytes(task.downloadedBytes)} / ${formatBytes(task.expectedSizeBytes)}"
                } else {
                    formatBytes(task.downloadedBytes)
                }
                val speedInfo = if (task.status == DownloadStatus.DOWNLOADING && task.speedBytesPerSec > 0L) {
                    " • ${formatSpeed(task.speedBytesPerSec)}"
                } else ""

                Text(
                    text = "$sizeInfo$speedInfo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (task.status) {
                        DownloadStatus.DOWNLOADING, DownloadStatus.RESOLVING, DownloadStatus.QUEUED -> {
                            IconButton(onClick = onPause) {
                                Icon(Icons.Rounded.Pause, contentDescription = "إيقاف مؤقت")
                            }
                            IconButton(onClick = onCancel) {
                                Icon(Icons.Rounded.Cancel, contentDescription = "إلغاء")
                            }
                        }
                        DownloadStatus.PAUSED -> {
                            IconButton(onClick = onResume) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "استكمال التحميل")
                            }
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف")
                            }
                        }
                        DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                            IconButton(onClick = onRetry) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "إعادة المحاولة")
                            }
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف")
                            }
                        }
                        DownloadStatus.COMPLETED -> {
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "إزالة من القائمة")
                            }
                        }
                    }
                }
            }

            if (!task.errorMessage.isNullOrBlank()) {
                Text(
                    text = task.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (!task.localUri.isNullOrBlank() && task.status == DownloadStatus.COMPLETED) {
                Text(
                    text = "تم الحفظ: Downloads/StreamGrab/${task.fileName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = semantics.success,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyQueueCard() {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.DownloadForOffline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "طابور التحميل فارغ حالياً",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "استخرج روابط الفيديو من تبويب «المستخرج» وأرسل دقة 360p أو الدفعات الآلية هنا للتحميل مع الاستكمال التلقائي.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
