package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM download_tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DownloadEntity?

    @Query("SELECT * FROM download_tasks WHERE status IN ('QUEUED', 'RESOLVING', 'DOWNLOADING') ORDER BY createdAt ASC")
    suspend fun getPendingOrActive(): List<DownloadEntity>

    @Query("SELECT COUNT(*) FROM download_tasks WHERE status IN ('RESOLVING', 'DOWNLOADING')")
    suspend fun countActive(): Int

    @Query("SELECT * FROM download_tasks WHERE status = 'QUEUED' ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getNextQueued(limit: Int): List<DownloadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadEntity): Long

    @Query(
        "UPDATE download_tasks SET downloadedBytes = :downloaded, expectedSizeBytes = :expectedSize, speedBytesPerSec = :speed, supportsRange = :supportsRange, updatedAt = :updatedAt WHERE id = :id"
    )
    suspend fun updateProgress(
        id: Long,
        downloaded: Long,
        expectedSize: Long,
        speed: Long,
        supportsRange: Boolean,
        updatedAt: Long
    )

    @Query("UPDATE download_tasks SET status = :status, errorMessage = :errorMessage, speedBytesPerSec = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, errorMessage: String?, updatedAt: Long)

    @Query("UPDATE download_tasks SET directUrl = :directUrl, headersJson = :headersJson, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDirectUrl(id: Long, directUrl: String, headersJson: String, updatedAt: Long)

    @Query(
        "UPDATE download_tasks SET status = 'COMPLETED', localUri = :localUri, downloadedBytes = :downloadedBytes, expectedSizeBytes = :downloadedBytes, speedBytesPerSec = 0, errorMessage = NULL, updatedAt = :updatedAt WHERE id = :id"
    )
    suspend fun markCompleted(id: Long, localUri: String, downloadedBytes: Long, updatedAt: Long)

    @Query("UPDATE download_tasks SET retryCount = retryCount + 1, status = 'QUEUED', errorMessage = NULL, updatedAt = :updatedAt WHERE id = :id")
    suspend fun resetForRetry(id: Long, updatedAt: Long)

    @Query("UPDATE download_tasks SET status = 'PAUSED', speedBytesPerSec = 0, updatedAt = :updatedAt WHERE status IN ('QUEUED', 'RESOLVING', 'DOWNLOADING')")
    suspend fun pauseAllActive(updatedAt: Long)

    @Query("UPDATE download_tasks SET status = 'QUEUED', errorMessage = NULL, updatedAt = :updatedAt WHERE status = 'PAUSED'")
    suspend fun resumeAllPaused(updatedAt: Long)

    @Query("DELETE FROM download_tasks WHERE status IN ('COMPLETED', 'CANCELLED')")
    suspend fun clearFinished()

    @Query("DELETE FROM download_tasks WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM resolved_history ORDER BY createdAt DESC LIMIT 50")
    fun observeHistory(): Flow<List<ResolvedHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ResolvedHistoryEntity): Long

    @Query("DELETE FROM resolved_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM resolved_history")
    suspend fun clearAll()
}
