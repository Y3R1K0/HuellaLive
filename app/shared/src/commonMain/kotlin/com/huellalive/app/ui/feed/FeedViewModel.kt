package com.huellalive.app.ui.feed

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.huellalive.app.data.model.VideoDto
import com.huellalive.app.data.repository.AuthRepository
import com.huellalive.app.data.repository.FeedRepository
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedUiState(
    val videos: List<VideoDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false
)

class FeedViewModel(
    private val feedRepository: FeedRepository,
    val authRepository: AuthRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init { loadFeed() }

    fun loadFeed() {
        screenModelScope.launch {
            _uiState.value = FeedUiState(isLoading = true)
            when (val result = feedRepository.getFeed(1)) {
                is Resource.Success -> _uiState.value = FeedUiState(
                    videos = result.data,
                    hasMore = result.data.size >= 10
                )
                is Resource.Error -> _uiState.value = FeedUiState(errorMessage = result.message)
                else -> Unit
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        screenModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)
            val next = state.currentPage + 1
            when (val result = feedRepository.getFeed(next)) {
                is Resource.Success -> _uiState.value = state.copy(
                    videos = state.videos + result.data,
                    currentPage = next,
                    isLoadingMore = false,
                    hasMore = result.data.size >= 10
                )
                else -> _uiState.value = state.copy(isLoadingMore = false)
            }
        }
    }
}