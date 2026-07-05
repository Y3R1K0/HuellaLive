package com.huellalive.app.data.repository

import com.huellalive.app.data.local.SessionManager
import com.huellalive.app.auth.FirebaseAuthClient
import com.huellalive.app.data.model.*
import com.huellalive.app.data.remote.ApiService
import com.huellalive.app.utils.Resource
import com.huellalive.app.utils.safeApiCall

class AuthRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    private val firebaseAuthClient: FirebaseAuthClient
) {
    private fun saveSession(response: AuthResponse) {
        sessionManager.saveSession(
            accessToken   = response.accessToken,
            refreshToken  = response.refreshToken,
            userId        = response.user.id,
            name          = response.user.name,
            email         = response.user.email,
            role          = response.user.role,
            shelterStatus = response.user.shelterStatus
        )
    }

    suspend fun loginHuman(email: String, password: String): Resource<AuthResponse> {
        val cleanEmail = email.trim().lowercase()
        val cleanPassword = password.trim()
        val result = safeApiCall { apiService.login(LoginRequest(cleanEmail, cleanPassword)) }
        if (result is Resource.Success) {
            if (result.data.user.role != "HUMAN")
                return Resource.Error("Esta cuenta no es de un humano")
            saveSession(result.data)
        }
        return result
    }

    suspend fun loginShelter(email: String, password: String): Resource<AuthResponse> {
        val cleanEmail = email.trim().lowercase()
        val cleanPassword = password.trim()
        val result = safeApiCall { apiService.login(LoginRequest(cleanEmail, cleanPassword)) }
        if (result is Resource.Success) {
            if (result.data.user.role != "SHELTER")
                return Resource.Error("Esta cuenta no es de un albergue")
            saveSession(result.data)
        }
        return result
    }

    suspend fun loginHumanWithFirebase(idToken: String): Resource<AuthResponse> {
        val result = safeApiCall { apiService.loginWithFirebase(FirebaseLoginRequest(idToken)) }
        if (result is Resource.Success) {
            if (result.data.user.role != "HUMAN") {
                return Resource.Error("Esta cuenta pertenece a un albergue")
            }
            saveSession(result.data)
        }
        return result
    }

    suspend fun previewFirebaseProfile(idToken: String): Resource<FirebaseProfilePreviewDto> =
        safeApiCall { apiService.previewFirebaseProfile(FirebaseLoginRequest(idToken)) }

    suspend fun sendPasswordResetEmail(email: String): Resource<Unit> {
        if (email.isBlank()) return Resource.Error("Ingresa tu correo")
        if (!firebaseAuthClient.isConfigured()) {
            return Resource.Error("La recuperacion por correo aun no esta configurada")
        }
        return firebaseAuthClient.sendPasswordResetEmail(email).fold(
            onSuccess = { Resource.Success(Unit) },
            onFailure = { Resource.Error(it.message ?: "No se pudo enviar el correo") }
        )
    }

    suspend fun registerHuman(name: String, email: String, password: String): Resource<AuthResponse> {
        val result = safeApiCall {
            apiService.registerHuman(RegisterHumanRequest(name, email, password))
        }
        if (result is Resource.Success) saveSession(result.data)
        return result
    }

    suspend fun registerShelter(
        name: String, email: String, password: String,
        description: String, location: String,
        phone: String, docs: List<String>
    ): Resource<AuthResponse> {
        val result = safeApiCall {
            apiService.registerShelter(
                RegisterShelterRequest(name, email, password, description, location, phone, docs)
            )
        }
        if (result is Resource.Success) saveSession(result.data)
        return result
    }

    suspend fun linkAnimal(username: String, password: String): Resource<Any> =
        safeApiCall { apiService.linkAnimal(LinkAnimalRequest(username, password)) }

    fun logout() = sessionManager.clearSession()

    private suspend fun firebaseLoginWithEmail(
        email: String,
        password: String,
        expectedRole: String
    ): Resource<AuthResponse>? {
        if (!firebaseAuthClient.isConfigured()) return null
        val idToken = firebaseAuthClient.signInWithEmail(email, password).getOrNull() ?: return null
        val result = safeApiCall { apiService.loginWithFirebase(FirebaseLoginRequest(idToken)) }
        if (result is Resource.Success) {
            if (result.data.user.role != expectedRole) {
                val message = if (expectedRole == "HUMAN") {
                    "Esta cuenta no es de un humano"
                } else {
                    "Esta cuenta no es de un albergue"
                }
                return Resource.Error(message)
            }
            saveSession(result.data)
        }
        return result
    }
}
