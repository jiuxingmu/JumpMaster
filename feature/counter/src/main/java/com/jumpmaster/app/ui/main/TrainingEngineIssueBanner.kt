package com.jumpmaster.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jumpmaster.app.ui.theme.JumpMasterButtonShape

@Composable
internal fun TrainingEngineIssueBanner(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.padding(horizontal = 20.dp),
        shape = JumpMasterButtonShape,
        color = scheme.errorContainer.copy(alpha = 0.92f),
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Engineering,
                contentDescription = null,
                tint = scheme.onErrorContainer,
                modifier = Modifier.size(32.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = TrainingFriendlyCopy.ENGINE_PREPARING,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onErrorContainer,
                )
                Text(
                    text = TrainingFriendlyCopy.ENGINE_ASSISTANCE,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onErrorContainer.copy(alpha = 0.9f),
                    textAlign = TextAlign.Start,
                )
            }
            Button(
                onClick = onRetry,
                shape = JumpMasterButtonShape,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary,
                    ),
            ) {
                Text(text = "重试")
            }
        }
    }
}

@Composable
internal fun CameraBindFriendlyBanner(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.padding(horizontal = 20.dp),
        shape = JumpMasterButtonShape,
        color = scheme.primaryContainer.copy(alpha = 0.94f),
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.VideocamOff,
                contentDescription = null,
                tint = scheme.onPrimaryContainer,
                modifier = Modifier.size(30.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = TrainingFriendlyCopy.CAMERA_GENERIC,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onPrimaryContainer,
                )
                Text(
                    text = TrainingFriendlyCopy.CAMERA_HINT,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.9f),
                )
            }
            Button(
                onClick = onRetry,
                shape = JumpMasterButtonShape,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary,
                    ),
            ) {
                Text(text = "重试")
            }
        }
    }
}
