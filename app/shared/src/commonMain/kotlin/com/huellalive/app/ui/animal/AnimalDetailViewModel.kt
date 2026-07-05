package com.huellalive.app.ui.animal

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.huellalive.app.data.local.SessionManager
import com.huellalive.app.data.model.AdoptionRequestStateDto
import com.huellalive.app.data.model.ControlDto
import com.huellalive.app.data.model.AnimalDto
import com.huellalive.app.data.model.SearchSpeciesDto
import com.huellalive.app.data.model.UpdateAnimalRequest
import com.huellalive.app.data.model.UpdateAnimalCardRequest
import com.huellalive.app.data.model.VaccineDto
import com.huellalive.app.data.repository.AnimalRepository
import com.huellalive.app.data.repository.EngagementRepository
import com.huellalive.app.data.repository.MediaRepository
import com.huellalive.app.data.repository.WalletRepository
import com.huellalive.app.ui.feed.FeedRefreshSignal
import com.huellalive.app.utils.PickedMedia
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnimalDetailUiState(
    val animal: AnimalDto? = null,
    val species: List<SearchSpeciesDto> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingSpecies: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val actionInProgress: String? = null,
    val adoptionRequestState: AdoptionRequestStateDto? = null,
    val showCardDialog: Boolean = false,
    val mediaRevision: Int = 0
)

