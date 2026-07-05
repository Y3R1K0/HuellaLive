package com.huellalive.app.ui.explore

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.huellalive.app.data.model.AnimalDto
import com.huellalive.app.data.model.ShelterProfileDto
import com.huellalive.app.data.repository.ExploreRepository
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExploreUiState(
    val shelters: List<ShelterProfileDto> = emptyList(),
    val animals: List<AnimalDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ExploreViewModel(private val repository: ExploreRepository) : ScreenModel {
    private val _uiState = MutableStateFlow(ExploreUiState(isLoading = true))
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        screenModelScope.launch {
            val previousState = _uiState.value
            _uiState.value = previousState.copy(isLoading = true, errorMessage = null)
            val shelters = repository.searchShelters(null)
            val animals = repository.searchAnimals(null, null, "AVAILABLE")
            _uiState.value = ExploreUiState(
                shelters = (shelters as? Resource.Success)?.data ?: previousState.shelters,
                animals = (animals as? Resource.Success)?.data ?: previousState.animals,
                isLoading = false,
                errorMessage = listOf(shelters, animals).filterIsInstance<Resource.Error>().firstOrNull()?.message
            )
        }
    }
}
