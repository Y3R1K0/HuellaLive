package com.huellalive.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterHumanRequest(
    val name: String,
    val email: String,
    val password: String
)

@Serializable
data class RegisterShelterRequest(
    val name: String,
    val email: String,
    val password: String,
    val description: String,
    val location: String,
    val phone: String,
    val verificationDocs: List<String>
)

@Serializable
data class LinkAnimalRequest(
    val credentialUsername: String,
    val credentialPassword: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto
)

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val avatarUrl: String? = null,
    val shelterStatus: String? = null
)