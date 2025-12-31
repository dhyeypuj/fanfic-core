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
    private val chapterCache: ChapterCache
) : ViewModel() {

    private val _cacheSize = MutableStateFlow(0L)
    val cacheSize: StateFlow<Long> = _cacheSize.asStateFlow()

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
}
