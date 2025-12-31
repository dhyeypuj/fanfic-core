package com.dhyey.fanfic.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FanficRepository
) : ViewModel() {

    private val ficId: String = savedStateHandle.get<String>("ficId") ?: ""

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

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

    fun deleteFic() {
        viewModelScope.launch {
            repository.deleteFic(ficId)
        }
    }
}
