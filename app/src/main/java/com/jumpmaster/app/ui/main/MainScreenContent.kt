package com.jumpmaster.app.ui.main

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.Alignment

@Composable
fun MainCameraContent(
    innerPadding: PaddingValues,
    cameraPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    previewView: PreviewView,
    cameraBindError: String?,
    hasAnalyzerFrames: Boolean,
    hint: String,
    jumpCount: Int,
) {
    Box(
        modifier =
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
    ) {
        if (!cameraPermissionGranted) {
            CameraPermissionGate(onRequestPermission = onRequestPermission)
            return@Box
        }

        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        cameraBindError?.let { ErrorMessageCard(message = it) }
        if (cameraBindError == null && !hasAnalyzerFrames) WaitingFrameCard()
        HintCard(hint = hint)
        CounterCard(count = jumpCount)
    }
}

@Composable
private fun CameraPermissionGate(onRequestPermission: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = onRequestPermission) { Text("授予摄像头权限") }
    }
}

@Composable
private fun BoxScope.ErrorMessageCard(message: String) {
    Surface(
        modifier = Modifier.align(Alignment.Center).padding(16.dp),
        tonalElevation = 6.dp,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun BoxScope.WaitingFrameCard() {
    Surface(
        modifier = Modifier.align(Alignment.Center).padding(16.dp),
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.large,
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
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun BoxScope.CounterCard(count: Int) {
    Surface(
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
        tonalElevation = 6.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.94f),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            text = "计数：$count",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
    }
}
