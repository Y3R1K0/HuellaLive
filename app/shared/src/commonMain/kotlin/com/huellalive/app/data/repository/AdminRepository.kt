package com.huellalive.app.data.repository

import com.huellalive.app.data.model.CreateSpeciesRequest
import com.huellalive.app.data.model.SearchSpeciesDto
import com.huellalive.app.data.model.SpeciesRequestDto
import com.huellalive.app.data.model.UpdateSpeciesRequest
import com.huellalive.app.data.remote.ApiService
import com.huellalive.app.utils.Resource
import com.huellalive.app.utils.safeApiCall

class AdminRepository(private val api: ApiService) {
    suspend fun getSpecies(): Resource<List<SearchSpeciesDto>> =
        safeApiCall { api.getAdminSpecies() }

    suspend fun createSpecies(name: String): Resource<SearchSpeciesDto> =
        safeApiCall { api.createAdminSpecies(CreateSpeciesRequest(name)) }

    suspend fun setSpeciesActive(id: String, active: Boolean): Resource<SearchSpeciesDto> =
        safeApiCall { api.updateAdminSpecies(id, UpdateSpeciesRequest(isActive = active)) }

    suspend fun deleteSpecies(id: String): Resource<Unit> =
        safeApiCall { api.deleteAdminSpecies(id) }

    suspend fun getSpeciesRequests(): Resource<List<SpeciesRequestDto>> =
        safeApiCall { api.getSpeciesRequests() }

    suspend fun approveSpeciesRequest(id: String): Resource<Unit> =
        safeApiCall { api.approveSpeciesRequest(id) }

    suspend fun rejectSpeciesRequest(id: String, reason: String?): Resource<Unit> =
        safeApiCall { api.rejectSpeciesRequest(id, reason) }
}
