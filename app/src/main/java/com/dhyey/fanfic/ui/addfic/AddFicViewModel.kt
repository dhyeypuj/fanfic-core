package com.dhyey.fanfic.ui.addfic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhyey.fanfic.network.FicFetcher
import com.dhyey.fanfic.repository.FanficRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AddFicUiState {
    data object Idle : AddFicUiState()
    data object Loading : AddFicUiState()
    data class Success(val ficId: String) : AddFicUiState()
    data class Error(val message: String) : AddFicUiState()
}

@HiltViewModel
class AddFicViewModel @Inject constructor(
    private val ficFetcher: FicFetcher,
    private val repository: FanficRepository
) : ViewModel() {

    var url by mutableStateOf("")
        private set

    private val _uiState = MutableStateFlow<AddFicUiState>(AddFicUiState.Idle)
    val uiState: StateFlow<AddFicUiState> = _uiState.asStateFlow()

    fun updateUrl(newUrl: String) {
        url = newUrl
        if (_uiState.value is AddFicUiState.Error) {
            _uiState.value = AddFicUiState.Idle
        }
    }

    fun addFic() {
        if (url.isBlank()) return

        viewModelScope.launch {
            _uiState.value = AddFicUiState.Loading

            try {
                val metadata = ficFetcher.fetchMetadata(url)
                val chapters = ficFetcher.fetchChapters(url)

                val ficId = generateFicId(url)

                repository.saveFic(ficId, metadata, chapters)

                _uiState.value = AddFicUiState.Success(ficId)
            } catch (e: Exception) {
                _uiState.value = AddFicUiState.Error(
                    e.message ?: "Failed to add story"
                )
            }
        }
    }

    private fun generateFicId(url: String): String {
        // Extract story ID from FFN URL: https://www.fanfiction.net/s/{id}/...
        val parts = url.split("/")
        val sIndex = parts.indexOf("s")
        return if (sIndex != -1 && sIndex + 1 < parts.size) {
            "ffn:${parts[sIndex + 1]}"
        } else {
            "fic:${url.hashCode()}"
        }
    }
}
