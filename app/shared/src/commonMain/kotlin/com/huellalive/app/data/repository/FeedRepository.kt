package com.huellalive.app.data.repository

import com.huellalive.app.data.model.VideoDto
import com.huellalive.app.data.remote.ApiService
import com.huellalive.app.utils.Resource
import com.huellalive.app.utils.safeApiCall

class FeedRepository(private val api: ApiService) {
    suspend fun getFeed(page: Int): Resource<List<VideoDto>> = safeApiCall { api.getFeed(page) }
    suspend fun likeVideo(videoId: String): Resource<VideoDto> = safeApiCall { api.likeVideo(videoId) }
    suspend fun unlikeVideo(videoId: String): Resource<VideoDto> = safeApiCall { api.unlikeVideo(videoId) }
    suspend fun markVideoViewed(videoId: String): Resource<Unit> = safeApiCall { api.markVideoViewed(videoId) }
    suspend fun getViewedVideos(page: Int = 1): Resource<List<VideoDto>> = safeApiCall { api.getViewedVideos(page) }
    suspend fun reportVideo(videoId: String, reason: String): Resource<Map<String, String>> =
        safeApiCall { api.reportVideo(videoId, reason) }
    suspend fun reportAnimal(animalId: String, reason: String): Resource<Map<String, String>> =
        safeApiCall { api.reportAnimal(animalId, reason) }
}
