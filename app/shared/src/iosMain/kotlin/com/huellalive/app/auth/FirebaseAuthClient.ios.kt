package com.huellalive.app.auth

actual class FirebaseAuthClient actual constructor() {
    actual fun isConfigured(): Boolean = false

    actual suspend fun signInWithEmail(email: String, password: String): Result<String> =
        Result.failure(IllegalStateException("Firebase Auth para iOS esta pendiente de configurar"))

    actual suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
        Result.failure(IllegalStateException("Firebase Auth para iOS esta pendiente de configurar"))
}
