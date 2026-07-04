package com.huellalive.app.ui.feed

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object FeedRefreshSignal {
    private val _version = MutableStateFlow(0)
    val version = _version.asStateFlow()

    fun invalidate() {
        _version.value += 1
    }
}
