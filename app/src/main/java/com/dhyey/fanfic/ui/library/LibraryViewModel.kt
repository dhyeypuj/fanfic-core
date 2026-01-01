package com.dhyey.fanfic.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyey.fanfic.storage.dao.FicDao
import com.dhyey.fanfic.storage.entity.FicEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed class LibraryUiState {
    data object Loading : LibraryUiState()
    data object Empty : LibraryUiState()
    data class Success(val fics: List<FicEntity>) : LibraryUiState()
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    ficDao: FicDao
) : ViewModel() {

    val uiState: StateFlow<LibraryUiState> = ficDao.observeAllFics()
        .map { fics ->
            if (fics.isEmpty()) {
                LibraryUiState.Empty
            } else {
                LibraryUiState.Success(fics)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LibraryUiState.Loading
        )
}

