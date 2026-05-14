package com.jumpmaster.app.ui.main

import android.util.Log
import com.jumpmaster.app.data.local.db.JumpRepository
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MainSessionPersistCoordinator @Inject constructor(
    private val jumpRepository: JumpRepository,
) {
    private val mutex = Mutex()
    private var trainingEndPersistSucceeded = false

    fun resetForNewEnd() {
        trainingEndPersistSucceeded = false
    }

    suspend fun persistIfNeeded(summary: SessionSummary): SessionPersistWorkResult =
        mutex.withLock {
            if (trainingEndPersistSucceeded) {
                return@withLock SessionPersistWorkResult.Skipped
            }
            runCatching {
                jumpRepository.insertCompletedTrainingSession(
                    summary.jumpCount,
                    summary.effectiveDurationMs,
                )
            }.fold(
                onSuccess = {
                    trainingEndPersistSucceeded = true
                    SessionPersistWorkResult.Saved
                },
                onFailure = {
                    Log.e(POSE_PIPELINE_LOG_TAG, "session persist failed", it)
                    SessionPersistWorkResult.Failed(it)
                },
            )
        }
}
