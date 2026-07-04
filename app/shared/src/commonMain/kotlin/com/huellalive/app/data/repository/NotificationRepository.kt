package com.huellalive.app.data.repository

import com.huellalive.app.data.model.NotificationDto
import com.huellalive.app.data.remote.ApiService
import com.huellalive.app.utils.Resource
import com.huellalive.app.utils.safeApiCall

class NotificationRepository(private val api: ApiService) {
    suspend fun getNotifications(): Resource<List<NotificationDto>> = safeApiCall { api.getNotifications() }
    suspend fun markRead(id: String): Resource<Unit> = safeApiCall { api.markNotificationRead(id) }
}
