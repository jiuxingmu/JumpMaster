package com.jumpmaster.app.ui.main

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun BoxScope.TrainingIdleStartOverlay(onStart: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val ring =
        Brush.linearGradient(
            colors =
                listOf(
                    scheme.primary,
                    scheme.secondary,
                    scheme.primary,
                ),
        )
    Column(
        modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onStart,
            shape = CircleShape,
            color = Color(0xE6181520),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            modifier =
                Modifier
                    .size(108.dp)
                    .border(3.dp, ring, CircleShape),
        ) {
            Box(
                modifier = Modifier.size(108.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "开始",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
