package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.i18n.appStrings
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.FocusViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FocusScreen(
    initialTaskId: Long?,
    onBack: () -> Unit,
    viewModel: FocusViewModel = hiltViewModel()
) {
    val strings = appStrings()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showTaskPicker by remember { mutableStateOf(false) }

    LaunchedEffect(initialTaskId) {
        if (initialTaskId != null) {
            viewModel.linkTask(initialTaskId)
        }
    }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.focusTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("focus_back_btn")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.cancel)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Focus / Flexible Break mode switcher
                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !uiState.isBreakMode,
                            onClick = { viewModel.selectPreset(25, isBreak = false) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) { Text(strings.focusSessionTab) }
                        SegmentedButton(
                            selected = uiState.isBreakMode,
                            onClick = { viewModel.selectPreset(5, isBreak = true) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) { Text(strings.breakSessionTab) }
                    }
                }

                // Linked Task Selector Card
                item {
                    Surface(
                        onClick = { showTaskPicker = true },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("focus_link_task_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.linkedTaskLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = uiState.linkedTask?.title ?: strings.noTaskLinked,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            TextButton(onClick = { showTaskPicker = true }) {
                                Text(strings.selectTaskToLink)
                            }
                        }
                    }
                }

                // Circular Timer Ring
                item {
                    val animatedProgress by animateFloatAsState(
                        targetValue = uiState.progress,
                        animationSpec = spring(),
                        label = "focusProgress"
                    )
                    val ringColor = if (uiState.isBreakMode) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant

                    val mins = uiState.remainingSeconds / 60
                    val secs = uiState.remainingSeconds % 60
                    val formattedTime = "%02d:%02d".format(mins, secs)

                    Box(
                        modifier = Modifier
                            .size(230.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 14.dp.toPx()
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("focus_timer_display")
                            )
                            Text(
                                text = if (uiState.isBreakMode) strings.breakSessionTab else strings.focusSessionTab,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Presets + Custom stepper
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = if (uiState.isBreakMode) listOf(5, 10, 15) else listOf(15, 25, 45)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            presets.forEach { preset ->
                                FilterChip(
                                    selected = uiState.targetMinutes == preset,
                                    onClick = { viewModel.selectPreset(preset, uiState.isBreakMode) },
                                    label = { Text("$preset ${strings.minutesShort}") }
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.selectPreset(
                                        (uiState.targetMinutes - 5).coerceAtLeast(5),
                                        uiState.isBreakMode
                                    )
                                }
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "-5")
                            }
                            IconButton(
                                onClick = {
                                    viewModel.selectPreset(
                                        (uiState.targetMinutes + 5).coerceAtMost(120),
                                        uiState.isBreakMode
                                    )
                                }
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "+5")
                            }
                        }
                    }
                }

                // Controls: Start/Pause, Reset, +5m, Flexible Break
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = viewModel::resetTimer) {
                            Icon(Icons.Rounded.Refresh, contentDescription = strings.resetFocus)
                        }
                        Button(
                            onClick = viewModel::startOrPause,
                            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                            modifier = Modifier.testTag("focus_start_pause_btn")
                        ) {
                            Icon(
                                imageVector = if (uiState.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (uiState.isRunning) strings.pauseFocus else strings.startFocus)
                        }
                        OutlinedButton(onClick = viewModel::extendFiveMinutes) {
                            Text(strings.extendFiveMin)
                        }
                    }
                }

                // Direct Task Completion & Flexible Break row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!uiState.isBreakMode) {
                            OutlinedButton(
                                onClick = { viewModel.startFlexibleBreak(5) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Coffee, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.flexibleBreak)
                            }
                        }
                        if (uiState.linkedTask != null) {
                            Button(
                                onClick = viewModel::completeLinkedTaskFromFocus,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("focus_complete_task_btn")
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.completeLinkedTask)
                            }
                        }
                    }
                }

                // Session Log per Task
                item {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(strings.sessionLogTitle, style = MaterialTheme.typography.titleMedium)
                    }
                }

                if (uiState.sessionLogs.isEmpty()) {
                    item {
                        Text(
                            text = strings.emptySessionLog,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    items(uiState.sessionLogs, key = { it.id }) { session ->
                        val timeStr = Instant.ofEpochMilli(session.completedAt)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = session.taskTitleSnapshot.ifBlank { strings.noTaskLinked },
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = timeStr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${session.durationMinutes} ${strings.minutesShort}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (session.isBreak) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTaskPicker) {
        ModalBottomSheet(onDismissRequest = { showTaskPicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(strings.selectTaskToLink, style = MaterialTheme.typography.headlineSmall)
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.linkTask(null)
                            showTaskPicker = false
                        }
                ) {
                    Text(
                        text = strings.noTaskLinked,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(uiState.availableTasks, key = { it.id }) { task ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.linkTask(task.id)
                                    showTaskPicker = false
                                }
                        ) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
