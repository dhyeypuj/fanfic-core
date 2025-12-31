package com.dhyey.fanfic.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyey.fanfic.storage.dao.FicDao
import com.dhyey.fanfic.storage.entity.FicEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LibraryUiState {
    data object Loading : LibraryUiState()
    data object Empty : LibraryUiState()
    data class Success(val fics: List<FicEntity>) : LibraryUiState()
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val ficDao: FicDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadFics()
    }

    fun loadFics() {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading
            val fics = ficDao.getAllFics()
            _uiState.value = if (fics.isEmpty()) {
                LibraryUiState.Empty
            } else {
                LibraryUiState.Success(fics)
            }
        }
    }
}
