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
