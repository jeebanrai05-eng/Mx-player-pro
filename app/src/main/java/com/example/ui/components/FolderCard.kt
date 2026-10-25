package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FolderItem
import com.example.ui.theme.FolderCameraGrad
import com.example.ui.theme.FolderGenericGrad
import com.example.ui.theme.FolderRecordGrad
import com.example.ui.theme.FolderRestoredGrad
import com.example.ui.theme.FolderSampleGrad
import com.example.ui.theme.FolderShowsGrad
import com.example.ui.theme.FolderWhatsappGifGrad
import com.example.ui.theme.FolderWhatsappVideoGrad
import com.example.ui.theme.StatusRed

@Composable
fun FolderCard(
    folder: FolderItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (gradientColors, iconVector, badgeText) = getFolderIconAttributes(folder.name, folder.videoCount)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("folder_item_${folder.name}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rounded folder icon with gradient
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(gradientColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = folder.name,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Notification badge (like WhatsApp Videos badge in reference image)
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(StatusRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (folder.name.contains("Show", ignoreCase = true) || folder.name.contains("Movie", ignoreCase = true)) {
                        "Tap to Watch Now"
                    } else {
                        "${folder.videoCount} ${if (folder.videoCount == 1) "video" else "videos"}"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Open folder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

private fun getFolderIconAttributes(name: String, count: Int): Triple<List<Color>, ImageVector, String?> {
    val lower = name.lowercase()
    return when {
        lower.contains("camera") || lower.contains("dcim") -> {
            Triple(FolderCameraGrad, Icons.Default.CameraAlt, null)
        }
        lower.contains("record") || lower.contains("screen") -> {
            Triple(FolderRecordGrad, Icons.Default.Videocam, null)
        }
        lower.contains("restore") || lower.contains("download") -> {
            Triple(FolderRestoredGrad, Icons.Default.Download, null)
        }
        lower.contains("whatsapp animated") || lower.contains("gif") -> {
            Triple(FolderWhatsappGifGrad, Icons.Default.ChatBubble, null)
        }
        lower.contains("whatsapp") || lower.contains("social") -> {
            Triple(FolderWhatsappVideoGrad, Icons.Default.PlayArrow, if (count > 0) count.coerceAtMost(99).toString() else null)
        }
        lower.contains("hindi") || lower.contains("show") || lower.contains("movie") || lower.contains("tv") -> {
            Triple(FolderShowsGrad, Icons.Default.Tv, null)
        }
        lower.contains("sample") || lower.contains("demo") -> {
            Triple(FolderSampleGrad, Icons.Default.Folder, null)
        }
        else -> {
            Triple(FolderGenericGrad, Icons.Default.Folder, null)
        }
    }
}
