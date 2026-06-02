package com.huellalive.app.ui.animal

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.huellalive.app.data.model.CreateAnimalRequest
import com.huellalive.app.data.repository.AnimalRepository
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateAnimalUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class CreateAnimalViewModel(
    private val animalRepository: AnimalRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(CreateAnimalUiState())
    val uiState: StateFlow<CreateAnimalUiState> = _uiState.asStateFlow()

    fun createAnimal(
        name: String,
        species: String,
        breed: String?,
        age: Int?,
        description: String?,
        status: String,
        birthDate: String?
    ) {
        screenModelScope.launch {
            _uiState.value = CreateAnimalUiState(isLoading = true)
            val result = animalRepository.createAnimal(
                CreateAnimalRequest(
                    name = name,
                    species = species,
                    breed = breed,
                    age = age,
                    description = description,
                    status = status,
                    birthDate = birthDate
                )
            )
            _uiState.value = when (result) {
                is Resource.Success -> CreateAnimalUiState(isSuccess = true)
                is Resource.Error   -> CreateAnimalUiState(errorMessage = result.message)
                else -> CreateAnimalUiState()
            }
        }
    }
}