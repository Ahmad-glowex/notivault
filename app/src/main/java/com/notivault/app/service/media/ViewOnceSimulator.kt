package com.notivault.app.service.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.notivault.app.NotiVaultApp
import com.notivault.app.data.local.entity.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ViewOnceSimulator {

    /**
     * Injects a verified sample image into the latest View-Once or photo message
     * so the user can immediately test and confirm that the chat bubble renders
     * the image directly in place of the placeholder text.
     */
    suspend fun injectTestViewOnceMedia(context: Context): Boolean = withContext(Dispatchers.IO) {
        val app = context.applicationContext as? NotiVaultApp ?: return@withContext false
        val msgDao = app.database.messageDao()
        val cacheManager = MediaCacheManager(context)

        // 1. Generate a sample high-fidelity test image
        val width = 720
        val height = 480
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dark teal background
        val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#042F2E") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Inner card
        val cardPaint = Paint().apply { color = android.graphics.Color.parseColor("#115E59") }
        canvas.drawRoundRect(40f, 40f, (width - 40).toFloat(), (height - 40).toFloat(), 32f, 32f, cardPaint)

        // Title text
        val titlePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 40f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("👁️ View-Once Preserved", (width / 2).toFloat(), 180f, titlePaint)

        // Subtitle
        val subPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#2DD4BF")
            textSize = 28f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("NotiVault Direct Bubble Test", (width / 2).toFloat(), 250f, subPaint)

        // Timestamp
        val timePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 20f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val formattedTime = SimpleDateFormat("h:mm a, dd MMM yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Verified at $formattedTime", (width / 2).toFloat(), 320f, timePaint)

        // 2. Cache the sample bitmap to app storage
        val cachedFile = cacheManager.cacheBitmap(bitmap, prefix = "test_view_once") ?: return@withContext false

        // 3. Find the most appropriate message to update
        val allMessages = msgDao.getAllMessagesSync()
        // Priority 1: View-Once message without media
        var targetMsg = allMessages.lastOrNull { msg ->
            (msg.messageText.contains("①") ||
                    msg.messageText.contains("\u2460") ||
                    msg.messageText.contains("view once", ignoreCase = true) ||
                    msg.messageText.contains("একবার দেখার")) &&
                    msg.mediaUri.isNullOrBlank()
        }

        // Priority 2: Any pending media message
        if (targetMsg == null) {
            targetMsg = allMessages.lastOrNull { msg ->
                (msg.hasMedia || msg.messageText.contains("photo", ignoreCase = true) || msg.messageText.contains("ছবি")) &&
                        msg.mediaUri.isNullOrBlank()
            }
        }

        // Priority 3: The last message overall
        if (targetMsg == null) {
            targetMsg = allMessages.lastOrNull()
        }

        val now = System.currentTimeMillis()
        if (targetMsg != null) {
            msgDao.updateMessageMedia(targetMsg.id, cachedFile.absolutePath, "image/jpeg")
            app.mediaRepository.saveCachedMedia(
                packageName = targetMsg.packageName,
                originalPath = "test_view_once_${cachedFile.name}",
                internalSavedPath = cachedFile.absolutePath,
                fileName = cachedFile.name,
                mimeType = "image/jpeg",
                fileSizeBytes = cachedFile.length(),
                mediaType = "VIEW_ONCE_IMAGE",
                threadId = targetMsg.threadId,
                messageId = targetMsg.id
            )
            true
        } else {
            // Create a demo View-Once message from Ahmad
            val threadId = "com.whatsapp_Ahmad"
            val demoMsg = MessageEntity(
                threadId = threadId,
                packageName = "com.whatsapp",
                senderName = "Ahmad",
                messageText = "📷 ① Sent a photo",
                timestamp = now,
                originalNotificationKey = "demo_vo_${now}",
                hasMedia = true,
                mediaUri = cachedFile.absolutePath,
                mediaMimeType = "image/jpeg"
            )
            val msgId = msgDao.insertMessage(demoMsg)
            app.mediaRepository.saveCachedMedia(
                packageName = "com.whatsapp",
                originalPath = "test_view_once_${cachedFile.name}",
                internalSavedPath = cachedFile.absolutePath,
                fileName = cachedFile.name,
                mimeType = "image/jpeg",
                fileSizeBytes = cachedFile.length(),
                mediaType = "VIEW_ONCE_IMAGE",
                threadId = threadId,
                messageId = msgId
            )
            true
        }
    }
}