class AnimalDetailViewModel(
    private val animalId: String,
    private val animalRepository: AnimalRepository,
    private val engagementRepository: EngagementRepository,
    private val mediaRepository: MediaRepository,
    private val walletRepository: WalletRepository,
    val sessionManager: SessionManager
) : ScreenModel {
    private var handledAnimalUpdateVersion = 0

    private val _uiState = MutableStateFlow(AnimalDetailUiState())
    val uiState: StateFlow<AnimalDetailUiState> = _uiState.asStateFlow()

    init {
        loadAnimal()
        loadSpecies()
        screenModelScope.launch {
            AnimalUpdateSignal.event.collect { event ->
                val animal = event.animal ?: return@collect
                if (animal.id != animalId || event.version <= handledAnimalUpdateVersion) return@collect
                handledAnimalUpdateVersion = event.version
                _uiState.value = _uiState.value.copy(animal = animal)
            }
        }
    }

    fun loadAnimal() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = animalRepository.getAnimalById(animalId)) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(
                    animal = result.data,
                    isLoading = false
                )
                is Resource.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
                else -> Unit
            }
        }
    }

    fun toggleCardDialog() {
        _uiState.value = _uiState.value.copy(showCardDialog = !_uiState.value.showCardDialog)
    }

    private fun loadSpecies() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSpecies = true)
            when (val result = animalRepository.getSpecies()) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(
                    species = result.data,
                    isLoadingSpecies = false
                )
                is Resource.Error -> _uiState.value = _uiState.value.copy(
                    actionMessage = result.message,
                    isLoadingSpecies = false
                )
                else -> _uiState.value = _uiState.value.copy(isLoadingSpecies = false)
            }
        }
    }

    fun setActionMessage(message: String) {
        _uiState.value = _uiState.value.copy(actionMessage = message)
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }

    fun requestAdoption() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = "adoption")
            when (val result = engagementRepository.requestAdoption(animalId)) {
                is Resource.Success -> {
                    val previousCount = _uiState.value.adoptionRequestState?.requestsToday ?: 0
                    _uiState.value = _uiState.value.copy(
                        actionMessage = "Solicitud enviada al albergue",
                        actionInProgress = null,
                        adoptionRequestState = AdoptionRequestStateDto(
                            hasActiveRequest = true,
                            activeStatus = "PENDING",
                            requestsToday = previousCount + 1,
                            dailyLimit = 10,
                            dailyLimitReached = previousCount + 1 >= 10
                        )
                    )
                }
                is Resource.Error -> _uiState.value = _uiState.value.copy(
                    actionMessage = result.message,
                    actionInProgress = null
                )
                else -> _uiState.value = _uiState.value.copy(actionInProgress = null)
            }
        }
    }

    fun deleteVideo(videoId: String) {
        screenModelScope.launch {
            when (val result = engagementRepository.deleteVideo(videoId)) {
                is Resource.Success -> {
                    FeedRefreshSignal.invalidate()
                    val animal = _uiState.value.animal
                    _uiState.value = _uiState.value.copy(
                        animal = animal?.copy(videos = animal.videos.filterNot { it.id == videoId }),
                        actionMessage = "Video eliminado"
                    )
                }
                is Resource.Error -> _uiState.value = _uiState.value.copy(actionMessage = result.message)
                else -> Unit
            }
            loadAdoptionState()
        }
    }

    fun refreshForSession() {
        loadAnimal()
    }

    private fun loadAdoptionState() {
        if (!sessionManager.isHuman()) {
            _uiState.value = _uiState.value.copy(adoptionRequestState = null)
            return
        }
        screenModelScope.launch {
            when (val result = engagementRepository.getAdoptionRequestState(animalId)) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(adoptionRequestState = result.data)
                is Resource.Error -> _uiState.value = _uiState.value.copy(actionMessage = result.message)
                else -> Unit
            }
        }
    }

    fun updateAnimal(
        name: String,
        species: String,
        breed: String,
        age: String,
        description: String,
        status: String,
        selectedPhoto: PickedMedia?,
        currentPhotoUrl: String?,
        onDone: () -> Unit
    ) {
        val parsedAge = age.toIntOrNull()
        if (name.isBlank() || species.isBlank()) {
            _uiState.value = _uiState.value.copy(actionMessage = "Nombre y especie son requeridos")
            return
        }
        if (age.isNotBlank() && parsedAge == null) {
            _uiState.value = _uiState.value.copy(actionMessage = "Edad invalida")
            return
        }

        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(actionMessage = "Guardando animal...")
            var photoUrl = currentPhotoUrl
            if (selectedPhoto != null) {
                when (val upload = mediaRepository.uploadAnimalPhoto(
                    animalId = animalId,
                    bytes = selectedPhoto.bytes,
                    fileName = selectedPhoto.fileName,
                    contentType = selectedPhoto.contentType
                )) {
                    is Resource.Success -> photoUrl = upload.data.url
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(actionMessage = upload.message)
                        return@launch
                    }
                    else -> Unit
                }
            }

            val request = UpdateAnimalRequest(
                name = name.trim(),
                species = species.trim(),
                breed = breed.ifBlank { null },
                age = parsedAge,
                description = description.ifBlank { null },
                photoUrl = photoUrl,
                status = status
            )
            when (val result = animalRepository.updateAnimal(animalId, request)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        animal = result.data,
                        actionMessage = "Animal actualizado",
                        mediaRevision = _uiState.value.mediaRevision + if (selectedPhoto != null) 1 else 0
                    )
                    AnimalUpdateSignal.publish(result.data)
                    onDone()
                }
                is Resource.Error -> _uiState.value = _uiState.value.copy(actionMessage = result.message)
                else -> Unit
            }
        }
    }

    fun updateAnimalCard(
        sex: String,
        birthDate: String,
        isSterilized: Boolean,
        vaccines: List<VaccineDto>,
        controls: List<ControlDto>,
        notes: String,
        onDone: () -> Unit
    ) {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = "card", actionMessage = null)
            val request = UpdateAnimalCardRequest(
                sex = sex.ifBlank { null },
                birthDate = birthDate.ifBlank { null },
                isSterilized = isSterilized,
                vaccines = vaccines,
                controls = controls,
                notes = notes.ifBlank { null }
            )
            when (val result = animalRepository.updateAnimalCard(animalId, request)) {
                is Resource.Success -> {
                    val animal = _uiState.value.animal
                    _uiState.value = _uiState.value.copy(
                        animal = animal?.copy(card = result.data),
                        actionInProgress = null,
                        actionMessage = "Cartilla actualizada"
                    )
                    onDone()
                }
                is Resource.Error -> _uiState.value = _uiState.value.copy(
                    actionInProgress = null,
                    actionMessage = result.message
                )
                else -> _uiState.value = _uiState.value.copy(actionInProgress = null)
            }
        }
    }
}
