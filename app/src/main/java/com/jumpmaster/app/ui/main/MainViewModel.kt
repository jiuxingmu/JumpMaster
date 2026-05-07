package com.jumpmaster.app.ui.main

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.jumpmaster.app.data.pose.PoseLandmarkerFactory
import com.jumpmaster.app.data.sensor.DeviceTiltProvider
import com.jumpmaster.app.domain.camera.CameraJumpDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val poseLandmarkerFactory: PoseLandmarkerFactory,
    private val tiltProvider: DeviceTiltProvider,
) : ViewModel() {

    companion object {
        private const val TAG = "JumpMasterPose"
        private const val MAX_TILT_FROM_VERTICAL_DEG = 20f
    }

    private val detector = CameraJumpDetector()

    private val _jumpCount = MutableStateFlow(0)
    val jumpCount: StateFlow<Int> = _jumpCount.asStateFlow()

    private val _hint = MutableStateFlow<String>("面向摄像头开始做跳跃 Demo")
    val hint: StateFlow<String> = _hint.asStateFlow()
    private val _poseOverlayPoints = MutableStateFlow<PoseOverlayPoints?>(null)
    val poseOverlayPoints: StateFlow<PoseOverlayPoints?> = _poseOverlayPoints.asStateFlow()
    private var frameCounter: Int = 0
    private var noPoseStreak: Int = 0
    private var lastFrameState: FrameState = FrameState.VALID

    private enum class FrameState {
        VALID,
        NO_POSE,
        INVALID_BODY,
        INVALID_TILT,
        OUT_OF_RANGE,
    }

    private sealed interface FrameEvaluation {
        data class Valid(val hipY: Float, val stampMs: Long) : FrameEvaluation

        data class Invalid(val state: FrameState, val payload: Float? = null) : FrameEvaluation
    }

    init {
        viewModelScope.launch {
            detector.jumpEvents.collect {
                _jumpCount.update { it + 1 }
            }
        }
    }

    /**
     * 在 CameraX 分析线程调用；同步完成图像拷贝 + Pose 推断，避免 backlog。
     * @param lensFacingFront 若为前置摄像头，需要水平镜像以对齐 Overlay 坐标语义。
     */
    fun processCameraFrame(imageProxy: ImageProxy, lensFacingFront: Boolean) {
        val landmarker = acquireLandmarkerOrNull(imageProxy) ?: return
        val stampMs = extractStampMs(imageProxy)
        when (val evaluation = evaluateFrame(landmarker, imageProxy, lensFacingFront, stampMs)) {
            is FrameEvaluation.Valid -> handleValidFrame(evaluation)
            is FrameEvaluation.Invalid -> handleInvalidFrame(evaluation)
        }
    }

    override fun onCleared() {
        super.onCleared()
        tiltProvider.stop()
        poseLandmarkerFactory.releaseLocked()
    }

    private fun shouldLogFrameState(
        currentState: FrameState,
        frame: Int,
    ): Boolean = currentState != lastFrameState || frame % 30 == 0

    private fun acquireLandmarkerOrNull(imageProxy: ImageProxy): com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker? =
        runCatching { poseLandmarkerFactory.acquire() }.getOrElse {
            imageProxy.close()
            val msg = it.message ?: it.javaClass.simpleName
            Log.e(TAG, "PoseLandmarker acquire failed: $msg", it)
            _hint.value = "Pose 模型初始化失败：$msg"
            null
        }

    private fun extractStampMs(imageProxy: ImageProxy): Long =
        TimeUnit.NANOSECONDS.toMillis(imageProxy.imageInfo.timestamp).coerceAtLeast(0L)

    private fun detectPose(
        landmarker: com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker,
        imageProxy: ImageProxy,
        lensFacingFront: Boolean,
        stampMs: Long,
    ): PoseLandmarkerResult? {
        val bitmapBuffer = copyFrameBitmap(imageProxy)
        val oriented = orientBitmap(bitmapBuffer, imageProxy, lensFacingFront)
        val mpImage = BitmapImageBuilder(oriented).build()
        val detection =
            runCatching { landmarker.detectForVideo(mpImage, stampMs) }
                .onFailure {
                    Log.e(TAG, "detectForVideo failed: ${it.message}", it)
                    _hint.value = "姿态推断异常：${it.message ?: it.javaClass.simpleName}"
                }
                .getOrNull()
        oriented.recycle()
        bitmapBuffer.recycle()
        return detection
    }

    private fun evaluateFrame(
        landmarker: com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker,
        imageProxy: ImageProxy,
        lensFacingFront: Boolean,
        stampMs: Long,
    ): FrameEvaluation {
        val detection = detectPose(landmarker, imageProxy, lensFacingFront, stampMs)
        _poseOverlayPoints.value = detection?.extractPoseOverlayPoints()
        val frameMetrics = detection?.extractPoseFrameMetrics()
        val hipY = frameMetrics?.hipY ?: return FrameEvaluation.Invalid(FrameState.NO_POSE)
        if (frameMetrics.isValidFrame.not()) return FrameEvaluation.Invalid(FrameState.INVALID_BODY)
        if (isTiltInvalid()) return FrameEvaluation.Invalid(FrameState.INVALID_TILT)
        if (hipY !in 0f..1.2f) return FrameEvaluation.Invalid(FrameState.OUT_OF_RANGE, payload = hipY)
        return FrameEvaluation.Valid(hipY = hipY, stampMs = stampMs)
    }

    private fun copyFrameBitmap(imageProxy: ImageProxy): Bitmap {
        val bitmap = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
        try {
            imageProxy.planes[0].buffer.rewind()
            bitmap.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
        } finally {
            imageProxy.close()
        }
        return bitmap
    }

    private fun orientBitmap(bitmap: Bitmap, imageProxy: ImageProxy, lensFacingFront: Boolean): Bitmap {
        val matrix =
            Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                if (lensFacingFront) {
                    postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
                }
            }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun handleValidFrame(valid: FrameEvaluation.Valid) {
        noPoseStreak = 0
        lastFrameState = FrameState.VALID

        val nowElapsed = SystemClock.elapsedRealtime()
        val counted = detector.onRawHipY(valid.hipY, nowElapsed)
        frameCounter += 1

        logDetectorState(valid.hipY, valid.stampMs, nowElapsed, counted)
        updateHintThrottled()
    }

    private fun handleInvalidFrame(invalid: FrameEvaluation.Invalid) {
        when (invalid.state) {
            FrameState.NO_POSE -> handleNoPoseFrame()
            FrameState.INVALID_BODY -> handleInvalidBodyFrame()
            FrameState.INVALID_TILT -> handleInvalidTiltFrame()
            FrameState.OUT_OF_RANGE -> handleOutOfRangeFrame(invalid.payload ?: Float.NaN)
            FrameState.VALID -> Unit
        }
    }

    private fun handleNoPoseFrame() {
        frameCounter += 1
        noPoseStreak += 1
        if (noPoseStreak >= 25) {
            detector.reset()
            noPoseStreak = 0
            Log.w(TAG, "frame=$frameCounter no_pose_streak_reset")
        }
        emitFrameState(FrameState.NO_POSE, "frame=$frameCounter no_pose hipAvg=null")
        _hint.value = "未检测到人体姿态，尝试调整取景范围"
    }

    private fun handleInvalidBodyFrame() {
        frameCounter += 1
        noPoseStreak = 0
        emitFrameState(FrameState.INVALID_BODY, "frame=$frameCounter invalid_body_landmarks")
        _hint.value = "请确保头部、腹部和髋关节都在画面内"
    }

    private fun handleInvalidTiltFrame() {
        frameCounter += 1
        noPoseStreak = 0
        val tilt = tiltProvider.tiltFromVerticalDeg.value
        emitFrameState(
            FrameState.INVALID_TILT,
            "frame=$frameCounter invalid_tilt tilt=%.1f max=%.1f".format(tilt, MAX_TILT_FROM_VERTICAL_DEG),
        )
        _hint.value = "请竖直握持手机（当前倾角 %.1f°, 需 < %.1f°）".format(tilt, MAX_TILT_FROM_VERTICAL_DEG)
    }

    private fun handleOutOfRangeFrame(hipY: Float) {
        frameCounter += 1
        noPoseStreak = 0
        emitFrameState(FrameState.OUT_OF_RANGE, "frame=$frameCounter hip_out_of_range raw=$hipY")
        _hint.value = "姿态值异常（hipY=$hipY），忽略该帧"
    }

    private fun isTiltInvalid(): Boolean {
        val tilt = tiltProvider.tiltFromVerticalDeg.value
        return tilt.isNaN() || tilt > MAX_TILT_FROM_VERTICAL_DEG
    }

    private fun emitFrameState(newState: FrameState, logMessage: String) {
        if (shouldLogFrameState(newState, frameCounter)) Log.w(TAG, logMessage)
        lastFrameState = newState
    }

    private fun logDetectorState(hipY: Float, stampMs: Long, nowElapsed: Long, counted: Boolean) {
        val hipF = detector.lastFilteredY
        val base = detector.lastBaselineY
        val delta = detector.lastDeltaThreshold
        val diff = if (!hipF.isNaN() && !base.isNaN()) base - hipF else Float.NaN
        Log.d(
            TAG,
            "frame=$frameCounter tsMs=$stampMs hipRaw=%.4f hip=%.4f base=%.4f diff=%.4f Δ=%.4f counted=$counted count=${_jumpCount.value} rtMs=$nowElapsed"
                .format(hipY, hipF, base, diff, delta),
        )
    }

    private fun updateHintThrottled() {
        if (frameCounter % 8 != 0) return
        val hipF = detector.lastFilteredY
        val base = detector.lastBaselineY
        val delta = detector.lastDeltaThreshold
        val diff = if (!hipF.isNaN() && !base.isNaN()) base - hipF else Float.NaN
        _hint.value = "hip=%.3f base=%.3f diff=%.3f Δ=%.3f".format(hipF, base, diff, delta)
    }
}
