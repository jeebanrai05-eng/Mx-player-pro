package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun CinematicSeekerBar(
    currentPositionMs: Long,
    totalDurationMs: Long,
    bufferedPositionMs: Long = 0L,
    modifier: Modifier = Modifier,
    onSeek: (Long) -> Unit
) {
    val density = LocalDensity.current

    var isDragging by remember { mutableStateOf(false) }
    var dragProgressFraction by remember { mutableFloatStateOf(0f) }

    val actualProgressFraction = if (totalDurationMs > 0) {
        (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val displayFraction = if (isDragging) dragProgressFraction else actualProgressFraction

    val bufferedFraction = if (totalDurationMs > 0) {
        (bufferedPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // Thumb sizing animation on press/drag
    val outerGlowRadiusDp by animateDpAsState(
        targetValue = if (isDragging) 11.dp else 8.dp,
        animationSpec = tween(durationMillis = 150),
        label = "thumb_glow"
    )
    val innerCoreRadiusDp by animateDpAsState(
        targetValue = if (isDragging) 6.dp else 4.5.dp,
        animationSpec = tween(durationMillis = 150),
        label = "thumb_core"
    )

    val activeGradient = remember {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF00E5FF),
                Color(0xFF0080FF),
                Color(0xFF0055FF)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .testTag("cinematic_seeker_bar"),
        contentAlignment = Alignment.Center
    ) {
        // Floating preview bubble while scrubbing
        if (isDragging && totalDurationMs > 0) {
            val previewMs = (displayFraction * totalDurationMs).toLong()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-28).dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xEE0B111E),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
                ) {
                    Text(
                        text = formatSeekTime(previewMs),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .pointerInput(totalDurationMs) {
                    detectTapGestures { offset ->
                        if (totalDurationMs > 0 && size.width > 0) {
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            onSeek((fraction * totalDurationMs).toLong())
                        }
                    }
                }
                .pointerInput(totalDurationMs) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            if (size.width > 0) {
                                dragProgressFraction = (offset.x / size.width).coerceIn(0f, 1f)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (size.width > 0) {
                                dragProgressFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            if (totalDurationMs > 0) {
                                onSeek((dragProgressFraction * totalDurationMs).toLong())
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val trackHeightPx = 5.dp.toPx()
            val trackTop = (canvasHeight - trackHeightPx) / 2f
            val cornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)

            // 1. Inactive Background Track (Translucent Dark Gray)
            drawRoundRect(
                color = Color(0x40FFFFFF),
                topLeft = Offset(0f, trackTop),
                size = Size(canvasWidth, trackHeightPx),
                cornerRadius = cornerRadius
            )

            // 2. Buffered Track (Lighter Translucent White)
            if (bufferedFraction > 0f) {
                val bufferedWidth = canvasWidth * bufferedFraction
                drawRoundRect(
                    color = Color(0x60FFFFFF),
                    topLeft = Offset(0f, trackTop),
                    size = Size(bufferedWidth, trackHeightPx),
                    cornerRadius = cornerRadius
                )
            }

            // 3. Active Played Track (Electric Blue Gradient with Glow)
            val activeWidth = canvasWidth * displayFraction
            if (activeWidth > 0f) {
                drawRoundRect(
                    brush = activeGradient,
                    topLeft = Offset(0f, trackTop),
                    size = Size(activeWidth, trackHeightPx),
                    cornerRadius = cornerRadius
                )
            }

            // 4. Floating Glowing Thumb
            val thumbCenterX = (canvasWidth * displayFraction).coerceIn(0f, canvasWidth)
            val thumbCenterY = canvasHeight / 2f

            val outerGlowPx = outerGlowRadiusDp.toPx()
            val innerCorePx = innerCoreRadiusDp.toPx()

            // Outer Neon Halo
            drawCircle(
                color = Color(0x5500E5FF),
                radius = outerGlowPx + 4.dp.toPx(),
                center = Offset(thumbCenterX, thumbCenterY)
            )

            // Mid Cyan Aura
            drawCircle(
                color = Color(0xAA0099FF),
                radius = outerGlowPx,
                center = Offset(thumbCenterX, thumbCenterY)
            )

            // Inner Core (Pure Solid White)
            drawCircle(
                color = Color.White,
                radius = innerCorePx,
                center = Offset(thumbCenterX, thumbCenterY)
            )
        }
    }
}

private fun formatSeekTime(ms: Long): String {
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
