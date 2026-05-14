package com.jumpmaster.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ImmersivePillShape = RoundedCornerShape(28.dp)

@Composable
internal fun ImmersiveBackChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.92f),
        tonalElevation = 4.dp,
        shadowElevation = 10.dp,
    ) {
        IconButton(onClick = onClick) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "返回",
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
internal fun ImmersiveCameraToolbar(
    onToggleCamera: () -> Unit,
    overlayRenderMode: OverlayRenderMode,
    onToggleOverlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = ImmersivePillShape,
        color = Color.White.copy(alpha = 0.92f),
        tonalElevation = 4.dp,
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            IconButton(
                onClick = onToggleCamera,
                colors =
                    IconButtonDefaults.iconButtonColors(
                        contentColor = scheme.primary,
                    ),
            ) {
                Icon(
                    Icons.Outlined.Cameraswitch,
                    contentDescription = "切换摄像头",
                    modifier = Modifier.size(26.dp),
                )
            }
            IconButton(
                onClick = onToggleOverlay,
                colors =
                    IconButtonDefaults.iconButtonColors(
                        contentColor = scheme.secondary,
                    ),
            ) {
                Icon(
                    imageVector =
                        if (overlayRenderMode == OverlayRenderMode.SKELETON) {
                            Icons.Outlined.Visibility
                        } else {
                            Icons.Outlined.VisibilityOff
                        },
                    contentDescription = "切换骨骼叠加",
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}
