package com.jumpmaster.app.domain.counter

/**
 * Pure domain contract for rope-jump counting.
 * Implementations fuse pose landmarks + optional IMU signals.
 */
interface JumpCounter {

    suspend fun processFrame(/* landmarks + optional accel bundle */): JumpDecision
}

sealed interface JumpDecision {
    data object NoJump : JumpDecision
    data class Counted(val total: Int, val confidence: Float) : JumpDecision
}
