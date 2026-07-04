package com.huellalive.app.data.remote

import com.huellalive.app.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

class ApiService(private val client: HttpClient) {

    // MEDIA
    suspend fun uploadHumanAvatar(bytes: ByteArray, fileName: String, contentType: String): MediaUploadResponse =
        uploadMedia("media/human/avatar", bytes, fileName, contentType)

    suspend fun uploadShelterAvatar(bytes: ByteArray, fileName: String, contentType: String): MediaUploadResponse =
        uploadMedia("media/shelter/avatar", bytes, fileName, contentType)

    suspend fun uploadShelterCover(bytes: ByteArray, fileName: String, contentType: String): MediaUploadResponse =
        uploadMedia("media/shelter/cover", bytes, fileName, contentType)

    suspend fun uploadShelterStoryVideoFile(bytes: ByteArray, fileName: String, contentType: String): MediaUploadResponse =
        uploadMedia("media/shelter/story-video", bytes, fileName, contentType)

    suspend fun uploadAnimalPhoto(animalId: String, bytes: ByteArray, fileName: String, contentType: String): MediaUploadResponse =
        uploadMedia("media/animals/$animalId/photo", bytes, fileName, contentType)

    suspend fun uploadAnimalThumbnail(animalId: String, bytes: ByteArray, fileName: String, contentType: String): MediaUploadResponse =
        uploadMedia("media/animals/$animalId/thumbnail", bytes, fileName, contentType)

    suspend fun uploadAnimalVideoFile(animalId: String, bytes: ByteArray, fileName: String, contentType: String): MediaUploadResponse =
        uploadMedia("media/animals/$animalId/video", bytes, fileName, contentType)

    suspend fun uploadChatImage(chatId: String, bytes: ByteArray, fileName: String, contentType: String): MediaUploadResponse =
        uploadMedia("media/chats/$chatId/image", bytes, fileName, contentType)

    // AUTH
    suspend fun login(request: LoginRequest): AuthResponse =
        client.post("auth/login") { setBody(request) }.body()

    suspend fun loginWithFirebase(request: FirebaseLoginRequest): AuthResponse =
        client.post("auth/firebase") { setBody(request) }.body()

    suspend fun previewFirebaseProfile(request: FirebaseLoginRequest): FirebaseProfilePreviewDto =
        client.post("auth/firebase/preview") { setBody(request) }.body()

    suspend fun registerHuman(request: RegisterHumanRequest): AuthResponse =
        client.post("auth/register/human") { setBody(request) }.body()

    suspend fun registerShelter(request: RegisterShelterRequest): AuthResponse =
        client.post("auth/register/shelter") { setBody(request) }.body()

    suspend fun linkAnimal(request: LinkAnimalRequest): AnimalDto =
        client.post("auth/link-animal") { setBody(request) }.body()

    // FEED
    suspend fun getFeed(page: Int = 1): List<VideoDto> =
        client.get("feed") { parameter("page", page) }.body()

    suspend fun likeVideo(videoId: String): VideoDto =
        client.post("likes/$videoId").body()

    suspend fun unlikeVideo(videoId: String): VideoDto =
        client.delete("likes/$videoId").body()

    suspend fun markVideoViewed(videoId: String) {
        client.post("feed/viewed/$videoId")
    }

    suspend fun getViewedVideos(page: Int = 1): List<VideoDto> =
        client.get("feed/history") { parameter("page", page) }.body()

    suspend fun reportVideo(videoId: String, reason: String): Map<String, String> =
        client.post("reports/videos/$videoId") { setBody(ReportRequest(reason)) }.body()

    suspend fun reportAnimal(animalId: String, reason: String): Map<String, String> =
        client.post("reports/animals/$animalId") { setBody(ReportRequest(reason)) }.body()

    suspend fun deleteVideo(videoId: String) {
        client.delete("videos/$videoId")
    }

    // ANIMALS
    suspend fun getAnimalById(id: String): AnimalDto =
        client.get("animals/$id").body()

    suspend fun getMyAnimals(): List<AnimalDto> =
        client.get("animals/shelter/mine").body()

    suspend fun getAdoptedAnimals(): List<AnimalDto> =
        client.get("animals/human/adopted").body()

