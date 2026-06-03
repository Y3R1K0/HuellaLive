package com.huellalive.app.ui.explore

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.huellalive.app.data.model.RankingItemDto
import com.huellalive.app.data.model.ShelterProfileDto
import com.huellalive.app.data.repository.SearchRepository
import com.huellalive.app.data.repository.ShelterRepository
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExploreUiState(
    val shelters: List<ShelterProfileDto> = emptyList(),
    val ranking: List<RankingItemDto> = emptyList(),
    val isLoading: Boolean = false,
    val selectedRankingUser: RankingItemDto? = null
)

class ExploreViewModel(
    private val shelterRepository: ShelterRepository,
    private val searchRepository: SearchRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        screenModelScope.launch {
            _uiState.value = ExploreUiState(isLoading = true)
            val sheltersResult = shelterRepository.getNearbyShelters()
            val rankingResult = searchRepository.getWeeklyRanking()
            _uiState.value = ExploreUiState(
                shelters = (sheltersResult as? Resource.Success)?.data ?: emptyList(),
                ranking = (rankingResult as? Resource.Success)?.data ?: emptyList(),
                isLoading = false
            )
        }
    }

    fun selectRankingUser(user: RankingItemDto?) {
        _uiState.value = _uiState.value.copy(selectedRankingUser = user)
    }
}