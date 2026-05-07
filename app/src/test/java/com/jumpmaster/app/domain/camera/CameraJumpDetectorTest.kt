package com.jumpmaster.app.domain.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraJumpDetectorTest {

    @Test
    fun smallOscillation_likeHandWaving_doesNotCount() {
        val detector =
            CameraJumpDetector(
                smoothingWindow = 3,
            )

        var now = 0L
        repeat(12) {
            detector.onRawHipY(0.60f, now)
            now += 20L
        }

        var counted = 0
        val jitter =
            listOf(
                0.595f, 0.603f, 0.598f, 0.606f, 0.597f, 0.604f,
                0.596f, 0.602f, 0.599f, 0.605f, 0.598f, 0.603f,
            )
        jitter.forEach { y ->
            if (detector.onRawHipY(y, now)) counted += 1
            now += 20L
        }

        assertEquals(0, counted)
    }

    @Test
    fun repeatedSyntheticJumps_areCountedConsistently() {
        val detector =
            CameraJumpDetector(
                smoothingWindow = 3,
                minSpacingMs = 250L,
            )

        var now = 0L
        repeat(12) {
            detector.onRawHipY(0.60f, now)
            now += 20L
        }

        var counted = 0
        repeat(6) {
            val cycle =
                listOf(
                    0.58f, 0.54f, 0.50f, 0.46f, 0.44f, // 下蹲/离地
                    0.48f, 0.53f, 0.57f, 0.60f, 0.62f, // 回升
                    0.60f, 0.59f, 0.60f, // 回到基线附近
                )
            cycle.forEach { y ->
                if (detector.onRawHipY(y, now)) counted += 1
                now += 28L
            }
        }

        assertTrue("should count most synthetic jumps", counted >= 5)
    }

    @Test
    fun jumpReturningNearBaselineWithoutCrossing_stillCounts() {
        val detector =
            CameraJumpDetector(
                smoothingWindow = 3,
                minSpacingMs = 120L,
            )

        var now = 0L
        repeat(12) {
            detector.onRawHipY(0.60f, now)
            now += 20L
        }

        var counted = 0
        val oneJumpWithoutOvershoot =
            listOf(
                0.59f, 0.56f, 0.52f, 0.49f, 0.47f, // 起跳，y 明显下降
                0.50f, 0.54f, 0.57f, 0.59f, 0.595f, // 回升到基线附近，但不上穿基线
                0.592f, 0.590f, 0.591f,
            )
        oneJumpWithoutOvershoot.forEach { y ->
            if (detector.onRawHipY(y, now)) counted += 1
            now += 28L
        }

        assertEquals(1, counted)
    }
}
