package com.notivault.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.notivault.app.data.local.converter.Converters
import com.notivault.app.data.local.dao.AppDao
import com.notivault.app.data.local.dao.ChatDao
import com.notivault.app.data.local.dao.MediaDao
import com.notivault.app.data.local.dao.MessageDao
import com.notivault.app.data.local.entity.AppEntity
import com.notivault.app.data.local.entity.ChatThreadEntity
import com.notivault.app.data.local.entity.MediaEntity
import com.notivault.app.data.local.entity.MessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AppEntity::class,
        ChatThreadEntity::class,
        MessageEntity::class,
        MediaEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS messages_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        threadId TEXT NOT NULL,
                        packageName TEXT NOT NULL,
                        senderName TEXT NOT NULL,
                        messageText TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL,
                        deletedTimestamp INTEGER,
                        originalNotificationKey TEXT,
                        hasMedia INTEGER NOT NULL,
                        mediaUri TEXT,
                        mediaMimeType TEXT,
                        isSelf INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    INSERT INTO messages_new (id, threadId, packageName, senderName, messageText, timestamp, isDeleted, deletedTimestamp, originalNotificationKey, hasMedia, mediaUri, mediaMimeType, isSelf)
                    SELECT id, threadId, packageName, senderName, messageText, timestamp, isDeleted, deletedTimestamp, originalNotificationKey, hasMedia, mediaUri, mediaMimeType, isSelf FROM messages
                """)
                db.execSQL("DROP TABLE messages")
                db.execSQL("ALTER TABLE messages_new RENAME TO messages")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_threadId ON messages(threadId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_packageName ON messages(packageName)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_timestamp ON messages(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_isDeleted ON messages(isDeleted)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notivault_database.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Prepopulate default monitored messaging apps
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).appDao().insertApps(
                                    listOf(
                                        AppEntity(
                                            packageName = "com.whatsapp",
                                            appName = "WhatsApp",
                                            isEnabled = true,
                                            colorHex = "#25D366"
                                        ),
                                        AppEntity(
                                            packageName = "com.facebook.orca",
                                            appName = "Messenger",
                                            isEnabled = true,
                                            colorHex = "#0084FF"
                                        ),
                                        AppEntity(
                                            packageName = "com.instagram.android",
                                            appName = "Instagram",
                                            isEnabled = true,
                                            colorHex = "#E1306C"
                                        ),
                                        AppEntity(
                                            packageName = "org.telegram.messenger",
                                            appName = "Telegram",
                                            isEnabled = true,
                                            colorHex = "#229ED9"
                                        )
                                    )
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
