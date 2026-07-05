package com.huellalive.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AnimalDto(
    val id: String,
    val name: String,
    val species: String,
    val breed: String? = null,
    val age: Int? = null,
    val description: String? = null,
    val photoUrl: String? = null,
    val status: String,
    val shelter: ShelterBriefDto? = null,
    val adoptedBy: UserPublicDto? = null,
    val card: AnimalCardDto? = null,
    val videos: List<VideoDto> = emptyList()
)

@Serializable
data class ShelterBriefDto(
    val id: String,
    val description: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val user: UserPublicDto
)

@Serializable
data class AnimalCardDto(
    val id: String,
    val sex: String? = null,
    val birthDate: String? = null,
    val isSterilized: Boolean = false,
    val vaccines: List<VaccineDto> = emptyList(),
    val controls: List<ControlDto> = emptyList(),
    val notes: String? = null
)

@Serializable
data class UpdateAnimalCardRequest(
    val sex: String? = null,
    val birthDate: String? = null,
    val isSterilized: Boolean = false,
    val vaccines: List<VaccineDto> = emptyList(),
    val controls: List<ControlDto> = emptyList(),
    val notes: String? = null
)

@Serializable
data class VaccineDto(
    val name: String,
    val date: String,
    val appliedBy: String? = null
)

@Serializable
data class ControlDto(
    val description: String,
    val date: String,
    val nextDate: String? = null
)

@Serializable
data class CreateAnimalRequest(
    val name: String,
    val species: String,
    val requestedSpeciesName: String? = null,
    val breed: String? = null,
    val age: Int? = null,
    val description: String? = null,
    val status: String = "AVAILABLE",
    val birthDate: String? = null
)

@Serializable
data class UpdateAnimalRequest(
    val name: String,
    val species: String,
    val breed: String? = null,
    val age: Int? = null,
    val description: String? = null,
    val photoUrl: String? = null,
    val status: String = "AVAILABLE"
)

@Serializable
data class AnimalCredentialsDto(
    val username: String,
    val password: String
)
