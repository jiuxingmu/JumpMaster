package com.jumpmaster.app.ui.main

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

@HiltViewModel
class MainViewModel @Inject constructor(
    private val poseLandmarkerFactory: PoseLandmarkerFactory,
) : ViewModel() {

    private val detector = CameraJumpDetector()

    private val _jumpCount = MutableStateFlow(0)
    val jumpCount: StateFlow<Int> = _jumpCount.asStateFlow()

    private val _hint = MutableStateFlow<String>("面向摄像头开始做跳跃 Demo")
    val hint: StateFlow<String> = _hint.asStateFlow()
    private var frameCounter: Int = 0

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
        val landmarker =
            runCatching { poseLandmarkerFactory.acquire() }.getOrElse {
                imageProxy.close()
                _hint.value = "Pose 模型初始化失败：${it.message ?: it.javaClass.simpleName}"
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
                    _hint.value = "姿态推断异常：${it.message ?: it.javaClass.simpleName}"
                }
                .getOrNull()

        oriented.recycle()
        bitmapBuffer.recycle()

        val hipY = detection?.hipAverageY()
        if (hipY == null) {
            _hint.value = "未检测到人体姿态，尝试调整取景范围"
            return
        }

        detector.onRawHipY(hipY, SystemClock.elapsedRealtime())
        frameCounter += 1
        // 降频更新提示，避免每帧刷新 UI 文本。
        if (frameCounter % 8 == 0) {
            val diff = detector.lastBaselineY - detector.lastFilteredY
            _hint.value =
                "hip=%.3f base=%.3f diff=%.3f Δ=%.3f".format(
                    detector.lastFilteredY,
                    detector.lastBaselineY,
                    diff,
                    detector.lastDeltaThreshold,
                )
        }
    }

    override fun onCleared() {
        super.onCleared()
        poseLandmarkerFactory.releaseLocked()
    }
}

private fun PoseLandmarkerResult.hipAverageY(): Float? {
    val pose =
        landmarks().firstOrNull() ?: return null
    if (pose.size <= PoseLandmarkerFactory.RIGHT_HIP) return null
    val lh = pose[PoseLandmarkerFactory.LEFT_HIP]
    val rh = pose[PoseLandmarkerFactory.RIGHT_HIP]
    return (lh.y() + rh.y()) / 2f
}
