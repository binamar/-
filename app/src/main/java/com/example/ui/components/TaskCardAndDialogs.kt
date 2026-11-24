package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.domain.model.PlannerTask
import com.example.domain.model.Priority
import com.example.domain.model.Subtask
import com.example.domain.model.TaskTemplate
import com.example.domain.model.TimeBlock
import com.example.ui.i18n.appStrings
import com.example.ui.theme.LocalMinimalistMode
import com.example.ui.theme.PriorityHighColor
import com.example.ui.theme.PriorityLowColor
import com.example.ui.theme.PriorityMediumColor
import com.example.ui.theme.PriorityUrgentColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

fun priorityColor(priority: Priority): Color = when (priority) {
    Priority.LOW -> PriorityLowColor
    Priority.MEDIUM -> PriorityMediumColor
    Priority.HIGH -> PriorityHighColor
    Priority.URGENT -> PriorityUrgentColor
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskCard(
    task: PlannerTask,
    isExpanded: Boolean,
    onToggleComplete: () -> Unit,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLaunchFocus: () -> Unit,
    onSaveAsTemplate: () -> Unit,
    onAddSubtask: (String) -> Unit,
    onToggleSubtask: (Subtask) -> Unit,
    onDeleteSubtask: (Long) -> Unit,
    onReorderSubtasks: (List<Subtask>) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = appStrings()
    val minimalist = LocalMinimalistMode.current
    val accent = priorityColor(task.priority)
    val checkColor by animateColorAsState(
        targetValue = if (task.isCompleted) MaterialTheme.colorScheme.primary else accent,
        label = "checkColor"
    )
    var quickSubtaskText by remember(task.id) { mutableStateOf("") }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (task.isCompleted) 0.dp else 2.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (task.isRepeatedlyPostponed) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Priority stripe + circular completion button
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (task.isCompleted) checkColor else checkColor.copy(alpha = 0.14f)
                        )
                        .clickable(onClick = onToggleComplete)
                        .testTag("task_complete_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = strings.taskCompletedMsg,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(accent)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onToggleExpand)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (task.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (task.notes.isNotBlank() && !minimalist) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = task.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!minimalist && (task.dueTime != null || task.timeBlock != TimeBlock.ANYTIME || task.tags.isNotEmpty() || task.isRepeatedlyPostponed)) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (task.dueTime != null || task.timeBlock != TimeBlock.ANYTIME) {
                                val timeText = task.dueTime?.format(DateTimeFormatter.ofPattern("HH:mm"))
                                    ?: strings.timeBlockName(task.timeBlock)
                                MetaPill(
                                    text = timeText,
                                    container = MaterialTheme.colorScheme.secondaryContainer,
                                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                                    icon = Icons.Outlined.AccessTime
                                )
                            }
                            if (task.isRepeatedlyPostponed) {
                                MetaPill(
                                    text = "${strings.repeatedlyPostponed}${task.postponeCount}",
                                    container = MaterialTheme.colorScheme.errorContainer,
                                    content = MaterialTheme.colorScheme.onErrorContainer,
                                    icon = Icons.Outlined.WarningAmber
                                )
                            }
                            task.tags.forEach { tag ->
                                MetaPill(
                                    text = "#$tag",
                                    container = MaterialTheme.colorScheme.surfaceVariant,
                                    content = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (task.subtasks.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        val doneCount = task.subtasks.count { it.isCompleted }
                        val progress by animateFloatAsState(
                            targetValue = task.completionProgress,
                            animationSpec = spring(),
                            label = "subtaskProgress"
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                            Text(
                                text = "$doneCount/${task.subtasks.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!task.isCompleted) {
                        IconButton(
                            onClick = onLaunchFocus,
                            modifier = Modifier.testTag("task_focus_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayCircle,
                                contentDescription = strings.focusNow,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.testTag("task_expand_${task.id}")
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = strings.subtasksLabel
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(10.dp))

                    // Draggable subtasks list
                    DraggableSubtaskList(
                        subtasks = task.subtasks,
                        onToggleSubtask = onToggleSubtask,
                        onDeleteSubtask = onDeleteSubtask,
                        onReorder = onReorderSubtasks
                    )

                    // Quick sequential subtask entry
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = quickSubtaskText,
                            onValueChange = { quickSubtaskText = it },
                            placeholder = { Text(strings.addSubtaskHint, style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (quickSubtaskText.isNotBlank()) {
                                        onAddSubtask(quickSubtaskText.trim())
                                        quickSubtaskText = ""
                                    }
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_subtask_input_${task.id}"),
                            shape = MaterialTheme.shapes.small
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (quickSubtaskText.isNotBlank()) {
                                    onAddSubtask(quickSubtaskText.trim())
                                    quickSubtaskText = ""
                                }
                            },
                            modifier = Modifier.testTag("quick_subtask_add_${task.id}")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = strings.addTask)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onSaveAsTemplate) {
                            Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.saveAsTemplate)
                        }
                        TextButton(onClick = onEdit) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.editTask)
                        }
                        TextButton(onClick = onDelete) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(strings.delete, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableSubtaskList(
    subtasks: List<Subtask>,
    onToggleSubtask: (Subtask) -> Unit,
    onDeleteSubtask: (Long) -> Unit,
    onReorder: (List<Subtask>) -> Unit
) {
    val strings = appStrings()
    val localOrder = remember(subtasks) {
        mutableStateListOf<Subtask>().apply { addAll(subtasks) }
    }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        localOrder.forEachIndexed { index, sub ->
            val isDragging = draggingIndex == index
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffsetY else 0f
                        scaleX = if (isDragging) 1.02f else 1f
                        scaleY = if (isDragging) 1.02f else 1f
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isDragging) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DragHandle,
                    contentDescription = strings.dragToReorder,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(2.dp)
                        .pointerInput(localOrder.size) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    draggingIndex = index
                                    dragOffsetY = 0f
                                },
                                onDragEnd = {
                                    draggingIndex = -1
                                    dragOffsetY = 0f
                                    onReorder(localOrder.toList())
                                },
                                onDragCancel = {
                                    draggingIndex = -1
                                    dragOffsetY = 0f
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount
                                    val thresholdPx = 64f
                                    val currentIdx = draggingIndex
                                    if (currentIdx in localOrder.indices) {
                                        if (dragOffsetY > thresholdPx && currentIdx < localOrder.lastIndex) {
                                            val item = localOrder.removeAt(currentIdx)
                                            localOrder.add(currentIdx + 1, item)
                                            draggingIndex = currentIdx + 1
                                            dragOffsetY -= thresholdPx
                                        } else if (dragOffsetY < -thresholdPx && currentIdx > 0) {
                                            val item = localOrder.removeAt(currentIdx)
                                            localOrder.add(currentIdx - 1, item)
                                            draggingIndex = currentIdx - 1
                                            dragOffsetY += thresholdPx
                                        }
                                    }
                                }
                            )
                        }
                )
                Checkbox(
                    checked = sub.isCompleted,
                    onCheckedChange = { onToggleSubtask(sub) }
                )
                Text(
                    text = sub.title,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (sub.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onDeleteSubtask(sub.id) }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = strings.delete,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MetaPill(
    text: String,
    container: Color,
    content: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = container
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = content,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
    existingTask: PlannerTask?,
    defaultDate: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        notes: String,
        priority: Priority,
        dueDate: LocalDate?,
        dueTime: LocalTime?,
        timeBlock: TimeBlock,
        tags: List<String>,
        reminderMinutesBefore: Int?
    ) -> Unit
) {
    val strings = appStrings()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(existingTask?.title.orEmpty()) }
    var notes by remember { mutableStateOf(existingTask?.notes.orEmpty()) }
    var priority by remember { mutableStateOf(existingTask?.priority ?: Priority.MEDIUM) }
    var dueDate by remember { mutableStateOf<LocalDate?>(existingTask?.dueDate ?: defaultDate) }
    var timeBlock by remember { mutableStateOf(existingTask?.timeBlock ?: TimeBlock.ANYTIME) }
    var timeHourText by remember { mutableStateOf(existingTask?.dueTime?.hour?.toString()?.padStart(2, '0') ?: "") }
    var timeMinText by remember { mutableStateOf(existingTask?.dueTime?.minute?.toString()?.padStart(2, '0') ?: "") }
    var tagsText by remember { mutableStateOf(existingTask?.tags?.joinToString(", ").orEmpty()) }
    var reminderOffset by remember { mutableStateOf(existingTask?.reminderMinutesBefore) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (existingTask == null) strings.newTask else strings.editTask,
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(strings.titleLabel) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_title_input")
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(strings.notesLabel) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Text(strings.priorityLabel, style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(strings.priorityName(p)) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Flag,
                                contentDescription = null,
                                tint = priorityColor(p),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            Text(strings.dueDateLabel, style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val today = LocalDate.now()
                FilterChip(
                    selected = dueDate == today,
                    onClick = { dueDate = today },
                    label = { Text(strings.todayWord) }
                )
                FilterChip(
                    selected = dueDate == today.plusDays(1),
                    onClick = { dueDate = today.plusDays(1) },
                    label = { Text(strings.tomorrowWord) }
                )
                FilterChip(
                    selected = dueDate == today.plusDays(3),
                    onClick = { dueDate = today.plusDays(3) },
                    label = { Text("+3d") }
                )
                FilterChip(
                    selected = dueDate == null,
                    onClick = { dueDate = null },
                    label = { Text(strings.noDateWord) }
                )
            }

            Text(strings.timeBlockLabel, style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeBlock.entries.forEach { tb ->
                    FilterChip(
                        selected = timeBlock == tb,
                        onClick = { timeBlock = tb },
                        label = { Text(strings.timeBlockName(tb)) }
                    )
                }
            }

            Text(strings.dueTimeLabel, style = MaterialTheme.typography.labelLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = timeHourText,
                    onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) timeHourText = it },
                    label = { Text("HH") },
                    singleLine = true,
                    modifier = Modifier.width(84.dp)
                )
                Text(":", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = timeMinText,
                    onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) timeMinText = it },
                    label = { Text("MM") },
                    singleLine = true,
                    modifier = Modifier.width(84.dp)
                )
                if (timeHourText.isNotEmpty() || timeMinText.isNotEmpty()) {
                    TextButton(onClick = {
                        timeHourText = ""
                        timeMinText = ""
                    }) {
                        Text(strings.clearTime)
                    }
                }
            }

            Text(strings.reminderLabel, style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to "Off", 0 to "0m", 10 to "10m", 30 to "30m").forEach { (offset, label) ->
                    FilterChip(
                        selected = reminderOffset == offset,
                        onClick = { reminderOffset = offset },
                        label = { Text(label) }
                    )
                }
            }

            OutlinedTextField(
                value = tagsText,
                onValueChange = { tagsText = it },
                label = { Text(strings.tagsLabel) },
                placeholder = { Text(strings.tagsHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text(strings.cancel) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val h = timeHourText.toIntOrNull()?.takeIf { it in 0..23 }
                        val m = timeMinText.toIntOrNull()?.takeIf { it in 0..59 } ?: 0
                        val parsedTime = if (h != null) LocalTime.of(h, m) else null
                        val parsedTags = tagsText.split(",", "،", "#")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .distinct()
                        onSave(
                            title,
                            notes,
                            priority,
                            dueDate,
                            parsedTime,
                            timeBlock,
                            parsedTags,
                            reminderOffset
                        )
                        onDismiss()
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.testTag("editor_save_button")
                ) {
                    Text(strings.save)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatePickerSheet(
    templates: List<TaskTemplate>,
    onDismiss: () -> Unit,
    onSelectTemplate: (TaskTemplate) -> Unit
) {
    val strings = appStrings()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(strings.templates, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(templates) { tpl ->
                    val displayName = if (strings.isArabic && tpl.nameAr.isNotBlank()) tpl.nameAr else tpl.name
                    val subs = if (strings.isArabic && tpl.defaultSubtasksAr.isNotEmpty()) tpl.defaultSubtasksAr else tpl.defaultSubtasks
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectTemplate(tpl)
                                onDismiss()
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(displayName, style = MaterialTheme.typography.titleMedium)
                                AssistChip(
                                    onClick = {
                                        onSelectTemplate(tpl)
                                        onDismiss()
                                    },
                                    label = { Text(strings.priorityName(tpl.priority)) }
                                )
                            }
                            if (subs.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = subs.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolloverSheet(
    overdueTasks: List<PlannerTask>,
    onDismiss: () -> Unit,
    onRolloverSelected: (List<Long>) -> Unit,
    onRolloverAll: () -> Unit
) {
    val strings = appStrings()
    val selectedIds = remember(overdueTasks) {
        mutableStateListOf<Long>().apply { addAll(overdueTasks.map { it.id }) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(strings.rolloverBannerAction, style = MaterialTheme.typography.headlineSmall)
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(overdueTasks, key = { it.id }) { task ->
                    val checked = selectedIds.contains(task.id)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (checked) selectedIds.remove(task.id) else selectedIds.add(task.id)
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                if (checked) selectedIds.remove(task.id) else selectedIds.add(task.id)
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.title, style = MaterialTheme.typography.bodyLarge)
                            task.dueDate?.let {
                                Text(
                                    text = it.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onRolloverSelected(selectedIds.toList())
                        onDismiss()
                    },
                    enabled = selectedIds.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.rolloverSelected)
                }
                Button(
                    onClick = {
                        onRolloverAll()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.rolloverAll)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
