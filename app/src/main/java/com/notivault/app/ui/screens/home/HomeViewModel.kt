package com.notivault.app.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notivault.app.NotiVaultApp
import com.notivault.app.data.local.entity.ChatThreadEntity
import com.notivault.app.ui.screens.home.components.AppTab
import com.notivault.app.ui.screens.home.components.isNotificationAccessGranted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NotiVaultApp
    private val messageRepo = app.messageRepository
    private val mediaRepo = app.mediaRepository

    private val _selectedTab = MutableStateFlow(AppTab.ALL)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isPermissionGranted = MutableStateFlow(isNotificationAccessGranted(application))
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    val deletedCount: StateFlow<Int> = messageRepo.getDeletedMessagesCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val mediaCount: StateFlow<Int> = mediaRepo.getMediaCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val threads: StateFlow<List<ChatThreadEntity>> = combine(_selectedTab, _searchQuery) { tab, query ->
        Pair(tab, query)
    }.flatMapLatest { (tab, query) ->
        val flow = if (query.isNotBlank()) {
            messageRepo.searchThreads(query, tab.packageName)
        } else if (tab.packageName != null) {
            messageRepo.getThreadsByPackage(tab.packageName)
        } else {
            messageRepo.getAllThreads()
        }
        flow
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun checkPermissionStatus() {
        _isPermissionGranted.value = isNotificationAccessGranted(getApplication())
    }

    fun togglePinThread(threadId: String, currentPin: Boolean) {
        viewModelScope.launch {
            messageRepo.setThreadPinned(threadId, !currentPin)
        }
    }

    fun deleteThread(threadId: String) {
        viewModelScope.launch {
            messageRepo.deleteThread(threadId)
        }
    }
}
