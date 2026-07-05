package com.huellalive.app.ui.human

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.huellalive.app.data.model.AnimalDto
import com.huellalive.app.data.model.UserProfileDto
import com.huellalive.app.data.repository.AnimalRepository
import com.huellalive.app.data.repository.AuthRepository
import com.huellalive.app.data.repository.MediaRepository
import com.huellalive.app.data.repository.UserRepository
import com.huellalive.app.utils.PickedMedia
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HumanProfileUiState(
    val user: UserProfileDto? = null,
    val adoptedAnimals: List<AnimalDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val mediaRevision: Int = 0
)

class HumanProfileViewModel(
    private val userRepository: UserRepository,
    private val animalRepository: AnimalRepository,
    private val mediaRepository: MediaRepository,
    val authRepository: AuthRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(HumanProfileUiState())
    val uiState: StateFlow<HumanProfileUiState> = _uiState.asStateFlow()

    init { loadProfile() }

    fun loadProfile() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val userResult = userRepository.getMe()
            val animalsResult = animalRepository.getAdoptedAnimals()
            _uiState.value = _uiState.value.copy(
                user = (userResult as? Resource.Success)?.data,
                adoptedAnimals = (animalsResult as? Resource.Success)?.data ?: emptyList(),
                isLoading = false,
                errorMessage = (userResult as? Resource.Error)?.message,
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

    fun linkAnimal(username: String, password: String) {
        screenModelScope.launch {
            when (val result = authRepository.linkAnimal(username, password)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(actionMessage = "Mascota vinculada")
                    loadProfile()
                }
                is Resource.Error -> _uiState.value = _uiState.value.copy(actionMessage = result.message)
                else -> Unit
            }
        }
    }

    fun updateProfile(name: String, avatar: PickedMedia?, selectedBadgeId: String?) {
        screenModelScope.launch {
            if (name.isBlank()) {
                _uiState.value = _uiState.value.copy(actionMessage = "El nombre es requerido")
                return@launch
            }
            _uiState.value = _uiState.value.copy(actionMessage = "Guardando perfil...")
            var avatarUrl = _uiState.value.user?.avatarUrl

            if (avatar != null) {
                when (val upload = mediaRepository.uploadHumanAvatar(avatar.bytes, avatar.fileName, avatar.contentType)) {
                    is Resource.Success -> avatarUrl = upload.data.url
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(actionMessage = upload.message)
                        return@launch
                    }
                    else -> Unit
                }
            }

            val data = buildMap {
                put("name", name.trim())
                if (!avatarUrl.isNullOrBlank()) put("avatarUrl", avatarUrl)
            }
            when (val result = userRepository.updateMe(data)) {
                is Resource.Success -> {
                    when (val badgeResult = userRepository.selectProfileBadge(selectedBadgeId)) {
                        is Resource.Success -> _uiState.value = _uiState.value.copy(
                            user = badgeResult.data,
                            actionMessage = "Perfil actualizado",
                            mediaRevision = _uiState.value.mediaRevision + if (avatar != null) 1 else 0
                        )
                        is Resource.Error -> _uiState.value = _uiState.value.copy(actionMessage = badgeResult.message)
                        else -> Unit
                    }
                }
                is Resource.Error -> _uiState.value = _uiState.value.copy(actionMessage = result.message)
                else -> Unit
            }
        }
    }
}
