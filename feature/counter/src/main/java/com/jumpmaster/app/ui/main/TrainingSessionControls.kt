package com.jumpmaster.app.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jumpmaster.app.ui.theme.JumpMasterButtonShape

@Composable
internal fun SessionTrainingControls(
    trainingState: TrainingSessionState,
    onToggleActivePause: () -> Unit,
    onRequestEndTraining: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val endShape = JumpMasterButtonShape
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onRequestEndTraining,
            enabled = trainingState != TrainingSessionState.Idle,
            modifier =
                Modifier
                    .fillMaxWidth(0.94f)
                    .height(52.dp),
            shape = endShape,
            border = BorderStroke(2.dp, scheme.primary.copy(alpha = 0.85f)),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    containerColor = Color.Black.copy(alpha = 0.28f),
                ),
        ) {
            Icon(
                imageVector = Icons.Rounded.StopCircle,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "结束训练",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Button(
            onClick = onToggleActivePause,
            modifier =
                Modifier
                    .fillMaxWidth(0.94f)
                    .height(58.dp),
            shape = JumpMasterButtonShape,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                ),
            elevation =
                ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 2.dp,
                ),
        ) {
            val icon =
                when (trainingState) {
                    TrainingSessionState.Idle -> Icons.Rounded.PlayArrow
                    TrainingSessionState.Active -> Icons.Rounded.Pause
                    TrainingSessionState.Paused -> Icons.Rounded.PlayArrow
                }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = mainTrainingPrimaryLabel(trainingState),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
