package com.huellalive.app.ui.search

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.huellalive.app.data.model.AnimalDto
import com.huellalive.app.data.model.ShelterProfileDto
import com.huellalive.app.data.repository.SearchRepository
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val animals: List<AnimalDto> = emptyList(),
    val shelters: List<ShelterProfileDto> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val query: String = "",
    val filterSpecies: String? = null,
    val filterCity: String? = null,
    val filterStatus: String? = null
)

class SearchViewModel(
    private val searchRepository: SearchRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        searchJob?.cancel()
        if (query.length >= 2) {
            searchJob = screenModelScope.launch {
                delay(400)
                search()
            }
        }
    }

    fun setFilter(species: String? = null, city: String? = null, status: String? = null) {
        _uiState.value = _uiState.value.copy(
            filterSpecies = species,
            filterCity = city,
            filterStatus = status
        )
        search()
    }

    fun search() {
        val state = _uiState.value
        screenModelScope.launch {
            _uiState.value = state.copy(isLoading = true)
            when (val result = searchRepository.search(
                query = state.query,
                species = state.filterSpecies,
                city = state.filterCity,
                status = state.filterStatus
            )) {
                is Resource.Success -> _uiState.value = state.copy(
                    animals = result.data.animals,
                    shelters = result.data.shelters,
                    isLoading = false,
                    hasSearched = true
                )
                is Resource.Error -> _uiState.value = state.copy(isLoading = false, hasSearched = true)
                else -> Unit
            }
        }
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(filterSpecies = null, filterCity = null, filterStatus = null)
        search()
    }
}