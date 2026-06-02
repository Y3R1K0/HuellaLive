package com.huellalive.app.data.local

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

class SessionManager(private val settings: Settings) {

    companion object {
        private const val KEY_ACCESS_TOKEN   = "access_token"
        private const val KEY_REFRESH_TOKEN  = "refresh_token"
        private const val KEY_USER_ID        = "user_id"
        private const val KEY_USER_NAME      = "user_name"
        private const val KEY_USER_EMAIL     = "user_email"
        private const val KEY_USER_ROLE      = "user_role"
        private const val KEY_SHELTER_STATUS = "shelter_status"
    }

    fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        name: String,
        email: String,
        role: String,
        shelterStatus: String? = null
    ) {
        settings[KEY_ACCESS_TOKEN]   = accessToken
        settings[KEY_REFRESH_TOKEN]  = refreshToken
        settings[KEY_USER_ID]        = userId
        settings[KEY_USER_NAME]      = name
        settings[KEY_USER_EMAIL]     = email
        settings[KEY_USER_ROLE]      = role
        settings[KEY_SHELTER_STATUS] = shelterStatus ?: ""
    }

    fun getAccessToken()   = settings.getStringOrNull(KEY_ACCESS_TOKEN)
    fun getRefreshToken()  = settings.getStringOrNull(KEY_REFRESH_TOKEN)
    fun getUserId()        = settings.getStringOrNull(KEY_USER_ID)
    fun getUserName()      = settings.getStringOrNull(KEY_USER_NAME)
    fun getUserEmail()     = settings.getStringOrNull(KEY_USER_EMAIL)
    fun getUserRole()      = settings.getStringOrNull(KEY_USER_ROLE)
    fun getShelterStatus() = settings.getStringOrNull(KEY_SHELTER_STATUS)

    fun isLoggedIn()  = getAccessToken() != null
    fun isHuman()     = getUserRole() == "HUMAN"
    fun isShelter()   = getUserRole() == "SHELTER"
    fun isAdmin()     = getUserRole() == "ADMIN"
    fun isApproved()  = getShelterStatus() == "APPROVED"

    fun clearSession() = settings.clear()
}