package com.notivault.app.ui.screens.media

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notivault.app.NotiVaultApp
import com.notivault.app.data.local.entity.MediaEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NotiVaultApp
    private val mediaRepo = app.mediaRepository

    private val _selectedFilter = MutableStateFlow("ALL")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _selectedMedia = MutableStateFlow<MediaEntity?>(null)
    val selectedMedia: StateFlow<MediaEntity?> = _selectedMedia.asStateFlow()

    val mediaList: StateFlow<List<MediaEntity>> = _selectedFilter.flatMapLatest { filter ->
        when (filter) {
            "ALL" -> mediaRepo.getAllMedia()
            "VIEW_ONCE" -> mediaRepo.getViewOnceMedia()
            else -> mediaRepo.getMediaByType(filter)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun openMediaModal(media: MediaEntity) {
        _selectedMedia.value = media
    }

    fun closeMediaModal() {
        _selectedMedia.value = null
    }

    fun deleteMedia(id: Long) {
        viewModelScope.launch {
            mediaRepo.deleteMedia(id)
        }
    }

    fun clearAllMedia() {
        viewModelScope.launch {
            mediaRepo.clearAllMedia()
        }
    }
}
