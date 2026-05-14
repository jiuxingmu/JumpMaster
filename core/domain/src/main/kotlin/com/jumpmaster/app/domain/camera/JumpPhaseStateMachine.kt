package com.jumpmaster.app.domain.camera

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.abs

private enum class JumpPhase {
    IDLE,
    IN_JUMP,
}

internal class JumpPhaseStateMachine(
    private val minJumpAmplitude: Float,
    private val minSpacingMs: Long,
    private val maxJumpPhaseMs: Long,
) {

    private var phase = JumpPhase.IDLE
    private var lastCountRealtimeMs = 0L
    private var jumpPhaseStartRealtimeMs = Long.MIN_VALUE
    private var jumpDirectionSign = 0
    private var jumpMaxAbsDeviation = 0f

    private val _jumpPulse =
        MutableSharedFlow<Unit>(
            replay = 0,
            extraBufferCapacity = 16,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val jumpEvents: SharedFlow<Unit> = _jumpPulse.asSharedFlow()

    fun reset() {
        phase = JumpPhase.IDLE
        jumpPhaseStartRealtimeMs = Long.MIN_VALUE
        jumpDirectionSign = 0
        jumpMaxAbsDeviation = 0f
    }

    fun onSanitizedBaselineReset() {
        phase = JumpPhase.IDLE
        jumpPhaseStartRealtimeMs = Long.MIN_VALUE
    }

    fun isInJump(): Boolean = phase == JumpPhase.IN_JUMP

    fun onRawFilteredFrame(
        filtered: Float,
        baseline: Float,
        thresholds: JumpThresholds,
        elapsedRealtimeMs: Long,
        baselineTracker: JumpBaselineTracker,
    ): Boolean {
        return when (phase) {
            JumpPhase.IDLE ->
                handleIdlePhase(filtered, baseline, thresholds.jumpUpDelta, elapsedRealtimeMs)
            JumpPhase.IN_JUMP ->
                handleInJumpPhase(filtered, baseline, thresholds, elapsedRealtimeMs, baselineTracker)
        }
    }

    private fun handleIdlePhase(
        filtered: Float,
        baseline: Float,
        jumpUpDelta: Float,
        elapsedRealtimeMs: Long,
    ): Boolean {
        val deviation = filtered - baseline
        if (abs(deviation) >= jumpUpDelta) {
            phase = JumpPhase.IN_JUMP
            jumpPhaseStartRealtimeMs = elapsedRealtimeMs
            jumpDirectionSign = if (deviation >= 0f) 1 else -1
            jumpMaxAbsDeviation = abs(deviation)
        }
        return false
    }

    private fun handleTimeoutIfNeeded(
        filtered: Float,
        baseline: Float,
        elapsedRealtimeMs: Long,
        baselineTracker: JumpBaselineTracker,
    ): Boolean {
        val isTimedOut =
            jumpPhaseStartRealtimeMs != Long.MIN_VALUE &&
                elapsedRealtimeMs - jumpPhaseStartRealtimeMs > maxJumpPhaseMs
        if (!isTimedOut) return false
        phase = JumpPhase.IDLE
        jumpPhaseStartRealtimeMs = Long.MIN_VALUE
        if (abs(filtered - baseline) > 0.08f) baselineTracker.baselineY = filtered
        jumpDirectionSign = 0
        jumpMaxAbsDeviation = 0f
        return true
    }

    private fun handleInJumpPhase(
        filtered: Float,
        baseline: Float,
        thresholds: JumpThresholds,
        elapsedRealtimeMs: Long,
        baselineTracker: JumpBaselineTracker,
    ): Boolean {
        if (handleTimeoutIfNeeded(filtered, baseline, elapsedRealtimeMs, baselineTracker)) return false
        val deviation = filtered - baseline
        jumpMaxAbsDeviation = maxOf(jumpMaxAbsDeviation, abs(deviation))
        val hasReturnedNearBaseline = abs(deviation) <= thresholds.recoverDelta
        val hasEnoughAmplitude = jumpMaxAbsDeviation >= maxOf(minJumpAmplitude, thresholds.jumpUpDelta * 1.5f)
        val hasMinSpacing = lastCountRealtimeMs == 0L || elapsedRealtimeMs - lastCountRealtimeMs >= minSpacingMs
        if (!hasReturnedNearBaseline || !hasEnoughAmplitude || !hasMinSpacing) return false
        phase = JumpPhase.IDLE
        jumpPhaseStartRealtimeMs = Long.MIN_VALUE
        lastCountRealtimeMs = elapsedRealtimeMs
        jumpDirectionSign = 0
        jumpMaxAbsDeviation = 0f
        _jumpPulse.tryEmit(Unit)
        return true
    }
}
