package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TaskEntity::class,
        SubtaskEntity::class,
        TaskTemplateEntity::class,
        RoutineEntity::class,
        RoutineLogEntity::class,
        FocusSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomTypeConverters::class)
abstract class FlowDayDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun templateDao(): TemplateDao
    abstract fun routineDao(): RoutineDao
    abstract fun focusDao(): FocusDao
}
