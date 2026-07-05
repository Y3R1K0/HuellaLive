package com.huellalive.app.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.huellalive.app.data.model.VideoDto
import com.huellalive.app.data.repository.FeedRepository
import com.huellalive.app.ui.animal.AnimalDetailScreenData
import com.huellalive.app.ui.components.HuellaLoadingIndicator
import com.huellalive.app.ui.theme.*
import com.huellalive.app.utils.cloudinaryVideoPosterUrl
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data class VideoHistoryUiState(
    val videos: List<VideoDto> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class VideoHistoryViewModel(private val feedRepository: FeedRepository) : ScreenModel {
    private val _uiState = MutableStateFlow(VideoHistoryUiState())
    val uiState: StateFlow<VideoHistoryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = feedRepository.getViewedVideos()) {
                is Resource.Success -> _uiState.value = VideoHistoryUiState(
                    videos = result.data,
                    isLoading = false
                )
                is Resource.Error -> _uiState.value = VideoHistoryUiState(
                    isLoading = false,
                    errorMessage = result.message
                )
                else -> Unit
            }
        }
    }
}

class VideoHistoryScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val feedRepository = koinInject<FeedRepository>()
        val viewModel = rememberScreenModel { VideoHistoryViewModel(feedRepository) }
        val state by viewModel.uiState.collectAsState()

        Column(
            Modifier
                .fillMaxSize()
                .background(Background)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Videos vistos",
                        color = TextPrimary,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Tu historial reciente del feed",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = viewModel::load) {
                    Icon(Icons.Default.History, contentDescription = "Actualizar historial", tint = DustyRose)
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    HuellaLoadingIndicator()
                }
                state.errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.errorMessage.orEmpty(), color = Error)
                }
                state.videos.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, null, tint = DustyRose, modifier = Modifier.size(46.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("Aun no hay videos vistos", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Cuando explores el feed apareceran aqui", color = TextSecondary)
                    }
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 26.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.videos, key = { it.id }) { video ->
                        HistoryVideoCard(video) {
                            video.animal?.id?.let { animalId -> navigator.push(AnimalDetailScreenData(animalId)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryVideoCard(video: VideoDto, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.78f)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(SurfaceRaised)
            ) {
                AsyncImage(
                    model = video.thumbnailUrl ?: cloudinaryVideoPosterUrl(video.videoUrl),
                    contentDescription = video.animal?.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.68f))
                            )
                        )
                )
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.Center).size(42.dp)
                )
                Text(
                    "${video.likesCount} me gusta",
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
                )
            }
            Column(Modifier.padding(10.dp)) {
                Text(
                    video.animal?.name ?: "Animal",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    video.animal?.shelter?.user?.name ?: video.author.name,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
