package com.huellalive.app.ui.shelter

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.huellalive.app.data.local.SessionManager
import com.huellalive.app.data.model.ShelterProfileDto
import com.huellalive.app.data.repository.EngagementRepository
import com.huellalive.app.data.repository.ShelterRepository
import com.huellalive.app.data.repository.WalletRepository
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShelterDetailUiState(
    val shelter: ShelterProfileDto? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val actionInProgress: String? = null,
    val checkoutUrl: String? = null
)

class ShelterDetailViewModel(
    private val shelterId: String,
    private val shelterRepository: ShelterRepository,
    private val engagementRepository: EngagementRepository,
    private val walletRepository: WalletRepository,
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

    fun follow() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = "follow")
            when (val result = engagementRepository.followShelter(shelterId)) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(
                    actionMessage = "Ahora sigues este albergue",
                    shelter = _uiState.value.shelter?.copy(isFollowing = true),
                    actionInProgress = null
                )
                is Resource.Error -> _uiState.value = _uiState.value.copy(
                    actionMessage = result.message,
                    actionInProgress = null
                )
                else -> _uiState.value = _uiState.value.copy(actionInProgress = null)
            }
        }
    }

    fun donate(amount: Double) {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = "donate")
            when (val result = walletRepository.donate(shelterId, amount)) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(
                    actionMessage = "Te llevamos a Mercado Pago para completar la donacion",
                    actionInProgress = null,
                    checkoutUrl = result.data.checkoutUrl ?: result.data.sandboxCheckoutUrl
                )
                is Resource.Error -> _uiState.value = _uiState.value.copy(actionMessage = result.message, actionInProgress = null)
                else -> _uiState.value = _uiState.value.copy(actionInProgress = null)
            }
        }
    }

    fun showActionMessage(message: String) {
        _uiState.value = _uiState.value.copy(actionMessage = message)
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }

    fun clearCheckoutUrl() {
        _uiState.value = _uiState.value.copy(checkoutUrl = null)
    }

}
