package com.notivault.app.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.notivault.app.data.local.entity.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    suspend fun exportToJson(
        context: Context,
        chatTitle: String,
        messages: List<MessageEntity>
    ): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
        val cleanTitle = chatTitle.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(exportDir, "export_${cleanTitle}_$timestamp.json")

        val root = JSONObject()
        root.put("chatTitle", chatTitle)
        root.put("exportedAt", dateFormat.format(Date()))
        root.put("messageCount", messages.size)

        val array = JSONArray()
        for (msg in messages) {
            val obj = JSONObject()
            obj.put("id", msg.id)
            obj.put("sender", msg.senderName)
            obj.put("text", msg.messageText)
            obj.put("timestamp", dateFormat.format(Date(msg.timestamp)))
            obj.put("isDeleted", msg.isDeleted)
            if (msg.deletedTimestamp != null) {
                obj.put("deletedAt", dateFormat.format(Date(msg.deletedTimestamp)))
            }
            obj.put("package", msg.packageName)
            array.put(obj)
        }
        root.put("messages", array)

        FileWriter(file).use { writer ->
            writer.write(root.toString(2))
        }

        file
    }

    suspend fun exportToCsv(
        context: Context,
        chatTitle: String,
        messages: List<MessageEntity>
    ): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
        val cleanTitle = chatTitle.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(exportDir, "export_${cleanTitle}_$timestamp.csv")

        FileWriter(file).use { writer ->
            writer.append("ID,Timestamp,Sender,Message,IsDeleted,DeletedAt,Package\n")
            for (msg in messages) {
                val formattedTime = dateFormat.format(Date(msg.timestamp))
                val deletedAt = msg.deletedTimestamp?.let { dateFormat.format(Date(it)) } ?: ""
                val escapedMsg = escapeCsv(msg.messageText)
                val escapedSender = escapeCsv(msg.senderName)

                writer.append("${msg.id},\"$formattedTime\",$escapedSender,$escapedMsg,${msg.isDeleted},\"$deletedAt\",\"${msg.packageName}\"\n")
            }
        }

        file
    }

    fun getShareIntent(context: Context, file: File, mimeType: String): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun escapeCsv(value: String): String {
        return "\"" + value.replace("\"", "\"\"").replace("\r\n", " ").replace("\n", " ").replace("\r", " ") + "\""
    }
}
