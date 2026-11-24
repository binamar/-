package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Routine
import com.example.domain.model.RoutineCategory
import com.example.domain.model.RoutineDayVariation
import com.example.domain.model.RoutineWithStats
import com.example.ui.components.MetaPill
import com.example.ui.i18n.appStrings
import com.example.ui.theme.LocalMinimalistMode
import com.example.ui.viewmodel.RoutinesViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun RoutinesScreen(
    viewModel: RoutinesViewModel = hiltViewModel()
) {
    val strings = appStrings()
    val minimalist = LocalMinimalistMode.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf<RoutineCategory?>(null) }
    var editingRoutine by remember { mutableStateOf<Routine?>(null) }
    var showCreateSheet by remember { mutableStateOf(false) }

    val filtered = remember(uiState.routines, selectedCategory) {
        uiState.routines.filter { selectedCategory == null || it.routine.category == selectedCategory }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                modifier = Modifier.testTag("routines_fab_add")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = strings.newRoutine)
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = strings.routinesTitle,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                // Category filter strip
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { selectedCategory = null },
                                label = { Text(strings.filterAll) }
                            )
                        }
                        items(RoutineCategory.entries) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(strings.routineCategoryName(cat)) }
                            )
                        }
                    }
                }

                // Grouped Routine Reminders Info Card
                if (!minimalist && uiState.groupedRemindersEnabled && uiState.routines.isNotEmpty()) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = strings.groupedRemindersInfo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = viewModel::triggerGroupedNotificationPreview) {
                                    Text(strings.testGroupedNotification)
                                }
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
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
                                    Icons.Outlined.Repeat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(strings.emptyRoutinesTitle, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    strings.emptyRoutinesSubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filtered, key = { it.routine.id }) { item ->
                        RoutineCard(
                            item = item,
                            todayDow = uiState.today.dayOfWeek.value,
                            onToggleComplete = { viewModel.toggleCompletionToday(item.routine.id) },
                            onTogglePause = { viewModel.togglePause(item.routine) },
                            onEdit = { editingRoutine = item.routine },
                            onDelete = { viewModel.deleteRoutine(item.routine.id) }
                        )
                    }
                }

                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showCreateSheet || editingRoutine != null) {
        RoutineEditorSheet(
            existing = editingRoutine,
            onDismiss = {
                showCreateSheet = false
                editingRoutine = null
            },
            onSave = { title, desc, category, days, time, duration, variations ->
                viewModel.saveRoutine(
                    existing = editingRoutine,
                    title = title,
                    description = desc,
                    category = category,
                    activeDays = days,
                    reminderTime = time,
                    defaultDurationMinutes = duration,
                    variations = variations
                )
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoutineCard(
    item: RoutineWithStats,
    todayDow: Int,
    onToggleComplete: () -> Unit,
    onTogglePause: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = appStrings()
    val minimalist = LocalMinimalistMode.current
    val routine = item.routine
    val todayVariation = routine.variationFor(todayDow)
    val effectiveDuration = routine.effectiveDurationFor(todayDow)

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (routine.isPaused) 0.dp else 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("routine_card_${routine.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                routine.isPaused -> MaterialTheme.colorScheme.surfaceVariant
                                item.isCompletedToday -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            }
                        )
                        .clickable(enabled = !routine.isPaused, onClick = onToggleComplete)
                        .testTag("routine_toggle_${routine.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isCompletedToday) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = routine.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (routine.isPaused) {
                            MetaPill(
                                text = strings.pausedBadge,
                                container = MaterialTheme.colorScheme.surfaceVariant,
                                content = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val subtitle = todayVariation?.note?.takeIf { it.isNotBlank() }
                        ?: routine.description
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${effectiveDuration}${strings.minutesShort}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(10.dp))

            // Active Days Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                (1..7).forEach { dow ->
                    val active = routine.activeDays.contains(dow)
                    val isToday = dow == todayDow
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    active && isToday -> MaterialTheme.colorScheme.primary
                                    active -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.dayShort(dow),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                active && isToday -> MaterialTheme.colorScheme.onPrimary
                                active -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                routine.reminderTime?.let {
                    Text(
                        text = it.format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!minimalist) {
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetaPill(
                        text = "${item.currentStreak} ${strings.streakDays}",
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        content = MaterialTheme.colorScheme.onTertiaryContainer,
                        icon = Icons.Outlined.LocalFireDepartment
                    )
                    if (item.graceUsedThisWeek) {
                        MetaPill(
                            text = strings.graceShieldActive,
                            container = MaterialTheme.colorScheme.secondaryContainer,
                            content = MaterialTheme.colorScheme.onSecondaryContainer,
                            icon = Icons.Outlined.Shield
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { item.totalCompletionRate },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Text(
                        text = "${strings.completionRateLabel}: ${(item.totalCompletionRate * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onTogglePause) {
                    Icon(
                        imageVector = if (routine.isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.PauseCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (routine.isPaused) strings.resumeRoutine else strings.pauseRoutine)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = strings.editRoutine)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RoutineEditorSheet(
    existing: Routine?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        category: RoutineCategory,
        activeDays: Set<Int>,
        reminderTime: LocalTime?,
        defaultDurationMinutes: Int,
        variations: Map<Int, RoutineDayVariation>
    ) -> Unit
) {
    val strings = appStrings()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var category by remember { mutableStateOf(existing?.category ?: RoutineCategory.MORNING) }
    var activeDays by remember { mutableStateOf(existing?.activeDays ?: setOf(1, 2, 3, 4, 5, 6, 7)) }
    var hourText by remember { mutableStateOf(existing?.reminderTime?.hour?.toString()?.padStart(2, '0') ?: "07") }
    var minText by remember { mutableStateOf(existing?.reminderTime?.minute?.toString()?.padStart(2, '0') ?: "30") }
    var durationText by remember { mutableStateOf((existing?.defaultDurationMinutes ?: 15).toString()) }
    var showVariations by remember { mutableStateOf(existing?.variations?.isNotEmpty() == true) }
    val variationNotes = remember {
        mutableStateMapOf<Int, String>().apply {
            existing?.variations?.forEach { (dow, v) -> put(dow, v.note) }
        }
    }
    val variationDurations = remember {
        mutableStateMapOf<Int, String>().apply {
            existing?.variations?.forEach { (dow, v) -> put(dow, v.durationMinutes.toString()) }
        }
    }

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
                text = if (existing == null) strings.newRoutine else strings.editRoutine,
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(strings.titleLabel) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("routine_title_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(strings.notesLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(strings.categoryLabel, style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoutineCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(strings.routineCategoryName(cat)) }
                    )
                }
            }

            Text(strings.activeDaysLabel, style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                (1..7).forEach { dow ->
                    val selected = activeDays.contains(dow)
                    Surface(
                        onClick = {
                            activeDays = if (selected && activeDays.size > 1) activeDays - dow else activeDays + dow
                        },
                        shape = CircleShape,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = strings.dayShort(dow),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = hourText,
                    onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) hourText = it },
                    label = { Text("HH") },
                    singleLine = true,
                    modifier = Modifier.width(78.dp)
                )
                Text(":", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = minText,
                    onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) minText = it },
                    label = { Text("MM") },
                    singleLine = true,
                    modifier = Modifier.width(78.dp)
                )
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) durationText = it },
                    label = { Text(strings.durationMinutesLabel) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedButton(
                onClick = { showVariations = !showVariations },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(strings.dayVariationTitle)
            }

            AnimatedVisibility(visible = showVariations) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeDays.sorted().forEach { dow ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = strings.dayFull(dow),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.width(56.dp)
                            )
                            OutlinedTextField(
                                value = variationNotes[dow].orEmpty(),
                                onValueChange = { variationNotes[dow] = it },
                                placeholder = { Text(strings.variationHint, style = MaterialTheme.typography.bodySmall) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = variationDurations[dow].orEmpty(),
                                onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) variationDurations[dow] = it },
                                placeholder = { Text(durationText) },
                                singleLine = true,
                                modifier = Modifier.width(72.dp)
                            )
                        }
                    }
                }
            }

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
                        val h = hourText.toIntOrNull()?.takeIf { it in 0..23 }
                        val m = minText.toIntOrNull()?.takeIf { it in 0..59 } ?: 0
                        val time = if (h != null) LocalTime.of(h, m) else null
                        val baseDur = durationText.toIntOrNull()?.coerceIn(1, 240) ?: 15
                        val map = buildMap {
                            activeDays.forEach { dow ->
                                val note = variationNotes[dow].orEmpty().trim()
                                val dur = variationDurations[dow]?.toIntOrNull()?.coerceIn(1, 240)
                                if (note.isNotBlank() || dur != null) {
                                    put(dow, RoutineDayVariation(dow, note, dur ?: baseDur))
                                }
                            }
                        }
                        onSave(title, description, category, activeDays, time, baseDur, map)
                        onDismiss()
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.testTag("routine_save_button")
                ) {
                    Text(strings.save)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
