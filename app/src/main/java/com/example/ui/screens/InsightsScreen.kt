package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.DailyCompletionPoint
import com.example.domain.model.InsightPeriod
import com.example.domain.model.InsightTypeFilter
import com.example.domain.model.TimeOfDayBucket
import com.example.ui.i18n.appStrings
import com.example.ui.viewmodel.InsightsViewModel
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(
    onOpenFocus: (Long) -> Unit,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val strings = appStrings()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val summary = uiState.summary

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = strings.insightsTitle,
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            // Period Switcher (Weekly / Monthly)
            item {
                val periods = InsightPeriod.entries
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("insights_period_switcher")
                ) {
                    periods.forEachIndexed { idx, p ->
                        SegmentedButton(
                            selected = uiState.period == p,
                            onClick = { viewModel.setPeriod(p) },
                            shape = SegmentedButtonDefaults.itemShape(idx, periods.size)
                        ) {
                            Text(if (p == InsightPeriod.WEEKLY) strings.weeklyPeriod else strings.monthlyPeriod)
                        }
                    }
                }
            }

            // Filter by Type & Tag
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(InsightTypeFilter.entries) { filter ->
                        val label = when (filter) {
                            InsightTypeFilter.ALL -> strings.filterAll
                            InsightTypeFilter.TASKS -> strings.filterTasks
                            InsightTypeFilter.ROUTINES -> strings.filterRoutines
                        }
                        FilterChip(
                            selected = uiState.typeFilter == filter,
                            onClick = { viewModel.setTypeFilter(filter) },
                            label = { Text(label) }
                        )
                    }
                    if (summary.availableTags.isNotEmpty()) {
                        item {
                            FilterChip(
                                selected = uiState.selectedTag == null,
                                onClick = { viewModel.selectTag(null) },
                                label = { Text(strings.allTags) }
                            )
                        }
                        items(summary.availableTags) { tag ->
                            FilterChip(
                                selected = uiState.selectedTag == tag,
                                onClick = {
                                    viewModel.selectTag(if (uiState.selectedTag == tag) null else tag)
                                },
                                label = { Text("#$tag") }
                            )
                        }
                    }
                }
            }

            // KPI Summary Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.weight(1.4f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = strings.completionRateCard,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "${(summary.completionRate * 100).roundToInt()}%",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            val positive = summary.deltaPercent >= 0
                            val sign = if (positive) "+" else ""
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (positive) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                                    contentDescription = null,
                                    tint = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "$sign${summary.deltaPercent}% ${strings.vsPreviousPeriod}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(strings.focusMinutesCard, style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        "${summary.focusMinutesTotal} ${strings.minutesShort}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(strings.bestStreakCard, style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        "${summary.bestActiveStreak} ${strings.streakDays}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Daily Trend Canvas Chart
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(strings.dailyTrendTitle, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(14.dp))
                        DailyCompletionBarChart(
                            points = summary.dailyPoints.takeLast(14),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        )
                    }
                }
            }

            // Time of Day Distribution
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(strings.timeOfDayTitle, style = MaterialTheme.typography.titleMedium)
                        val maxBucket = summary.timeOfDayDistribution.values.maxOrNull()?.coerceAtLeast(1) ?: 1
                        TimeOfDayBucket.entries.forEach { bucket ->
                            val count = summary.timeOfDayDistribution[bucket] ?: 0
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = strings.timeOfDayName(bucket),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.width(84.dp)
                                )
                                LinearProgressIndicator(
                                    progress = { count.toFloat() / maxBucket.toFloat() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                )
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Repeatedly Postponed Flag Alerts
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = if (summary.repeatedlyPostponedTasks.isNotEmpty()) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(strings.postponedAlertsTitle, style = MaterialTheme.typography.titleMedium)
                        }
                        if (summary.repeatedlyPostponedTasks.isEmpty()) {
                            Text(
                                text = strings.noPostponedAlerts,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            summary.repeatedlyPostponedTasks.forEach { task ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                                        .padding(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(task.title, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "${strings.repeatedlyPostponed}${task.postponeCount}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    AssistChip(
                                        onClick = { onOpenFocus(task.id) },
                                        label = { Text(strings.focusNow) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun DailyCompletionBarChart(
    points: List<DailyCompletionPoint>,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas
        val count = points.size
        val gap = 8.dp.toPx()
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(6f)
        points.forEachIndexed { idx, point ->
            val left = idx * (barWidth + gap)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(left, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
            val fillHeight = (size.height * point.rate).coerceAtLeast(if (point.completed > 0) 8f else 0f)
            if (fillHeight > 0f) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, size.height - fillHeight),
                    size = Size(barWidth, fillHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }
    }
}