    suspend fun createAnimal(request: CreateAnimalRequest): AnimalDto =
        client.post("animals") { setBody(request) }.body()

    suspend fun updateAnimal(id: String, request: UpdateAnimalRequest): AnimalDto =
        client.patch("animals/$id") { setBody(request) }.body()

    suspend fun updateAnimalStatus(id: String, status: String): AnimalDto =
        client.patch("animals/$id/status") { setBody(mapOf("status" to status)) }.body()

    suspend fun getAnimalCredentials(id: String): AnimalCredentialsDto =
        client.get("animals/$id/credentials").body()

    // SHELTERS
    suspend fun getShelterById(id: String): ShelterProfileDto =
        client.get("shelters/$id").body()

    suspend fun getMyShelterProfile(): ShelterProfileDto =
        client.get("shelters/me/profile").body()

    suspend fun updateShelterProfile(data: Map<String, String>): ShelterProfileDto =
        client.patch("shelters/me/profile") { setBody(data) }.body()

    suspend fun getNearbyShelters(
        latitude: Double? = null,
        longitude: Double? = null,
        radiusKm: Double = 100.0
    ): List<ShelterProfileDto> = client.get("shelters/nearby") {
        if (latitude != null && longitude != null) {
            parameter("lat", latitude)
            parameter("lng", longitude)
            parameter("radiusKm", radiusKm)
        }
    }.body()

    suspend fun searchLocations(query: String): List<GeocodingResultDto> =
        client.get("shelters/geocoding/search") {
            parameter("q", query)
        }.body()

    suspend fun reverseLocation(latitude: Double, longitude: Double): GeocodingResultDto =
        client.get("shelters/geocoding/reverse") {
            parameter("lat", latitude)
            parameter("lng", longitude)
        }.body()

    // USERS
    suspend fun getMe(): UserProfileDto =
        client.get("users/me").body()

    suspend fun updateMe(data: Map<String, String>): UserProfileDto =
        client.patch("users/me") { setBody(data) }.body()

    suspend fun selectProfileBadge(badgeId: String?): UserProfileDto =
        client.patch("users/me/badge") { setBody(SelectBadgeRequest(badgeId)) }.body()

    suspend fun getShelterAdoptedAnimals(): List<AnimalDto> =
        client.get("animals/shelter/adopted").body()

    suspend fun updateAnimalCard(id: String, request: UpdateAnimalCardRequest): AnimalCardDto =
        client.put("animals/$id/card") { setBody(request) }.body()

    suspend fun getAnimalVideos(id: String): List<VideoDto> =
        client.get("animals/$id/videos").body()

    suspend fun uploadAnimalVideo(id: String, request: UploadVideoRequest): VideoDto =
        client.post("animals/$id/videos") { setBody(request) }.body()

    suspend fun uploadShelterStory(request: UploadVideoRequest): VideoDto =
        client.post("shelters/me/stories") { setBody(request) }.body()

    suspend fun deleteShelterStory(storyId: String) {
        client.delete("shelter-stories/$storyId")
    }

    suspend fun requestAdoption(animalId: String): AdoptionRequestDto =
        client.post("adoption-requests/$animalId").body()

    suspend fun getAdoptionRequestState(animalId: String): AdoptionRequestStateDto =
        client.get("adoption-requests/animal/$animalId/state").body()

    // EXPLORE / SEARCH
    suspend fun searchAnimals(
        species: String? = null,
        city: String? = null,
        status: String? = null,
        page: Int = 1
    ): List<AnimalDto> = client.get("search") {
        parameter("type", "animals")
        parameter("page", page)
        if (!species.isNullOrBlank()) parameter("species", species)
        if (!city.isNullOrBlank()) parameter("city", city)
        if (!status.isNullOrBlank()) parameter("status", status)
    }.body()

    suspend fun searchShelters(city: String? = null, page: Int = 1): List<ShelterProfileDto> =
        client.get("search") {
            parameter("type", "shelters")
            parameter("page", page)
            if (!city.isNullOrBlank()) parameter("city", city)
        }.body()

    suspend fun getSearchCities(): List<SearchCityDto> =
        client.get("search/cities").body()

    suspend fun getSearchSpecies(): List<SearchSpeciesDto> =
        client.get("search/species").body()

