package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.ProBlue

enum class HdrPlaybackMode(val title: String, val subtitle: String) {
    AUTO("Auto Detect HDR / HDR10+", "Automatically detects HDR10, HDR10+, HLG, and Dolby Vision"),
    FORCE_HDR("Force HDR10+ Wide Gamut", "Enables 10-bit BT.2020 color mode on all content"),
    SDR("Standard (SDR)", "Standard dynamic range with normal color gamut")
}

@Composable
fun QualityHdrDialog(
    isHdrActive: Boolean,
    hdrFormatLabel: String,
    resolutionLabel: String,
    width: Int,
    height: Int,
    isHdrBoostEnabled: Boolean,
    currentHdrMode: HdrPlaybackMode,
    onHdrBoostToggled: (Boolean) -> Unit,
    onHdrModeSelected: (HdrPlaybackMode) -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("quality_hdr_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF141923)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B3545))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF0070F3), Color(0xFF00C6FF))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Video Quality & HDR+",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "High Dynamic Range & Hardware Decoding",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // HDR Engine Status Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isHdrActive) {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF0F2B48), Color(0xFF163E66))
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF1E2638), Color(0xFF263248))
                                )
                            }
                        )
                        .border(
                            1.dp,
                            if (isHdrActive) Color(0xFF0084FF) else Color(0xFF334155),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isHdrActive) Color(0xFF0070F3) else Color(0xFF334155)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isHdrActive) "$hdrFormatLabel Engine" else "HDR10+ Ready",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isHdrActive) Color(0xFF00E5FF) else Color(0xFF64748B))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isHdrActive) "ACTIVE" else "READY",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isHdrActive) Color(0xFF003B5C) else Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isHdrActive) {
                                    "10-Bit BT.2020 Wide Color Gamut & PQ Tone Mapping Active"
                                } else {
                                    "Standard Dynamic Range (SDR) with HW+ acceleration"
                                },
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Spec info items
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpecChip(
                        modifier = Modifier.weight(1f),
                        label = "Resolution",
                        value = if (width > 0 && height > 0) "${width}×${height}" else resolutionLabel
                    )
                    SpecChip(
                        modifier = Modifier.weight(1f),
                        label = "Color Space",
                        value = if (isHdrActive) "BT.2020" else "BT.709"
                    )
                    SpecChip(
                        modifier = Modifier.weight(1f),
                        label = "Decoder",
                        value = "HW+ Surface"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // HDR Vivid Boost Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E2638))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrightnessHigh,
                            contentDescription = null,
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "HDR+ Vivid Brightness Boost",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "Boosts peak screen brightness for HDR highlights",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Switch(
                        checked = isHdrBoostEnabled,
                        onCheckedChange = onHdrBoostToggled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF0070F3)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFF263248))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "HDR Color & Playback Profile",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mode Options
                HdrPlaybackMode.values().forEach { mode ->
                    val isSelected = currentHdrMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onHdrModeSelected(mode) }
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .background(Color(0xFF1E2D48))
                                        .border(1.dp, Color(0xFF0070F3), RoundedCornerShape(10.dp))
                                } else {
                                    Modifier.background(Color.Transparent)
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF38BDF8) else Color.White
                            )
                            Text(
                                text = mode.subtitle,
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun SpecChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E2638))
            .border(1.dp, Color(0xFF2B3545), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}
