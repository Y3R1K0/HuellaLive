package com.huellalive.app.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.huellalive.app.data.model.NotificationDto
import com.huellalive.app.data.repository.NotificationRepository
import com.huellalive.app.utils.Resource
import com.huellalive.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data class NotificationsUiState(val items: List<NotificationDto> = emptyList(), val isLoading: Boolean = false, val errorMessage: String? = null)

class NotificationsViewModel(private val repository: NotificationRepository) : ScreenModel {
    private val _uiState = MutableStateFlow(NotificationsUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()
    init { load() }
    fun load() {
        screenModelScope.launch {
            when (val result = repository.getNotifications()) {
                is Resource.Success -> _uiState.value = NotificationsUiState(items = result.data)
                is Resource.Error -> _uiState.value = NotificationsUiState(errorMessage = result.message)
                else -> Unit
            }
        }
    }
    fun markRead(id: String) {
        screenModelScope.launch {
            when (repository.markRead(id)) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(
                    items = _uiState.value.items.map { if (it.id == id) it.copy(isRead = true) else it }
                )
                is Resource.Error -> Unit
                else -> Unit
            }
        }
    }
}

class NotificationsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository = koinInject<NotificationRepository>()
        val viewModel = rememberScreenModel { NotificationsViewModel(repository) }
        val state by viewModel.uiState.collectAsState()

        LazyColumn(
            Modifier
                .fillMaxSize()
                .background(Background)
                .statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(
                        onClick = { navigator.pop() },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = SurfaceRaised)
                    ) { Icon(Icons.Default.ArrowBack, "Volver", tint = TextPrimary) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Avisos", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        val unread = state.items.count { !it.isRead }
                        Text(
                            if (unread > 0) "$unread sin leer" else "Todo está al día",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = DustyRose)
                    }
                }
            }
            if (state.errorMessage != null) item {
                Surface(color = Error.copy(alpha = 0.12f), shape = RoundedCornerShape(14.dp)) {
                    Text(state.errorMessage!!, color = Error, modifier = Modifier.padding(14.dp))
                }
            }
            if (state.isLoading && state.items.isEmpty()) item {
                Box(Modifier.fillParentMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DustyRose)
                }
            }
            if (!state.isLoading && state.items.isEmpty()) item { NotificationsEmptyState() }
            items(state.items) { item ->
                NotificationCard(item = item, onMarkRead = { viewModel.markRead(item.id) })
            }
            item { Spacer(Modifier.height(70.dp)) }
        }
    }
}

@Composable
private fun NotificationCard(item: NotificationDto, onMarkRead: () -> Unit) {
    val visual = notificationVisual(item.type)
    Surface(
        onClick = { if (!item.isRead) onMarkRead() },
        color = if (item.isRead) Surface else visual.color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (item.isRead) Outline else visual.color.copy(alpha = 0.38f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.Top) {
            Surface(
                color = visual.color.copy(alpha = 0.17f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    visual.icon,
                    contentDescription = null,
                    tint = visual.color,
                    modifier = Modifier.padding(10.dp).size(23.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        color = TextPrimary,
                        fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (!item.isRead) {
                        Box(Modifier.size(8.dp).background(visual.color, CircleShape))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(item.body, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    notificationTime(item.createdAt),
                    color = TextTertiary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (!item.isRead) {
                IconButton(onClick = onMarkRead, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Done, contentDescription = "Marcar como leído", tint = Success)
                }
            }
        }
    }
}

private data class NotificationVisual(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: androidx.compose.ui.graphics.Color
)

private fun notificationVisual(type: String): NotificationVisual = when (type) {
    "ADOPTION_REQUEST" -> NotificationVisual(Icons.Default.Pets, DustyRose)
    "ADOPTION_ACCEPTED" -> NotificationVisual(Icons.Default.Favorite, AdoptionGreen)
    "ADOPTION_REJECTED" -> NotificationVisual(Icons.Default.Info, StatusRecovering)
    "ADOPTION_TRANSFERRED" -> NotificationVisual(Icons.Default.Home, AdoptionGreen)
    else -> NotificationVisual(Icons.Default.Notifications, InfoBlue)
}

private fun notificationTime(value: String): String {
    val date = value.take(10)
    val time = value.substringAfter('T', "").take(5)
    return listOf(date, time).filter { it.isNotBlank() }.joinToString(" · ")
}

@Composable
private fun NotificationsEmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(color = DustyRose.copy(alpha = 0.14f), shape = CircleShape) {
            Icon(
                Icons.Default.NotificationsNone,
                contentDescription = null,
                tint = DustyRose,
                modifier = Modifier.padding(18.dp).size(34.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text("Sin avisos pendientes", color = TextPrimary, fontWeight = FontWeight.Bold)
        Text("Aquí verás novedades sobre adopciones y cuidados.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}