    suspend fun getWeeklyRanking(): List<WeeklyRankingDto> =
        client.get("ranking/weekly").body()

    // ADMIN
    suspend fun getAdminSpecies(): List<SearchSpeciesDto> =
        client.get("admin/search-species").body()

    suspend fun createAdminSpecies(request: CreateSpeciesRequest): SearchSpeciesDto =
        client.post("admin/search-species") { setBody(request) }.body()

    suspend fun updateAdminSpecies(id: String, request: UpdateSpeciesRequest): SearchSpeciesDto =
        client.patch("admin/search-species/$id") { setBody(request) }.body()

    suspend fun deleteAdminSpecies(id: String) {
        client.delete("admin/search-species/$id")
    }

    suspend fun getSpeciesRequests(): List<SpeciesRequestDto> =
        client.get("admin/species-requests").body()

    suspend fun approveSpeciesRequest(id: String) {
        client.patch("admin/species-requests/$id/approve")
    }

    suspend fun rejectSpeciesRequest(id: String, reason: String?) {
        client.patch("admin/species-requests/$id/reject") {
            setBody(RejectSpeciesRequest(reason))
        }
    }

    // SOCIAL / WALLET
    suspend fun followShelter(shelterId: String): FollowDto =
        client.post("follows/$shelterId").body()

    suspend fun unfollowShelter(shelterId: String) {
        client.delete("follows/$shelterId")
    }

    suspend fun donate(shelterId: String, amount: Double, currency: String = "PEN"): MercadoPagoCheckoutDto =
        client.post("donations/$shelterId") { setBody(AmountRequest(amount, currency)) }.body()

    suspend fun getShelterWalletDashboard(): ShelterWalletDashboardDto =
        client.get("wallet/shelter/dashboard").body()

    suspend fun getMercadoPagoConnectUrl(): MercadoPagoConnectDto =
        client.get("wallet/shelter/mercadopago/connect").body()

    suspend fun requestShelterWithdrawal(request: WithdrawalRequest): ShelterWithdrawalDto =
        client.post("wallet/shelter/withdrawals") { setBody(request) }.body()

    // CHAT / ADOPTIONS
    suspend fun getChats(): List<ChatDto> =
        client.get("chats").body()

    suspend fun getMessages(chatId: String): List<MessageDto> =
        client.get("chats/$chatId/messages").body()

    suspend fun sendMessage(chatId: String, request: SendMessageRequest): MessageDto =
        client.post("chats/$chatId/messages") { setBody(request) }.body()

    suspend fun deleteChat(chatId: String) {
        client.delete("chats/$chatId")
    }

    suspend fun deleteEmptyChat(chatId: String) {
        client.delete("chats/$chatId/empty")
    }

    suspend fun getTransferableAdoptions(chatId: String): List<AdoptionRequestDto> =
        client.get("chats/$chatId/transferable-adoptions").body()

    suspend fun transferAnimalFromChat(chatId: String, animalId: String): AnimalDto =
        client.post("chats/$chatId/transfer/$animalId").body()

    suspend fun getAdoptionRequests(): List<AdoptionRequestDto> =
        client.get("adoption-requests").body()

    suspend fun updateAdoptionRequest(id: String, status: String): AdoptionRequestDto =
        client.patch("adoption-requests/$id") { setBody(mapOf("status" to status)) }.body()

    suspend fun transferAdoption(id: String): AnimalDto =
        client.post("adoption-requests/$id/transfer").body()

    // NOTIFICATIONS
    suspend fun getNotifications(): List<NotificationDto> =
        client.get("notifications").body()

    suspend fun markNotificationRead(id: String) {
        client.patch("notifications/$id/read")
    }

    private suspend fun uploadMedia(path: String, bytes: ByteArray, fileName: String, contentType: String): MediaUploadResponse =
        client.post(path) { setBody(multipartBody(bytes, fileName, contentType)) }.body()

    private fun multipartBody(bytes: ByteArray, fileName: String, contentType: String) =
        MultiPartFormDataContent(
            formData {
                append(
                    "file",
                    bytes,
                    Headers.build {
                        append(HttpHeaders.ContentType, contentType)
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    },
                )
            },
        )
}
