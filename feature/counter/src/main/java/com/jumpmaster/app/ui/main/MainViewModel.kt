package com.jumpmaster.app.ui.main

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumpmaster.app.data.local.db.JumpRepository
import com.jumpmaster.app.data.pose.PoseLandmarkerFactory
import com.jumpmaster.app.data.sensor.DeviceTiltProvider
import com.jumpmaster.app.domain.camera.CameraJumpDetector
import android.os.SystemClock
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val jumpRepository: JumpRepository,
) : ViewModel() {

    internal companion object {
        const val TAG = "JumpMasterPose"
        const val MAX_TILT_FROM_VERTICAL_DEG = 20f
        private const val CALORIES_PER_JUMP = 0.18f
    }

    private val detector = CameraJumpDetector()

    private val _jumpCount = MutableStateFlow(0)
    val jumpCount: StateFlow<Int> = _jumpCount.asStateFlow()

    private val _hint = MutableStateFlow("面向摄像头开始做跳跃 Demo")
    val hint: StateFlow<String> = _hint.asStateFlow()

    private val _poseOverlayPoints = MutableStateFlow<PoseOverlayPoints?>(null)
    val poseOverlayPoints: StateFlow<PoseOverlayPoints?> = _poseOverlayPoints.asStateFlow()

    private var sessionStartTime: Long = 0L

    private val frameProcessor =
        MainCameraFrameProcessor(
            poseLandmarkerFactory = poseLandmarkerFactory,
            tiltProvider = tiltProvider,
            detector = detector,
            hint = _hint,
            poseOverlayPoints = _poseOverlayPoints,
            jumpCount = { _jumpCount.value },
        )

    init {
        viewModelScope.launch {
            detector.jumpEvents.collect {
                _jumpCount.update { it + 1 }
            }
        }
    }

    fun startSession() {
        _jumpCount.value = 0
        sessionStartTime = SystemClock.elapsedRealtime()
        detector.reset()
        _hint.value = "开始跳绳！"
    }

    fun saveSession() {
        val count = _jumpCount.value
        if (count == 0) {
            _hint.value = "没有跳绳记录可保存"
            return
        }
        val durationMs = SystemClock.elapsedRealtime() - sessionStartTime
        val calories = (count * CALORIES_PER_JUMP).toInt()
        viewModelScope.launch {
            jumpRepository.saveRecord(count, calories, durationMs)
            _hint.value = "已保存：$count 个，消耗 $calories 千卡"
        }
    }

    fun resetCount() {
        _jumpCount.value = 0
        detector.reset()
        _hint.value = "计数已重置"
    }

    fun processCameraFrame(imageProxy: ImageProxy, lensFacingFront: Boolean) {
        frameProcessor.processCameraFrame(imageProxy, lensFacingFront)
    }

    override fun onCleared() {
        super.onCleared()
        tiltProvider.stop()
        poseLandmarkerFactory.releaseLocked()
    }
}
