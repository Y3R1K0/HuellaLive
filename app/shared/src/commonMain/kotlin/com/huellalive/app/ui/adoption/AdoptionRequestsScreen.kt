package com.huellalive.app.ui.adoption

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.huellalive.app.data.model.AdoptionRequestDto
import com.huellalive.app.data.repository.AdoptionRepository
import com.huellalive.app.utils.Resource
import com.huellalive.app.ui.chat.ChatScreen
import com.huellalive.app.ui.components.HuellaLoadingIndicator
import com.huellalive.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data class AdoptionUiState(
    val requests: List<AdoptionRequestDto> = emptyList(),
    val isLoading: Boolean = false,
    val actionInProgress: String? = null,
    val errorMessage: String? = null
)

class AdoptionRequestsViewModel(private val repository: AdoptionRepository) : ScreenModel {
    private val _uiState = MutableStateFlow(AdoptionUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()
    init { load() }
    fun load() {
        screenModelScope.launch {
            when (val result = repository.getRequests()) {
                is Resource.Success -> _uiState.value = AdoptionUiState(requests = result.data)
                is Resource.Error -> _uiState.value = AdoptionUiState(errorMessage = result.message)
                else -> Unit
            }
        }
    }
    fun update(id: String, status: String) {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = id, errorMessage = null)
            when (val result = repository.updateRequest(id, status)) {
                is Resource.Success -> {
                    _uiState.value = if (status == "REJECTED") {
                        _uiState.value.copy(
                            requests = _uiState.value.requests.filterNot { it.id == id },
                            actionInProgress = null,
                            errorMessage = null
                        )
                    } else {
                        _uiState.value.copy(
                            requests = _uiState.value.requests.map { if (it.id == id) result.data else it },
                            actionInProgress = null,
                            errorMessage = null
                        )
                    }
                }
                is Resource.Error -> _uiState.value = _uiState.value.copy(actionInProgress = null, errorMessage = result.message)
                else -> Unit
            }
        }
    }
    fun acceptAndOpenChat(id: String, onChatReady: (AdoptionRequestDto) -> Unit) {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = id, errorMessage = null)
            when (val result = repository.updateRequest(id, "ACCEPTED")) {
                is Resource.Success -> {
                    val updated = result.data
                    _uiState.value = _uiState.value.copy(
                        requests = _uiState.value.requests.map { if (it.id == id) updated else it },
                        actionInProgress = null,
                        errorMessage = null
                    )
                    if (updated.chatId != null) {
                        onChatReady(updated)
                    }
                }
                is Resource.Error -> _uiState.value = _uiState.value.copy(actionInProgress = null, errorMessage = result.message)
                else -> Unit
            }
        }
    }
}

class AdoptionRequestsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository = koinInject<AdoptionRepository>()
        val viewModel = rememberScreenModel { AdoptionRequestsViewModel(repository) }
        val state by viewModel.uiState.collectAsState()
        var requestToReject by remember { mutableStateOf<AdoptionRequestDto?>(null) }

        requestToReject?.let { request ->
            AlertDialog(
                onDismissRequest = { requestToReject = null },
                icon = { Icon(Icons.Default.Cancel, contentDescription = null, tint = Error) },
                title = { Text("Rechazar solicitud", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Se cerrará la solicitud de ${request.human?.name ?: "esta persona"} para adoptar a ${request.animal?.name ?: "este animal"}.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            requestToReject = null
                            viewModel.update(request.id, "REJECTED")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Error)
                    ) { Text("Rechazar", color = TextOnAccent) }
                },
                dismissButton = {
                    OutlinedButton(onClick = { requestToReject = null }) { Text("Cancelar") }
                },
                containerColor = Surface
            )
        }

        LazyColumn(
            Modifier
                .fillMaxSize()
                .background(Background)
                .statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        Text("Solicitudes", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Revisa y coordina cada adopción", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { viewModel.load() }) { Icon(Icons.Default.Refresh, null, tint = DustyRose) }
                }
            }
            if (state.errorMessage != null) item {
                Surface(color = Error.copy(alpha = 0.12f), shape = RoundedCornerShape(14.dp)) {
                    Text(state.errorMessage!!, color = Error, modifier = Modifier.padding(14.dp))
                }
            }
            if (state.isLoading && state.requests.isEmpty()) item {
                Box(Modifier.fillParentMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.Center) {
                    HuellaLoadingIndicator()
                }
            }
            if (!state.isLoading && state.requests.isEmpty()) item {
                AdoptionEmptyState()
            }
            items(state.requests) { request ->
                AdoptionRequestCard(
                    request = request,
                    isWorking = state.actionInProgress == request.id,
                    onReject = { requestToReject = request },
                    onAccept = {
                        viewModel.acceptAndOpenChat(request.id) { accepted ->
                            navigator.push(
                                ChatScreen(
                                    accepted.chatId!!,
                                    accepted.human?.name ?: "Adopción",
                                    accepted.human?.avatarUrl
                                )
                            )
                        }
                    },
                    onOpenChat = {
                        request.chatId?.let {
                            navigator.push(ChatScreen(it, request.human?.name ?: "Adopción", request.human?.avatarUrl))
                        }
                    }
                )
            }
            item { Spacer(Modifier.height(70.dp)) }
        }
    }
}

@Composable
private fun AdoptionRequestCard(
    request: AdoptionRequestDto,
    isWorking: Boolean,
    onReject: () -> Unit,
    onAccept: () -> Unit,
    onOpenChat: () -> Unit
) {
    val accepted = request.status == "ACCEPTED"
    val animal = request.animal
    val human = request.human
    Surface(
        color = Surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (accepted) AdoptionGreen.copy(alpha = 0.5f) else Outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    AsyncImage(
                        model = animal?.photoUrl,
                        contentDescription = animal?.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(68.dp).clip(RoundedCornerShape(18.dp)).background(SurfaceRaised)
                    )
                    Surface(
                        color = if (accepted) AdoptionGreen else DustyRose,
                        shape = CircleShape,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Icon(
                            if (accepted) Icons.Default.Check else Icons.Default.Pets,
                            contentDescription = null,
                            tint = TextOnAccent,
                            modifier = Modifier.padding(5.dp).size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(animal?.name ?: "Animal", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(animal?.species ?: "Especie no registrada", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(5.dp))
                    Surface(
                        color = (if (accepted) AdoptionGreen else StatusRecovering).copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            if (accepted) "Coordinando adopción" else "Pendiente de revisión",
                            color = if (accepted) AdoptionGreen else StatusRecovering,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().background(SurfaceRaised, RoundedCornerShape(14.dp)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = human?.avatarUrl,
                    contentDescription = human?.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(DustyRose.copy(alpha = 0.18f))
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(human?.name ?: request.humanId.take(8), color = TextPrimary, fontWeight = FontWeight.Medium)
                    Text(
                        if (accepted) "Continúa la coordinación en el chat" else "Quiere ofrecerle un hogar",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (accepted) {
                Button(
                    onClick = onOpenChat,
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdoptionGreen)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("Abrir chat", fontWeight = FontWeight.Bold)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedButton(
                        onClick = onReject,
                        enabled = !isWorking,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Rechazar", color = Error) }
                    Button(
                        onClick = onAccept,
                        enabled = !isWorking,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                    ) {
                        if (isWorking) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = TextOnAccent)
                        } else {
                            Icon(Icons.Default.Favorite, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Aceptar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdoptionEmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(color = DustyRose.copy(alpha = 0.14f), shape = CircleShape) {
            Icon(Icons.Default.Pets, contentDescription = null, tint = DustyRose, modifier = Modifier.padding(18.dp).size(34.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text("No hay solicitudes", color = TextPrimary, fontWeight = FontWeight.Bold)
        Text("Las nuevas solicitudes aparecerán aquí.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}
