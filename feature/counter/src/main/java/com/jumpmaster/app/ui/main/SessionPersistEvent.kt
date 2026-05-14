package com.jumpmaster.app.ui.main

sealed interface SessionPersistEvent {
    data object Saved : SessionPersistEvent

    data object Failed : SessionPersistEvent
}

sealed class SessionPersistWorkResult {
    data object Skipped : SessionPersistWorkResult()

    data object Saved : SessionPersistWorkResult()

    data class Failed(val error: Throwable) : SessionPersistWorkResult()
}
