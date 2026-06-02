package com.huellalive.app.ui.human

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.huellalive.app.data.model.AnimalDto
import com.huellalive.app.data.model.UserProfileDto
import com.huellalive.app.data.repository.AnimalRepository
import com.huellalive.app.data.repository.AuthRepository
import com.huellalive.app.data.repository.UserRepository
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HumanProfileUiState(
    val user: UserProfileDto? = null,
    val adoptedAnimals: List<AnimalDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class HumanProfileViewModel(
    private val userRepository: UserRepository,
    private val animalRepository: AnimalRepository,
    val authRepository: AuthRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(HumanProfileUiState())
    val uiState: StateFlow<HumanProfileUiState> = _uiState.asStateFlow()

    init { loadProfile() }

    fun loadProfile() {
        screenModelScope.launch {
            _uiState.value = HumanProfileUiState(isLoading = true)
            val userResult = userRepository.getMe()
            val animalsResult = animalRepository.getAdoptedAnimals()
            _uiState.value = HumanProfileUiState(
                user = (userResult as? Resource.Success)?.data,
                adoptedAnimals = (animalsResult as? Resource.Success)?.data ?: emptyList(),
                isLoading = false,
                errorMessage = (userResult as? Resource.Error)?.message
            )
        }
    }

    fun logout() = authRepository.logout()
}