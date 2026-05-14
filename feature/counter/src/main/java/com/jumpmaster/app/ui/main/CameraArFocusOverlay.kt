package com.jumpmaster.app.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
internal fun CameraArFocusOverlay(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier = modifier.fillMaxSize()) {
        val len = 40.dp.toPx()
        val stroke = 3.dp.toPx()
        val inset = 28.dp.toPx()
        val c = scheme.primary.copy(alpha = 0.82f)
        val strokeStyle = Stroke(width = stroke)

        fun cornerL(topLeftX: Float, topLeftY: Float, flipX: Boolean, flipY: Boolean) {
            val sx = if (flipX) -1f else 1f
            val sy = if (flipY) -1f else 1f
            val path = Path()
            path.moveTo(topLeftX, topLeftY + len * sy)
            path.lineTo(topLeftX, topLeftY)
            path.lineTo(topLeftX + len * sx, topLeftY)
            drawPath(path, color = c, style = strokeStyle)
        }

        cornerL(inset, inset, flipX = false, flipY = false)
        cornerL(size.width - inset, inset, flipX = true, flipY = false)
        cornerL(inset, size.height - inset, flipX = false, flipY = true)
        cornerL(size.width - inset, size.height - inset, flipX = true, flipY = true)
    }
}
