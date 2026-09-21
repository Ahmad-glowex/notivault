package com.notivault.app.ui.screens.viewonce

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notivault.app.NotiVaultApp
import com.notivault.app.data.local.entity.MediaEntity
import com.notivault.app.service.media.MediaCacheManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ViewOnceViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NotiVaultApp
    private val mediaRepo = app.mediaRepository
    private val cacheManager = MediaCacheManager(application)

    val viewOnceMedia: StateFlow<List<MediaEntity>> = mediaRepo.getViewOnceMedia()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val viewOnceCount: StateFlow<Int> = mediaRepo.getViewOnceMediaCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _connectionStatus = MutableStateFlow("INITIALIZING")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _selectedMedia = MutableStateFlow<MediaEntity?>(null)
    val selectedMedia: StateFlow<MediaEntity?> = _selectedMedia.asStateFlow()

    private val _isGuideExpanded = MutableStateFlow(false)
    val isGuideExpanded: StateFlow<Boolean> = _isGuideExpanded.asStateFlow()

    private val _lastCapturedTime = MutableStateFlow<Long?>(null)
    val lastCapturedTime: StateFlow<Long?> = _lastCapturedTime.asStateFlow()

    private val _pairingCode = MutableStateFlow<String?>(null)
    val pairingCode: StateFlow<String?> = _pairingCode.asStateFlow()

    fun updateConnectionStatus(status: String) {
        _connectionStatus.value = status
    }

    fun updatePairingCode(code: String?) {
        _pairingCode.value = code
    }

    fun clearPairingCode() {
        _pairingCode.value = null
    }

    fun toggleGuide() {
        _isGuideExpanded.value = !_isGuideExpanded.value
    }

    fun openMediaModal(media: MediaEntity) {
        _selectedMedia.value = media
    }

    fun closeMediaModal() {
        _selectedMedia.value = null
    }

    fun saveCapturedMedia(base64Data: String, mimeType: String, isViewOnce: Boolean = true) {
        viewModelScope.launch {
            try {
                val cleanMime = if (mimeType.isBlank()) "image/jpeg" else mimeType
                val cachedFile = cacheManager.cacheBase64Data(
                    base64Data = base64Data,
                    mimeType = cleanMime,
                    prefix = "view_once"
                )
                if (cachedFile != null) {
                    val mediaType = if (cleanMime.startsWith("video")) "VIEW_ONCE_VIDEO" else "VIEW_ONCE_IMAGE"
                    mediaRepo.saveCachedMedia(
                        packageName = "com.whatsapp.web",
                        originalPath = "view_once_${System.currentTimeMillis()}",
                        internalSavedPath = cachedFile.absolutePath,
                        fileName = cachedFile.name,
                        mimeType = cleanMime,
                        fileSizeBytes = cachedFile.length(),
                        mediaType = mediaType,
                        threadId = "whatsapp_web_view_once"
                    )
                    _lastCapturedTime.value = System.currentTimeMillis()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteMedia(id: Long) {
        viewModelScope.launch {
            mediaRepo.deleteMedia(id)
        }
    }

    fun clearAllViewOnceMedia() {
        viewModelScope.launch {
            viewOnceMedia.value.forEach { media ->
                mediaRepo.deleteMedia(media.id)
            }
        }
    }
}
