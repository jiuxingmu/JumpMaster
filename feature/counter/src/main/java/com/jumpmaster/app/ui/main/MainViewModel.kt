package com.jumpmaster.app.ui.main

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jumpmaster.app.data.pose.PoseLandmarkerFactory
import com.jumpmaster.app.data.sensor.DeviceTiltProvider
import com.jumpmaster.app.domain.camera.CameraJumpDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val poseLandmarkerFactory: PoseLandmarkerFactory,
    private val tiltProvider: DeviceTiltProvider,
    private val sessionPersistCoordinator: MainSessionPersistCoordinator,
) : ViewModel() {

    private val detector = CameraJumpDetector()
    private val durationTracker = ActiveDurationTracker()

    private val _jumpCount = MutableStateFlow(0)
    val jumpCount: StateFlow<Int> = _jumpCount.asStateFlow()

    private val _hint = MutableStateFlow("面向摄像头开始做跳跃 Demo")
    val hint: StateFlow<String> = _hint.asStateFlow()

    private val _poseOverlayPoints = MutableStateFlow<PoseOverlayPoints?>(null)
    val poseOverlayPoints: StateFlow<PoseOverlayPoints?> = _poseOverlayPoints.asStateFlow()

    private val _trainingState = MutableStateFlow(TrainingSessionState.Idle)
    val trainingState: StateFlow<TrainingSessionState> = _trainingState.asStateFlow()

    private val _sessionSummary = MutableStateFlow<SessionSummary?>(null)
    val sessionSummary: StateFlow<SessionSummary?> = _sessionSummary.asStateFlow()

    private var persistRetrySummary: SessionSummary? = null

    private val _sessionPersistEvents =
        MutableSharedFlow<SessionPersistEvent>(extraBufferCapacity = 1)
    val sessionPersistEvents: SharedFlow<SessionPersistEvent> = _sessionPersistEvents.asSharedFlow()

    private val _poseEngineRetrySuggested = MutableStateFlow(false)
    val poseEngineRetrySuggested: StateFlow<Boolean> = _poseEngineRetrySuggested.asStateFlow()

    private val onRecoverablePoseFailure: () -> Unit = {
        _poseEngineRetrySuggested.value = true
        _hint.value = TrainingFriendlyCopy.ENGINE_ASSISTANCE
    }

    private val frameProcessor =
        MainCameraFrameProcessor(
            poseLandmarkerFactory = poseLandmarkerFactory,
            tiltProvider = tiltProvider,
            detector = detector,
            hint = _hint,
            poseOverlayPoints = _poseOverlayPoints,
            jumpCount = { _jumpCount.value },
            isCountingActive = { isJumpCountingEnabled() },
            onRecoverablePoseFailure = onRecoverablePoseFailure,
        )

    init {
        tiltProvider.start()
        viewModelScope.launch {
            detector.jumpEvents.collect {
                if (isJumpCountingEnabled()) _jumpCount.update { c -> c + 1 }
            }
        }
    }

    fun toggleActivePause() {
        when (_trainingState.value) {
            TrainingSessionState.Idle -> startFromIdle()
            TrainingSessionState.Active -> pauseFromActive()
            TrainingSessionState.Paused -> resumeFromPaused()
        }
    }

    fun confirmEndTraining() {
        if (_trainingState.value == TrainingSessionState.Idle) return
        sessionPersistCoordinator.resetForNewEnd()
        val now = nowElapsedRealtime()
        val jumps = _jumpCount.value
        val effectiveMs = durationTracker.effectiveMsAt(now)
        durationTracker.pauseOpenSegment(now)
        _poseEngineRetrySuggested.value = false
        val summary = SessionSummary(jumpCount = jumps, effectiveDurationMs = effectiveMs)
        _sessionSummary.value = summary
        _trainingState.value = TrainingSessionState.Idle
        _hint.value = "本次训练已结束"
        scheduleSessionPersist(summary)
    }

    fun retryPersistSession() {
        val summary = persistRetrySummary ?: return
        scheduleSessionPersist(summary)
    }

    fun dismissSessionSummary() {
        if (_sessionSummary.value == null) return
        _sessionSummary.value = null
        persistRetrySummary = null
        resetCounterForNewSession()
        _poseEngineRetrySuggested.value = false
        _hint.value = "面向摄像头开始做跳跃 Demo"
    }

    fun retryPoseEngine() {
        poseLandmarkerFactory.releaseLocked()
        _poseEngineRetrySuggested.value = false
        _hint.value = TrainingFriendlyCopy.ENGINE_PREPARING
    }

    /** 离开「开始」页（切 Tab / 进后台）且非配置变更时回到 Idle，与 C-01 手动测试要点一致。 */
    fun onScreenHiddenResetIfInSession() {
        if (_sessionSummary.value != null) dismissSessionSummary()
        persistRetrySummary = null
        sessionPersistCoordinator.resetForNewEnd()
        if (_trainingState.value == TrainingSessionState.Idle) return
        _trainingState.value = TrainingSessionState.Idle
        durationTracker.reset()
        resetCounterForNewSession()
        _poseEngineRetrySuggested.value = false
        _hint.value = "面向摄像头开始做跳跃 Demo"
    }

    fun processCameraFrame(imageProxy: ImageProxy, lensFacingFront: Boolean) {
        frameProcessor.processCameraFrame(imageProxy, lensFacingFront)
    }

    override fun onCleared() {
        super.onCleared()
        tiltProvider.stop()
        poseLandmarkerFactory.releaseLocked()
    }

    private fun isJumpCountingEnabled(): Boolean =
        _trainingState.value == TrainingSessionState.Active && _sessionSummary.value == null

    private fun startFromIdle() {
        _jumpCount.value = 0
        detector.reset()
        durationTracker.reset()
        _poseEngineRetrySuggested.value = false
        val now = nowElapsedRealtime()
        durationTracker.startOrResumeSegment(now)
        _trainingState.value = TrainingSessionState.Active
        _hint.value = "开始跳绳！"
    }

    private fun pauseFromActive() {
        if (_trainingState.value != TrainingSessionState.Active) return
        durationTracker.pauseOpenSegment(nowElapsedRealtime())
        _trainingState.value = TrainingSessionState.Paused
        _hint.value = "已暂停计数"
    }

    private fun resumeFromPaused() {
        if (_trainingState.value != TrainingSessionState.Paused) return
        durationTracker.startOrResumeSegment(nowElapsedRealtime())
        _trainingState.value = TrainingSessionState.Active
        _hint.value = "继续计数"
    }

    private fun resetCounterForNewSession() {
        _jumpCount.value = 0
        detector.reset()
        durationTracker.reset()
    }

    private fun scheduleSessionPersist(summary: SessionSummary) {
        viewModelScope.launch {
            val work = sessionPersistCoordinator.persistIfNeeded(summary)
            when (work) {
                SessionPersistWorkResult.Skipped -> Unit
                SessionPersistWorkResult.Saved -> {
                    persistRetrySummary = null
                    _sessionPersistEvents.emit(SessionPersistEvent.Saved)
                }
                is SessionPersistWorkResult.Failed -> {
                    persistRetrySummary = summary
                    _sessionPersistEvents.emit(SessionPersistEvent.Failed)
                }
            }
        }
    }
}
