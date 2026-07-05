package com.huellalive.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement

@Serializable
data class WeeklyRankingDto(
    val id: String,
    val userId: String,
    val weekStart: String,
    val totalDonated: Double = 0.0,
    @SerialName("totalGalletas")
    val legacySupportCount: Int = 0,
    val sheltersHelped: Int = 0,
    val animalsAdopted: Int = 0,
    val rank: Int,
    val userName: String = "Donador HuellaLive",
    val avatarUrl: String? = null
)

@Serializable
data class ChatDto(
    val id: String,
    val humanId: String,
    val shelterId: String,
    val createdAt: String,
    val human: UserPublicDto? = null,
    val shelter: ShelterProfileDto? = null,
    val messages: List<MessageDto> = emptyList()
)

@Serializable
data class MessageDto(
    val id: String,
    val chatId: String,
    val senderId: String,
    val content: String? = null,
    val mediaUrl: String? = null,
    val isRead: Boolean = false,
    val createdAt: String,
    val sender: VideoUserDto? = null
)

@Serializable
data class SendMessageRequest(
    val content: String? = null,
    val mediaUrl: String? = null
)

@Serializable
data class AdoptionRequestDto(
    val id: String,
    val animalId: String,
    val humanId: String,
    val shelterId: String,
    val status: String,
    val chatId: String? = null,
    val createdAt: String,
    val animal: AdoptionAnimalDto? = null,
    val human: UserPublicDto? = null,
    val chat: ChatDto? = null
)

@Serializable
data class AdoptionRequestStateDto(
    val hasActiveRequest: Boolean = false,
    val activeStatus: String? = null,
    val requestsToday: Int = 0,
    val dailyLimit: Int = 10,
    val dailyLimitReached: Boolean = false
)

@Serializable
data class AdoptionAnimalDto(
    val id: String,
    val name: String,
    val species: String,
    val photoUrl: String? = null,
    val status: String
)

@Serializable
data class AmountRequest(
    val amount: Double,
    val currency: String = "PEN",
    val status: String? = null
)

@Serializable
data class UploadVideoRequest(
    val videoUrl: String,
    val thumbnailUrl: String? = null,
    val description: String = ""
)

@Serializable
data class ReportRequest(
    val reason: String,
    val details: String? = null
)

@Serializable
data class NotificationDto(
    val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val body: String,
    val data: JsonElement? = null,
    val isRead: Boolean = false,
    val createdAt: String
)

@Serializable
data class FollowDto(
    val id: String,
    val humanId: String,
    val shelterId: String,
    val createdAt: String
)
