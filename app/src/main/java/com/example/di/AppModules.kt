package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.local.FlowDayDatabase
import com.example.data.local.FocusDao
import com.example.data.local.RoutineDao
import com.example.data.local.TaskDao
import com.example.data.local.TemplateDao
import com.example.data.repository.FocusRepositoryImpl
import com.example.data.repository.RoutineRepositoryImpl
import com.example.data.repository.SettingsRepositoryImpl
import com.example.data.repository.TaskRepositoryImpl
import com.example.domain.repository.FocusRepository
import com.example.domain.repository.RoutineRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FlowDayDatabase {
        return Room.databaseBuilder(
            context,
            FlowDayDatabase::class.java,
            "flowday_planner.db"
        ).fallbackToDestructiveMigration(dropAllTables = false)
            .build()
    }

    @Provides
    fun provideTaskDao(db: FlowDayDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideTemplateDao(db: FlowDayDatabase): TemplateDao = db.templateDao()

    @Provides
    fun provideRoutineDao(db: FlowDayDatabase): RoutineDao = db.routineDao()

    @Provides
    fun provideFocusDao(db: FlowDayDatabase): FocusDao = db.focusDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindRoutineRepository(impl: RoutineRepositoryImpl): RoutineRepository

    @Binds
    @Singleton
    abstract fun bindFocusRepository(impl: FocusRepositoryImpl): FocusRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
