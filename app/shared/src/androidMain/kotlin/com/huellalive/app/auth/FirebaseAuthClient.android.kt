package com.huellalive.app.auth

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

actual class FirebaseAuthClient actual constructor() {
    actual fun isConfigured(): Boolean = runCatching {
        FirebaseApp.getInstance()
        true
    }.getOrDefault(false)

    actual suspend fun signInWithEmail(email: String, password: String): Result<String> {
        if (!isConfigured()) {
            return Result.failure(IllegalStateException("Firebase Authentication aun no esta configurado"))
        }

        return suspendCancellableCoroutine { continuation ->
            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email.trim(), password)
                .addOnCompleteListener { signInTask ->
                    if (!continuation.isActive) return@addOnCompleteListener
                    if (!signInTask.isSuccessful) {
                        continuation.resume(
                            Result.failure(signInTask.exception ?: IllegalStateException("Credenciales incorrectas"))
                        )
                        return@addOnCompleteListener
                    }

                    val user = signInTask.result.user
                    if (user == null) {
                        continuation.resume(Result.failure(IllegalStateException("No se pudo obtener la cuenta")))
                        return@addOnCompleteListener
                    }

                    user.getIdToken(true).addOnCompleteListener { tokenTask ->
                        if (!continuation.isActive) return@addOnCompleteListener
                        val token = tokenTask.result?.token
                        if (tokenTask.isSuccessful && token != null) {
                            continuation.resume(Result.success(token))
                        } else {
                            continuation.resume(
                                Result.failure(tokenTask.exception ?: IllegalStateException("No se pudo obtener la sesion"))
                            )
                        }
                    }
                }
        }
    }

    actual suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        if (!isConfigured()) {
            return Result.failure(IllegalStateException("Firebase Authentication aun no esta configurado"))
        }

        return suspendCancellableCoroutine { continuation ->
            FirebaseAuth.getInstance()
                .sendPasswordResetEmail(email.trim())
                .addOnCompleteListener { task ->
                    if (!continuation.isActive) return@addOnCompleteListener
                    if (task.isSuccessful) {
                        continuation.resume(Result.success(Unit))
                    } else {
                        continuation.resume(
                            Result.failure(task.exception ?: IllegalStateException("No se pudo enviar el correo"))
                        )
                    }
                }
        }
    }
}
