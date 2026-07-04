package com.huellalive.app.data.repository

import com.huellalive.app.data.model.AdoptionRequestDto
import com.huellalive.app.data.model.AnimalDto
import com.huellalive.app.data.remote.ApiService
import com.huellalive.app.utils.Resource
import com.huellalive.app.utils.safeApiCall

class AdoptionRepository(private val api: ApiService) {
    suspend fun getRequests(): Resource<List<AdoptionRequestDto>> = safeApiCall { api.getAdoptionRequests() }
    suspend fun updateRequest(id: String, status: String): Resource<AdoptionRequestDto> =
        safeApiCall { api.updateAdoptionRequest(id, status) }
    suspend fun transfer(id: String): Resource<AnimalDto> = safeApiCall { api.transferAdoption(id) }
}
