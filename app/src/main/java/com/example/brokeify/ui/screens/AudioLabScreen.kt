package com.example.brokeify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brokeify.model.EQPresetName
import com.example.brokeify.viewmodel.BrokeifyViewModel

@Composable
fun AudioLabScreen(
    viewModel: BrokeifyViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val bandFrequencies = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A0F))
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("audio_lab_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "AUDIO LAB // 5-BAND DSP",
                color = Color(0xFF38BDF8),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = "Hardware-inspired graphic equalizer and dynamic loudness leveler.",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        }

        // Equalizer Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D131C)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GRAPHIC EQUALIZER",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = uiState.audioPrefs.eqPreset.displayName,
                        color = Color(0xFF00FF66),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Presets Pills Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        EQPresetName.FLAT,
                        EQPresetName.CAR_STEREO,
                        EQPresetName.BASS_BOOST,
                        EQPresetName.ROCK,
                        EQPresetName.LATE_NIGHT
                    )
                    presets.forEach { preset ->
                        val isSelected = uiState.audioPrefs.eqPreset == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setEqPreset(preset) },
                            label = { Text(preset.displayName, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00FF66).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF00FF66),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5 Frequency Bands Sliders
                bandFrequencies.forEachIndexed { index, freqLabel ->
                    val gain = uiState.audioPrefs.eqGains.getOrElse(index) { 0f }
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = freqLabel,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${if (gain > 0) "+" else ""}${String.format("%.1f", gain)} dB",
                                color = if (gain > 0) Color(0xFF00FF66) else if (gain < 0) Color(0xFFEF4444) else Color(0xFF94A3B8),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                        Slider(
                            value = gain,
                            onValueChange = { viewModel.updateEqBand(index, it) },
                            valueRange = -12f..12f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00FF66),
                                activeTrackColor = Color(0xFF00FF66),
                                inactiveTrackColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.testTag("eq_band_slider_$index")
                        )
                    }
                }
            }
        }

        // DSP Loudness & Leveler Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D131C)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "LOUDNESS STABILIZATION & LEVELING",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )

                // EBU R128 LUFS Normalization
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LUFS / ReplayGain Normalization",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Matches streaming volume across library tracks (-14 LUFS target).",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = uiState.audioPrefs.lufsNormalization,
                        onCheckedChange = { viewModel.toggleLufsNormalization() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00FF66),
                            checkedTrackColor = Color(0xFF00FF66).copy(alpha = 0.3f),
                            uncheckedThumbColor = Color(0xFF64748B),
                            uncheckedTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.testTag("lufs_switch")
                    )
                }

                Divider(color = Color(0xFF1E293B))

                // Road Noise Car Volume Leveler
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Road Noise Car Volume Leveler",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Dynamic audio compression to keep quiet vocals audible over highway cabin noise.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = uiState.audioPrefs.carVolumeLeveler,
                        onCheckedChange = { viewModel.toggleCarVolumeLeveler() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFF59E0B),
                            checkedTrackColor = Color(0xFFF59E0B).copy(alpha = 0.3f),
                            uncheckedThumbColor = Color(0xFF64748B),
                            uncheckedTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.testTag("car_leveler_switch")
                    )
                }

                Divider(color = Color(0xFF1E293B))

                // Crossfade
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Crossfade Duration", color = Color.White, fontSize = 13.sp)
                        Text(
                            text = "${uiState.audioPrefs.crossfadeSeconds}s",
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Slider(
                        value = uiState.audioPrefs.crossfadeSeconds.toFloat(),
                        onValueChange = { viewModel.setCrossfadeSeconds(it.toInt()) },
                        valueRange = 0f..12f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF38BDF8),
                            inactiveTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.testTag("crossfade_slider")
                    )
                }
            }
        }
    }
}
