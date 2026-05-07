package com.jumpmaster.app.domain.camera

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.abs
import kotlin.math.min

private enum class JumpPhase {
    IDLE,
    IN_JUMP,
}

/**
 * Hip Y（归一化） + 滑动平均防抖 + 滞后阈值的小型状态机（PRD 方案 A）。
 * 约定：画面中 **较小的 y 表示髋关节更靠近画面上方**，一次完整跳跃会先压缩 y，再回落至站立高度。
 */
class CameraJumpDetector(
    private val smoothingWindow: Int = 4,
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

    private val buffer = FloatArray(smoothingWindow) { Float.NaN }
    private var bufferCount = 0
    private var bufferIndex = 0

    private var phase = JumpPhase.IDLE
    private var lastCountRealtimeMs = 0L
    private var jumpPhaseStartRealtimeMs = Long.MIN_VALUE
    private var baselineY: Float? = null
    private var jumpDirectionSign: Int = 0
    private var jumpMaxAbsDeviation: Float = 0f
    private var noiseEma: Float = 0.002f
    private var largeOffsetIdleFrames: Int = 0

    @Volatile
    var lastFilteredY: Float = Float.NaN
        private set

    @Volatile
    var lastBaselineY: Float = Float.NaN
        private set

    @Volatile
    var lastDeltaThreshold: Float = Float.NaN
        private set

    private val _jumpPulse =
        MutableSharedFlow<Unit>(
            replay = 0,
            extraBufferCapacity = 16,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val jumpEvents: SharedFlow<Unit> = _jumpPulse.asSharedFlow()

    fun reset() {
        buffer.fill(Float.NaN)
        bufferCount = 0
        bufferIndex = 0
        phase = JumpPhase.IDLE
        jumpPhaseStartRealtimeMs = Long.MIN_VALUE
        jumpDirectionSign = 0
        jumpMaxAbsDeviation = 0f
        largeOffsetIdleFrames = 0
        baselineY = null
        noiseEma = 0.002f
        lastFilteredY = Float.NaN
        lastBaselineY = Float.NaN
        lastDeltaThreshold = Float.NaN
    }

    private fun pushSample(sample: Float): Float {
        buffer[bufferIndex] = sample
        bufferIndex = (bufferIndex + 1) % smoothingWindow
        bufferCount = min(bufferCount + 1, smoothingWindow)
        var sum = 0f
        repeat(bufferCount) { i ->
            sum += buffer[(bufferIndex + smoothingWindow - 1 - i) % smoothingWindow]
        }
        return sum / bufferCount
    }

    private data class Thresholds(
        val jumpUpDelta: Float,
        val recoverDelta: Float,
    )

    // ---------- 输入与预处理 ----------
    private fun normalizeAndFilter(rawNormalizedY: Float): Float? {
        if (rawNormalizedY.isNaN() || rawNormalizedY.isInfinite()) return null
        val filtered = pushSample(rawNormalizedY.coerceIn(0f, 1f))
        if (filtered.isNaN()) return null
        lastFilteredY = filtered
        return filtered
    }

    // ---------- 基线与阈值 ----------
    // 基线异常时快速重置，避免状态机卡死在错误参考线上。
    private fun sanitizeBaselineIfNeeded(filtered: Float) {
        baselineY?.let { old ->
            if (old !in 0f..1f || abs(old - filtered) > 0.35f) {
                baselineY = filtered
                phase = JumpPhase.IDLE
                jumpPhaseStartRealtimeMs = Long.MIN_VALUE
            }
        }
    }

    private fun updateBaseline(filtered: Float): Float {
        val currentBaseline = baselineY?.let { old -> computeAdaptiveBaseline(old, filtered) } ?: filtered
        baselineY = currentBaseline
        lastBaselineY = currentBaseline
        return currentBaseline
    }

    // IDLE 时自适应更新基线；IN_JUMP 冻结基线，避免动作本身把参考线拉偏。
    private fun computeAdaptiveBaseline(old: Float, filtered: Float): Float {
        if (phase == JumpPhase.IN_JUMP) return old
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

    private fun computeThresholds(filtered: Float, baseline: Float): Thresholds {
        val deviation = abs(filtered - baseline)
        noiseEma += noiseEmaAlpha * (deviation - noiseEma)
        val jumpUpDelta = (noiseEma * jumpSigma).coerceIn(minJumpUpDelta, maxJumpUpDelta)
        val recoverDelta = maxOf(minRecoverDelta, jumpUpDelta * recoverRatio)
        lastDeltaThreshold = jumpUpDelta
        return Thresholds(jumpUpDelta = jumpUpDelta, recoverDelta = recoverDelta)
    }

    // ---------- 状态机 ----------
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

    // 跳跃相位超时保护：长时间未闭环则重置，防止一直锁在 IN_JUMP。
    private fun handleTimeoutIfNeeded(
        filtered: Float,
        baseline: Float,
        elapsedRealtimeMs: Long,
    ): Boolean {
        val isTimedOut =
            jumpPhaseStartRealtimeMs != Long.MIN_VALUE &&
                elapsedRealtimeMs - jumpPhaseStartRealtimeMs > maxJumpPhaseMs
        if (!isTimedOut) return false
        phase = JumpPhase.IDLE
        jumpPhaseStartRealtimeMs = Long.MIN_VALUE
        if (abs(filtered - baseline) > 0.08f) baselineY = filtered
        jumpDirectionSign = 0
        jumpMaxAbsDeviation = 0f
        return true
    }

    private fun handleInJumpPhase(
        filtered: Float,
        baseline: Float,
        thresholds: Thresholds,
        elapsedRealtimeMs: Long,
    ): Boolean {
        if (handleTimeoutIfNeeded(filtered, baseline, elapsedRealtimeMs)) return false
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

    /**
     * @param elapsedRealtimeMs [`android.os.SystemClock.elapsedRealtime`]
     * @return 若本帧计数 +1（即状态机闭环完成）则为 `true`，否则 `false`。
     */
    fun onRawHipY(rawNormalizedY: Float, elapsedRealtimeMs: Long): Boolean {
        val filtered = normalizeAndFilter(rawNormalizedY) ?: return false
        sanitizeBaselineIfNeeded(filtered)
        val baseline = updateBaseline(filtered)
        val thresholds = computeThresholds(filtered, baseline)
        return when (phase) {
            JumpPhase.IDLE -> handleIdlePhase(filtered, baseline, thresholds.jumpUpDelta, elapsedRealtimeMs)
            JumpPhase.IN_JUMP -> handleInJumpPhase(filtered, baseline, thresholds, elapsedRealtimeMs)
        }
    }
}
