package com.huellalive.app.ui.shelter

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.huellalive.app.data.model.AnimalDto
import com.huellalive.app.data.model.ShelterProfileDto
import com.huellalive.app.data.repository.AnimalRepository
import com.huellalive.app.data.repository.AuthRepository
import com.huellalive.app.data.repository.EngagementRepository
import com.huellalive.app.data.repository.MediaRepository
import com.huellalive.app.data.repository.ShelterRepository
import com.huellalive.app.data.repository.UserRepository
import com.huellalive.app.ui.animal.AnimalUpdateSignal
import com.huellalive.app.ui.feed.FeedRefreshSignal
import com.huellalive.app.utils.PickedMedia
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShelterProfileUiState(
    val shelter: ShelterProfileDto? = null,
    val animals: List<AnimalDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val mediaRevision: Int = 0
)

class ShelterProfileViewModel(
    private val shelterRepository: ShelterRepository,
    private val animalRepository: AnimalRepository,
    private val mediaRepository: MediaRepository,
    private val userRepository: UserRepository,
    private val engagementRepository: EngagementRepository,
    val authRepository: AuthRepository
) : ScreenModel {
    private var handledAnimalUpdateVersion = 0
    private var handledStoryUpdateVersion = 0

    private val _uiState = MutableStateFlow(ShelterProfileUiState())
    val uiState: StateFlow<ShelterProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        screenModelScope.launch {
            AnimalUpdateSignal.event.collect { event ->
                val animal = event.animal ?: return@collect
                if (event.version <= handledAnimalUpdateVersion) return@collect
                handledAnimalUpdateVersion = event.version

                val state = _uiState.value
                val animalStillBelongsToShelter = animal.status != "ADOPTED" && animal.adoptedBy == null
                _uiState.value = state.copy(
                    animals = if (animalStillBelongsToShelter) {
                        state.animals.map { current -> if (current.id == animal.id) animal else current }
                    } else {
                        state.animals.filterNot { it.id == animal.id }
                    },
                    shelter = state.shelter?.copy(
                        animals = if (animalStillBelongsToShelter) {
                            state.shelter.animals.map { current -> if (current.id == animal.id) animal else current }
                        } else {
                            state.shelter.animals.filterNot { it.id == animal.id }
                        }
                    ),
                    mediaRevision = state.mediaRevision + 1
                )
            }
        }
        screenModelScope.launch {
            ShelterStoryUpdateSignal.event.collect { event ->
                if (event.version <= handledStoryUpdateVersion) return@collect
                handledStoryUpdateVersion = event.version

                val state = _uiState.value
                val shelter = state.shelter ?: return@collect
                val nextStories = when {
                    event.deletedStoryId != null -> shelter.stories.filterNot { it.id == event.deletedStoryId }
                    event.story != null -> listOf(event.story) + shelter.stories.filterNot { it.id == event.story.id }
                    else -> shelter.stories
                }
                _uiState.value = state.copy(
                    shelter = shelter.copy(stories = nextStories.take(12)),
                    mediaRevision = state.mediaRevision + 1
                )
            }
        }
    }

    fun loadProfile() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val shelterResult = shelterRepository.getMyShelterProfile()
            val animalsResult = animalRepository.getMyAnimals()
            _uiState.value = _uiState.value.copy(
                shelter = (shelterResult as? Resource.Success)?.data,
                animals = (animalsResult as? Resource.Success)?.data ?: emptyList(),
                isLoading = false,
                errorMessage = (shelterResult as? Resource.Error)?.message,
                actionMessage = _uiState.value.actionMessage
            )
        }
    }

    fun logout() = authRepository.logout()

    fun setActionMessage(message: String) {
        _uiState.value = _uiState.value.copy(actionMessage = message)
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }

    fun deleteShelterStory(storyId: String) {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(actionMessage = "Eliminando historia...")
            when (val result = engagementRepository.deleteShelterStory(storyId)) {
                is Resource.Success -> {
                    val state = _uiState.value
                    _uiState.value = state.copy(
                        shelter = state.shelter?.copy(
                            stories = state.shelter.stories.filterNot { it.id == storyId }
                        ),
                        actionMessage = "Historia eliminada",
                        mediaRevision = state.mediaRevision + 1
                    )
                    ShelterStoryUpdateSignal.delete(storyId)
                    FeedRefreshSignal.invalidate()
                }
                is Resource.Error -> _uiState.value = _uiState.value.copy(actionMessage = result.message)
                else -> Unit
            }
        }
    }

    fun updateProfile(
        name: String,
        description: String,
        location: String,
        latitude: String,
        longitude: String,
        avatar: PickedMedia?,
        cover: PickedMedia?
    ) {
        screenModelScope.launch {
            if (name.isBlank()) {
                _uiState.value = _uiState.value.copy(actionMessage = "El nombre del albergue es requerido")
                return@launch
            }
            val latitudeNumber = latitude.trim().takeIf { it.isNotBlank() }?.toDoubleOrNull()
            val longitudeNumber = longitude.trim().takeIf { it.isNotBlank() }?.toDoubleOrNull()
            if (latitude.isNotBlank() && latitudeNumber == null || longitude.isNotBlank() && longitudeNumber == null) {
                _uiState.value = _uiState.value.copy(actionMessage = "Coordenadas invalidas")
                return@launch
            }
            _uiState.value = _uiState.value.copy(actionMessage = "Guardando perfil...")
            var avatarUrl = _uiState.value.shelter?.user?.avatarUrl
            var coverUrl = _uiState.value.shelter?.coverUrl

            if (avatar != null) {
                when (val upload = mediaRepository.uploadShelterAvatar(avatar.bytes, avatar.fileName, avatar.contentType)) {
                    is Resource.Success -> avatarUrl = upload.data.url
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(actionMessage = upload.message)
                        return@launch
                    }
                    else -> Unit
                }
            }

            if (cover != null) {
                when (val upload = mediaRepository.uploadShelterCover(cover.bytes, cover.fileName, cover.contentType)) {
                    is Resource.Success -> coverUrl = upload.data.url
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(actionMessage = upload.message)
                        return@launch
                    }
                    else -> Unit
                }
            }

            val shelterData = buildMap {
                put("description", description)
                put("location", location)
                put("latitude", latitudeNumber?.toString().orEmpty())
                put("longitude", longitudeNumber?.toString().orEmpty())
                if (!coverUrl.isNullOrBlank()) put("coverUrl", coverUrl)
            }

            val userData = buildMap {
                put("name", name.trim())
                if (!avatarUrl.isNullOrBlank()) put("avatarUrl", avatarUrl)
            }
            if (userData.isNotEmpty()) {
                when (val userResult = userRepository.updateMe(userData)) {
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(actionMessage = userResult.message)
                        return@launch
                    }
                    else -> Unit
                }
            }

            when (val result = shelterRepository.updateShelterProfile(shelterData)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        shelter = result.data,
                        actionMessage = "Perfil actualizado",
                        mediaRevision = _uiState.value.mediaRevision +
                            if (avatar != null || cover != null) 1 else 0
                    )
                    loadProfile()
                }
                is Resource.Error -> _uiState.value = _uiState.value.copy(actionMessage = result.message)
                else -> Unit
            }
        }
    }
}
