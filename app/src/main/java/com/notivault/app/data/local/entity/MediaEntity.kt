package com.notivault.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a cached media file saved by the Media Observer.
 */
@Entity(
    tableName = "saved_media",
    indices = [
        Index(value = ["threadId"]),
        Index(value = ["packageName"]),
        Index(value = ["timestamp"]),
        Index(value = ["mediaType"])
    ]
)
data class MediaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val threadId: String? = null,
    val messageId: Long? = null,
    val packageName: String,
    val originalPath: String,
    val internalSavedPath: String,
    val fileName: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val mediaType: String = "IMAGE" // IMAGE, VIDEO, AUDIO, DOCUMENT
)
