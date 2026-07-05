package com.huellalive.app.data.repository

import com.huellalive.app.data.model.RankingItemDto
import com.huellalive.app.data.model.SearchResultDto
import com.huellalive.app.data.remote.ApiService
import com.huellalive.app.utils.Resource
import com.huellalive.app.utils.safeApiCall

class SearchRepository(private val api: ApiService) {
    suspend fun search(
        query: String = "",
        species: String? = null,
        city: String? = null,
        status: String? = null
    ): Resource<SearchResultDto> = safeApiCall {
        api.search(query, species, city, status)
    }

    suspend fun getWeeklyRanking(): Resource<List<RankingItemDto>> =
        safeApiCall { api.getWeeklyRanking() }
}