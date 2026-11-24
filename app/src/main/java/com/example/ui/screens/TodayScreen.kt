package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.PlannerTask
import com.example.domain.model.TimeBlock
import com.example.ui.components.MetaPill
import com.example.ui.components.RolloverSheet
import com.example.ui.components.TaskCard
import com.example.ui.components.TaskEditorSheet
import com.example.ui.components.TemplatePickerSheet
import com.example.ui.i18n.appStrings
import com.example.ui.theme.LocalMinimalistMode
import com.example.ui.viewmodel.TodayEvent
import com.example.ui.viewmodel.TodayViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(
    onOpenFocus: (Long?) -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val strings = appStrings()
    val minimalist = LocalMinimalistMode.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val expandedIds by viewModel.expandedTaskIds.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var editingTask by remember { mutableStateOf<PlannerTask?>(null) }
    var showNewTaskSheet by remember { mutableStateOf(false) }
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var showRolloverSheet by remember { mutableStateOf(false) }
    var showCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TodayEvent.ShowUndoComplete -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "${strings.taskCompletedMsg}: ${event.taskTitle}",
                        actionLabel = strings.undo,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoCompletion(event.taskId)
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewTaskSheet = true },
                modifier = Modifier.testTag("today_fab_add")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = strings.newTask)
            }
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
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header + Hero Progress Card
                item {
                    val locale = if (strings.isArabic) Locale("ar") else Locale.ENGLISH
                    val dateText = uiState.today.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.todayTitle,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "$dateText • ${strings.todaySubtitle}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            onClick = { onOpenFocus(null) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.testTag("today_focus_mode_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Timer,
                                    contentDescription = strings.focusTitle,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = strings.focusNow,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                if (!minimalist) {
                    item {
                        TodayHeroProgressCard(
                            completionRatio = uiState.completionRatio,
                            doneTasks = uiState.completedTasks.size,
                            totalTasks = uiState.todayTasks.size,
                            doneRoutines = uiState.todayRoutines.count { it.isCompletedToday },
                            totalRoutines = uiState.todayRoutines.size
                        )
                    }
                }

                // Natural Language Quick Input Bar + Live Parsed Preview
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = uiState.quickInput,
                                    onValueChange = viewModel::onQuickInputChange,
                                    placeholder = {
                                        Text(
                                            strings.quickAddHint,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { viewModel.submitQuickAdd() }),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("nlp_quick_input"),
                                    shape = MaterialTheme.shapes.medium
                                )
                                IconButton(
                                    onClick = { showTemplatesSheet = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .testTag("templates_button")
                                ) {
                                    Icon(
                                        Icons.Outlined.ViewCarousel,
                                        contentDescription = strings.templates,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                IconButton(
                                    onClick = viewModel::submitQuickAdd,
                                    enabled = uiState.quickInput.isNotBlank(),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (uiState.quickInput.isNotBlank()) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        )
                                        .testTag("nlp_submit_button")
                                ) {
                                    Icon(
                                        Icons.Rounded.ArrowUpward,
                                        contentDescription = strings.addTask,
                                        tint = if (uiState.quickInput.isNotBlank()) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }

                            AnimatedVisibility(visible = uiState.parsedPreview != null) {
                                uiState.parsedPreview?.let { parsed ->
                                    FlowRow(
                                        modifier = Modifier.padding(top = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        parsed.dueDate?.let {
                                            MetaPill(
                                                text = it.toString(),
                                                container = MaterialTheme.colorScheme.primaryContainer,
                                                content = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        parsed.dueTime?.let {
                                            MetaPill(
                                                text = it.format(DateTimeFormatter.ofPattern("HH:mm")),
                                                container = MaterialTheme.colorScheme.secondaryContainer,
                                                content = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        } ?: run {
                                            if (parsed.timeBlock != TimeBlock.ANYTIME) {
                                                MetaPill(
                                                    text = strings.timeBlockName(parsed.timeBlock),
                                                    container = MaterialTheme.colorScheme.secondaryContainer,
                                                    content = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                        MetaPill(
                                            text = strings.priorityName(parsed.priority),
                                            container = MaterialTheme.colorScheme.tertiaryContainer,
                                            content = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        parsed.tags.forEach { tag ->
                                            MetaPill(
                                                text = "#$tag",
                                                container = MaterialTheme.colorScheme.surfaceVariant,
                                                content = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Selective / Bulk Rollover Banner
                if (uiState.overdueTasks.isNotEmpty()) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rollover_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "${uiState.overdueTasks.size} ${strings.rolloverBannerTitle}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { showRolloverSheet = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        contentColor = MaterialTheme.colorScheme.onTertiary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(strings.rolloverBannerAction)
                                }
                            }
                        }
                    }
                }

                // Today's Routines quick strip
                if (uiState.todayRoutines.isNotEmpty()) {
                    item {
                        Text(
                            text = strings.todayRoutinesSection,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.todayRoutines, key = { it.routine.id }) { item ->
                                val done = item.isCompletedToday
                                Surface(
                                    onClick = { viewModel.toggleRoutineToday(item.routine.id) },
                                    shape = MaterialTheme.shapes.medium,
                                    color = if (done) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    tonalElevation = if (done) 0.dp else 2.dp
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (done) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (done) {
                                                Icon(
                                                    Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = item.routine.title,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                            if (!minimalist) {
                                                Text(
                                                    text = "${item.currentStreak} ${strings.streakDays}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Active Tasks Section
                item {
                    Text(
                        text = strings.dueTasksSection,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (uiState.activeTasks.isEmpty() && uiState.completedTasks.isEmpty()) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(strings.emptyTodayTitle, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    strings.emptyTodaySubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.activeTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            isExpanded = expandedIds.contains(task.id),
                            onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                            onToggleExpand = { viewModel.toggleExpanded(task.id) },
                            onEdit = { editingTask = task },
                            onDelete = { viewModel.deleteTask(task.id) },
                            onLaunchFocus = { onOpenFocus(task.id) },
                            onSaveAsTemplate = { viewModel.saveTaskAsTemplate(task) },
                            onAddSubtask = { subTitle -> viewModel.addSubtask(task.id, subTitle) },
                            onToggleSubtask = { sub -> viewModel.toggleSubtask(sub) },
                            onDeleteSubtask = { subId -> viewModel.deleteSubtask(subId) },
                            onReorderSubtasks = { ordered -> viewModel.reorderSubtasks(task.id, ordered) }
                        )
                    }
                }

                // Completed Tasks Collapsible Section
                if (uiState.completedTasks.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCompleted = !showCompleted }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "${strings.completedSection} (${uiState.completedTasks.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { showCompleted = !showCompleted }) {
                                Text(if (showCompleted) "−" else "+")
                            }
                        }
                    }
                    if (showCompleted) {
                        items(uiState.completedTasks, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                isExpanded = expandedIds.contains(task.id),
                                onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                                onToggleExpand = { viewModel.toggleExpanded(task.id) },
                                onEdit = { editingTask = task },
                                onDelete = { viewModel.deleteTask(task.id) },
                                onLaunchFocus = { onOpenFocus(task.id) },
                                onSaveAsTemplate = { viewModel.saveTaskAsTemplate(task) },
                                onAddSubtask = { subTitle -> viewModel.addSubtask(task.id, subTitle) },
                                onToggleSubtask = { sub -> viewModel.toggleSubtask(sub) },
                                onDeleteSubtask = { subId -> viewModel.deleteSubtask(subId) },
                                onReorderSubtasks = { ordered -> viewModel.reorderSubtasks(task.id, ordered) }
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showNewTaskSheet || editingTask != null) {
        TaskEditorSheet(
            existingTask = editingTask,
            defaultDate = uiState.today,
            onDismiss = {
                showNewTaskSheet = false
                editingTask = null
            },
            onSave = { title, notes, priority, dueDate, dueTime, timeBlock, tags, reminderOffset ->
                viewModel.saveTask(
                    existingTask = editingTask,
                    title = title,
                    notes = notes,
                    priority = priority,
                    dueDate = dueDate,
                    dueTime = dueTime,
                    timeBlock = timeBlock,
                    tags = tags,
                    reminderMinutesBefore = reminderOffset
                )
            }
        )
    }

    if (showTemplatesSheet) {
        TemplatePickerSheet(
            templates = uiState.templates,
            onDismiss = { showTemplatesSheet = false },
            onSelectTemplate = { tpl ->
                viewModel.createTaskFromTemplate(tpl, strings.isArabic)
            }
        )
    }

    if (showRolloverSheet) {
        RolloverSheet(
            overdueTasks = uiState.overdueTasks,
            onDismiss = { showRolloverSheet = false },
            onRolloverSelected = { ids -> viewModel.rolloverSelectedToToday(ids) },
            onRolloverAll = { viewModel.rolloverAllToToday() }
        )
    }
}

@Composable
private fun TodayHeroProgressCard(
    completionRatio: Float,
    doneTasks: Int,
    totalTasks: Int,
    doneRoutines: Int,
    totalRoutines: Int
) {
    val strings = appStrings()
    val animatedRatio by animateFloatAsState(
        targetValue = completionRatio,
        animationSpec = spring(),
        label = "heroRatio"
    )
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.surfaceVariant

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            primary.copy(alpha = 0.08f),
                            secondary.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 7.dp.toPx()
                        drawArc(
                            color = track,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = primary,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedRatio,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "${(animatedRatio * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$doneTasks / $totalTasks ${strings.dueTasksSection}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "$doneRoutines / $totalRoutines ${strings.todayRoutinesSection}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
