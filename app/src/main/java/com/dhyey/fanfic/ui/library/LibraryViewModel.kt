package com.dhyey.fanfic.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyey.fanfic.network.FicFetcher
import com.dhyey.fanfic.repository.FanficRepository
import com.dhyey.fanfic.storage.dao.FicDao
import com.dhyey.fanfic.storage.entity.FicEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

import timber.log.Timber

sealed class LibraryUiState {
    data object Loading : LibraryUiState()
    data object Empty : LibraryUiState()
    data class Success(val fics: List<FicEntity>) : LibraryUiState()
}

data class RefreshState(
    val isRefreshing: Boolean = false,
    val progress: Int = 0,
    val total: Int = 0,
    val currentFic: String = ""
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val ficDao: FicDao,
    private val ficFetcher: FicFetcher,
    private val repository: FanficRepository,
    private val syncManager: com.dhyey.fanfic.sync.SyncManager
) : ViewModel() {

    val isSyncing = syncManager.isSyncing
    val lastSynced = syncManager.lastSynced

    fun syncCloud() {
        viewModelScope.launch {
            val result = syncManager.sync()
            if (result.isFailure) {
                Timber.e(result.exceptionOrNull(), "Sync failed")
            } else {
                Timber.d("Sync success")
            }
        }
    }

    private val _filters = MutableStateFlow(LibraryFilters())
    val filters: StateFlow<LibraryFilters> = _filters.asStateFlow()

    private val _refreshState = MutableStateFlow(RefreshState())
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    val uiState: StateFlow<LibraryUiState> = combine(
        ficDao.observeAllFics(),
        _filters
    ) { fics, filters ->
        if (fics.isEmpty()) {
            LibraryUiState.Empty
        } else {
            val filteredFics = fics
                .filter { fic -> 
                    filters.query.isBlank() || 
                    fic.title.contains(filters.query, ignoreCase = true) || 
                    fic.author.contains(filters.query, ignoreCase = true)
                }
                .filter { fic -> matchesSourceFilter(fic, filters.source) }
                .filter { fic -> matchesStatusFilter(fic, filters.status) }
            
            val sortedFics = sortFics(filteredFics, filters.sort, filters.sortAscending)
            
            if (sortedFics.isEmpty()) {
                LibraryUiState.Empty
            } else {
                LibraryUiState.Success(sortedFics)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState.Loading
    )

    fun setSearchQuery(query: String) {
        _filters.value = _filters.value.copy(query = query)
    }

    fun setSort(option: SortOption) {
        _filters.value = _filters.value.copy(sort = option)
    }

    fun toggleSortOrder() {
        _filters.value = _filters.value.copy(sortAscending = !_filters.value.sortAscending)
    }

    fun setSourceFilter(filter: SourceFilter) {
        _filters.value = _filters.value.copy(source = filter)
    }

    fun setStatusFilter(filter: StatusFilter) {
        _filters.value = _filters.value.copy(status = filter)
    }

    fun clearFilters() {
        _filters.value = LibraryFilters()
    }

    fun refreshAll() {
        if (_refreshState.value.isRefreshing) return
        
        viewModelScope.launch {
            val allFics = ficDao.getAllFics()
            if (allFics.isEmpty()) return@launch
            
            _refreshState.value = RefreshState(
                isRefreshing = true,
                progress = 0,
                total = allFics.size
            )
            
            allFics.forEachIndexed { index, fic ->
                _refreshState.value = _refreshState.value.copy(
                    progress = index,
                    currentFic = fic.title
                )
                
                try {
                    val metadata = ficFetcher.fetchMetadata(fic.url)
                    val chapters = ficFetcher.fetchChapters(fic.url)
                    repository.updateFicMetadata(fic.ficId, metadata, chapters)
                } catch (e: Exception) {
                    // Continue with next fic even if one fails
                }
            }
            
            _refreshState.value = RefreshState(isRefreshing = false)
        }
    }

    private fun matchesSourceFilter(fic: FicEntity, filter: SourceFilter): Boolean {
        return when (filter) {
            SourceFilter.ALL -> true
            SourceFilter.FFN -> fic.site == "FFN"
            SourceFilter.AO3 -> fic.site == "AO3"
        }
    }

    private fun matchesStatusFilter(fic: FicEntity, filter: StatusFilter): Boolean {
        return when (filter) {
            StatusFilter.ALL -> true
            StatusFilter.COMPLETE -> fic.isComplete
            StatusFilter.ONGOING -> !fic.isComplete
        }
    }

    private fun sortFics(
        fics: List<FicEntity>,
        sortOption: SortOption,
        ascending: Boolean
    ): List<FicEntity> {
        val sorted = when (sortOption) {
            SortOption.LAST_READ -> fics.sortedByDescending { it.lastReadAt ?: 0L }
            SortOption.LAST_UPDATED -> fics.sortedByDescending { it.updated ?: "" }
            SortOption.WORD_COUNT -> fics.sortedByDescending { it.words }
            SortOption.CHAPTER_COUNT -> fics.sortedByDescending { it.chapters }
            SortOption.ALPHABETICAL -> fics.sortedBy { it.title.lowercase() }
            SortOption.DATE_ADDED -> fics.sortedByDescending { it.dateAdded }
        }
        
        return if (ascending && sortOption != SortOption.ALPHABETICAL) {
            sorted.reversed()
        } else if (!ascending && sortOption == SortOption.ALPHABETICAL) {
            sorted.reversed()
        } else {
            sorted
        }
    }
}

