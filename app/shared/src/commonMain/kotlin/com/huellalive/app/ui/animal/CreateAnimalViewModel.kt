package com.huellalive.app.ui.animal

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.huellalive.app.data.model.CreateAnimalRequest
import com.huellalive.app.data.model.SearchSpeciesDto
import com.huellalive.app.data.repository.AnimalRepository
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateAnimalUiState(
    val species: List<SearchSpeciesDto> = emptyList(),
    val isLoadingSpecies: Boolean = false,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class CreateAnimalViewModel(
    private val animalRepository: AnimalRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(CreateAnimalUiState())
    val uiState: StateFlow<CreateAnimalUiState> = _uiState.asStateFlow()

    init {
        loadSpecies()
    }

    private fun loadSpecies() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSpecies = true, errorMessage = null)
            _uiState.value = when (val result = animalRepository.getSpecies()) {
                is Resource.Success -> _uiState.value.copy(
                    species = result.data,
                    isLoadingSpecies = false
                )
                is Resource.Error -> _uiState.value.copy(
                    isLoadingSpecies = false,
                    errorMessage = result.message
                )
                else -> _uiState.value.copy(isLoadingSpecies = false)
            }
        }
    }

    fun createAnimal(
        name: String,
        species: String,
        requestedSpeciesName: String?,
        breed: String?,
        age: Int?,
        description: String?,
        status: String,
        birthDate: String?
    ) {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = animalRepository.createAnimal(
                CreateAnimalRequest(
                    name = name,
                    species = species,
                    requestedSpeciesName = requestedSpeciesName,
                    breed = breed,
                    age = age,
                    description = description,
                    status = status,
                    birthDate = birthDate
                )
            )
            _uiState.value = when (result) {
                is Resource.Success -> _uiState.value.copy(isLoading = false, isSuccess = true)
                is Resource.Error   -> _uiState.value.copy(isLoading = false, errorMessage = result.message)
                else -> _uiState.value.copy(isLoading = false)
            }
        }
    }
}
