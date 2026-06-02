package com.huellalive.app.data.repository

import com.huellalive.app.data.model.UserProfileDto
import com.huellalive.app.data.remote.ApiService
import com.huellalive.app.utils.Resource
import com.huellalive.app.utils.safeApiCall

class UserRepository(private val api: ApiService) {
    suspend fun getMe(): Resource<UserProfileDto> = safeApiCall { api.getMe() }
    suspend fun updateMe(data: Map<String, String>): Resource<UserProfileDto> = safeApiCall { api.updateMe(data) }
}