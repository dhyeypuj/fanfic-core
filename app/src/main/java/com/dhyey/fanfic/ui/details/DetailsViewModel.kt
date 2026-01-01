package com.dhyey.fanfic.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyey.fanfic.network.FicFetcher
import com.dhyey.fanfic.repository.FanficRepository
import com.dhyey.fanfic.storage.entity.ChapterEntity
import com.dhyey.fanfic.storage.entity.FicEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DetailsUiState {
    data object Loading : DetailsUiState()
    data class Success(val fic: FicEntity, val chapters: List<ChapterEntity>) : DetailsUiState()
    data class Error(val message: String) : DetailsUiState()
}

data class DownloadState(
    val isDownloading: Boolean = false,
    val progress: Int = 0,
    val total: Int = 0,
    val currentChapter: String = ""
)

@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FanficRepository,
    private val ficFetcher: FicFetcher
) : ViewModel() {

    private val ficId: String = savedStateHandle.get<String>("ficId") ?: ""

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    init {
        loadDetails()
    }

    private fun loadDetails() {
        viewModelScope.launch {
            _uiState.value = DetailsUiState.Loading
            try {
                val fic = repository.getFic(ficId)
                val chapters = repository.getChapters(ficId)

                if (fic != null) {
                    _uiState.value = DetailsUiState.Success(fic, chapters)
                } else {
                    _uiState.value = DetailsUiState.Error("Story not found")
                }
            } catch (e: Exception) {
                _uiState.value = DetailsUiState.Error(e.message ?: "Failed to load")
            }
        }
    }

    fun downloadAllChapters() {
        val state = _uiState.value
        if (state !is DetailsUiState.Success || _downloadState.value.isDownloading) return

        viewModelScope.launch {
            val fic = state.fic
            val chapters = state.chapters
            val uncachedChapters = chapters.filter { it.localPath == null }

            if (uncachedChapters.isEmpty()) return@launch

            _downloadState.value = DownloadState(
                isDownloading = true,
                progress = 0,
                total = uncachedChapters.size
            )

            uncachedChapters.forEachIndexed { index, chapter ->
                try {
                    _downloadState.value = _downloadState.value.copy(
                        progress = index,
                        currentChapter = chapter.title
                    )

                    val html = ficFetcher.fetchChapterContent(fic.url, chapter.chapterNumber)
                    val storyContent = extractStoryContent(html)
                    repository.cacheChapterContent(ficId, chapter.chapterNumber, storyContent, 50 * 1024 * 1024)
                } catch (e: Exception) {
                    // Skip failed chapters, continue with others
                }
            }

            _downloadState.value = DownloadState(isDownloading = false)
            loadDetails() // Refresh to show updated cache status
        }
    }

    fun cancelDownload() {
        _downloadState.value = DownloadState(isDownloading = false)
    }

    fun deleteFic() {
        viewModelScope.launch {
            repository.deleteFic(ficId)
        }
    }

    private fun extractStoryContent(html: String): String {
        return try {
            val doc = org.jsoup.Jsoup.parse(html)
            doc.selectFirst("#storytext")?.html()
                ?: doc.selectFirst("div.storycontent")?.html()
                ?: doc.selectFirst("article")?.html()
                ?: html
        } catch (e: Exception) {
            html
        }
    }
}

