package com.huellalive.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class VideoDto(
    val id: String,
    val videoUrl: String,
    val thumbnailUrl: String? = null,
    val description: String,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val user: VideoUserDto? = null,
    val uploadedBy: VideoUserDto? = null,
    val animal: VideoAnimalDto? = null,
    val shelter: VideoShelterDto? = null,
    val type: String = "ANIMAL_VIDEO",
    val createdAt: String
) {
    val author: VideoUserDto
        get() = user ?: uploadedBy ?: VideoUserDto(
            id = "",
            name = "HuellaLive",
            avatarUrl = null,
            role = "SHELTER"
        )

    val isShelterStory: Boolean
        get() = type == "SHELTER_STORY" || animal == null
}

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
    val photoUrl: String? = null,
    val status: String,
    val shelter: VideoShelterDto? = null
)

@Serializable
data class VideoShelterDto(
    val id: String,
    val user: VideoShelterUserDto
)

@Serializable
data class VideoShelterUserDto(
    val name: String,
    val avatarUrl: String? = null
)
