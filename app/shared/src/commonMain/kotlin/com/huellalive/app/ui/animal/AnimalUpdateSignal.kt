package com.huellalive.app.ui.animal

import com.huellalive.app.data.model.AnimalDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AnimalUpdateEvent(
    val version: Int = 0,
    val animal: AnimalDto? = null
)

object AnimalUpdateSignal {
    private val _event = MutableStateFlow(AnimalUpdateEvent())
    val event = _event.asStateFlow()

    fun publish(animal: AnimalDto) {
        _event.value = AnimalUpdateEvent(
            version = _event.value.version + 1,
            animal = animal
        )
    }
}
