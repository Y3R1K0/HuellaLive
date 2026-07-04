package com.huellalive.app.data.repository

import com.huellalive.app.data.model.*
import com.huellalive.app.data.remote.ApiService
import com.huellalive.app.utils.Resource
import com.huellalive.app.utils.safeApiCall

class EngagementRepository(private val api: ApiService) {
    suspend fun likeVideo(videoId: String): Resource<VideoDto> = safeApiCall { api.likeVideo(videoId) }
    suspend fun followShelter(shelterId: String): Resource<FollowDto> = safeApiCall { api.followShelter(shelterId) }
    suspend fun requestAdoption(animalId: String): Resource<AdoptionRequestDto> = safeApiCall { api.requestAdoption(animalId) }
    suspend fun getAdoptionRequestState(animalId: String): Resource<AdoptionRequestStateDto> =
        safeApiCall { api.getAdoptionRequestState(animalId) }
    suspend fun uploadAnimalVideo(animalId: String, request: UploadVideoRequest): Resource<VideoDto> =
        safeApiCall { api.uploadAnimalVideo(animalId, request) }
    suspend fun uploadShelterStory(request: UploadVideoRequest): Resource<VideoDto> =
        safeApiCall { api.uploadShelterStory(request) }
    suspend fun deleteVideo(videoId: String): Resource<Unit> = safeApiCall { api.deleteVideo(videoId) }
    suspend fun deleteShelterStory(storyId: String): Resource<Unit> = safeApiCall { api.deleteShelterStory(storyId) }
}
