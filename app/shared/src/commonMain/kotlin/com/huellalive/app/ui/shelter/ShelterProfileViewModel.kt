package com.huellalive.app.ui.shelter

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.huellalive.app.data.model.AnimalDto
import com.huellalive.app.data.model.ShelterProfileDto
import com.huellalive.app.data.repository.AnimalRepository
import com.huellalive.app.data.repository.AuthRepository
import com.huellalive.app.data.repository.ShelterRepository
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShelterProfileUiState(
    val shelter: ShelterProfileDto? = null,
    val animals: List<AnimalDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ShelterProfileViewModel(
    private val shelterRepository: ShelterRepository,
    private val animalRepository: AnimalRepository,
    val authRepository: AuthRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(ShelterProfileUiState())
    val uiState: StateFlow<ShelterProfileUiState> = _uiState.asStateFlow()

    init { loadProfile() }

    fun loadProfile() {
        screenModelScope.launch {
            _uiState.value = ShelterProfileUiState(isLoading = true)
            val shelterResult = shelterRepository.getMyShelterProfile()
            val animalsResult = animalRepository.getMyAnimals()
            _uiState.value = ShelterProfileUiState(
                shelter = (shelterResult as? Resource.Success)?.data,
                animals = (animalsResult as? Resource.Success)?.data ?: emptyList(),
                isLoading = false,
                errorMessage = (shelterResult as? Resource.Error)?.message
            )
        }
    }

    fun logout() = authRepository.logout()
}