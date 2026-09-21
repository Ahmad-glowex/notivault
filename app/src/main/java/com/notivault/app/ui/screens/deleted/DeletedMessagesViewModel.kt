package com.notivault.app.ui.screens.deleted

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notivault.app.NotiVaultApp
import com.notivault.app.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeletedMessagesViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NotiVaultApp
    private val messageRepo = app.messageRepository

    val deletedMessages: StateFlow<List<MessageEntity>> = messageRepo.getAllDeletedMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearAllDeleted() {
        viewModelScope.launch {
            messageRepo.clearAllDeletedMessages()
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            messageRepo.deleteMessage(messageId)
        }
    }
}
