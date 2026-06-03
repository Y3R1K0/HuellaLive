package com.huellalive.app.data.repository

import com.huellalive.app.data.model.ShelterProfileDto
import com.huellalive.app.data.remote.ApiService
import com.huellalive.app.model.AnimalSearchFilters
import com.huellalive.app.model.AnimalSearchResult
import com.huellalive.app.utils.Resource
import com.huellalive.app.utils.safeApiCall

class ShelterRepository(private val api: ApiService) {
    suspend fun getShelterById(id: String): Resource<ShelterProfileDto> = safeApiCall { api.getShelterById(id) }
    suspend fun getMyShelterProfile(): Resource<ShelterProfileDto> = safeApiCall { api.getMyShelterProfile() }
    suspend fun updateShelterProfile(data: Map<String, String>): Resource<ShelterProfileDto> = safeApiCall { api.updateShelterProfile(data) }
    suspend fun getNearbyShelters(): Resource<List<ShelterProfileDto>> = safeApiCall { api.getNearbyShelters() }
    suspend fun searchAnimals(filters: AnimalSearchFilters): Resource<AnimalSearchResult> = safeApiCall { api.searchAnimals(filters) }
}