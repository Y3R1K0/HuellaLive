package com.huellalive.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val selectedBadgeId: String? = null,
    val role: String,
    val shelterProfile: ShelterProfileDto? = null,
    val badges: List<UserBadgeDto> = emptyList(),
    val adoptedAnimals: List<AnimalDto> = emptyList()
)

@Serializable
data class UserBadgeDto(
    val badge: BadgeDto,
    val assignedAt: String
)

@Serializable
data class BadgeDto(
    val id: String,
    val key: String = "",
    val name: String,
    val description: String? = null,
    val iconUrl: String? = null,
    val tier: String,
    val activity: String = "SPECIAL",
    val threshold: Int = 0
)

@Serializable
data class SelectBadgeRequest(val badgeId: String? = null)
