package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ProBlue

sealed class GestureOverlayType {
    object None : GestureOverlayType()
    data class Volume(val percentage: Int) : GestureOverlayType()
    data class Brightness(val percentage: Int) : GestureOverlayType()
    data class Scrubbing(val targetPositionMs: Long, val deltaSeconds: Int, val totalDurationMs: Long) : GestureOverlayType()
    data class DoubleTapSeek(val isForward: Boolean, val seekSeconds: Int) : GestureOverlayType()
}

@Composable
fun GestureFeedbackOverlay(
    overlayType: GestureOverlayType,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = overlayType !is GestureOverlayType.None,
            enter = fadeIn() + scaleIn(initialScale = 0.82f),
            exit = fadeOut() + scaleOut(targetScale = 0.82f)
        ) {
            when (overlayType) {
                is GestureOverlayType.Volume -> {
                    VolumeFeedbackPill(percentage = overlayType.percentage)
                }
                is GestureOverlayType.Brightness -> {
                    BrightnessFeedbackPill(percentage = overlayType.percentage)
                }
                is GestureOverlayType.Scrubbing -> {
                    ScrubbingFeedbackPill(
                        targetMs = overlayType.targetPositionMs,
                        deltaSec = overlayType.deltaSeconds,
                        totalMs = overlayType.totalDurationMs
                    )
                }
                is GestureOverlayType.DoubleTapSeek -> {
                    DoubleTapSeekPulse(
                        isForward = overlayType.isForward,
                        seconds = overlayType.seekSeconds
                    )
                }
                is GestureOverlayType.None -> Unit
            }
        }
    }
}

@Composable
private fun VolumeFeedbackPill(percentage: Int) {
    val fillFraction = (percentage / 100f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xEE0B111E))
            .border(1.5.dp, Brush.verticalGradient(listOf(Color(0x663B82F6), Color(0x221E293B))), RoundedCornerShape(24.dp))
            .padding(horizontal = 26.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val icon = when {
                percentage <= 0 -> Icons.Default.VolumeMute
                percentage < 40 -> Icons.Default.VolumeDown
                else -> Icons.Default.VolumeUp
            }

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0x331E60FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Volume",
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Volume",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "$percentage%",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Modern glowing progress track
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x33FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillFraction)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF38BDF8), Color(0xFF1E60FF))
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun BrightnessFeedbackPill(percentage: Int) {
    val fillFraction = (percentage / 100f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xEE0B111E))
            .border(1.5.dp, Brush.verticalGradient(listOf(Color(0x66F59E0B), Color(0x221E293B))), RoundedCornerShape(24.dp))
            .padding(horizontal = 26.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val icon = when {
                percentage < 33 -> Icons.Default.BrightnessLow
                percentage < 66 -> Icons.Default.BrightnessMedium
                else -> Icons.Default.BrightnessHigh
            }

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0x33F59E0B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Brightness",
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Brightness",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "$percentage%",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Glowing amber progress track
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x33FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillFraction)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun ScrubbingFeedbackPill(targetMs: Long, deltaSec: Int, totalMs: Long) {
    val sign = if (deltaSec >= 0) "+" else ""
    val formattedTarget = formatMs(targetMs)
    val formattedTotal = formatMs(totalMs)
    val progressFraction = if (totalMs > 0) (targetMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f) else 0f

    Box(
        modifier = Modifier
            .shadow(20.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xF00A0F1D))
            .border(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            .padding(horizontal = 28.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x3338BDF8)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (deltaSec >= 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "$sign${deltaSec}s",
                    color = Color(0xFF38BDF8),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$formattedTarget / $formattedTotal",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Mini scrubbing scrub bar
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x44FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF38BDF8), ProBlue)
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun DoubleTapSeekPulse(isForward: Boolean, seconds: Int) {
    Box(
        modifier = Modifier
            .size(110.dp)
            .shadow(16.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(0xDD0F172A))
            .border(2.dp, Brush.radialGradient(listOf(Color(0xFF60A5FA), Color(0xFF1E60FF))), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                contentDescription = null,
                tint = Color(0xFF60A5FA),
                modifier = Modifier.size(38.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${if (isForward) "+" else "-"}${seconds}s",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms.coerceAtLeast(0) / 1000).toInt()
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}
