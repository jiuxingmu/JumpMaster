package com.jumpmaster.app.domain.camera

import kotlinx.coroutines.flow.SharedFlow

/**
 * Hip Y（归一化） + 滑动平均防抖 + 滞后阈值的小型状态机（PRD 方案 A）。
 * 约定：画面中 **较小的 y 表示髋关节更靠近画面上方**，一次完整跳跃会先压缩 y，再回落至站立高度。
 */
class CameraJumpDetector(
    smoothingWindow: Int = 4,
    private val minJumpUpDelta: Float = 0.006f,
    private val maxJumpUpDelta: Float = 0.012f,
    private val noiseEmaAlpha: Float = 0.12f,
    private val jumpSigma: Float = 2.2f,
    private val recoverRatio: Float = 0.60f,
    private val minRecoverDelta: Float = 0.008f,
    private val baselineAlpha: Float = 0.03f,
    private val minJumpAmplitude: Float = 0.022f,
    private val minSpacingMs: Long = 240L,
    private val maxJumpPhaseMs: Long = 1400L,
) {

    init {
        require(smoothingWindow > 1) { "window must be > 1" }
        require(minJumpUpDelta > 0f) { "minJumpUpDelta must be > 0" }
        require(maxJumpUpDelta > minJumpUpDelta) { "maxJumpUpDelta must be > minJumpUpDelta" }
        require(jumpSigma > 0f) { "jumpSigma must be > 0" }
        require(recoverRatio in 0.1f..0.9f) { "recoverRatio should be in [0.1, 0.9]" }
        require(minRecoverDelta > 0f) { "minRecoverDelta must be > 0" }
    }

    private val baselineTracker =
        JumpBaselineTracker(
            baselineAlpha = baselineAlpha,
            noiseEmaAlpha = noiseEmaAlpha,
            jumpSigma = jumpSigma,
            minJumpUpDelta = minJumpUpDelta,
            maxJumpUpDelta = maxJumpUpDelta,
            minRecoverDelta = minRecoverDelta,
            recoverRatio = recoverRatio,
        )

    private val phaseMachine =
        JumpPhaseStateMachine(
            minJumpAmplitude = minJumpAmplitude,
            minSpacingMs = minSpacingMs,
            maxJumpPhaseMs = maxJumpPhaseMs,
        )

    private val filter =
        JumpSignalFilter(smoothingWindow = smoothingWindow) { filtered ->
            lastFilteredY = filtered
        }

    @Volatile
    var lastFilteredY: Float = Float.NaN
        private set

    val lastBaselineY: Float get() = baselineTracker.lastBaselineY

    val lastDeltaThreshold: Float get() = baselineTracker.lastDeltaThreshold

    val jumpEvents: SharedFlow<Unit> = phaseMachine.jumpEvents

    fun reset() {
        filter.reset()
        baselineTracker.reset()
        phaseMachine.reset()
        lastFilteredY = Float.NaN
    }

    /**
     * @param elapsedRealtimeMs 单调时钟毫秒（与 `SystemClock.elapsedRealtime` 语义一致）
     * @return 若本帧计数 +1（即状态机闭环完成）则为 `true`，否则 `false`。
     */
    fun onRawHipY(rawNormalizedY: Float, elapsedRealtimeMs: Long): Boolean {
        val filtered = filter.normalizeAndFilter(rawNormalizedY) ?: return false
        if (baselineTracker.sanitizeBaselineIfNeeded(filtered)) {
            phaseMachine.onSanitizedBaselineReset()
        }
        val baseline = baselineTracker.updateBaseline(filtered, phaseIsInJump = phaseMachine.isInJump())
        val thresholds = baselineTracker.computeThresholds(filtered, baseline)
        return phaseMachine.onRawFilteredFrame(filtered, baseline, thresholds, elapsedRealtimeMs, baselineTracker)
    }
}
