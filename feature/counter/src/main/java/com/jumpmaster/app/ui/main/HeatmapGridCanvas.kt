package com.jumpmaster.app.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private const val VisibleWeeks = 26

@Composable
internal fun HeatmapGridCanvas(
    dailyTotals: Map<LocalDate, Int>,
    modifier: Modifier = Modifier,
    cell: Dp = 11.dp,
    gap: Dp = 3.dp,
) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val today = LocalDate.now()
    val gridOrigin =
        remember(today) {
            today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                .minusWeeks(VisibleWeeks - 1L)
        }
    val maxDay = remember(dailyTotals) { dailyTotals.values.maxOrNull() ?: 0 }
    val heatStops =
        remember(scheme) {
            listOf(
                scheme.primary.copy(alpha = 0.12f),
                scheme.primary.copy(alpha = 0.35f),
                scheme.primary.copy(alpha = 0.55f),
                scheme.primary.copy(alpha = 0.78f),
                scheme.primary,
            )
        }
    val outline = scheme.outline.copy(alpha = 0.22f)
    val cellPx = with(density) { cell.toPx() }
    val gapPx = with(density) { gap.toPx() }
    val cornerPx = with(density) { 2.dp.toPx() }
    val strokePx = with(density) { 1.dp.toPx() }
    val rows = 7
    val cols = VisibleWeeks
    val gridW = cols * cellPx + (cols - 1) * gapPx
    val gridH = rows * cellPx + (rows - 1) * gapPx
    val widthDp = with(density) { gridW.toDp() }
    val heightDp = with(density) { gridH.toDp() }

    Canvas(modifier = modifier.requiredSize(widthDp, heightDp)) {
        for (d in 0 until rows) {
            for (w in 0 until cols) {
                val date = gridOrigin.plusDays(w * 7L + d.toLong())
                val total = dailyTotals[date] ?: 0
                val level = intensityLevel(total, maxDay)
                val left = w * (cellPx + gapPx)
                val top = d * (cellPx + gapPx)
                val brush =
                    if (level <= 0) {
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    scheme.surfaceVariant.copy(alpha = 0.35f),
                                    scheme.surfaceVariant.copy(alpha = 0.55f),
                                ),
                        )
                    } else {
                        val hi = heatStops[level.coerceIn(1, heatStops.lastIndex)]
                        val lo = heatStops[(level - 1).coerceAtLeast(0)]
                        Brush.verticalGradient(colors = listOf(lo, hi))
                    }
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(left, top),
                    size = Size(cellPx, cellPx),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                )
                drawRoundRect(
                    color = outline,
                    topLeft = Offset(left, top),
                    size = Size(cellPx, cellPx),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = strokePx),
                )
            }
        }
    }
}

internal fun intensityLevel(
    dayTotal: Int,
    maxInPeriod: Int,
): Int {
    if (dayTotal <= 0) return 0
    if (maxInPeriod <= 0) return 1
    val ratio = dayTotal.toFloat() / maxInPeriod
    return when {
        ratio < 0.25f -> 1
        ratio < 0.5f -> 2
        ratio < 0.75f -> 3
        else -> 4
    }
}
