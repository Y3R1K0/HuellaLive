package com.huellalive.app.auth

import androidx.compose.runtime.Composable

@Composable
expect fun rememberGoogleSignInAction(
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit
): GoogleSignInAction
