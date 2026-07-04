package com.huellalive.app.ui.shelter

import com.huellalive.app.data.model.VideoDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ShelterStoryUpdateEvent(
    val version: Int = 0,
    val story: VideoDto? = null,
    val deletedStoryId: String? = null
)

object ShelterStoryUpdateSignal {
    private val _event = MutableStateFlow(ShelterStoryUpdateEvent())
    val event = _event.asStateFlow()

    fun publish(story: VideoDto) {
        _event.value = ShelterStoryUpdateEvent(
            version = _event.value.version + 1,
            story = story
        )
    }

    fun delete(storyId: String) {
        _event.value = ShelterStoryUpdateEvent(
            version = _event.value.version + 1,
            deletedStoryId = storyId
        )
    }
}
