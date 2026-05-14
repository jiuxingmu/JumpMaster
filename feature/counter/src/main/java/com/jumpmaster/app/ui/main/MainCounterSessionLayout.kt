package com.jumpmaster.app.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun MainCounterSessionLayout(
    onToggleCamera: () -> Unit,
    overlayRenderMode: OverlayRenderMode,
    onToggleOverlay: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    cameraContent: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        cameraContent()
        ImmersiveBackChip(
            onClick = onNavigateUp,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 12.dp, top = 8.dp),
        )
        ImmersiveCameraToolbar(
            onToggleCamera = onToggleCamera,
            overlayRenderMode = overlayRenderMode,
            onToggleOverlay = onToggleOverlay,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 12.dp),
        )
    }
}
