package com.notivault.app.data.repository

import com.notivault.app.data.local.AppDatabase
import com.notivault.app.data.local.entity.MediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepositoryImpl(
    private val db: AppDatabase
) : MediaRepository {

    private val mediaDao = db.mediaDao()

    override fun getAllMedia(): Flow<List<MediaEntity>> = mediaDao.getAllMedia()

    override fun getMediaByType(type: String): Flow<List<MediaEntity>> =
        mediaDao.getMediaByType(type)

    override fun getMediaForThread(threadId: String): Flow<List<MediaEntity>> =
        mediaDao.getMediaForThread(threadId)

    override fun getMediaCount(): Flow<Int> = mediaDao.getMediaCount()

    override suspend fun getMediaById(id: Long): MediaEntity? = withContext(Dispatchers.IO) {
        mediaDao.getMediaById(id)
    }

    override suspend fun saveCachedMedia(
        packageName: String,
        originalPath: String,
        internalSavedPath: String,
        fileName: String,
        mimeType: String,
        fileSizeBytes: Long,
        mediaType: String,
        threadId: String?,
        messageId: Long?
    ): Long = withContext(Dispatchers.IO) {
        // Prevent duplicate media entries
        val existing = mediaDao.getMediaByOriginalPath(originalPath)
        if (existing != null) {
            return@withContext existing.id
        }

        val entity = MediaEntity(
            threadId = threadId,
            messageId = messageId,
            packageName = packageName,
            originalPath = originalPath,
            internalSavedPath = internalSavedPath,
            fileName = fileName,
            mimeType = mimeType,
            fileSizeBytes = fileSizeBytes,
            timestamp = System.currentTimeMillis(),
            mediaType = mediaType
        )
        mediaDao.insertMedia(entity)
    }

    override suspend fun deleteMedia(id: Long) = withContext(Dispatchers.IO) {
        val media = mediaDao.getMediaById(id)
        if (media != null) {
            // Delete internal file
            try {
                val file = File(media.internalSavedPath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {}
            mediaDao.deleteMedia(id)
        }
    }

    override suspend fun clearAllMedia() = withContext(Dispatchers.IO) {
        mediaDao.deleteAllMedia()
    }
}
