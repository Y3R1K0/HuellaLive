package com.huellalive.app.ui.feed

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.huellalive.app.data.model.VideoDto
import com.huellalive.app.data.repository.AuthRepository
import com.huellalive.app.data.repository.FeedRepository
import com.huellalive.app.ui.animal.AnimalUpdateSignal
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
    val currentVideoIndex: Int = 0,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val animalMediaRevision: Int = 0,
    val actionMessage: String? = null
)

class FeedViewModel(
    private val feedRepository: FeedRepository,
    val authRepository: AuthRepository
) : ScreenModel {
    private var handledRefreshVersion = 0
    private var handledAnimalUpdateVersion = 0
    private val markedViewedVideoIds = mutableSetOf<String>()

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        screenModelScope.launch {
            AnimalUpdateSignal.event.collect { event ->
                val animal = event.animal ?: return@collect
                if (event.version <= handledAnimalUpdateVersion) return@collect
                handledAnimalUpdateVersion = event.version

                val state = _uiState.value
                _uiState.value = state.copy(
                    videos = state.videos.map { video ->
                        val currentAnimal = video.animal
                        if (currentAnimal?.id != animal.id) {
                            video
                        } else {
                            video.copy(
                                animal = currentAnimal.copy(
                                    name = animal.name,
                                    species = animal.species,
                                    breed = animal.breed,
                                    photoUrl = animal.photoUrl,
                                    status = animal.status
                                )
                            )
                        }
                    },
                    animalMediaRevision = state.animalMediaRevision + 1
                )
            }
        }
    }

    fun loadFeedIfNeeded() {
        val state = _uiState.value
        if (state.videos.isNotEmpty() || state.isLoading) return
        loadFeed()
    }

    fun refreshIfNeeded(version: Int) {
        if (version <= handledRefreshVersion) {
            loadFeedIfNeeded()
            return
        }
        handledRefreshVersion = version
        loadFeed()
    }

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

    fun updateCurrentVideoIndex(index: Int) {
        val state = _uiState.value
        if (index == state.currentVideoIndex) return
        _uiState.value = state.copy(currentVideoIndex = index)
    }

    fun toggleLike(video: VideoDto) {
        if (video.isShelterStory) return
        val state = _uiState.value
        val nextVideo = video.copy(
            isLiked = !video.isLiked,
            likesCount = (video.likesCount + if (video.isLiked) -1 else 1).coerceAtLeast(0)
        )
        _uiState.value = state.replaceVideo(nextVideo)

        screenModelScope.launch {
            when (val result = if (video.isLiked) {
                feedRepository.unlikeVideo(video.id)
            } else {
                feedRepository.likeVideo(video.id)
            }) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.replaceVideo(result.data)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.replaceVideo(video)
                }
                else -> Unit
            }
        }
    }

    fun reportVideo(video: VideoDto, reason: String) {
        if (video.isShelterStory) {
            _uiState.value = _uiState.value.copy(actionMessage = "Reporte de historias disponible pronto")
            return
        }
        screenModelScope.launch {
            when (feedRepository.reportVideo(video.id, reason)) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(actionMessage = "Reporte enviado para revision")
                is Resource.Error -> _uiState.value = _uiState.value.copy(actionMessage = "No se pudo enviar el reporte")
                else -> Unit
            }
        }
    }

    fun reportAnimal(video: VideoDto, reason: String) {
        val animalId = video.animal?.id ?: return
        screenModelScope.launch {
            when (feedRepository.reportAnimal(animalId, reason)) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(actionMessage = "Perfil reportado para revision")
                is Resource.Error -> _uiState.value = _uiState.value.copy(actionMessage = "No se pudo reportar el perfil")
                else -> Unit
            }
        }
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }

    fun markVideoViewed(video: VideoDto) {
        if (video.isShelterStory) return
        if (!markedViewedVideoIds.add(video.id)) return
        screenModelScope.launch {
            feedRepository.markVideoViewed(video.id)
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

private fun FeedUiState.replaceVideo(video: VideoDto): FeedUiState =
    copy(videos = videos.map { current -> if (current.id == video.id) video else current })
