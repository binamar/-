package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.ScheduleSend
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.CalendarViewMode
import com.example.domain.model.PlannerTask
import com.example.domain.model.TimeBlock
import com.example.ui.components.MetaPill
import com.example.ui.components.priorityColor
import com.example.ui.i18n.appStrings
import com.example.ui.theme.WorkloadHeavyColor
import com.example.ui.theme.WorkloadLightColor
import com.example.ui.theme.WorkloadModerateColor
import com.example.ui.viewmodel.CalendarViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private fun workloadColor(count: Int): Color = when {
    count <= 0 -> Color.Transparent
    count <= 2 -> WorkloadLightColor
    count <= 5 -> WorkloadModerateColor
    else -> WorkloadHeavyColor
}

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val strings = appStrings()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var reschedulingTask by remember { mutableStateOf<PlannerTask?>(null) }

    val locale = if (strings.isArabic) Locale("ar") else Locale.ENGLISH

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
                    text = strings.calendarTitle,
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            // Instant View Switcher preserving selectedDate
            item {
                val modes = CalendarViewMode.entries
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calendar_view_switcher")
                ) {
                    modes.forEachIndexed { idx, mode ->
                        val label = when (mode) {
                            CalendarViewMode.DAY -> strings.viewDay
                            CalendarViewMode.WEEK -> strings.viewWeek
                            CalendarViewMode.MONTH -> strings.viewMonth
                        }
                        SegmentedButton(
                            selected = uiState.viewMode == mode,
                            onClick = { viewModel.switchViewMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = modes.size)
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            // Date navigator + View Mode body
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = {
                                    val prev = when (uiState.viewMode) {
                                        CalendarViewMode.DAY -> uiState.selectedDate.minusDays(1)
                                        CalendarViewMode.WEEK -> uiState.selectedDate.minusWeeks(1)
                                        CalendarViewMode.MONTH -> uiState.selectedDate.minusMonths(1)
                                    }
                                    viewModel.selectDate(prev)
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous")
                            }

                            Text(
                                text = uiState.selectedDate.format(
                                    DateTimeFormatter.ofPattern("MMMM yyyy", locale)
                                ),
                                style = MaterialTheme.typography.titleMedium
                            )

                            IconButton(
                                onClick = {
                                    val next = when (uiState.viewMode) {
                                        CalendarViewMode.DAY -> uiState.selectedDate.plusDays(1)
                                        CalendarViewMode.WEEK -> uiState.selectedDate.plusWeeks(1)
                                        CalendarViewMode.MONTH -> uiState.selectedDate.plusMonths(1)
                                    }
                                    viewModel.selectDate(next)
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next")
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        when (uiState.viewMode) {
                            CalendarViewMode.DAY -> {
                                val load = uiState.workloadByDate[uiState.selectedDate] ?: 0
                                val label = when {
                                    load <= 2 -> strings.workloadLight
                                    load <= 5 -> strings.workloadModerate
                                    else -> strings.workloadHeavy
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = uiState.selectedDate.format(
                                            DateTimeFormatter.ofPattern("EEEE, d MMM", locale)
                                        ),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    MetaPill(
                                        text = "$label ($load)",
                                        container = workloadColor(load.coerceAtLeast(1)).copy(alpha = 0.18f),
                                        content = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            CalendarViewMode.WEEK -> {
                                val weekStart = uiState.selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    (0..6).forEach { offset ->
                                        val date = weekStart.plusDays(offset.toLong())
                                        CalendarDayCell(
                                            date = date,
                                            isSelected = date == uiState.selectedDate,
                                            isCurrentMonth = true,
                                            workload = uiState.workloadByDate[date] ?: 0,
                                            onClick = { viewModel.selectDate(date) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            CalendarViewMode.MONTH -> {
                                val ym = YearMonth.from(uiState.selectedDate)
                                val firstOfMonth = ym.atDay(1)
                                val startOffset = firstOfMonth.dayOfWeek.value - 1
                                val totalCells = ((startOffset + ym.lengthOfMonth() + 6) / 7) * 7
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        (1..7).forEach { dow ->
                                            Text(
                                                text = strings.dayShort(dow),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                    for (rowStart in 0 until totalCells step 7) {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            for (col in 0..6) {
                                                val cellIndex = rowStart + col
                                                val date = firstOfMonth.plusDays((cellIndex - startOffset).toLong())
                                                CalendarDayCell(
                                                    date = date,
                                                    isSelected = date == uiState.selectedDate,
                                                    isCurrentMonth = date.month == ym.month,
                                                    workload = uiState.workloadByDate[date] ?: 0,
                                                    onClick = { viewModel.selectDate(date) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Selected Day Timeline (Tasks + Active Routines)
            item {
                val dateLabel = uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale))
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (uiState.tasksForSelectedDate.isEmpty() && uiState.routinesForSelectedDate.isEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Outlined.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(strings.emptyCalendarDay, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                items(uiState.tasksForSelectedDate, key = { "cal_task_${it.id}" }) { task ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (task.isCompleted) MaterialTheme.colorScheme.primary
                                        else priorityColor(task.priority).copy(alpha = 0.18f)
                                    )
                                    .clickable { viewModel.toggleTaskCompletion(task) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (task.isCompleted) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                )
                                val timeDisplay = task.dueTime?.format(DateTimeFormatter.ofPattern("HH:mm"))
                                    ?: strings.timeBlockName(task.timeBlock)
                                Text(
                                    text = timeDisplay,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { reschedulingTask = task }) {
                                Icon(Icons.Outlined.ScheduleSend, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.quickReschedule)
                            }
                        }
                    }
                }

                items(uiState.routinesForSelectedDate, key = { "cal_routine_${it.id}" }) { routine ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(routine.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    strings.routineCategoryName(routine.category),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            routine.reminderTime?.let {
                                Text(
                                    text = it.format(DateTimeFormatter.ofPattern("HH:mm")),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }

            // Unscheduled Pool
            item {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = "${strings.unscheduledPool} (${uiState.unscheduledTasks.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (uiState.unscheduledTasks.isEmpty()) {
                item {
                    Text(
                        text = strings.emptyUnscheduled,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(uiState.unscheduledTasks, key = { "pool_${it.id}" }) { task ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(task.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = strings.priorityName(task.priority),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = priorityColor(task.priority)
                                )
                            }
                            AssistChip(
                                onClick = {
                                    viewModel.quickReschedule(
                                        taskId = task.id,
                                        targetDate = uiState.selectedDate,
                                        timeBlock = TimeBlock.MORNING,
                                        dueTime = task.dueTime
                                    )
                                },
                                label = { Text(strings.assignToSelectedDate) }
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    reschedulingTask?.let { task ->
        QuickRescheduleSheet(
            task = task,
            onDismiss = { reschedulingTask = null },
            onConfirm = { newDate, newBlock ->
                viewModel.quickReschedule(task.id, newDate, newBlock, task.dueTime)
                reschedulingTask = null
            }
        )
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    isSelected: Boolean,
    isCurrentMonth: Boolean,
    workload: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isToday = date == LocalDate.now()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                !isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(width = 16.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (isSelected && workload > 0) MaterialTheme.colorScheme.onPrimary
                    else workloadColor(workload)
                )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun QuickRescheduleSheet(
    task: PlannerTask,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate?, TimeBlock) -> Unit
) {
    val strings = appStrings()
    val today = LocalDate.now()
    var targetDate by remember { mutableStateOf<LocalDate?>(task.dueDate ?: today) }
    var targetBlock by remember { mutableStateOf(task.timeBlock) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("${strings.quickReschedule}: ${task.title}", style = MaterialTheme.typography.titleLarge)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = targetDate == today,
                    onClick = { targetDate = today },
                    label = { Text(strings.todayWord) }
                )
                FilterChip(
                    selected = targetDate == today.plusDays(1),
                    onClick = { targetDate = today.plusDays(1) },
                    label = { Text(strings.tomorrowWord) }
                )
                FilterChip(
                    selected = targetDate == today.plusDays(3),
                    onClick = { targetDate = today.plusDays(3) },
                    label = { Text("+3d") }
                )
                FilterChip(
                    selected = targetDate == today.plusWeeks(1),
                    onClick = { targetDate = today.plusWeeks(1) },
                    label = { Text("+1w") }
                )
                FilterChip(
                    selected = targetDate == null,
                    onClick = { targetDate = null },
                    label = { Text(strings.noDateWord) }
                )
            }

            Text(strings.timeBlockLabel, style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeBlock.entries.forEach { block ->
                    FilterChip(
                        selected = targetBlock == block,
                        onClick = { targetBlock = block },
                        label = { Text(strings.timeBlockName(block)) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text(strings.cancel) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onConfirm(targetDate, targetBlock) }) {
                    Text(strings.save)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
