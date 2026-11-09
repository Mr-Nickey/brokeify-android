package com.example.brokeify.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun RetroSpectrumVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF00FF66),
    barCount: Int = 18
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SpectrumAnim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color(0xFF04080C), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("spectrum_visualizer")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalWidth = size.width
            val totalHeight = size.height
            val barSpacing = 4.dp.toPx()
            val barWidth = (totalWidth - (barSpacing * (barCount - 1))) / barCount

            for (i in 0 until barCount) {
                val normalizedBar = i.toFloat() / barCount
                val heightMultiplier = if (isPlaying) {
                    val wave1 = kotlin.math.sin(phase + i * 0.45f) * 0.4f + 0.5f
                    val wave2 = kotlin.math.cos(phase * 1.5f + i * 0.7f) * 0.25f
                    (wave1 + wave2).coerceIn(0.12f, 0.98f)
                } else {
                    0.08f
                }

                val currentBarHeight = totalHeight * heightMultiplier
                val left = i * (barWidth + barSpacing)
                val top = totalHeight - currentBarHeight

                val color = when {
                    heightMultiplier > 0.85f -> Color(0xFFEF4444)
                    heightMultiplier > 0.65f -> Color(0xFFF59E0B)
                    else -> accentColor
                }

                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, currentBarHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )

                // Peak indicator dot
                if (isPlaying) {
                    val peakTop = (top - 5.dp.toPx()).coerceAtLeast(0f)
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.85f),
                        topLeft = Offset(left, peakTop),
                        size = Size(barWidth, 2.dp.toPx()),
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    )
                }
            }
        }
    }
}
