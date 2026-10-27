package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
        modifier = modifier.fillMaxSize()
    ) {
        // Left Side: Sleek Vertical Brightness Line HUD
        AnimatedVisibility(
            visible = overlayType is GestureOverlayType.Brightness,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { -it / 2 }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -it / 2 }),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 28.dp)
        ) {
            if (overlayType is GestureOverlayType.Brightness) {
                VerticalBrightnessBar(percentage = overlayType.percentage)
            }
        }

        // Right Side: Sleek Vertical Volume Line HUD
        AnimatedVisibility(
            visible = overlayType is GestureOverlayType.Volume,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 2 }),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 28.dp)
        ) {
            if (overlayType is GestureOverlayType.Volume) {
                VerticalVolumeBar(percentage = overlayType.percentage)
            }
        }

        // Center: Scrubbing Pill
        AnimatedVisibility(
            visible = overlayType is GestureOverlayType.Scrubbing,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            if (overlayType is GestureOverlayType.Scrubbing) {
                ScrubbingFeedbackPill(
                    targetMs = overlayType.targetPositionMs,
                    deltaSec = overlayType.deltaSeconds,
                    totalMs = overlayType.totalDurationMs
                )
            }
        }

        // Double-Tap Seek Pulse (Left or Right)
        AnimatedVisibility(
            visible = overlayType is GestureOverlayType.DoubleTapSeek,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier.align(
                if (overlayType is GestureOverlayType.DoubleTapSeek && overlayType.isForward) {
                    Alignment.CenterEnd
                } else {
                    Alignment.CenterStart
                }
            ).padding(horizontal = 60.dp)
        ) {
            if (overlayType is GestureOverlayType.DoubleTapSeek) {
                DoubleTapSeekPulse(
                    isForward = overlayType.isForward,
                    seconds = overlayType.seekSeconds
                )
            }
        }
    }
}

/**
 * Modern Left-side Vertical Brightness HUD with glowing electric blue track line
 */
@Composable
private fun VerticalBrightnessBar(percentage: Int) {
    val fraction = (percentage / 100f).coerceIn(0f, 1f)

    val icon = when {
        percentage < 33 -> Icons.Default.BrightnessLow
        percentage < 66 -> Icons.Default.BrightnessMedium
        else -> Icons.Default.BrightnessHigh
    }

    Box(
        modifier = Modifier
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xDD0A0F1E))
            .border(1.dp, Brush.verticalGradient(listOf(Color(0x6600E5FF), Color(0x221E293B))), RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .testTag("vertical_brightness_hud"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x2200E5FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Brightness",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Vertical glowing blue line track
            Canvas(
                modifier = Modifier
                    .width(8.dp)
                    .height(105.dp)
            ) {
                val trackWidth = size.width
                val trackHeight = size.height
                val cornerRadius = CornerRadius(trackWidth / 2f, trackWidth / 2f)

                // Unfilled track
                drawRoundRect(
                    color = Color(0x35FFFFFF),
                    topLeft = Offset(0f, 0f),
                    size = Size(trackWidth, trackHeight),
                    cornerRadius = cornerRadius
                )

                // Filled track from bottom up
                val filledHeight = trackHeight * fraction
                val filledTop = trackHeight - filledHeight

                if (filledHeight > 0f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF00E5FF), Color(0xFF0066FF))
                        ),
                        topLeft = Offset(0f, filledTop),
                        size = Size(trackWidth, filledHeight),
                        cornerRadius = cornerRadius
                    )

                    // Glowing bead at level position
                    drawCircle(
                        color = Color.White,
                        radius = 4.5.dp.toPx(),
                        center = Offset(trackWidth / 2f, filledTop)
                    )
                    drawCircle(
                        color = Color(0x5500E5FF),
                        radius = 7.dp.toPx(),
                        center = Offset(trackWidth / 2f, filledTop)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Percentage
            Text(
                text = "$percentage%",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

/**
 * Modern Right-side Vertical Volume HUD with glowing electric blue track line
 */
@Composable
private fun VerticalVolumeBar(percentage: Int) {
    val fraction = (percentage / 100f).coerceIn(0f, 1f)

    val icon = when {
        percentage <= 0 -> Icons.Default.VolumeMute
        percentage < 40 -> Icons.Default.VolumeDown
        else -> Icons.Default.VolumeUp
    }

    Box(
        modifier = Modifier
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xDD0A0F1E))
            .border(1.dp, Brush.verticalGradient(listOf(Color(0x660084FF), Color(0x221E293B))), RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .testTag("vertical_volume_hud"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x220084FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Volume",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Vertical glowing blue line track
            Canvas(
                modifier = Modifier
                    .width(8.dp)
                    .height(105.dp)
            ) {
                val trackWidth = size.width
                val trackHeight = size.height
                val cornerRadius = CornerRadius(trackWidth / 2f, trackWidth / 2f)

                // Unfilled track
                drawRoundRect(
                    color = Color(0x35FFFFFF),
                    topLeft = Offset(0f, 0f),
                    size = Size(trackWidth, trackHeight),
                    cornerRadius = cornerRadius
                )

                // Filled track from bottom up
                val filledHeight = trackHeight * fraction
                val filledTop = trackHeight - filledHeight

                if (filledHeight > 0f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF38BDF8), Color(0xFF0052FF))
                        ),
                        topLeft = Offset(0f, filledTop),
                        size = Size(trackWidth, filledHeight),
                        cornerRadius = cornerRadius
                    )

                    // Glowing bead at level position
                    drawCircle(
                        color = Color.White,
                        radius = 4.5.dp.toPx(),
                        center = Offset(trackWidth / 2f, filledTop)
                    )
                    drawCircle(
                        color = Color(0x550084FF),
                        radius = 7.dp.toPx(),
                        center = Offset(trackWidth / 2f, filledTop)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Percentage
            Text(
                text = "$percentage%",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Composable
private fun ScrubbingFeedbackPill(targetMs: Long, deltaSec: Int, totalMs: Long) {
    val sign = if (deltaSec >= 0) "+" else ""
    val formattedTarget = formatMs(targetMs)
    val formattedTotal = formatMs(totalMs)

    Box(
        modifier = Modifier
            .shadow(16.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xEE0B111E))
            .border(1.dp, Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFF0066FF))), RoundedCornerShape(20.dp))
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (deltaSec >= 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = "$sign${deltaSec}s",
                    color = Color(0xFF00E5FF),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$formattedTarget / $formattedTotal",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DoubleTapSeekPulse(isForward: Boolean, seconds: Int) {
    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(CircleShape)
            .background(Color(0x4400E5FF)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = "${seconds}s",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
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
