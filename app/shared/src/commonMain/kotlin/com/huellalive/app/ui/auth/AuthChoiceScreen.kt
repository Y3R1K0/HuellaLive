package com.huellalive.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.huellalive.app.ui.theme.*

class AuthChoiceScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🐾", fontSize = 64.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "HuellaLive",
                style = MaterialTheme.typography.headlineLarge,
                color = DustyRose
            )
            Text(
                "¿Cómo quieres continuar?",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(48.dp))

            AuthOptionCard(
                icon = Icons.Default.Person,
                title = "Soy una persona",
                subtitle = "Explora, adopta y apoya albergues",
                onLogin = { navigator.push(LoginHumanScreen()) },
                onRegister = { navigator.push(RegisterHumanScreen()) }
            )

            Spacer(Modifier.height(16.dp))

            AuthOptionCard(
                icon = Icons.Default.Home,
                title = "Soy un albergue",
                subtitle = "Gestiona animales y encuentra familias",
                onLogin = { navigator.push(LoginShelterScreen()) },
                onRegister = { navigator.push(RegisterShelterScreen()) }
            )
        }
    }
}

@Composable
private fun AuthOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onLogin: () -> Unit,
    onRegister: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, LavenderSoft.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = DustyRose, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onLogin,
                modifier = Modifier.weight(1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, DustyRose.copy(alpha = 0.5f))
            ) {
                Text("Iniciar sesión", color = DustyRose)
            }
            Button(
                onClick = onRegister,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
            ) {
                Text("Registrarse", color = TextOnAccent)
            }
        }
    }
}