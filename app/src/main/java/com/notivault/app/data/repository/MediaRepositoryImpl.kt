package com.notivault.app.data.repository

import android.content.Context
import com.notivault.app.data.local.AppDatabase
import com.notivault.app.data.local.entity.MediaEntity
import com.notivault.app.service.media.MediaStoreObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepositoryImpl(
    private val db: AppDatabase
) : MediaRepository {

    private val mediaDao = db.mediaDao()

    override fun getAllMedia(): Flow<List<MediaEntity>> = mediaDao.getAllMedia()

    override fun getMediaByType(type: String): Flow<List<MediaEntity>> =
        mediaDao.getMediaByType(type)

    override fun getViewOnceMedia(): Flow<List<MediaEntity>> =
        mediaDao.getViewOnceMedia()

    override fun getViewOnceMediaCount(): Flow<Int> =
        mediaDao.getViewOnceMediaCount()

    override fun getMediaForThread(threadId: String): Flow<List<MediaEntity>> =
        mediaDao.getMediaForThread(threadId)

    override fun getMediaCount(): Flow<Int> = mediaDao.getMediaCount()

    override suspend fun getMediaById(id: Long): MediaEntity? = withContext(Dispatchers.IO) {
        mediaDao.getMediaById(id)
    }

    override suspend fun getMediaByOriginalPath(originalPath: String): MediaEntity? = withContext(Dispatchers.IO) {
        mediaDao.getMediaByOriginalPath(originalPath)
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
        // 1. Strict blacklist rejection (screenshots, camera DCIM, downloads, sent media)
        if (MediaStoreObserver.isBlacklisted(originalPath, "", fileName) ||
            MediaStoreObserver.isBlacklisted(internalSavedPath, "", fileName)) {
            try {
                File(internalSavedPath).delete()
            } catch (_: Exception) {}
            return@withContext 0L
        }

        // 2. Prevent duplicate media entries by exact originalPath
        val existing = mediaDao.getMediaByOriginalPath(originalPath)
        if (existing != null) {
            // Delete freshly created duplicate file to avoid disk leak
            if (internalSavedPath != existing.internalSavedPath) {
                try {
                    File(internalSavedPath).delete()
                } catch (_: Exception) {}
            }
            return@withContext existing.id
        }

        // 3. Deduplication by fileName and fileSizeBytes within 5 minutes
        val existingByNameSize = mediaDao.getMediaByNameAndSize(fileName, fileSizeBytes, System.currentTimeMillis() - 300000L)
        if (existingByNameSize != null) {
            if (internalSavedPath != existingByNameSize.internalSavedPath) {
                try {
                    File(internalSavedPath).delete()
                } catch (_: Exception) {}
            }
            return@withContext existingByNameSize.id
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

    override fun getTotalMediaBytes(): Flow<Long> =
        mediaDao.getTotalMediaBytes().map { it ?: 0L }

    override suspend fun deleteMediaOlderThan(days: Int): Int = withContext(Dispatchers.IO) {
        if (days <= 0) return@withContext 0
        val cutoff = System.currentTimeMillis() - (days.toLong() * 86_400_000L)
        val oldMediaList = mediaDao.getMediaOlderThan(cutoff)
        for (m in oldMediaList) {
            try {
                val file = File(m.internalSavedPath)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
        mediaDao.deleteMediaOlderThan(cutoff)
    }

    override suspend fun purgeOrphanMediaFiles(context: Context): Long = withContext(Dispatchers.IO) {
        var reclaimedBytes = 0L
        val orphanPaths = mediaDao.getOrphanMediaPaths()
        for (path in orphanPaths) {
            try {
                val file = File(path)
                if (file.exists()) {
                    val len = file.length()
                    if (file.delete()) reclaimedBytes += len
                }
            } catch (_: Exception) {}
        }
        mediaDao.deleteOrphanMedia()

        val savedDir = File(context.filesDir, "saved_media")
        if (savedDir.exists() && savedDir.isDirectory) {
            val validPaths = mediaDao.getValidMediaPaths().toSet()
            savedDir.listFiles()?.forEach { file ->
                if (!validPaths.contains(file.absolutePath)) {
                    val len = file.length()
                    if (file.delete()) reclaimedBytes += len
                }
            }
        }
        reclaimedBytes
    }
}
