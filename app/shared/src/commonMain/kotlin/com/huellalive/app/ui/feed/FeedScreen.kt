package com.huellalive.app.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.huellalive.app.data.local.SessionManager
import com.huellalive.app.data.repository.AuthRepository
import com.huellalive.app.data.repository.FeedRepository
import com.huellalive.app.ui.animal.AnimalDetailScreenData
import com.huellalive.app.ui.auth.AuthChoiceScreen
import com.huellalive.app.ui.components.BottomNavBar
import com.huellalive.app.ui.components.HuellaLoadingIndicator
import com.huellalive.app.ui.components.PreloadVideo
import com.huellalive.app.ui.human.HumanProfileScreen
import com.huellalive.app.ui.main.MainScreen
import com.huellalive.app.ui.shelter.ShelterProfileScreen
import com.huellalive.app.ui.theme.DustyRose
import com.huellalive.app.ui.theme.TextOnAccent
import com.huellalive.app.ui.video.SelectAnimalForVideoScreen
import org.koin.compose.koinInject

class FeedScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val feedRepository = koinInject<FeedRepository>()
        val authRepository = koinInject<AuthRepository>()
        val sessionManager = koinInject<SessionManager>()
        val viewModel = rememberScreenModel { FeedViewModel(feedRepository, authRepository) }
        val state by viewModel.uiState.collectAsState()
        val isLoggedIn = sessionManager.isLoggedIn()
        val userRole = sessionManager.getUserRole()

        val pagerState = rememberPagerState { state.videos.size }

        LaunchedEffect(pagerState.currentPage) {
            if (state.videos.isNotEmpty() && pagerState.currentPage >= state.videos.size - 3) {
                viewModel.loadMore()
            }
        }

        LaunchedEffect(pagerState.settledPage, state.videos) {
            if (state.videos.isNotEmpty()) {
                state.videos.getOrNull(pagerState.settledPage)?.let(viewModel::markVideoViewed)
            }
        }

        Scaffold(
            containerColor = Color.Black,
            bottomBar = {
                BottomNavBar(
                    isLoggedIn = isLoggedIn,
                    userRole = userRole,
                    onFeedClick = { navigator.replaceAll(MainScreen("feed")) },
                    onExploreClick = { navigator.replaceAll(MainScreen("explore")) },
                    onSearchClick = { navigator.replaceAll(MainScreen("search")) },
                    onProfileClick = {
                        when (userRole) {
                            "HUMAN" -> navigator.push(HumanProfileScreen())
                            "SHELTER" -> navigator.push(ShelterProfileScreen())
                            else -> navigator.push(AuthChoiceScreen())
                        }
                    },
                    onLoginClick = { navigator.push(AuthChoiceScreen()) },
                    onUploadClick = { navigator.push(SelectAnimalForVideoScreen()) }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(bottom = padding.calculateBottomPadding())
            ) {
                when {
                    state.isLoading -> HuellaLoadingIndicator(Modifier.align(Alignment.Center))

                    state.errorMessage != null -> Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.errorMessage!!, color = Color.White)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadFeed() },
                            colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                        ) {
                            Text("Reintentar", color = TextOnAccent)
                        }
                    }

                    state.videos.isEmpty() -> Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Paw", fontSize = 24.sp, color = DustyRose)
                        Spacer(Modifier.height(16.dp))
                        Text("No hay videos aun", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text("Los videos apareceran aqui", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }

                    else -> {
                        PreloadVideo(
                            state.videos.getOrNull(pagerState.settledPage + 1)
                                ?.videoUrl
                                ?.let(::stableCloudinaryVideoUrl)
                        )
                        VerticalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            VideoFeedItem(
                                video = state.videos[page],
                                isActive = pagerState.settledPage == page,
                                animalMediaRevision = state.animalMediaRevision,
                                onLikeClick = viewModel::toggleLike,
                                onReportClick = { video, reason -> viewModel.reportVideo(video, reason) },
                                onReportProfileClick = { video, reason -> viewModel.reportAnimal(video, reason) },
                                onAnimalClick = { animalId -> navigator.push(AnimalDetailScreenData(animalId)) }
                            )
                        }
                    }
                }
            }
        }
    }
}
