package com.huellalive.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

class RegisterHumanScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authRepository = koinInject<AuthRepository>()
        val viewModel = rememberScreenModel { AuthViewModel(authRepository) }
        val state by viewModel.state.collectAsState()
        val focusManager = LocalFocusManager.current

        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }

        val passwordsMatch = confirmPassword.isEmpty() || password == confirmPassword
        val isEmailValid = email.isEmpty() || email.contains("@")
        val isFormValid = name.isNotBlank() && email.isNotBlank() && isEmailValid && password.length >= 6 && password == confirmPassword

        LaunchedEffect(state.isSuccess) {
            if (state.isSuccess) navigator.replaceAll(FeedScreen())
        }

        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DustyRose,
            unfocusedBorderColor = Outline,
            focusedLabelColor = DustyRose,
            cursorColor = DustyRose
        )

        Box(modifier = Modifier.fillMaxSize().background(Background).imePadding()) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 80.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Crear cuenta", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                Text("Únete a HuellaLive", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre completo") }, leadingIcon = { Icon(Icons.Default.Person, null, tint = TextSecondary) }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = fieldColors)

                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Email, null, tint = TextSecondary) }, isError = !isEmailValid, supportingText = if (!isEmailValid) {{ Text("Email inválido", color = Error) }} else null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = fieldColors)

                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextSecondary) }, trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TextSecondary) } }, visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), supportingText = { Text("Mínimo 6 caracteres", color = TextTertiary) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = fieldColors)

                OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Confirmar contraseña") }, leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextSecondary) }, isError = !passwordsMatch, supportingText = if (!passwordsMatch) {{ Text("Las contraseñas no coinciden", color = Error) }} else null, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = fieldColors)

                if (state.errorMessage != null) {
                    Text(state.errorMessage!!, color = Error, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = { viewModel.registerHuman(name, email, password) },
                    enabled = !state.isLoading && isFormValid,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                ) {
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = TextOnAccent, strokeWidth = 2.dp)
                    else Text("Crear cuenta", style = MaterialTheme.typography.titleMedium, color = TextOnAccent)
                }
            }

            IconButton(onClick = { navigator.pop() }, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                Icon(Icons.Default.ArrowBack, "Volver", tint = TextPrimary)
            }
        }
    }
}