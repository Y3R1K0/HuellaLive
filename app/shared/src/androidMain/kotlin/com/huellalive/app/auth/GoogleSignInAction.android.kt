package com.huellalive.app.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
actual fun rememberGoogleSignInAction(
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit
): GoogleSignInAction {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val webClientId = remember(context) {
        val resourceId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resourceId == 0) "" else context.getString(resourceId)
    }
    val isAvailable = remember(webClientId) {
        webClientId.isNotBlank() && runCatching { FirebaseApp.getInstance() }.isSuccess
    }

    return remember(isAvailable, webClientId, onIdToken, onError) {
        GoogleSignInAction(isAvailable = isAvailable) {
            if (!isAvailable) {
                onError("El acceso con Google aun no esta configurado")
                return@GoogleSignInAction
            }

            scope.launch {
                runCatching {
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .setAutoSelectEnabled(false)
                        .setNonce(sha256(UUID.randomUUID().toString()))
                        .build()
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()
                    val response = credentialManager.getCredential(context, request)
                    val credential = response.credential
                    if (credential !is CustomCredential ||
                        credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        error("Google no devolvio una credencial valida")
                    }

                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                    val authResult = FirebaseAuth.getInstance()
                        .signInWithCredential(firebaseCredential)
                        .awaitResult()
                    val user = authResult.user ?: error("No se pudo obtener la cuenta de Google")
                    user.getIdToken(true).awaitResult().token
                        ?: error("Firebase no devolvio un token")
                }.onSuccess(onIdToken)
                    .onFailure {
                        val message = when (it) {
                            is NoCredentialException -> "No hay una cuenta Google disponible en este dispositivo. Agrega una cuenta en el emulador o prueba en un celular con Google."
                            else -> it.message ?: "No se pudo iniciar sesion con Google"
                        }
                        onError(message)
                    }
            }
        }
    }
}

private fun sha256(value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWith(Result.failure(task.exception ?: IllegalStateException("Operacion cancelada")))
            }
        }
    }
