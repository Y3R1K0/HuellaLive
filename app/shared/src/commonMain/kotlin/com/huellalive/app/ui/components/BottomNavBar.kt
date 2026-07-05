package com.huellalive.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.huellalive.app.ui.theme.BottomBar
import com.huellalive.app.ui.theme.SurfaceHigh
import com.huellalive.app.ui.theme.TextSecondary

@Composable
fun BottomNavBar(
    modifier: Modifier = Modifier,
    selectedItem: String = "feed",
    isLoggedIn: Boolean,
    userRole: String?,
    onFeedClick: () -> Unit,
    onExploreClick: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLoginClick: () -> Unit,
    onUploadClick: () -> Unit
) {
    val items = buildList {
        add(NavItem("feed", "Feed", Icons.Default.Home, onFeedClick))
        add(NavItem("explore", "Explorar", Icons.Default.Explore, onExploreClick))
        add(NavItem("search", "Buscar", Icons.Default.Search, onSearchClick))
        if (isLoggedIn && userRole == "SHELTER") {
            add(NavItem("upload", "Subir", Icons.Default.AddCircle, onUploadClick))
        }
        add(
            NavItem(
                "profile",
                if (isLoggedIn) "Perfil" else "Entrar",
                if (isLoggedIn) Icons.Default.Person else Icons.Default.Login,
                if (isLoggedIn) onProfileClick else onLoginClick
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = BottomBar.copy(alpha = 0.92f),
            shape = RoundedCornerShape(32.dp),
            tonalElevation = 0.dp,
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    DockNavItem(
                        item = item,
                        selected = selectedItem == item.key,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private data class NavItem(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun DockNavItem(item: NavItem, selected: Boolean, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.9f
            selected -> 1.05f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 560f),
        label = "${item.key} dock scale"
    )
    val chipWidth by animateDpAsState(
        targetValue = if (selected) 56.dp else 48.dp,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 520f),
        label = "${item.key} chip width"
    )
    val chipColor by animateColorAsState(
        targetValue = if (selected) SurfaceHigh else Color.Transparent,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 520f),
        label = "${item.key} chip color"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) Color.White else TextSecondary,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 520f),
        label = "${item.key} icon color"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = item.onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = chipColor,
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.width(chipWidth).height(50.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = iconColor,
                    modifier = Modifier.size(if (selected) 25.dp else 23.dp)
                )
            }
        }
    }
}
