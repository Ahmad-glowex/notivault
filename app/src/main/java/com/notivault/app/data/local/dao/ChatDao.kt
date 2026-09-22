package com.notivault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.notivault.app.data.local.entity.ChatThreadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_threads ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getAllThreads(): Flow<List<ChatThreadEntity>>

    @Query("""
        SELECT * FROM chat_threads 
        WHERE (:packageName IN ('com.whatsapp', 'com.whatsapp.w4b') AND packageName IN ('com.whatsapp', 'com.whatsapp.w4b'))
           OR (:packageName NOT IN ('com.whatsapp', 'com.whatsapp.w4b') AND packageName = :packageName)
        ORDER BY isPinned DESC, lastMessageTimestamp DESC
    """)
    fun getThreadsByPackage(packageName: String): Flow<List<ChatThreadEntity>>

    @Query("SELECT * FROM chat_threads WHERE threadId = :threadId LIMIT 1")
    fun getThread(threadId: String): Flow<ChatThreadEntity?>

    @Query("SELECT * FROM chat_threads WHERE threadId = :threadId LIMIT 1")
    suspend fun getThreadSync(threadId: String): ChatThreadEntity?

    @Upsert
    suspend fun insertOrUpdateThread(thread: ChatThreadEntity)

    @Query("""
        SELECT * FROM chat_threads 
        WHERE chatTitle LIKE '%' || :query || '%' 
           OR lastMessageText LIKE '%' || :query || '%' 
        ORDER BY lastMessageTimestamp DESC
    """)
    fun searchThreads(query: String): Flow<List<ChatThreadEntity>>

    @Query("""
        SELECT * FROM chat_threads 
        WHERE ((:packageName IN ('com.whatsapp', 'com.whatsapp.w4b') AND packageName IN ('com.whatsapp', 'com.whatsapp.w4b'))
           OR (:packageName NOT IN ('com.whatsapp', 'com.whatsapp.w4b') AND packageName = :packageName))
          AND (chatTitle LIKE '%' || :query || '%' OR lastMessageText LIKE '%' || :query || '%')
        ORDER BY lastMessageTimestamp DESC
    """)
    fun searchThreadsByPackage(packageName: String, query: String): Flow<List<ChatThreadEntity>>

    @Query("UPDATE chat_threads SET isPinned = :isPinned WHERE threadId = :threadId")
    suspend fun setPinned(threadId: String, isPinned: Boolean)

    @Query("DELETE FROM chat_threads WHERE threadId = :threadId")
    suspend fun deleteThread(threadId: String)

    @Query("DELETE FROM chat_threads WHERE packageName = :packageName")
    suspend fun deleteThreadsByPackage(packageName: String)

    @Query("DELETE FROM chat_threads")
    suspend fun deleteAllThreads()

    @Query("DELETE FROM chat_threads WHERE threadId NOT IN (SELECT DISTINCT threadId FROM messages)")
    suspend fun deleteEmptyThreads(): Int
}
