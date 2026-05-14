package com.jumpmaster.app.domain.camera

import kotlin.math.abs

internal data class JumpThresholds(
    val jumpUpDelta: Float,
    val recoverDelta: Float,
)

internal class JumpBaselineTracker(
    private val baselineAlpha: Float,
    private val noiseEmaAlpha: Float,
    private val jumpSigma: Float,
    private val minJumpUpDelta: Float,
    private val maxJumpUpDelta: Float,
    private val minRecoverDelta: Float,
    private val recoverRatio: Float,
) {

    internal var baselineY: Float? = null
    internal var noiseEma: Float = 0.002f
    private var largeOffsetIdleFrames: Int = 0

    @Volatile
    var lastBaselineY: Float = Float.NaN
        private set

    @Volatile
    var lastDeltaThreshold: Float = Float.NaN
        private set

    fun reset() {
        baselineY = null
        noiseEma = 0.002f
        largeOffsetIdleFrames = 0
        lastBaselineY = Float.NaN
        lastDeltaThreshold = Float.NaN
    }

    fun sanitizeBaselineIfNeeded(filtered: Float): Boolean {
        var didReset = false
        baselineY?.let { old ->
            if (old !in 0f..1f || abs(old - filtered) > 0.35f) {
                baselineY = filtered
                didReset = true
            }
        }
        return didReset
    }

    fun updateBaseline(filtered: Float, phaseIsInJump: Boolean): Float {
        val currentBaseline =
            baselineY?.let { old -> computeAdaptiveBaseline(old, filtered, phaseIsInJump) } ?: filtered
        baselineY = currentBaseline
        lastBaselineY = currentBaseline
        return currentBaseline
    }

    private fun computeAdaptiveBaseline(old: Float, filtered: Float, phaseIsInJump: Boolean): Float {
        if (phaseIsInJump) return old
        val offset = abs(filtered - old)
        largeOffsetIdleFrames = if (offset > 0.08f) largeOffsetIdleFrames + 1 else 0
        val adaptiveAlpha =
            when {
                largeOffsetIdleFrames >= 6 -> 0.35f
                offset > 0.08f -> 0.15f
                else -> baselineAlpha
            }
        return old + adaptiveAlpha * (filtered - old)
    }

    fun computeThresholds(filtered: Float, baseline: Float): JumpThresholds {
        val deviation = abs(filtered - baseline)
        noiseEma += noiseEmaAlpha * (deviation - noiseEma)
        val jumpUpDelta = (noiseEma * jumpSigma).coerceIn(minJumpUpDelta, maxJumpUpDelta)
        val recoverDelta = maxOf(minRecoverDelta, jumpUpDelta * recoverRatio)
        lastDeltaThreshold = jumpUpDelta
        return JumpThresholds(jumpUpDelta = jumpUpDelta, recoverDelta = recoverDelta)
    }
}
