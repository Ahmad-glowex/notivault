package com.notivault.app.ui.screens.chat

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notivault.app.NotiVaultApp
import com.notivault.app.data.local.entity.ChatThreadEntity
import com.notivault.app.data.local.entity.MessageEntity
import com.notivault.app.export.DataExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatDetailViewModel(
    application: Application,
    private val threadId: String
) : AndroidViewModel(application) {

    private val app = application as NotiVaultApp
    private val messageRepo = app.messageRepository

    val thread: StateFlow<ChatThreadEntity?> = messageRepo.getThread(threadId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val messages: StateFlow<List<MessageEntity>> = messageRepo.getMessagesForThread(threadId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _exportIntent = MutableStateFlow<Intent?>(null)
    val exportIntent: StateFlow<Intent?> = _exportIntent.asStateFlow()

    fun exportToJson(context: Context) {
        viewModelScope.launch {
            val threadTitle = thread.value?.chatTitle ?: "Chat"
            val file = DataExporter.exportToJson(context, threadTitle, messages.value)
            _exportIntent.value = DataExporter.getShareIntent(context, file, "application/json")
        }
    }

    fun exportToCsv(context: Context) {
        viewModelScope.launch {
            val threadTitle = thread.value?.chatTitle ?: "Chat"
            val file = DataExporter.exportToCsv(context, threadTitle, messages.value)
            _exportIntent.value = DataExporter.getShareIntent(context, file, "text/csv")
        }
    }

    fun clearExportIntent() {
        _exportIntent.value = null
    }

    fun deleteMessage(id: Long) {
        viewModelScope.launch {
            messageRepo.deleteMessage(id)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            messageRepo.deleteThread(threadId)
        }
    }
}
