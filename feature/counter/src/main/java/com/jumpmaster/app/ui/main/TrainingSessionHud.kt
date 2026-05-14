package com.jumpmaster.app.ui.main

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jumpmaster.app.ui.theme.JumpMasterCardShape

@Composable
internal fun SessionCounterHud(
    count: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val animated by animateIntAsState(
        targetValue = count,
        animationSpec = tween(durationMillis = 220),
        label = "sessionJumpCount",
    )
    val shape = JumpMasterCardShape
    val hudBg = scheme.surface
    Box(
        modifier =
            modifier
                .shadow(
                    elevation = 18.dp,
                    shape = shape,
                    spotColor = scheme.primary.copy(alpha = 0.5f),
                    ambientColor = scheme.primary.copy(alpha = 0.2f),
                )
                .clip(shape)
                .background(hudBg, shape),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 28.dp, vertical = 18.dp)
                    .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "本次跳跃",
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = animated.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = scheme.primary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "次",
                style = MaterialTheme.typography.titleMedium,
                color = scheme.secondary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
