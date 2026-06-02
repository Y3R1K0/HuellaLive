package com.huellalive.app.ui.auth

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
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
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

        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var socialLinks by remember { mutableStateOf("") }
        var showSuccessDialog by remember { mutableStateOf(false) }

        val isFormValid = name.isNotBlank() && email.contains("@") &&
                password.length >= 6 && description.isNotBlank() &&
                location.isNotBlank() && phone.isNotBlank()

        LaunchedEffect(state.isSuccess) {
            if (state.isSuccess) showSuccessDialog = true
        }

        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DustyRose,
            unfocusedBorderColor = Outline,
            focusedLabelColor = DustyRose,
            cursorColor = DustyRose
        )

        // Dialog de éxito
        if (showSuccessDialog) {
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
                        Text("🏠", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Solicitud enviada", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Revisaremos tu información y te notificaremos cuando sea aprobada.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { navigator.pop() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                        ) {
                            Text("Entendido", color = TextOnAccent)
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(Background).imePadding()) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 80.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Registrar Albergue", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                Text("Tu cuenta será revisada antes de ser aprobada", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(8.dp))

                SectionLabel("Datos de la cuenta")
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre del albergue") }, leadingIcon = { Icon(Icons.Default.Home, null, tint = TextSecondary) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = fieldColors)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Email, null, tint = TextSecondary) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = fieldColors)
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, leadingIcon = { Icon(Icons.Default.Lock, null, tint = TextSecondary) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = fieldColors)

                Spacer(Modifier.height(4.dp))
                SectionLabel("Información del albergue")
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descripción") }, leadingIcon = { Icon(Icons.Default.Info, null, tint = TextSecondary) }, minLines = 3, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = fieldColors)
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Ubicación / Dirección") }, leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = TextSecondary) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = fieldColors)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono") }, leadingIcon = { Icon(Icons.Default.Phone, null, tint = TextSecondary) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = fieldColors)

                Spacer(Modifier.height(4.dp))
                SectionLabel("Verificación (recomendado)")
                Text("Agrega links de redes sociales o referencias que demuestren que el albergue existe.", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                OutlinedTextField(value = socialLinks, onValueChange = { socialLinks = it }, label = { Text("Links o referencias") }, leadingIcon = { Icon(Icons.Default.Link, null, tint = TextSecondary) }, minLines = 2, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = fieldColors)

                if (state.errorMessage != null) {
                    Text(state.errorMessage!!, color = Error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        val docs = socialLinks.split("\n").filter { it.isNotBlank() }
                        viewModel.registerShelter(name, email, password, description, location, phone, docs)
                    },
                    enabled = !state.isLoading && isFormValid,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                ) {
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = TextOnAccent, strokeWidth = 2.dp)
                    else Text("Enviar solicitud", style = MaterialTheme.typography.titleMedium, color = TextOnAccent)
                }
            }

            IconButton(onClick = { navigator.pop() }, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                Icon(Icons.Default.ArrowBack, "Volver", tint = TextPrimary)
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