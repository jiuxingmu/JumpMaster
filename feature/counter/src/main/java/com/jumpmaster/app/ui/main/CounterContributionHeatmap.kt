package com.jumpmaster.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jumpmaster.app.ui.theme.JumpMasterCardShape
import java.time.LocalDate

private const val VisibleWeeks = 26

@Composable
internal fun CounterContributionHeatmap(
    dailyTotals: Map<LocalDate, Int>,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val legendBrushes =
        remember(scheme) {
            listOf(
                Brush.verticalGradient(
                    listOf(
                        scheme.surfaceVariant.copy(alpha = 0.35f),
                        scheme.surfaceVariant.copy(alpha = 0.55f),
                    ),
                ),
                Brush.verticalGradient(
                    listOf(
                        scheme.primary.copy(alpha = 0.25f),
                        scheme.primary.copy(alpha = 0.45f),
                    ),
                ),
                Brush.verticalGradient(
                    listOf(
                        scheme.primary.copy(alpha = 0.45f),
                        scheme.primary.copy(alpha = 0.65f),
                    ),
                ),
                Brush.verticalGradient(
                    listOf(
                        scheme.primary.copy(alpha = 0.65f),
                        scheme.primary.copy(alpha = 0.88f),
                    ),
                ),
                Brush.verticalGradient(
                    listOf(
                        scheme.primary.copy(alpha = 0.78f),
                        scheme.primary,
                    ),
                ),
            )
        }

    ElevatedCard(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = JumpMasterCardShape,
                    spotColor = scheme.primary.copy(alpha = 0.35f),
                    ambientColor = scheme.primary.copy(alpha = 0.12f),
                ),
        shape = JumpMasterCardShape,
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = scheme.surfaceContainer.copy(alpha = 0.95f),
            ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "最近 $VisibleWeeks 周",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                )
                HeatLegend(legendBrushes = legendBrushes)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                HeatmapGridCanvas(dailyTotals = dailyTotals)
            }
        }
    }
}

@Composable
private fun HeatLegend(legendBrushes: List<Brush>) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "少",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
        )
        legendBrushes.forEach { b ->
            Box(
                modifier =
                    Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(b),
            )
        }
        Text(
            text = "多",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
        )
    }
}
