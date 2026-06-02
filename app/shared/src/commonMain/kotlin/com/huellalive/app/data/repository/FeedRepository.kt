package com.huellalive.app.data.repository

import com.huellalive.app.data.model.VideoDto
import com.huellalive.app.data.remote.ApiService
import com.huellalive.app.utils.Resource
import com.huellalive.app.utils.safeApiCall

class FeedRepository(private val api: ApiService) {
    suspend fun getFeed(page: Int): Resource<List<VideoDto>> = safeApiCall { api.getFeed(page) }
}