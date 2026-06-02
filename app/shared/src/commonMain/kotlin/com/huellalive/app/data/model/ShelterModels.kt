package com.huellalive.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ShelterProfileDto(
    val id: String,
    val userId: String,
    val coverUrl: String? = null,
    val description: String? = null,
    val location: String? = null,
    val phone: String? = null,
    val status: String = "PENDING",
    val user: UserPublicDto,
    val animals: List<AnimalDto> = emptyList()
)

@Serializable
data class UserPublicDto(
    val id: String,
    val name: String,
    val avatarUrl: String? = null
)