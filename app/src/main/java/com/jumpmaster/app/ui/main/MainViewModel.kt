package com.jumpmaster.app.ui.main

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.jumpmaster.app.data.pose.PoseLandmarkerFactory
import com.jumpmaster.app.domain.camera.CameraJumpDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MIN_TORSO_HEIGHT_NORM = 0.12f
private const val MIN_HIP_WIDTH_NORM = 0.06f

@HiltViewModel
class MainViewModel @Inject constructor(
    private val poseLandmarkerFactory: PoseLandmarkerFactory,
    @ApplicationContext context: Context,
) : ViewModel() {

    companion object {
        private const val TAG = "JumpMasterPose"
        private const val MAX_TILT_FROM_VERTICAL_DEG = 20f
    }

    private val detector = CameraJumpDetector()
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var tiltFromVerticalDeg: Float = Float.NaN
    private val gravityListener =
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values.getOrNull(0) ?: return
                val y = event.values.getOrNull(1) ?: return
                val z = event.values.getOrNull(2) ?: return
                val norm = kotlin.math.sqrt(x * x + y * y + z * z)
                if (norm < 1e-3f) return
                val cosTheta = (kotlin.math.abs(y) / norm).coerceIn(0f, 1f)
                tiltFromVerticalDeg = kotlin.math.acos(cosTheta) * (180f / Math.PI.toFloat())
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

    private val _jumpCount = MutableStateFlow(0)
    val jumpCount: StateFlow<Int> = _jumpCount.asStateFlow()

    private val _hint = MutableStateFlow<String>("面向摄像头开始做跳跃 Demo")
    val hint: StateFlow<String> = _hint.asStateFlow()
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

    init {
        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val fallbackAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(
            gravityListener,
            gravitySensor ?: fallbackAccel,
            SensorManager.SENSOR_DELAY_GAME,
        )

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
        val landmarker =
            runCatching { poseLandmarkerFactory.acquire() }.getOrElse {
                imageProxy.close()
                val msg = it.message ?: it.javaClass.simpleName
                Log.e(TAG, "PoseLandmarker acquire failed: $msg", it)
                _hint.value = "Pose 模型初始化失败：$msg"
                return
            }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val frameWidth = imageProxy.width
        val frameHeight = imageProxy.height
        val stampMs =
            TimeUnit.NANOSECONDS.toMillis(imageProxy.imageInfo.timestamp).coerceAtLeast(0L)

        val bitmapBuffer =
            Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888)

        try {
            imageProxy.planes[0].buffer.rewind()
            bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
        } finally {
            imageProxy.close()
        }

        val matrix =
            Matrix().apply {
                postRotate(rotationDegrees.toFloat())
                if (lensFacingFront) {
                    postScale(
                        -1f,
                        1f,
                        frameWidth.toFloat(),
                        frameHeight.toFloat(),
                    )
                }
            }

        val oriented =
            Bitmap.createBitmap(
                bitmapBuffer,
                0,
                0,
                bitmapBuffer.width,
                bitmapBuffer.height,
                matrix,
                true,
            )

        val mpImage = BitmapImageBuilder(oriented).build()
        val detection: PoseLandmarkerResult? =
            runCatching { landmarker.detectForVideo(mpImage, stampMs) }
                .onFailure {
                    Log.e(TAG, "detectForVideo failed: ${it.message}", it)
                    _hint.value = "姿态推断异常：${it.message ?: it.javaClass.simpleName}"
                }
                .getOrNull()

        oriented.recycle()
        bitmapBuffer.recycle()

        val frameMetrics = detection?.extractPoseFrameMetrics()
        val hipY = frameMetrics?.hipY
        if (hipY == null) {
            frameCounter += 1
            noPoseStreak += 1
            val currentState = FrameState.NO_POSE
            if (noPoseStreak >= 25) {
                detector.reset()
                noPoseStreak = 0
                Log.w(TAG, "frame=$frameCounter no_pose_streak_reset")
            }
            if (shouldLogFrameState(currentState, frameCounter)) {
                Log.w(TAG, "frame=$frameCounter no_pose hipAvg=null")
            }
            lastFrameState = currentState
            _hint.value = "未检测到人体姿态，尝试调整取景范围"
            return
        }

        if (!frameMetrics.isValidFrame) {
            frameCounter += 1
            noPoseStreak = 0
            val currentState = FrameState.INVALID_BODY
            if (shouldLogFrameState(currentState, frameCounter)) {
                Log.w(TAG, "frame=$frameCounter invalid_body_landmarks")
            }
            lastFrameState = currentState
            _hint.value = "请确保头部、腹部和髋关节都在画面内"
            return
        }

        if (tiltFromVerticalDeg.isNaN() || tiltFromVerticalDeg > MAX_TILT_FROM_VERTICAL_DEG) {
            frameCounter += 1
            noPoseStreak = 0
            val currentState = FrameState.INVALID_TILT
            if (shouldLogFrameState(currentState, frameCounter)) {
                Log.w(
                    TAG,
                    "frame=$frameCounter invalid_tilt tilt=%.1f max=%.1f".format(
                        tiltFromVerticalDeg,
                        MAX_TILT_FROM_VERTICAL_DEG,
                    ),
                )
            }
            lastFrameState = currentState
            _hint.value =
                "请竖直握持手机（当前倾角 %.1f°, 需 < %.1f°）".format(
                    tiltFromVerticalDeg,
                    MAX_TILT_FROM_VERTICAL_DEG,
                )
            return
        }

        if (hipY !in 0f..1.2f) {
            frameCounter += 1
            noPoseStreak = 0
            val currentState = FrameState.OUT_OF_RANGE
            if (shouldLogFrameState(currentState, frameCounter)) {
                Log.w(TAG, "frame=$frameCounter hip_out_of_range raw=$hipY")
            }
            lastFrameState = currentState
            _hint.value = "姿态值异常（hipY=$hipY），忽略该帧"
            return
        }
        noPoseStreak = 0
        lastFrameState = FrameState.VALID

        val nowElapsed = SystemClock.elapsedRealtime()
        val counted = detector.onRawHipY(hipY, nowElapsed)
        frameCounter += 1

        val hipF = detector.lastFilteredY
        val base = detector.lastBaselineY
        val delta = detector.lastDeltaThreshold
        val diff =
            if (!hipF.isNaN() && !base.isNaN()) {
                base - hipF
            } else {
                Float.NaN
            }

        Log.d(
            TAG,
            "frame=$frameCounter tsMs=$stampMs hipRaw=%.4f hip=%.4f base=%.4f diff=%.4f Δ=%.4f counted=$counted count=${_jumpCount.value} rtMs=$nowElapsed"
                .format(hipY, hipF, base, diff, delta),
        )

        // 降频更新提示，避免每帧刷新 UI 文本。
        if (frameCounter % 8 == 0) {
            _hint.value =
                "hip=%.3f base=%.3f diff=%.3f Δ=%.3f".format(hipF, base, diff, delta)
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(gravityListener)
        poseLandmarkerFactory.releaseLocked()
    }

    private fun shouldLogFrameState(
        currentState: FrameState,
        frame: Int,
    ): Boolean = currentState != lastFrameState || frame % 30 == 0
}

private data class PoseFrameMetrics(
    val hipY: Float,
    val isValidFrame: Boolean,
)

private fun PoseLandmarkerResult.extractPoseFrameMetrics(): PoseFrameMetrics? {
    val pose =
        landmarks().firstOrNull() ?: return null
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
    val torsoHeight = kotlin.math.abs(hipY - shoulderY)
    val hipWidth = kotlin.math.abs(lh.x() - rh.x())
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

    return PoseFrameMetrics(
        hipY = hipY,
        isValidFrame = isValidFrame,
    )
}
