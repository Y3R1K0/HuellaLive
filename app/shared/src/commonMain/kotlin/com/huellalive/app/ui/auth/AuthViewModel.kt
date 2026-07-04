package com.huellalive.app.ui.auth

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.huellalive.app.data.model.FirebaseProfilePreviewDto
import com.huellalive.app.data.repository.AuthRepository
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val firebaseProfile: FirebaseProfilePreviewDto? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ScreenModel {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun loginHuman(email: String, password: String) {
        screenModelScope.launch {
            _state.value = AuthUiState(isLoading = true)
            _state.value = when (val r = authRepository.loginHuman(email, password)) {
                is Resource.Success -> AuthUiState(isSuccess = true)
                is Resource.Error   -> AuthUiState(errorMessage = r.message)
                else                -> AuthUiState()
            }
        }
    }

    fun loginShelter(email: String, password: String) {
        screenModelScope.launch {
            _state.value = AuthUiState(isLoading = true)
            _state.value = when (val r = authRepository.loginShelter(email, password)) {
                is Resource.Success -> AuthUiState(isSuccess = true)
                is Resource.Error   -> AuthUiState(errorMessage = r.message)
                else                -> AuthUiState()
            }
        }
    }

    fun loginHumanWithFirebase(idToken: String) {
        screenModelScope.launch {
            _state.value = AuthUiState(isLoading = true)
            _state.value = when (val r = authRepository.loginHumanWithFirebase(idToken)) {
                is Resource.Success -> AuthUiState(isSuccess = true)
                is Resource.Error -> AuthUiState(errorMessage = r.message)
                else -> AuthUiState()
            }
        }
    }

    fun previewFirebaseProfile(idToken: String) {
        screenModelScope.launch {
            _state.value = AuthUiState(isLoading = true)
            _state.value = when (val r = authRepository.previewFirebaseProfile(idToken)) {
                is Resource.Success -> AuthUiState(
                    successMessage = "Gmail verificado con Google",
                    firebaseProfile = r.data
                )
                is Resource.Error -> AuthUiState(errorMessage = r.message)
                else -> AuthUiState()
            }
        }
    }

    fun beginExternalLogin() {
        _state.value = AuthUiState(isLoading = true)
    }

    fun showError(message: String) {
        _state.value = AuthUiState(errorMessage = message)
    }

    fun resetPassword(email: String) {
        screenModelScope.launch {
            _state.value = AuthUiState(isLoading = true)
            _state.value = when (val result = authRepository.sendPasswordResetEmail(email)) {
                is Resource.Success -> AuthUiState(
                    successMessage = "Te enviamos un enlace para cambiar tu contrasena"
                )
                is Resource.Error -> AuthUiState(errorMessage = result.message)
                else -> AuthUiState()
            }
        }
    }

    fun registerHuman(name: String, email: String, password: String) {
        screenModelScope.launch {
            _state.value = AuthUiState(isLoading = true)
            _state.value = when (val r = authRepository.registerHuman(name, email, password)) {
                is Resource.Success -> AuthUiState(isSuccess = true)
                is Resource.Error   -> AuthUiState(errorMessage = r.message)
                else                -> AuthUiState()
            }
        }
    }

    fun registerShelter(
        name: String, email: String, password: String,
        description: String, location: String,
        phone: String, docs: List<String>
    ) {
        screenModelScope.launch {
            _state.value = AuthUiState(isLoading = true)
            _state.value = when (val r = authRepository.registerShelter(
                name, email, password, description, location, phone, docs
            )) {
                is Resource.Success -> AuthUiState(
                    isSuccess = true,
                    successMessage = "Solicitud enviada. Te notificaremos cuando sea aprobada."
                )
                is Resource.Error   -> AuthUiState(errorMessage = r.message)
                else                -> AuthUiState()
            }
        }
    }

    fun clearState() { _state.value = AuthUiState() }
}
