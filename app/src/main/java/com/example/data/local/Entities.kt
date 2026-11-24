package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.domain.model.Priority
import com.example.domain.model.RoutineCategory
import com.example.domain.model.TimeBlock

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val notes: String = "",
    val priority: Priority = Priority.MEDIUM,
    val dueDateEpochDay: Long? = null,
    val dueTimeMinuteOfDay: Int? = null,
    val timeBlock: TimeBlock = TimeBlock.ANYTIME,
    val tagsJson: String = "[]",
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val postponeCount: Int = 0,
    val reminderMinutesBefore: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val taskId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0
)

data class TaskWithSubtasks(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val subtasks: List<SubtaskEntity>
)

@Entity(tableName = "task_templates")
data class TaskTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val nameAr: String = "",
    val defaultTitle: String,
    val defaultTitleAr: String = "",
    val priority: Priority = Priority.MEDIUM,
    val timeBlock: TimeBlock = TimeBlock.ANYTIME,
    val tagsJson: String = "[]",
    val defaultSubtasksJson: String = "[]",
    val defaultSubtasksArJson: String = "[]",
    val isBuiltIn: Boolean = false
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String = "",
    val category: RoutineCategory = RoutineCategory.MORNING,
    val activeDaysJson: String = "[1,2,3,4,5,6,7]",
    val reminderMinuteOfDay: Int? = null,
    val isPaused: Boolean = false,
    val pausedUntilEpochDay: Long? = null,
    val defaultDurationMinutes: Int = 15,
    val variationsJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "routine_logs",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("routineId"),
        Index(value = ["routineId", "dateEpochDay"], unique = true)
    ]
)
data class RoutineLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val routineId: Long,
    val dateEpochDay: Long,
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "focus_sessions",
    indices = [Index("taskId")]
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val taskId: Long? = null,
    val taskTitleSnapshot: String = "",
    val durationMinutes: Int,
    val isBreak: Boolean = false,
    val startedAt: Long,
    val completedAt: Long
)
