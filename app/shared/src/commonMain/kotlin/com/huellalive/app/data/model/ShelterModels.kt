package com.huellalive.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ShelterProfileDto(
    val id: String,
    val userId: String,
    val coverUrl: String? = null,
    val description: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distanceKm: Double? = null,
    val phone: String? = null,
    val status: String = "PENDING",
    val isFollowing: Boolean = false,
    val user: UserPublicDto,
    val animals: List<AnimalDto> = emptyList(),
    val stories: List<VideoDto> = emptyList(),
    val _count: ShelterCountDto? = null
)

@Serializable
data class ShelterCountDto(val animals: Int = 0)

@Serializable
data class UserPublicDto(
    val id: String,
    val name: String,
    val avatarUrl: String? = null
)

@Serializable
data class SearchCityDto(
    val id: String,
    val name: String,
    val region: String? = null
)

@Serializable
data class GeocodingResultDto(
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val city: String? = null
)

@Serializable
data class SearchSpeciesDto(
    val id: String,
    val name: String,
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

@Serializable
data class SpeciesRequestDto(
    val id: String,
    val requestedName: String,
    val animalId: String,
    val shelterId: String,
    val status: String,
    val createdAt: String,
    val animal: SpeciesRequestAnimalDto? = null,
    val shelter: SpeciesRequestShelterDto? = null
)

@Serializable
data class SpeciesRequestAnimalDto(
    val id: String,
    val name: String,
    val species: String,
    val photoUrl: String? = null
)

@Serializable
data class SpeciesRequestShelterDto(
    val id: String,
    val userId: String,
    val user: SpeciesRequestShelterUserDto
)

@Serializable
data class SpeciesRequestShelterUserDto(val name: String)

@Serializable
data class CreateSpeciesRequest(val name: String, val sortOrder: Int = 100)

@Serializable
data class UpdateSpeciesRequest(
    val name: String? = null,
    val isActive: Boolean? = null,
    val sortOrder: Int? = null
)

@Serializable
data class RejectSpeciesRequest(val reason: String? = null)
