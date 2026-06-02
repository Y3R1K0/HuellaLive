package com.huellalive.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.huellalive.app.ui.theme.BottomBar
import com.huellalive.app.ui.theme.DustyRose
import com.huellalive.app.ui.theme.TextSecondary

@Composable
fun BottomNavBar(
    modifier: Modifier = Modifier,
    isLoggedIn: Boolean,
    userRole: String?,
    onFeedClick: () -> Unit,
    onExploreClick: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLoginClick: () -> Unit,
    onUploadClick: () -> Unit
) {
    NavigationBar(
        modifier = modifier,
        containerColor = BottomBar.copy(alpha = 0.94f),
        contentColor = Color.White
    ) {
        NavigationBarItem(
            selected = true,
            onClick = onFeedClick,
            icon = { Icon(Icons.Default.Home, contentDescription = "Feed") },
            label = { Text("Feed") },
            colors = bottomNavColors()
        )
        NavigationBarItem(
            selected = false,
            onClick = onExploreClick,
            icon = { Icon(Icons.Default.Explore, contentDescription = "Explorar") },
            label = { Text("Explorar") },
            colors = bottomNavColors()
        )
        NavigationBarItem(
            selected = false,
            onClick = onSearchClick,
            icon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            label = { Text("Buscar") },
            colors = bottomNavColors()
        )

        if (isLoggedIn && userRole == "SHELTER") {
            NavigationBarItem(
                selected = false,
                onClick = onUploadClick,
                icon = { Icon(Icons.Default.AddCircle, contentDescription = "Subir") },
                label = { Text("Subir") },
                colors = bottomNavColors()
            )
        }

        NavigationBarItem(
            selected = false,
            onClick = if (isLoggedIn) onProfileClick else onLoginClick,
            icon = {
                Icon(
                    imageVector = if (isLoggedIn) Icons.Default.Person else Icons.Default.Login,
                    contentDescription = if (isLoggedIn) "Perfil" else "Entrar"
                )
            },
            label = { Text(if (isLoggedIn) "Perfil" else "Entrar") },
            colors = bottomNavColors()
        )
    }
}

@Composable
private fun bottomNavColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = DustyRose,
    selectedTextColor = DustyRose,
    indicatorColor = DustyRose.copy(alpha = 0.16f),
    unselectedIconColor = TextSecondary,
    unselectedTextColor = TextSecondary
)
