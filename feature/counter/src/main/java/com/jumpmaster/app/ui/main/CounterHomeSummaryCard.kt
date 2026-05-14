package com.jumpmaster.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jumpmaster.app.ui.theme.JumpMasterCardShape

@Composable
internal fun CounterHomeSummaryCard(
    totalJumps: Int,
    sessionCount: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    ElevatedCard(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 14.dp,
                    shape = JumpMasterCardShape,
                    spotColor = scheme.primary.copy(alpha = 0.45f),
                    ambientColor = scheme.primary.copy(alpha = 0.18f),
                ),
        shape = JumpMasterCardShape,
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = scheme.surface,
            ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "JumpMaster",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatChip(
                    icon = Icons.Outlined.Bolt,
                    label = "累计次数",
                    valueText = "${totalJumps} 次",
                    modifier = Modifier.weight(1f),
                )
                StatChip(
                    icon = Icons.Outlined.EventAvailable,
                    label = "训练场次",
                    valueText = "${sessionCount} 场",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatChip(
    icon: ImageVector,
    label: String,
    valueText: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(26.dp),
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                )
            }
        }
    }
}
