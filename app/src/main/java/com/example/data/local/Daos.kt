package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Transaction
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, priority DESC, dueTimeMinuteOfDay ASC, createdAt DESC")
    fun observeAllTasks(): Flow<List<TaskWithSubtasks>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE dueDateEpochDay = :epochDay ORDER BY isCompleted ASC, priority DESC, dueTimeMinuteOfDay ASC, createdAt DESC")
    fun observeTasksForDate(epochDay: Long): Flow<List<TaskWithSubtasks>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND dueDateEpochDay IS NOT NULL AND dueDateEpochDay < :todayEpochDay ORDER BY dueDateEpochDay ASC, priority DESC")
    fun observeOverdueIncompleteTasks(todayEpochDay: Long): Flow<List<TaskWithSubtasks>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND (dueDateEpochDay IS NULL OR (dueTimeMinuteOfDay IS NULL AND timeBlock = 'ANYTIME')) ORDER BY priority DESC, createdAt DESC")
    fun observeUnscheduledTasks(): Flow<List<TaskWithSubtasks>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskWithSubtasks?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :completed, completedAt = :completedAt WHERE id = :taskId")
    suspend fun updateCompletion(taskId: Long, completed: Boolean, completedAt: Long?)

    @Query("UPDATE tasks SET dueDateEpochDay = :targetEpochDay, postponeCount = postponeCount + 1 WHERE id IN (:taskIds)")
    suspend fun rolloverTasks(taskIds: List<Long>, targetEpochDay: Long)

    @Query("UPDATE tasks SET dueDateEpochDay = :targetEpochDay, timeBlock = :timeBlock, dueTimeMinuteOfDay = :minuteOfDay WHERE id = :taskId")
    suspend fun rescheduleTask(taskId: Long, targetEpochDay: Long?, timeBlock: String, minuteOfDay: Int?)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: SubtaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtasks(subtasks: List<SubtaskEntity>)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM subtasks WHERE taskId = :taskId")
    suspend fun nextSubtaskOrder(taskId: Long): Int

    @Query("UPDATE subtasks SET isCompleted = :completed WHERE id = :subtaskId")
    suspend fun updateSubtaskCompletion(subtaskId: Long, completed: Boolean)

    @Query("UPDATE subtasks SET sortOrder = :sortOrder WHERE id = :subtaskId")
    suspend fun updateSubtaskOrder(subtaskId: Long, sortOrder: Int)

    @Query("DELETE FROM subtasks WHERE id = :subtaskId")
    suspend fun deleteSubtask(subtaskId: Long)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksForTask(taskId: Long)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM task_templates ORDER BY isBuiltIn DESC, id ASC")
    fun observeTemplates(): Flow<List<TaskTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TaskTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTemplates(templates: List<TaskTemplateEntity>)

    @Query("DELETE FROM task_templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long)

    @Query("SELECT COUNT(*) FROM task_templates")
    suspend fun countTemplates(): Int
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY isPaused ASC, reminderMinuteOfDay ASC, createdAt ASC")
    fun observeAllRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines")
    suspend fun getAllRoutinesOnce(): List<RoutineEntity>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutineById(id: Long): RoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutine(routine: RoutineEntity): Long

    @Query("UPDATE routines SET isPaused = :paused, pausedUntilEpochDay = :pausedUntil WHERE id = :routineId")
    suspend fun setPaused(routineId: Long, paused: Boolean, pausedUntil: Long?)

    @Query("DELETE FROM routines WHERE id = :routineId")
    suspend fun deleteRoutine(routineId: Long)

    @Query("SELECT * FROM routine_logs")
    fun observeAllLogs(): Flow<List<RoutineLogEntity>>

    @Query("SELECT * FROM routine_logs WHERE routineId = :routineId AND dateEpochDay = :epochDay LIMIT 1")
    suspend fun findLog(routineId: Long, epochDay: Long): RoutineLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: RoutineLogEntity)

    @Query("DELETE FROM routine_logs WHERE routineId = :routineId AND dateEpochDay = :epochDay")
    suspend fun deleteLog(routineId: Long, epochDay: Long)
}

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions ORDER BY completedAt DESC")
    fun observeAllSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE taskId = :taskId ORDER BY completedAt DESC")
    fun observeSessionsForTask(taskId: Long): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long
}
