package com.notivault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.notivault.app.data.local.entity.AppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM monitored_apps ORDER BY lastActivityTimestamp DESC")
    fun getAllApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM monitored_apps ORDER BY lastActivityTimestamp DESC")
    suspend fun getAllAppsSync(): List<AppEntity>

    @Query("SELECT * FROM monitored_apps WHERE isEnabled = 1")
    fun getEnabledApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM monitored_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getApp(packageName: String): AppEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertApp(app: AppEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateApp(app: AppEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertApps(apps: List<AppEntity>)

    @Update
    suspend fun updateApp(app: AppEntity)

    @Query("UPDATE monitored_apps SET isEnabled = :isEnabled WHERE packageName = :packageName")
    suspend fun setAppEnabled(packageName: String, isEnabled: Boolean)

    @Query("DELETE FROM monitored_apps WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)

    @Query("""
        UPDATE monitored_apps 
        SET totalMessages = totalMessages + 1, lastActivityTimestamp = :timestamp 
        WHERE packageName = :packageName
    """)
    suspend fun incrementMessageCount(packageName: String, timestamp: Long)
}
