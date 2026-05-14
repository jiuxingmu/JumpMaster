package com.jumpmaster.app.domain.camera

import kotlin.math.min

internal class JumpSignalFilter(
    private val smoothingWindow: Int,
    private val onFiltered: (Float) -> Unit,
) {

    private val buffer = FloatArray(smoothingWindow) { Float.NaN }
    private var bufferCount = 0
    private var bufferIndex = 0

    init {
        require(smoothingWindow > 1) { "window must be > 1" }
    }

    fun reset() {
        buffer.fill(Float.NaN)
        bufferCount = 0
        bufferIndex = 0
    }

    fun normalizeAndFilter(rawNormalizedY: Float): Float? {
        if (rawNormalizedY.isNaN() || rawNormalizedY.isInfinite()) return null
        val filtered = pushSample(rawNormalizedY.coerceIn(0f, 1f))
        if (filtered.isNaN()) return null
        onFiltered(filtered)
        return filtered
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
}
