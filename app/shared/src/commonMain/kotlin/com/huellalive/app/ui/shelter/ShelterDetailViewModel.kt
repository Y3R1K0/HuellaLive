package com.huellalive.app.ui.shelter

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.huellalive.app.data.local.SessionManager
import com.huellalive.app.data.model.ShelterProfileDto
import com.huellalive.app.data.repository.ShelterRepository
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShelterDetailUiState(
    val shelter: ShelterProfileDto? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ShelterDetailViewModel(
    private val shelterId: String,
    private val shelterRepository: ShelterRepository,
    val sessionManager: SessionManager
) : ScreenModel {

    private val _uiState = MutableStateFlow(ShelterDetailUiState())
    val uiState: StateFlow<ShelterDetailUiState> = _uiState.asStateFlow()

    init { loadShelter() }

    fun loadShelter() {
        screenModelScope.launch {
            _uiState.value = ShelterDetailUiState(isLoading = true)
            when (val result = shelterRepository.getShelterById(shelterId)) {
                is Resource.Success -> _uiState.value = ShelterDetailUiState(shelter = result.data)
                is Resource.Error   -> _uiState.value = ShelterDetailUiState(errorMessage = result.message)
                else -> Unit
            }
        }
    }
}