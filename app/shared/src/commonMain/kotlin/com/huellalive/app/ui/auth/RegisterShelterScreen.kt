package com.huellalive.app.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.huellalive.app.auth.rememberGoogleSignInAction
import com.huellalive.app.data.repository.AuthRepository
import com.huellalive.app.ui.theme.*
import org.koin.compose.koinInject

class RegisterShelterScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authRepository = koinInject<AuthRepository>()
        val viewModel = rememberScreenModel { AuthViewModel(authRepository) }
        val state by viewModel.state.collectAsState()

        var step by remember { mutableStateOf(0) }
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var socialLinks by remember { mutableStateOf("") }
        var googleVerified by remember { mutableStateOf(false) }
        var showSuccessDialog by remember { mutableStateOf(false) }

        val googleSignIn = rememberGoogleSignInAction(
            onIdToken = viewModel::previewFirebaseProfile,
            onError = viewModel::showError
        )

        LaunchedEffect(state.firebaseProfile) {
            val profile = state.firebaseProfile ?: return@LaunchedEffect
            email = profile.email
            if (name.isBlank()) name = profile.name
            googleVerified = true
        }

        val cleanEmail = email.trim().lowercase()
        val isGmailValid = cleanEmail.endsWith("@gmail.com") || cleanEmail.endsWith("@googlemail.com")
        val isAccountValid = name.isNotBlank() && isGmailValid && password.length >= 6
        val isShelterInfoValid = description.isNotBlank() && location.isNotBlank() && phone.isNotBlank()
        val isFormValid = isAccountValid && isShelterInfoValid

        LaunchedEffect(state.isSuccess) {
            if (state.isSuccess) showSuccessDialog = true
        }

        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DustyRose,
            unfocusedBorderColor = Outline,
            focusedLabelColor = DustyRose,
            cursorColor = DustyRose,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )

        if (showSuccessDialog) {
            ShelterRequestSentDialog(onDone = { navigator.pop() })
        }

        Box(modifier = Modifier.fillMaxSize().background(Background).imePadding()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 80.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Registrar albergue", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                Text("Completa la solicitud en tres pasos.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                ShelterStepIndicator(step)

                state.errorMessage?.let {
                    Text(it, color = Error, style = MaterialTheme.typography.bodySmall)
                }
                state.successMessage?.let {
                    Text(it, color = AdoptionGreen, style = MaterialTheme.typography.bodySmall)
                }

                AnimatedContent(targetState = step, label = "Shelter registration step") { currentStep ->
                    when (currentStep) {
                        0 -> AccountStep(
                            name = name,
                            email = email,
                            password = password,
                            googleVerified = googleVerified,
                            isGmailValid = isGmailValid,
                            fieldColors = fieldColors,
                            state = state,
                            onNameChange = {
                                name = it
                                googleVerified = false
                            },
                            onEmailChange = {
                                email = it
                                googleVerified = false
                            },
                            onPasswordChange = { password = it },
                            onGoogleClick = {
                                viewModel.beginExternalLogin()
                                googleSignIn.launch()
                            }
                        )
                        1 -> ShelterInfoStep(
                            description = description,
                            location = location,
                            phone = phone,
                            fieldColors = fieldColors,
                            onDescriptionChange = { description = it },
                            onLocationChange = { location = it },
                            onPhoneChange = { phone = it }
                        )
                        else -> ReviewStep(
                            name = name,
                            email = email,
                            description = description,
                            location = location,
                            phone = phone,
                            socialLinks = socialLinks,
                            fieldColors = fieldColors,
                            onSocialLinksChange = { socialLinks = it }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { if (step == 0) navigator.pop() else step -= 1 },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (step == 0) "Cancelar" else "Atras", color = TextSecondary)
                    }
                    Button(
                        onClick = {
                            when (step) {
                                0 -> step = 1
                                1 -> step = 2
                                else -> {
                                    val docs = socialLinks.split("\n").filter { it.isNotBlank() }
                                    viewModel.registerShelter(name, email, password, description, location, phone, docs)
                                }
                            }
                        },
                        enabled = !state.isLoading && when (step) {
                            0 -> isAccountValid
                            1 -> isShelterInfoValid
                            else -> isFormValid
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = TextOnAccent, strokeWidth = 2.dp)
                        } else {
                            Text(if (step == 2) "Enviar solicitud" else "Continuar", color = TextOnAccent)
                        }
                    }
                }
            }

            IconButton(onClick = { navigator.pop() }, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                Icon(Icons.Default.ArrowBack, "Volver", tint = TextPrimary)
            }
        }
    }
}

