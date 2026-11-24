package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.local.DownloadDao
import com.example.data.local.HistoryDao
import com.example.data.local.StreamGrabDatabase
import com.example.data.repository.DownloadRepositoryImpl
import com.example.data.repository.HistoryRepositoryImpl
import com.example.data.repository.ScraperRepositoryImpl
import com.example.data.repository.SettingsRepositoryImpl
import com.example.domain.repository.DownloadRepository
import com.example.domain.repository.HistoryRepository
import com.example.domain.repository.ScraperRepository
import com.example.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkAndDatabaseModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .writeTimeout(35, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StreamGrabDatabase {
        return Room.databaseBuilder(
            context,
            StreamGrabDatabase::class.java,
            "streamgrab_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideDownloadDao(db: StreamGrabDatabase): DownloadDao = db.downloadDao()

    @Provides
    fun provideHistoryDao(db: StreamGrabDatabase): HistoryDao = db.historyDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindModule {

    @Binds
    @Singleton
    abstract fun bindScraperRepository(impl: ScraperRepositoryImpl): ScraperRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
