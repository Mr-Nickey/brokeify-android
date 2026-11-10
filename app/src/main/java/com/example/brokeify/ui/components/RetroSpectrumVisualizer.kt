package com.example.brokeify.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val phase by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 6.28f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
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
                val heightMultiplier = if (isPlaying) {
                    val w1 = kotlin.math.sin(phase + i * 0.45f) * 0.38f + 0.48f
                    val w2 = kotlin.math.cos(phase * 1.6f + i * 0.65f) * 0.22f
                    (w1 + w2).coerceIn(0.10f, 0.96f)
                } else {
                    0.04f // resting flat baseline
                }

                val currentBarHeight = totalHeight * heightMultiplier
                val left = i * (barWidth + barSpacing)
                val top = totalHeight - currentBarHeight

                val color = when {
                    !isPlaying -> Color(0xFF1E293B)
                    heightMultiplier > 0.82f -> Color(0xFFEF4444)
                    heightMultiplier > 0.62f -> Color(0xFFF59E0B)
                    else -> accentColor
                }

                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, currentBarHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )

                if (isPlaying) {
                    val peakTop = (top - 4.dp.toPx()).coerceAtLeast(0f)
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
