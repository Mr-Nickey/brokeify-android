package com.example.brokeify.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.brokeify.model.VisualTheme

private val CyberDeckColorScheme = darkColorScheme(
    primary = Color(0xFF00FF66),
    secondary = Color(0xFF38BDF8),
    tertiary = Color(0xFFF59E0B),
    background = Color(0xFF070A0F),
    surface = Color(0xFF0D131C),
    onPrimary = Color(0xFF070A0F),
    onSecondary = Color(0xFF070A0F),
    onBackground = Color(0xFFF4F4F5),
    onSurface = Color(0xFFF4F4F5)
)

private val ClassicRackColorScheme = darkColorScheme(
    primary = Color(0xFFFBBF24),
    secondary = Color(0xFF10B981),
    tertiary = Color(0xFFF59E0B),
    background = Color(0xFF0A0C10),
    surface = Color(0xFF18181B),
    onPrimary = Color(0xFF111827),
    onBackground = Color(0xFFF4F4F5),
    onSurface = Color(0xFFF4F4F5)
)

private val ModularBentoColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    secondary = Color(0xFFA855F7),
    tertiary = Color(0xFF00FF66),
    background = Color(0xFF060913),
    surface = Color(0xFF0F172A),
    onPrimary = Color(0xFF060913),
    onBackground = Color(0xFFF4F4F5),
    onSurface = Color(0xFFF4F4F5)
)

private val AmberCrtColorScheme = darkColorScheme(
    primary = Color(0xFFF59E0B),
    secondary = Color(0xFFFB923C),
    tertiary = Color(0xFFD97706),
    background = Color(0xFF0C0804),
    surface = Color(0xFF1E140A),
    onPrimary = Color(0xFF0C0804),
    onBackground = Color(0xFFFEF3C7),
    onSurface = Color(0xFFFEF3C7)
)

private val PlatinumLightColorScheme = lightColorScheme(
    primary = Color(0xFF059669),
    secondary = Color(0xFF0284C7),
    background = Color(0xFFE2E8F0),
    surface = Color(0xFFF8FAFC),
    onPrimary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

private val SolarLightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    secondary = Color(0xFF10B981),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

@Composable
fun BrokeifyTheme(
    visualTheme: VisualTheme = VisualTheme.CYBER_DECK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (visualTheme) {
        VisualTheme.CYBER_DECK -> CyberDeckColorScheme
        VisualTheme.CLASSIC_RACK -> ClassicRackColorScheme
        VisualTheme.MODULAR_BENTO -> ModularBentoColorScheme
        VisualTheme.AMBER_CRT -> AmberCrtColorScheme
        VisualTheme.PLATINUM_LIGHT -> PlatinumLightColorScheme
        VisualTheme.SOLAR_LIGHT -> SolarLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
