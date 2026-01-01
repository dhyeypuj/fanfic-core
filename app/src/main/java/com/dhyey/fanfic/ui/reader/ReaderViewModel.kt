package com.dhyey.fanfic.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyey.fanfic.data.ReaderPreferences
import com.dhyey.fanfic.data.ReaderSettings
import com.dhyey.fanfic.data.ReaderTheme
import com.dhyey.fanfic.network.FicFetcher
import com.dhyey.fanfic.repository.FanficRepository
import com.dhyey.fanfic.storage.entity.FicEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ReaderUiState {
    data object Loading : ReaderUiState()
    data class Success(
        val chapterTitle: String,
        val htmlContent: String,
        val currentChapter: Int,
        val totalChapters: Int,
        val hasPrevious: Boolean,
        val hasNext: Boolean,
        val initialScrollPosition: Int = 0
    ) : ReaderUiState()
    data class Error(val message: String) : ReaderUiState()
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FanficRepository,
    private val ficFetcher: FicFetcher,
    private val readerPreferences: ReaderPreferences
) : ViewModel() {

    private val ficId: String = savedStateHandle.get<String>("ficId") ?: ""
    private val initialChapter: Int = savedStateHandle.get<Int>("chapter") ?: 1

    private var currentChapter = initialChapter
    private var fic: FicEntity? = null

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    val settings = readerPreferences.settings

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    init {
        loadChapter(initialChapter)
        // Update lastReadAt for "Last Read" sorting
        viewModelScope.launch {
            repository.updateLastRead(ficId)
        }
    }

    fun toggleSettings() {
        _showSettings.value = !_showSettings.value
    }

    fun setTheme(theme: ReaderTheme) {
        viewModelScope.launch {
            readerPreferences.setTheme(theme)
        }
    }

    fun setFontSize(size: Float) {
        viewModelScope.launch {
            readerPreferences.setFontSize(size)
        }
    }

    fun setLineHeight(height: Float) {
        viewModelScope.launch {
            readerPreferences.setLineHeight(height)
        }
    }

    private fun loadChapter(chapter: Int, saveCurrentPosition: Boolean = true) {
        viewModelScope.launch {
            // Save current reading position before switching chapters
            if (saveCurrentPosition) {
                val currentState = _uiState.value
                if (currentState is ReaderUiState.Success) {
                    // Position will be saved by the screen through saveScrollPosition
                }
            }
            
            _uiState.value = ReaderUiState.Loading

            try {
                if (fic == null) {
                    fic = repository.getFic(ficId)
                }

                val ficEntity = fic ?: throw IllegalStateException("Story not found")
                val chapters = repository.getChapters(ficId)
                val chapterEntity = chapters.firstOrNull { it.chapterNumber == chapter }

                val htmlContent = if (chapterEntity?.localPath != null) {
                    // Load from cache
                    try {
                        repository.loadChapterContent(chapterEntity)
                    } catch (e: Exception) {
                        // Cache corrupted, re-fetch
                        fetchAndCacheChapter(ficEntity, chapter, chapterEntity?.chapterId)
                    }
                } else {
                    // Fetch and cache for offline use
                    fetchAndCacheChapter(ficEntity, chapter, chapterEntity?.chapterId)
                }

                // Get saved reading position for this chapter
                val savedPosition = repository.getReadingPosition(ficId, chapter)

                currentChapter = chapter
                _uiState.value = ReaderUiState.Success(
                    chapterTitle = chapterEntity?.title ?: "Chapter $chapter",
                    htmlContent = htmlContent,
                    currentChapter = chapter,
                    totalChapters = ficEntity.chapters,
                    hasPrevious = chapter > 1,
                    hasNext = chapter < ficEntity.chapters,
                    initialScrollPosition = savedPosition
                )
            } catch (e: Exception) {
                _uiState.value = ReaderUiState.Error(e.message ?: "Failed to load chapter")
            }
        }
    }

    private suspend fun fetchAndCacheChapter(ficEntity: FicEntity, chapter: Int, chapterId: String? = null): String {
        // Pass chapterId for AO3 individual chapter navigation
        val html = ficFetcher.fetchChapterContent(ficEntity.url, chapter, chapterId)
        // Use site-aware content parsing from FicFetcher
        val storyContent = ficFetcher.parseChapterContent(html, ficEntity.url)
        
        // If parseChapterContent returns empty, fallback to legacy extraction
        val contentToCache = storyContent.ifBlank { extractStoryContent(html) }
        
        // Cache for offline reading (10MB max cache)
        try {
            repository.cacheChapterContent(ficEntity.ficId, chapter, contentToCache, 10 * 1024 * 1024)
        } catch (e: Exception) {
            // Caching failed, but we can still show the content
        }
        
        return contentToCache
    }

    fun nextChapter() {
        val state = _uiState.value
        if (state is ReaderUiState.Success && state.hasNext) {
            loadChapter(currentChapter + 1)
        }
    }

    fun previousChapter() {
        val state = _uiState.value
        if (state is ReaderUiState.Success && state.hasPrevious) {
            loadChapter(currentChapter - 1)
        }
    }

    fun saveScrollPosition(scrollY: Int) {
        viewModelScope.launch {
            repository.saveReadingPosition(ficId, currentChapter, scrollY)
        }
    }

    private fun extractStoryContent(html: String): String {
        return try {
            val doc = org.jsoup.Jsoup.parse(html)
            
            // Try to find FFN's story text div
            val storyText = doc.selectFirst("#storytext")
            if (storyText != null) {
                return storyText.html()
            }
            
            // Try mobile site format
            val storyDiv = doc.selectFirst("div.storycontent")
            if (storyDiv != null) {
                return storyDiv.html()
            }
            
            // Try to find main content area
            val mainContent = doc.selectFirst("article") 
                ?: doc.selectFirst(".storytext")
                ?: doc.selectFirst("#content")
            if (mainContent != null) {
                return mainContent.html()
            }
            
            // If still not found, try to extract body content without scripts/styles
            doc.select("script, style, nav, header, footer, aside").remove()
            doc.body()?.html() ?: html
        } catch (e: Exception) {
            html
        }
    }
}
