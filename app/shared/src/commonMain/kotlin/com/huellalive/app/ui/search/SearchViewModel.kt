package com.huellalive.app.ui.search

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.huellalive.app.data.model.AnimalDto
import com.huellalive.app.data.model.SearchCityDto
import com.huellalive.app.data.model.SearchSpeciesDto
import com.huellalive.app.data.model.ShelterProfileDto
import com.huellalive.app.data.repository.ExploreRepository
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val species: String = "",
    val city: String = "",
    val status: String = "",
    val cities: List<SearchCityDto> = emptyList(),
    val speciesOptions: List<SearchSpeciesDto> = emptyList(),
    val shelters: List<ShelterProfileDto> = emptyList(),
    val animals: List<AnimalDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class SearchViewModel(private val repository: ExploreRepository) : ScreenModel {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        load()
    }

    fun updateSpecies(value: String) { _uiState.value = _uiState.value.copy(species = value) }
    fun updateCity(value: String) { _uiState.value = _uiState.value.copy(city = value) }
    fun updateStatus(value: String) { _uiState.value = _uiState.value.copy(status = value) }

    fun load() {
        search(loadCities = true)
    }

    fun reset() {
        searchJob?.cancel()
        searchJob = null
        _uiState.value = SearchUiState(
            cities = _uiState.value.cities,
            speciesOptions = _uiState.value.speciesOptions
        )
    }

    fun ensureLoaded() {
        val state = _uiState.value
        if (!state.isLoading && state.animals.isEmpty() && state.shelters.isEmpty()) {
            search(loadCities = state.cities.isEmpty() || state.speciesOptions.isEmpty())
        }
    }

    fun search(loadCities: Boolean = false) {
        val state = _uiState.value
        searchJob?.cancel()
        searchJob = screenModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            val cities = if (loadCities) repository.searchCities() else null
            val speciesOptions = if (loadCities) repository.searchSpecies() else null
            val animals = repository.searchAnimals(
                state.species.ifBlank { null },
                state.city.ifBlank { null },
                state.status.ifBlank { null }
            )
            val shelters = repository.searchShelters(state.city.ifBlank { null })
            val error = listOfNotNull(cities, speciesOptions, animals, shelters)
                .filterIsInstance<Resource.Error>()
                .firstOrNull()

            _uiState.value = _uiState.value.copy(
                cities = (cities as? Resource.Success)?.data ?: state.cities,
                speciesOptions = (speciesOptions as? Resource.Success)?.data ?: state.speciesOptions,
                animals = (animals as? Resource.Success)?.data ?: state.animals,
                shelters = (shelters as? Resource.Success)?.data ?: state.shelters,
                isLoading = false,
                errorMessage = error?.message
            )
        }
    }
}
