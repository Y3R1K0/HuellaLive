package com.huellalive.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.huellalive.app.data.repository.AuthRepository
import com.huellalive.app.ui.feed.FeedScreen
import com.huellalive.app.ui.theme.*
import org.koin.compose.koinInject

class LoginShelterScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authRepository = koinInject<AuthRepository>()
        val viewModel = rememberScreenModel { AuthViewModel(authRepository) }
        val state by viewModel.state.collectAsState()
        val focusManager = LocalFocusManager.current

        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }

        LaunchedEffect(state.isSuccess) {
            if (state.isSuccess) navigator.replaceAll(FeedScreen())
        }

        Box(modifier = Modifier.fillMaxSize().background(Background).imePadding()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Panel Albergue 🏠", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                Text("Inicia sesión con tu cuenta de albergue", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(40.dp))

                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email del albergue") },
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DustyRose, unfocusedBorderColor = Outline, focusedLabelColor = DustyRose, cursorColor = DustyRose)
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextSecondary) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TextSecondary)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DustyRose, unfocusedBorderColor = Outline, focusedLabelColor = DustyRose, cursorColor = DustyRose)
                )

                if (state.errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(state.errorMessage!!, color = Error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = { viewModel.loginShelter(email, password) },
                    enabled = !state.isLoading && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                ) {
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = TextOnAccent, strokeWidth = 2.dp)
                    else Text("Iniciar sesión", style = MaterialTheme.typography.titleMedium, color = TextOnAccent)
                }
            }

            IconButton(onClick = { navigator.pop() }, modifier = Modifier.align(Alignment.TopStart).padding(16.dp).padding(top = 32.dp)) {
                Icon(Icons.Default.ArrowBack, "Volver", tint = TextPrimary)
            }
        }
    }
}