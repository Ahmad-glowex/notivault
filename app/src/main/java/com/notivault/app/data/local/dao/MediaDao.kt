package com.notivault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.notivault.app.data.local.entity.MediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM saved_media ORDER BY timestamp DESC")
    fun getAllMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM saved_media WHERE mediaType = :type ORDER BY timestamp DESC")
    fun getMediaByType(type: String): Flow<List<MediaEntity>>

    @Query("""
        SELECT * FROM saved_media 
        WHERE mediaType LIKE 'VIEW_ONCE%' 
           OR packageName = 'com.whatsapp.web' 
           OR threadId LIKE '%view_once%' 
        ORDER BY timestamp DESC
    """)
    fun getViewOnceMedia(): Flow<List<MediaEntity>>

    @Query("""
        SELECT COUNT(*) FROM saved_media 
        WHERE mediaType LIKE 'VIEW_ONCE%' 
           OR packageName = 'com.whatsapp.web' 
           OR threadId LIKE '%view_once%'
    """)
    fun getViewOnceMediaCount(): Flow<Int>

    @Query("SELECT * FROM saved_media WHERE threadId = :threadId ORDER BY timestamp DESC")
    fun getMediaForThread(threadId: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM saved_media WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: Long): MediaEntity?

    @Query("SELECT * FROM saved_media WHERE originalPath = :originalPath LIMIT 1")
    suspend fun getMediaByOriginalPath(originalPath: String): MediaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaEntity): Long

    @Query("SELECT COUNT(*) FROM saved_media")
    fun getMediaCount(): Flow<Int>

    @Query("DELETE FROM saved_media WHERE id = :id")
    suspend fun deleteMedia(id: Long)

    @Query("""
        SELECT * FROM saved_media 
        WHERE packageName = :packageName 
          AND (threadId IS NULL OR threadId = '') 
          AND timestamp >= :sinceTimestamp 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun getRecentUnlinkedMediaForPackage(packageName: String, sinceTimestamp: Long): MediaEntity?

    @Query("UPDATE saved_media SET threadId = :threadId, messageId = :messageId WHERE id = :id")
    suspend fun updateMediaLinkage(id: Long, threadId: String, messageId: Long?)

    @Query("SELECT * FROM saved_media")
    suspend fun getAllMediaList(): List<MediaEntity>

    @Query("""
        SELECT * FROM saved_media 
        WHERE fileName = :fileName 
          AND fileSizeBytes = :fileSizeBytes 
          AND timestamp >= :sinceTimestamp 
        LIMIT 1
    """)
    suspend fun getMediaByNameAndSize(fileName: String, fileSizeBytes: Long, sinceTimestamp: Long): MediaEntity?

    @Query("SELECT internalSavedPath FROM saved_media WHERE messageId IS NULL AND mediaType NOT LIKE 'VIEW_ONCE%'")
    suspend fun getOrphanMediaPaths(): List<String>

    @Query("DELETE FROM saved_media WHERE messageId IS NULL AND mediaType NOT LIKE 'VIEW_ONCE%'")
    suspend fun deleteOrphanMedia(): Int

    @Query("SELECT internalSavedPath FROM saved_media WHERE messageId IS NOT NULL OR mediaType LIKE 'VIEW_ONCE%'")
    suspend fun getValidMediaPaths(): List<String>

    @Query("DELETE FROM saved_media")
    suspend fun deleteAllMedia()
}
