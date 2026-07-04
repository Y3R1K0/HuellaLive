package com.huellalive.app.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberGoogleSignInAction(
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit
): GoogleSignInAction = remember(onError) {
    GoogleSignInAction(isAvailable = false) {
        onError("El acceso con Google para iOS esta pendiente de configurar")
    }
}
