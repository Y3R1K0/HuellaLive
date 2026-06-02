package com.huellalive.app.ui.animal

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.huellalive.app.data.local.SessionManager
import com.huellalive.app.data.model.AnimalDto
import com.huellalive.app.data.repository.AnimalRepository
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnimalDetailUiState(
    val animal: AnimalDto? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showCardDialog: Boolean = false
)

class AnimalDetailViewModel(
    private val animalId: String,
    private val animalRepository: AnimalRepository,
    val sessionManager: SessionManager
) : ScreenModel {

    private val _uiState = MutableStateFlow(AnimalDetailUiState())
    val uiState: StateFlow<AnimalDetailUiState> = _uiState.asStateFlow()

    init { loadAnimal() }

    fun loadAnimal() {
        screenModelScope.launch {
            _uiState.value = AnimalDetailUiState(isLoading = true)
            when (val result = animalRepository.getAnimalById(animalId)) {
                is Resource.Success -> _uiState.value = AnimalDetailUiState(animal = result.data)
                is Resource.Error   -> _uiState.value = AnimalDetailUiState(errorMessage = result.message)
                else -> Unit
            }
        }
    }

    fun toggleCardDialog() {
        _uiState.value = _uiState.value.copy(showCardDialog = !_uiState.value.showCardDialog)
    }
}