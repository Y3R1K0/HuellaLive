package com.huellalive.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class VideoDto(
    val id: String,
    val videoUrl: String,
    val thumbnailUrl: String? = null,
    val description: String,
    val likesCount: Int,
    val user: VideoUserDto,
    val animal: VideoAnimalDto? = null,
    val createdAt: String
)

@Serializable
data class VideoUserDto(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val role: String
)

@Serializable
data class VideoAnimalDto(
    val id: String,
    val name: String,
    val species: String,
    val breed: String? = null,
    val status: String
)