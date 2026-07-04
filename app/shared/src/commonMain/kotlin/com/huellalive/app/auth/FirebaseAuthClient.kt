package com.huellalive.app.auth

class GoogleSignInAction(
    val isAvailable: Boolean,
    val launch: () -> Unit
)

expect class FirebaseAuthClient() {
    fun isConfigured(): Boolean
    suspend fun signInWithEmail(email: String, password: String): Result<String>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
}
