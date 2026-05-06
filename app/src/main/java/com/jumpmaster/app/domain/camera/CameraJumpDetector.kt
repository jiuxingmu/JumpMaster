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
    private val smoothingWindow: Int = 5,
    private val minJumpUpDelta: Float = 0.006f,
    private val maxJumpUpDelta: Float = 0.014f,
    private val noiseEmaAlpha: Float = 0.12f,
    private val jumpSigma: Float = 2.2f,
    private val recoverRatio: Float = 0.60f,
    private val minRecoverDelta: Float = 0.008f,
    private val baselineAlpha: Float = 0.08f,
    private val minSpacingMs: Long = 380L,
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
    private var noiseEma: Float = 0.002f

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

    /**
     * @param elapsedRealtimeMs [`android.os.SystemClock.elapsedRealtime`]
     * @return 若本帧计数 +1（即状态机闭环完成）则为 `true`，否则 `false`。
     */
    fun onRawHipY(rawNormalizedY: Float, elapsedRealtimeMs: Long): Boolean {
        if (rawNormalizedY.isNaN() || rawNormalizedY.isInfinite()) return false
        val boundedRaw = rawNormalizedY.coerceIn(0f, 1f)

        val filtered = pushSample(boundedRaw)
        if (filtered.isNaN()) return false
        lastFilteredY = filtered

        // 若历史基线异常偏离（例如误检造成 > 1 或突然飘移），立即重置，避免状态机卡死。
        baselineY?.let { old ->
            if (old !in 0f..1f || abs(old - filtered) > 0.35f) {
                baselineY = filtered
                phase = JumpPhase.IDLE
                jumpPhaseStartRealtimeMs = Long.MIN_VALUE
            }
        }

        val currentBaseline =
            baselineY?.let { old ->
                // 跳跃相位冻结基线，避免动作本身把基线拉偏。
                if (phase == JumpPhase.IN_JUMP) old else old + baselineAlpha * (filtered - old)
            } ?: filtered
        baselineY = currentBaseline
        lastBaselineY = currentBaseline

        val deviation = abs(filtered - currentBaseline)
        noiseEma += noiseEmaAlpha * (deviation - noiseEma)
        val jumpUpDelta =
            (noiseEma * jumpSigma).coerceIn(
                minJumpUpDelta,
                maxJumpUpDelta,
            )
        val recoverDelta = maxOf(minRecoverDelta, jumpUpDelta * recoverRatio)
        lastDeltaThreshold = jumpUpDelta

        return when (phase) {
            JumpPhase.IDLE -> {
                val deviation = filtered - currentBaseline
                if (abs(deviation) >= jumpUpDelta) {
                    phase = JumpPhase.IN_JUMP
                    jumpPhaseStartRealtimeMs = elapsedRealtimeMs
                    jumpDirectionSign = if (deviation >= 0f) 1 else -1
                }
                false
            }

            JumpPhase.IN_JUMP -> {
                // 进入跳跃后长时间未完成回落，判定为异常检测，回到 IDLE 防止锁死。
                if (
                    jumpPhaseStartRealtimeMs != Long.MIN_VALUE &&
                    elapsedRealtimeMs - jumpPhaseStartRealtimeMs > maxJumpPhaseMs
                ) {
                    phase = JumpPhase.IDLE
                    jumpPhaseStartRealtimeMs = Long.MIN_VALUE
                    baselineY = filtered
                    jumpDirectionSign = 0
                    return false
                }

                val deviation = filtered - currentBaseline
                val isDirectionReversed =
                    if (jumpDirectionSign == 0) {
                        false
                    } else {
                        deviation * jumpDirectionSign.toFloat() <= 0f
                    }
                val hasReturnedNearBaseline = abs(deviation) <= recoverDelta

                if (isDirectionReversed &&
                    hasReturnedNearBaseline &&
                    (lastCountRealtimeMs == 0L || elapsedRealtimeMs - lastCountRealtimeMs >= minSpacingMs)
                ) {
                    phase = JumpPhase.IDLE
                    jumpPhaseStartRealtimeMs = Long.MIN_VALUE
                    lastCountRealtimeMs = elapsedRealtimeMs
                    jumpDirectionSign = 0
                    _jumpPulse.tryEmit(Unit)
                    true
                } else {
                    false
                }
            }
        }
    }
}
