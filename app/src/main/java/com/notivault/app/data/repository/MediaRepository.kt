package com.notivault.app.data.repository

import com.notivault.app.data.local.entity.MediaEntity
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getAllMedia(): Flow<List<MediaEntity>>
    fun getMediaByType(type: String): Flow<List<MediaEntity>>
    fun getMediaForThread(threadId: String): Flow<List<MediaEntity>>
    fun getMediaCount(): Flow<Int>
    suspend fun getMediaById(id: Long): MediaEntity?
    suspend fun getMediaByOriginalPath(originalPath: String): MediaEntity?
    suspend fun saveCachedMedia(
        packageName: String,
        originalPath: String,
        internalSavedPath: String,
        fileName: String,
        mimeType: String,
        fileSizeBytes: Long,
        mediaType: String,
        threadId: String? = null,
        messageId: Long? = null
    ): Long
    suspend fun deleteMedia(id: Long)
    suspend fun clearAllMedia()
}
