package com.jumpmaster.app.ui.main

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.jumpmaster.app.data.pose.PoseLandmarkerFactory
import kotlin.math.abs

private const val MIN_TORSO_HEIGHT_NORM = 0.12f
private const val MIN_HIP_WIDTH_NORM = 0.06f

data class PoseFrameMetrics(
    val hipY: Float,
    val isValidFrame: Boolean,
)

data class NormalizedPoint(
    val x: Float,
    val y: Float,
)

data class PoseOverlayPoints(
    val landmarks: List<NormalizedPoint>,
    val connections: List<Pair<Int, Int>>,
)

private val FULL_BODY_CONNECTIONS: List<Pair<Int, Int>> =
    listOf(
        0 to 1,
        0 to 4,
        1 to 2,
        2 to 3,
        4 to 5,
        5 to 6,
        9 to 10,
        11 to 12,
        11 to 13,
        13 to 15,
        15 to 17,
        15 to 19,
        15 to 21,
        17 to 19,
        12 to 14,
        14 to 16,
        16 to 18,
        16 to 20,
        16 to 22,
        18 to 20,
        11 to 23,
        12 to 24,
        23 to 24,
        23 to 25,
        24 to 26,
        25 to 27,
        27 to 29,
        29 to 31,
        26 to 28,
        28 to 30,
        30 to 32,
        27 to 31,
        28 to 32,
    )

fun PoseLandmarkerResult.extractPoseFrameMetrics(): PoseFrameMetrics? {
    val pose = landmarks().firstOrNull() ?: return null
    if (pose.size <= PoseLandmarkerFactory.RIGHT_HIP) return null
    if (pose.size <= PoseLandmarkerFactory.RIGHT_SHOULDER) return null
    if (pose.size <= PoseLandmarkerFactory.NOSE) return null

    fun inFrame(value: Float): Boolean = value in 0f..1f

    val nose = pose[PoseLandmarkerFactory.NOSE]
    val ls = pose[PoseLandmarkerFactory.LEFT_SHOULDER]
    val rs = pose[PoseLandmarkerFactory.RIGHT_SHOULDER]
    val lh = pose[PoseLandmarkerFactory.LEFT_HIP]
    val rh = pose[PoseLandmarkerFactory.RIGHT_HIP]

    val shoulderY = (ls.y() + rs.y()) / 2f
    val hipY = (lh.y() + rh.y()) / 2f
    val torsoHeight = abs(hipY - shoulderY)
    val hipWidth = abs(lh.x() - rh.x())
    val bellyX = (ls.x() + rs.x() + lh.x() + rh.x()) / 4f
    val bellyY = (ls.y() + rs.y() + lh.y() + rh.y()) / 4f

    val isValidFrame =
        inFrame(nose.x()) &&
            inFrame(nose.y()) &&
            inFrame(bellyX) &&
            inFrame(bellyY) &&
            inFrame(lh.x()) &&
            inFrame(lh.y()) &&
            inFrame(rh.x()) &&
            inFrame(rh.y()) &&
            torsoHeight >= MIN_TORSO_HEIGHT_NORM &&
            hipWidth >= MIN_HIP_WIDTH_NORM

    return PoseFrameMetrics(hipY = hipY, isValidFrame = isValidFrame)
}

fun PoseLandmarkerResult.extractPoseOverlayPoints(): PoseOverlayPoints? {
    val pose = landmarks().firstOrNull() ?: return null
    if (pose.size < 33) return null
    val points = pose.map { NormalizedPoint(x = it.x(), y = it.y()) }

    return PoseOverlayPoints(
        landmarks = points,
        connections = FULL_BODY_CONNECTIONS,
    )
}
