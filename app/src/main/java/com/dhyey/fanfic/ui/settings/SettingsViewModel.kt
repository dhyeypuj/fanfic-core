package com.dhyey.fanfic.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyey.fanfic.cache.ChapterCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val chapterCache: ChapterCache,
    private val authService: com.dhyey.fanfic.auth.AuthService,
    private val syncManager: com.dhyey.fanfic.sync.SyncManager
) : ViewModel() {

    private val _cacheSize = MutableStateFlow(0L)
    val cacheSize: StateFlow<Long> = _cacheSize.asStateFlow()

    val currentUser = authService.currentUser
    val isSyncing = syncManager.isSyncing
    val lastSynced = syncManager.lastSynced

    init {
        loadCacheSize()
    }

    private fun loadCacheSize() {
        viewModelScope.launch {
            _cacheSize.value = chapterCache.totalCacheSizeBytes()
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val chapters = chapterCache.listCachedChapters()
            chapters.forEach { chapter ->
                chapterCache.deleteChapter(chapter.path)
            }
            loadCacheSize()
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            syncManager.sync()
        }
    }

    fun logOut() {
        authService.signOut()
    }
}
