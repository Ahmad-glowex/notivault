package com.notivault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.notivault.app.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    fun getMessagesForThread(threadId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE threadId = :threadId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessagesForThread(threadId: String, limit: Int = 20): List<MessageEntity>

    @Query("""
        SELECT * FROM messages 
        WHERE threadId = :threadId 
          AND senderName = :senderName 
          AND isDeleted = 0 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun getLatestActiveMessageBySender(threadId: String, senderName: String): MessageEntity?

    @Query("""
        SELECT * FROM messages 
        WHERE threadId = :threadId 
          AND isDeleted = 0 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun getLatestActiveMessageInThread(threadId: String): MessageEntity?

    @Query("""
        UPDATE messages 
        SET isDeleted = 1, deletedTimestamp = :deletedTimestamp 
        WHERE id = :id
    """)
    suspend fun markMessageAsDeleted(id: Long, deletedTimestamp: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("""
        SELECT * FROM messages 
        WHERE messageText LIKE '%' || :query || '%' 
           OR senderName LIKE '%' || :query || '%' 
        ORDER BY timestamp DESC
    """)
    fun searchMessages(query: String): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages WHERE isDeleted = 1")
    fun getDeletedMessagesCount(): Flow<Int>

    @Query("SELECT * FROM messages WHERE isDeleted = 1 ORDER BY deletedTimestamp DESC")
    fun getAllDeletedMessages(): Flow<List<MessageEntity>>

    @Query("""
        SELECT * FROM messages 
        WHERE threadId = :threadId 
          AND senderName = :senderName 
          AND messageText = :messageText 
          AND timestamp = :timestamp 
        LIMIT 1
    """)
    suspend fun findExistingMessage(
        threadId: String,
        senderName: String,
        messageText: String,
        timestamp: Long
    ): MessageEntity?

    @Query("""
        SELECT * FROM messages 
        WHERE threadId = :threadId 
          AND senderName = :senderName 
          AND messageText = :messageText 
          AND ABS(timestamp - :timestamp) <= :toleranceMs
        ORDER BY ABS(timestamp - :timestamp) ASC
        LIMIT 1
    """)
    suspend fun findExistingMessageWithTolerance(
        threadId: String,
        senderName: String,
        messageText: String,
        timestamp: Long,
        toleranceMs: Long = 30000L
    ): MessageEntity?

    @Query("""
        SELECT * FROM messages 
        WHERE threadId = :threadId 
          AND messageText = :messageText 
          AND ABS(timestamp - :timestamp) <= :toleranceMs
        ORDER BY ABS(timestamp - :timestamp) ASC
        LIMIT 1
    """)
    suspend fun findExistingMessageInThread(
        threadId: String,
        messageText: String,
        timestamp: Long,
        toleranceMs: Long = 30000L
    ): MessageEntity?

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesSync(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    suspend fun getMessagesForThreadSync(threadId: String): List<MessageEntity>

    @Query("DELETE FROM messages WHERE threadId = :threadId")
    suspend fun deleteMessagesForThread(threadId: String)

    @Query("""
        SELECT * FROM messages 
        WHERE threadId = :threadId 
          AND isDeleted = 0 
          AND ABS(timestamp - :timestamp) <= :toleranceMs
        ORDER BY ABS(timestamp - :timestamp) ASC 
        LIMIT 1
    """)
    suspend fun getActiveMessageNearTimestamp(
        threadId: String,
        timestamp: Long,
        toleranceMs: Long = 30000L
    ): MessageEntity?

    @Query("""
        UPDATE messages 
        SET mediaUri = :mediaUri, hasMedia = 1, mediaMimeType = COALESCE(:mimeType, mediaMimeType, 'image/jpeg') 
        WHERE id = :id
    """)
    suspend fun updateMessageMedia(id: Long, mediaUri: String, mimeType: String?)

    @Query("""
        SELECT * FROM messages 
        WHERE packageName = :packageName 
          AND (hasMedia = 1 OR messageText LIKE '%photo%' OR messageText LIKE '%image%' OR messageText LIKE '%video%' OR messageText LIKE '%📷%') 
          AND (mediaUri IS NULL OR mediaUri = '') 
          AND timestamp >= :sinceTimestamp 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun getLatestPendingMediaMessage(packageName: String, sinceTimestamp: Long): MessageEntity?

    @Query("""
        SELECT * FROM messages 
        WHERE packageName = :packageName 
          AND timestamp >= :sinceTimestamp 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun getLatestMessageForPackage(packageName: String, sinceTimestamp: Long): MessageEntity?

    @Query("""
        SELECT * FROM messages 
        WHERE packageName = :packageName 
          AND isDeleted = 0 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun getLatestActiveMessageForPackage(packageName: String): MessageEntity?

    @Query("""
        SELECT * FROM messages 
        WHERE packageName = :packageName 
          AND senderName = :senderName 
          AND isDeleted = 0 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun getLatestActiveMessageBySenderForPackage(packageName: String, senderName: String): MessageEntity?

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM messages WHERE isDeleted = 1")
    suspend fun clearAllDeletedMessages()

    @Query("UPDATE messages SET hasMedia = 0, mediaUri = NULL, mediaMimeType = NULL WHERE id = :id")
    suspend fun clearMessageMedia(id: Long)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    @Query("""
        DELETE FROM messages 
        WHERE id NOT IN (
            SELECT MIN(id) 
            FROM messages 
            GROUP BY threadId, senderName, messageText, timestamp / 15000
        )
    """)
    suspend fun deleteDuplicateMessages()
}
