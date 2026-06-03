package com.huellalive.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchResultDto(
    val animals: List<AnimalDto> = emptyList(),
    val shelters: List<ShelterProfileDto> = emptyList()
)

@Serializable
data class RankingItemDto(
    val rank: Int,
    val user: RankingUserDto,
    val totalDonated: Double,
    val sheltersHelped: Int,
    val animalsAdopted: Int
)

@Serializable
data class RankingUserDto(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val badges: List<UserBadgeDto> = emptyList()
)