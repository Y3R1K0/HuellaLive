package com.huellalive.app.data.repository

import com.huellalive.app.data.model.AnimalDto
import com.huellalive.app.data.model.SearchCityDto
import com.huellalive.app.data.model.SearchSpeciesDto
import com.huellalive.app.data.model.ShelterProfileDto
import com.huellalive.app.data.model.WeeklyRankingDto
import com.huellalive.app.data.remote.ApiService
import com.huellalive.app.utils.Resource
import com.huellalive.app.utils.safeApiCall

class ExploreRepository(private val api: ApiService) {
    suspend fun weeklyRanking(): Resource<List<WeeklyRankingDto>> = safeApiCall { api.getWeeklyRanking() }
    suspend fun searchAnimals(species: String?, city: String?, status: String?): Resource<List<AnimalDto>> =
        safeApiCall { api.searchAnimals(species = species, city = city, status = status) }
    suspend fun searchShelters(city: String?): Resource<List<ShelterProfileDto>> =
        safeApiCall { api.searchShelters(city = city) }
    suspend fun searchCities(): Resource<List<SearchCityDto>> =
        safeApiCall { api.getSearchCities() }
    suspend fun searchSpecies(): Resource<List<SearchSpeciesDto>> =
        safeApiCall { api.getSearchSpecies() }
    suspend fun nearbyShelters(latitude: Double, longitude: Double, radiusKm: Double = 100.0): Resource<List<ShelterProfileDto>> =
        safeApiCall { api.getNearbyShelters(latitude, longitude, radiusKm) }
}
