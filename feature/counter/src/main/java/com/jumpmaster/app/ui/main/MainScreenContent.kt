package com.jumpmaster.app.ui.main

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

enum class OverlayRenderMode {
    OFF,
    SKELETON,
}

@Composable
fun MainCameraContent(
    cameraPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    previewView: PreviewView,
    cameraBindError: String?,
    hasAnalyzerFrames: Boolean,
    poseOverlayPoints: PoseOverlayPoints?,
    overlayRenderMode: OverlayRenderMode,
    hint: String,
    jumpCount: Int,
    trainingState: TrainingSessionState,
    poseEngineRetrySuggested: Boolean,
    onRetryPoseEngine: () -> Unit,
    onRetryCameraBind: () -> Unit,
    onToggleActivePause: () -> Unit,
    onRequestEndTraining: () -> Unit,
) {
    val sessionStarted = trainingState != TrainingSessionState.Idle

    Box(modifier = Modifier.fillMaxSize()) {
        if (!cameraPermissionGranted) {
            CameraPermissionGate(onRequestPermission = onRequestPermission)
            return@Box
        }

        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        CameraArFocusOverlay(Modifier.fillMaxSize())
        poseOverlayPoints?.let { PoseKeypointOverlay(points = it, mode = overlayRenderMode) }
        if (cameraBindError != null) {
            CameraBindFriendlyBanner(
                onRetry = onRetryCameraBind,
                modifier = Modifier.align(Alignment.Center),
            )
        } else if (poseEngineRetrySuggested) {
            TrainingEngineIssueBanner(
                onRetry = onRetryPoseEngine,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        if (cameraBindError == null && !hasAnalyzerFrames) {
            WaitingFrameCard()
        }

        if (sessionStarted) {
            if (!poseEngineRetrySuggested) {
                HintCard(hint = hint)
            }
            SessionCounterHud(
                count = jumpCount,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .statusBarsPadding(),
            )
            SessionTrainingControls(
                trainingState = trainingState,
                onToggleActivePause = onToggleActivePause,
                onRequestEndTraining = onRequestEndTraining,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp),
            )
        } else {
            TrainingIdleStartOverlay(onStart = onToggleActivePause)
        }
    }
}

@Composable
private fun BoxScope.PoseKeypointOverlay(points: PoseOverlayPoints, mode: OverlayRenderMode) {
    if (mode == OverlayRenderMode.OFF) return
    val skeletonColor = Color(0xB3FFFFFF)
    val keypointColor = Color(0xFF4FC3F7)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val radiusPx = 4.dp.toPx()
        val strokePx = 2.dp.toPx()
        if (mode == OverlayRenderMode.SKELETON) {
            points.connections.forEach { (from, to) ->
                drawSkeletonLine(points.landmarks.getOrNull(from), points.landmarks.getOrNull(to), skeletonColor, strokePx)
            }
        }
        points.landmarks.forEach { drawNormalizedPoint(it, keypointColor, radiusPx) }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNormalizedPoint(
    point: NormalizedPoint,
    color: Color,
    radiusPx: Float,
) {
    if (point.x !in 0f..1f || point.y !in 0f..1f) return
    drawCircle(
        color = color,
        radius = radiusPx,
        center = Offset(x = point.x * size.width, y = point.y * size.height),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSkeletonLine(
    from: NormalizedPoint?,
    to: NormalizedPoint?,
    color: Color,
    strokePx: Float,
) {
    if (from == null || to == null) return
    if (!from.inFrame() || !to.inFrame()) return
    drawLine(
        color = color,
        start = Offset(x = from.x * size.width, y = from.y * size.height),
        end = Offset(x = to.x * size.width, y = to.y * size.height),
        strokeWidth = strokePx,
    )
}

private fun NormalizedPoint.inFrame(): Boolean = x in 0f..1f && y in 0f..1f

@Composable
private fun CameraPermissionGate(onRequestPermission: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = onRequestPermission) { Text("授予摄像头权限") }
    }
}

@Composable
private fun BoxScope.WaitingFrameCard() {
    Surface(
        modifier = Modifier.align(Alignment.Center).padding(16.dp),
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            text = "相机已绑定，等待视频帧…（若持续不变，说明模拟器未向 CameraX 供帧）",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun BoxScope.HintCard(hint: String) {
    Surface(
        modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 168.dp, start = 16.dp, end = 16.dp),
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