@Composable
private fun AccountStep(
    name: String,
    email: String,
    password: String,
    googleVerified: Boolean,
    isGmailValid: Boolean,
    fieldColors: TextFieldColors,
    state: AuthUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onGoogleClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("Cuenta del albergue")
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Nombre del albergue") },
            leadingIcon = { Icon(Icons.Default.Home, null, tint = TextSecondary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors
        )
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Gmail del albergue") },
            leadingIcon = { Icon(Icons.Default.Email, null, tint = TextSecondary) },
            trailingIcon = {
                if (googleVerified) Icon(Icons.Default.Verified, null, tint = AdoptionGreen)
            },
            isError = email.isNotBlank() && !isGmailValid,
            supportingText = {
                Text(
                    when {
                        googleVerified -> "Gmail verificado con Google."
                        email.isBlank() || isGmailValid -> "Usaremos este Gmail para revisar y recuperar la cuenta."
                        else -> "Ingresa un Gmail valido. Solo se permite una solicitud por Gmail."
                    },
                    color = when {
                        googleVerified -> AdoptionGreen
                        email.isNotBlank() && !isGmailValid -> Error
                        else -> TextTertiary
                    }
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors
        )
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextSecondary) },
            supportingText = { Text("Minimo 6 caracteres", color = TextTertiary) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors
        )
        AuthDivider()
        GoogleLoginButton(
            enabled = !state.isLoading,
            text = "Usar Gmail de Google",
            onClick = onGoogleClick
        )
    }
}

@Composable
private fun ShelterInfoStep(
    description: String,
    location: String,
    phone: String,
    fieldColors: TextFieldColors,
    onDescriptionChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("Datos del albergue")
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Descripcion") },
            leadingIcon = { Icon(Icons.Default.Info, null, tint = TextSecondary) },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors
        )
        OutlinedTextField(
            value = location,
            onValueChange = onLocationChange,
            label = { Text("Ubicacion / direccion") },
            leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = TextSecondary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors
        )
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Telefono") },
            leadingIcon = { Icon(Icons.Default.Phone, null, tint = TextSecondary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors
        )
    }
}

@Composable
private fun ReviewStep(
    name: String,
    email: String,
    description: String,
    location: String,
    phone: String,
    socialLinks: String,
    fieldColors: TextFieldColors,
    onSocialLinksChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("Revisar solicitud")
        ReviewRow("Albergue", name)
        ReviewRow("Gmail", email)
        ReviewRow("Direccion", location)
        ReviewRow("Telefono", phone)
        ReviewRow("Descripcion", description)
        OutlinedTextField(
            value = socialLinks,
            onValueChange = onSocialLinksChange,
            label = { Text("Links o referencias") },
            leadingIcon = { Icon(Icons.Default.Link, null, tint = TextSecondary) },
            supportingText = {
                Text("Opcional, pero ayuda a aprobar el albergue mas rapido.", color = TextTertiary)
            },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors
        )
    }
}

@Composable
private fun ShelterStepIndicator(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("Cuenta", "Albergue", "Enviar").forEachIndexed { index, label ->
            Surface(
                modifier = Modifier.weight(1f),
                color = if (index <= step) DustyRose.copy(alpha = 0.2f) else SurfaceHigh,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("${index + 1}", color = if (index <= step) DustyRose else TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Text(label, color = if (index <= step) TextPrimary else TextSecondary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Surface(color = SurfaceHigh, shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            Text(value.ifBlank { "-" }, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ShelterRequestSentDialog(onDone: () -> Unit) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = Surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Solicitud enviada", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Revisaremos tu informacion y te notificaremos cuando sea aprobada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                ) {
                    Text("Entendido", color = TextOnAccent)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = DustyRose,
        modifier = Modifier.fillMaxWidth()
    )
}
