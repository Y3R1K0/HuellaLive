package com.huellalive.app.ui

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.huellalive.app.data.local.SessionManager
import com.huellalive.app.ui.feed.FeedScreen
import com.huellalive.app.ui.theme.HuellaLiveTheme
import org.koin.compose.koinInject

@Composable
fun App() {
    HuellaLiveTheme {
        val sessionManager = koinInject<SessionManager>()
        Navigator(FeedScreen()) { navigator ->
            SlideTransition(navigator)
        }
    }
}