package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.BatchItemProgress
import com.example.domain.model.BatchItemState
import com.example.domain.model.ExtractedVideo
import com.example.domain.model.QualityPreferencePolicy
import com.example.domain.model.ResolveMode
import com.example.domain.model.ScrapeInspectionReport
import com.example.domain.model.ScraperSettings
import com.example.ui.components.HostBadge
import com.example.ui.components.MetaChip
import com.example.ui.components.ResolutionPill
import com.example.ui.components.formatBytes
import com.example.ui.theme.LocalStreamGrabSemantics
import com.example.ui.viewmodel.ResolverUiState

@Composable
fun ResolverScreen(
    uiState: ResolverUiState,
    settings: ScraperSettings,
    onModeChange: (ResolveMode) -> Unit,
    onSingleUrlChange: (String) -> Unit,
    onBatchUrlsChange: (String) -> Unit,
    onInspectSingle: () -> Unit,
    onRunOfflineLab: () -> Unit,
    onSendEpisodesToBatch: () -> Unit,
    onStartAutoBatch: () -> Unit,
    onCancelAutoBatch: () -> Unit,
    onPolicyChange: (QualityPreferencePolicy) -> Unit,
    onQueueVideo: (ExtractedVideo) -> Unit,
    onShowDetails: (ExtractedVideo?) -> Unit,
    onToggleDiagnostics: (Boolean) -> Unit,
    onNotifyCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeroStudioHeader(
                activePolicy = settings.qualityPolicy,
                onRunOfflineLab = onRunOfflineLab
            )
        }

        item {
            DualModeSelector(
                selectedMode = uiState.mode,
                onModeChange = onModeChange
            )
        }

        if (uiState.mode == ResolveMode.INTERACTIVE) {
            item {
                InteractiveInputCard(
                    urlInput = uiState.singleUrlInput,
                    isInspecting = uiState.isInspecting,
                    onUrlChange = onSingleUrlChange,
                    onPasteClick = {
                        val pasted = readClipboardText(context)
                        if (pasted != null) onSingleUrlChange(pasted)
                    },
                    onInspectClick = onInspectSingle,
                    onRunOfflineLab = onRunOfflineLab
                )
            }

            if (uiState.errorMessage != null) {
                item {
                    ErrorBannerCard(message = uiState.errorMessage)
                }
            }

            val report = uiState.report
            if (report != null) {
                item {
                    ReportOverviewCard(
                        report = report,
                        onToggleDiagnostics = { onToggleDiagnostics(true) },
                        onSendEpisodesToBatch = onSendEpisodesToBatch
                    )
                }

                if (report.resolvedVideos.isEmpty()) {
                    item {
                        EmptyReportHintCard(
                            hasEpisodes = report.episodeLinks.isNotEmpty()
                        )
                    }
                } else {
                    items(report.resolvedVideos, key = { it.id }) { video ->
                        ExtractedVideoCard(
                            video = video,
                            isRecommended = report.recommendedVideo?.id == video.id,
                            onQueueDownload = { onQueueVideo(video) },
                            onCopyUrl = {
                                copyToClipboard(context, video.directUrl)
                                onNotifyCopy("تم نسخ الرابط المباشر (${video.quality.resolution.label})")
                            },
                            onShowDetails = { onShowDetails(video) }
                        )
                    }
                }
            } else if (!uiState.isInspecting) {
                item {
                    FirstLaunchGuideCard(onRunOfflineLab = onRunOfflineLab)
                }
            }
        } else {
            item {
                AutoBatchInputCard(
                    batchInput = uiState.batchUrlsInput,
                    activePolicy = settings.qualityPolicy,
                    isRunning = uiState.isBatchRunning,
                    interDelaySec = settings.interDownloadDelaySec,
                    onBatchInputChange = onBatchUrlsChange,
                    onPolicyChange = onPolicyChange,
                    onStartBatch = onStartAutoBatch,
                    onCancelBatch = onCancelAutoBatch
                )
            }

            if (uiState.errorMessage != null) {
                item {
                    ErrorBannerCard(message = uiState.errorMessage)
                }
            }

            if (uiState.batchProgress.isNotEmpty()) {
                item {
                    Text(
                        text = "نتائج الفحص المجمّع (${uiState.batchProgress.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(uiState.batchProgress, key = { it.url }) { item ->
                    BatchProgressRowCard(item = item)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // Video technical details dialog (Headers, Referer, Subtitle, Direct URL)
    val detailVideo = uiState.selectedVideoForDetails
    if (detailVideo != null) {
        VideoTechnicalDetailsDialog(
            video = detailVideo,
            onDismiss = { onShowDetails(null) },
            onCopyAll = {
                val payload = buildString {
                    appendLine("Title: ${detailVideo.title}")
                    appendLine("Host: ${detailVideo.host.displayName}")
                    appendLine("Quality: ${detailVideo.quality.resolution.label}")
                    appendLine("Direct URL: ${detailVideo.directUrl}")
                    detailVideo.subtitleUrl?.let { appendLine("Subtitle: $it") }
                    detailVideo.headers.forEach { (k, v) -> appendLine("Header [$k]: $v") }
                }
                copyToClipboard(context, payload)
                onNotifyCopy("تم نسخ الرابط والترويسات كاملة")
            }
        )
    }

    // Diagnostics log dialog
    if (uiState.showDiagnosticsSheet && uiState.report != null) {
        DiagnosticsLogDialog(
            report = uiState.report,
            onDismiss = { onToggleDiagnostics(false) }
        )
    }
}

@Composable
private fun HeroStudioHeader(
    activePolicy: QualityPreferencePolicy,
    onRunOfflineLab: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_resolver_hero_1795530403444),
                contentDescription = "خلفية محرك الاستخراج",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "محرك الاستخراج وفحص 360p",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "EgyDead • Doodstream • MixDrop • M3U8",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = activePolicy.shortTag,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = onRunOfflineLab,
                        modifier = Modifier.testTag("lab_sample_chip"),
                        label = {
                            Text(
                                text = "مختبر الفحص المحلي (Dood + MixDrop + M3U8)",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Science,
                                contentDescription = "تجربة مختبر الفحص",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DualModeSelector(
    selectedMode: ResolveMode,
    onModeChange: (ResolveMode) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dual_mode_selector")
    ) {
        SegmentedButton(
            selected = selectedMode == ResolveMode.INTERACTIVE,
            onClick = { onModeChange(ResolveMode.INTERACTIVE) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            modifier = Modifier.testTag("mode_interactive_btn"),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        ) {
            Text("الوضع التفاعلي المنظّم", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        SegmentedButton(
            selected = selectedMode == ResolveMode.AUTO_BATCH,
            onClick = { onModeChange(ResolveMode.AUTO_BATCH) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            modifier = Modifier.testTag("mode_batch_btn"),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.AutoMode,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        ) {
            Text("الوضع الآلي 360p", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun InteractiveInputCard(
    urlInput: String,
    isInspecting: Boolean,
    onUrlChange: (String) -> Unit,
    onPasteClick: () -> Unit,
    onInspectClick: () -> Unit,
    onRunOfflineLab: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "رابط المشاهدة أو الخادم المباشر",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = urlInput,
                onValueChange = onUrlChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("single_url_input"),
                placeholder = {
                    Text(
                        text = "https://... (EgyDead / Doodstream / MixDrop / M3U8)",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                singleLine = true,
                trailingIcon = {
                    Row {
                        if (urlInput.isNotEmpty()) {
                            IconButton(
                                onClick = { onUrlChange("") },
                                modifier = Modifier.testTag("clear_single_url_btn")
                            ) {
                                Icon(Icons.Rounded.Clear, contentDescription = "مسح الرابط")
                            }
                        }
                        IconButton(
                            onClick = onPasteClick,
                            modifier = Modifier.testTag("paste_single_url_btn")
                        ) {
                            Icon(Icons.Rounded.ContentPaste, contentDescription = "لصق من الحافظة")
                        }
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onInspectClick,
                    enabled = !isInspecting,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("inspect_single_btn")
                ) {
                    if (isInspecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري الفحص والاستخراج...")
                    } else {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("فحص الدقات واستخراج الروابط")
                    }
                }

                OutlinedButton(
                    onClick = onRunOfflineLab,
                    enabled = !isInspecting,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("lab_test_btn")
                ) {
                    Icon(Icons.Rounded.Science, contentDescription = "اختبار محلي", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun AutoBatchInputCard(
    batchInput: String,
    activePolicy: QualityPreferencePolicy,
    isRunning: Boolean,
    interDelaySec: Int,
    onBatchInputChange: (String) -> Unit,
    onPolicyChange: (QualityPreferencePolicy) -> Unit,
    onStartBatch: () -> Unit,
    onCancelBatch: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "قائمة الروابط المجمّعة (Auto-Batch)",
                    style = MaterialTheme.typography.titleMedium
                )
                MetaChip(
                    text = "فاصل: ${interDelaySec}ث",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            OutlinedTextField(
                value = batchInput,
                onValueChange = onBatchInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .testTag("batch_urls_input"),
                placeholder = {
                    Text(
                        text = "ضع رابطاً في كل سطر...\nhttps://ds2play.com/e/...\nhttps://mixdrop.co/e/...",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                maxLines = 8
            )

            Text(
                text = "قاعدة اختيار الجودة الآلية:",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QualityPreferencePolicy.entries.forEach { policy ->
                    FilterChip(
                        selected = policy == activePolicy,
                        onClick = { onPolicyChange(policy) },
                        label = { Text(policy.shortTag) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            if (isRunning) {
                Button(
                    onClick = onCancelBatch,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("cancel_batch_btn")
                ) {
                    Icon(Icons.Rounded.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إيقاف المعالجة المجمّعة")
                }
            } else {
                Button(
                    onClick = onStartBatch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("start_batch_btn")
                ) {
                    Icon(Icons.Rounded.AutoMode, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("بدء الفحص الآلي وإرسال 360p للطابور")
                }
            }
        }
    }
}

@Composable
private fun ReportOverviewCard(
    report: ScrapeInspectionReport,
    onToggleDiagnostics: () -> Unit,
    onSendEpisodesToBatch: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.pageTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "الروابط المستخرجة: ${report.resolvedVideos.size} • الزمن: ${report.durationMs}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onToggleDiagnostics,
                    modifier = Modifier.testTag("open_diagnostics_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Terminal,
                        contentDescription = "عرض سجل الفحص التقني",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (report.isOfflineSample) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "وضع المختبر المحلي: تم تحليل Dood (/pass_md5/) وMixDrop (Unpacker) وM3U8 فعلياً للمعاينة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            if (report.episodeLinks.isNotEmpty()) {
                OutlinedButton(
                    onClick = onSendEpisodesToBatch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("send_episodes_to_batch_btn")
                ) {
                    Icon(Icons.Rounded.PlaylistAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("نقل ${report.episodeLinks.size} حلقة إلى الوضع الآلي (360p)")
                }
            }
        }
    }
}

@Composable
private fun ExtractedVideoCard(
    video: ExtractedVideo,
    isRecommended: Boolean,
    onQueueDownload: () -> Unit,
    onCopyUrl: () -> Unit,
    onShowDetails: () -> Unit
) {
    val semantics = LocalStreamGrabSemantics.current
    val cardBorder = when {
        video.is360pPreferred -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        isRecommended -> BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = cardBorder
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HostBadge(host = video.host)
                    ResolutionPill(
                        label = video.quality.resolution.label,
                        is360p = video.is360pPreferred
                    )
                }
                if (isRecommended) {
                    MetaChip(
                        text = "موصى به",
                        tint = semantics.success
                    )
                }
            }

            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetaChip(text = "الحجم: ${formatBytes(video.quality.contentLengthBytes)}")
                if (video.quality.supportsRange) {
                    MetaChip(text = "يدعم الاستكمال (Range)", tint = semantics.success)
                }
                if (video.quality.width != null && video.quality.height != null) {
                    MetaChip(text = "${video.quality.width}x${video.quality.height}")
                }
                if (!video.subtitleUrl.isNullOrBlank()) {
                    MetaChip(
                        text = "ترجمة مدمجة",
                        tint = MaterialTheme.colorScheme.secondary,
                        hasSubtitleIcon = true
                    )
                }
                if (video.quality.inspectionSource.isNotBlank()) {
                    MetaChip(text = video.quality.inspectionSource)
                }
            }

            Text(
                text = video.directUrl,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onQueueDownload,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("queue_download_btn_${video.id}")
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إرسال للطابور")
                }

                IconButton(
                    onClick = onCopyUrl,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "نسخ الرابط المباشر")
                }

                IconButton(
                    onClick = onShowDetails,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = "عرض الترويسات والتفاصيل")
                }
            }
        }
    }
}

@Composable
private fun BatchProgressRowCard(item: BatchItemProgress) {
    val semantics = LocalStreamGrabSemantics.current
    val statusColor = when (item.state) {
        BatchItemState.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        BatchItemState.RESOLVING -> MaterialTheme.colorScheme.primary
        BatchItemState.QUEUED -> semantics.success
        BatchItemState.SKIPPED_NO_360P -> MaterialTheme.colorScheme.secondary
        BatchItemState.FAILED -> MaterialTheme.colorScheme.error
    }

    Card(
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (item.state == BatchItemState.RESOLVING) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = if (item.state == BatchItemState.FAILED) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.resolvedTitle ?: item.url,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
            }
            item.chosenQualityLabel?.let { q ->
                ResolutionPill(label = q, is360p = item.is360pMatch)
            }
        }
    }
}

@Composable
private fun ErrorBannerCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun EmptyReportHintCard(hasEpisodes: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = if (hasEpisodes) {
                "هذه الصفحة تحتوي على قائمة حلقات دون مشغلات مباشرة؛ اضغط على الزر أعلاه لنقل الحلقات إلى الوضع الآلي."
            } else {
                "لم يتم العثور على روابط Doodstream أو MixDrop أو M3U8 في هذه الصفحة."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun FirstLaunchGuideCard(onRunOfflineLab: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "جاهز لاستخراج الروابط وفحص 360p",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "• DoodExtractor: يتتبع التوجيهات، يفحص <title> بحثاً عن الدقة، ويحل مسار /pass_md5/ مع createHashTable.\n" +
                    "• MixDropExtractor: يفك سكربت MDCore عبر Unpacker ويستخرج Core.wurl والترجمة Core.remotesub.\n" +
                    "• QualityInspector: يحلّل M3U8 ويفحص Content-Length عبر HEAD/Range مع تمييز دقة 360p.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onRunOfflineLab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تشغيل عينة المختبر المحلي للتحقق")
            }
        }
    }
}

@Composable
private fun VideoTechnicalDetailsDialog(
    video: ExtractedVideo,
    onDismiss: () -> Unit,
    onCopyAll: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "التفاصيل التقنية والترويسات", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "المصدر: ${video.host.displayName}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "الدقة: ${video.quality.resolution.label} (${video.quality.inspectionSource})",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "الحجم: ${formatBytes(video.quality.contentLengthBytes)} • Range: ${if (video.quality.supportsRange) "نعم" else "لا"}",
                    style = MaterialTheme.typography.bodySmall
                )
                HorizontalDivider()
                Text(text = "الرابط المباشر:", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = video.directUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!video.subtitleUrl.isNullOrBlank()) {
                    Text(text = "رابط الترجمة (remotesub):", style = MaterialTheme.typography.labelLarge)
                    Text(text = video.subtitleUrl, style = MaterialTheme.typography.labelSmall)
                }
                if (video.headers.isNotEmpty()) {
                    Text(text = "الترويسات المطلوبة (Headers):", style = MaterialTheme.typography.labelLarge)
                    video.headers.forEach { (k, v) ->
                        Text(
                            text = "$k: $v",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onCopyAll) {
                Text("نسخ الكل")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
private fun DiagnosticsLogDialog(
    report: ScrapeInspectionReport,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("سجل خطوات الاستخراج والفحص", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                report.diagnostics.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("StreamGrab URL", text))
}

private fun readClipboardText(context: Context): String? {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = cm.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()?.trim()
}
