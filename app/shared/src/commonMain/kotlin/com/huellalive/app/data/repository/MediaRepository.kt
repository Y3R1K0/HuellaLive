package com.huellalive.app.data.repository

import com.huellalive.app.data.model.MediaUploadResponse
import com.huellalive.app.data.remote.ApiService
import com.huellalive.app.utils.Resource
import com.huellalive.app.utils.safeApiCall

class MediaRepository(private val api: ApiService) {
    suspend fun uploadHumanAvatar(bytes: ByteArray, fileName: String, contentType: String): Resource<MediaUploadResponse> =
        safeApiCall { api.uploadHumanAvatar(bytes, fileName, contentType) }

    suspend fun uploadShelterAvatar(bytes: ByteArray, fileName: String, contentType: String): Resource<MediaUploadResponse> =
        safeApiCall { api.uploadShelterAvatar(bytes, fileName, contentType) }

    suspend fun uploadShelterCover(bytes: ByteArray, fileName: String, contentType: String): Resource<MediaUploadResponse> =
        safeApiCall { api.uploadShelterCover(bytes, fileName, contentType) }

    suspend fun uploadShelterStoryVideo(bytes: ByteArray, fileName: String, contentType: String): Resource<MediaUploadResponse> =
        safeApiCall { api.uploadShelterStoryVideoFile(bytes, fileName, contentType) }

    suspend fun uploadAnimalPhoto(animalId: String, bytes: ByteArray, fileName: String, contentType: String): Resource<MediaUploadResponse> =
        safeApiCall { api.uploadAnimalPhoto(animalId, bytes, fileName, contentType) }

    suspend fun uploadAnimalThumbnail(animalId: String, bytes: ByteArray, fileName: String, contentType: String): Resource<MediaUploadResponse> =
        safeApiCall { api.uploadAnimalThumbnail(animalId, bytes, fileName, contentType) }

    suspend fun uploadAnimalVideo(animalId: String, bytes: ByteArray, fileName: String, contentType: String): Resource<MediaUploadResponse> =
        safeApiCall { api.uploadAnimalVideoFile(animalId, bytes, fileName, contentType) }

    suspend fun uploadChatImage(chatId: String, bytes: ByteArray, fileName: String, contentType: String): Resource<MediaUploadResponse> =
        safeApiCall { api.uploadChatImage(chatId, bytes, fileName, contentType) }
}
