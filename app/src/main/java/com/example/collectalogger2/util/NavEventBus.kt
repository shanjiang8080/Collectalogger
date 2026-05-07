package com.example.collectalogger2.util

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableSharedFlow

class NavEventBus {
    // SharedFlow ensures multiple screens can listen,
    // and events are dropped if no one is listening.
    val scrollToTopEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)

    suspend fun emitScrollEvent(route: String) {
        scrollToTopEvent.emit(route)
    }
}

// Create a CompositionLocal so it's accessible anywhere
val LocalNavEventBus = staticCompositionLocalOf<NavEventBus> {
    error("No NavEventBus provided")
}