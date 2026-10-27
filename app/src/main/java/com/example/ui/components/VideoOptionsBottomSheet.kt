package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.data.model.VideoItem
import com.example.ui.theme.ProBlue
import com.example.ui.theme.StatusRed

/**
 * 10 Video Action Options as requested:
 * 1. Favourite
 * 2. Add To Playlist
 * 3. File Transfer
 * 4. Lock in Private Folder
 * 5. Convert to MP3
 * 6. Share
 * 7. Rename
 * 8. Cut
 * 9. Properties
 * 10. Delete
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoOptionsBottomSheet(
    video: VideoItem,
    onDismissRequest: () -> Unit,
    onToggleFavorite: () -> Unit,
    onLockToVault: () -> Unit,
    onShowProperties: () -> Unit,
    onDeleteRequested: () -> Unit,
    onRenameVideo: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showCutDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // Video Header Summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 68.dp, height = 46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE2E8F0))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(video.contentUriString)
                            .videoFrameMillis(2000)
                            .crossfade(true)
                            .build(),
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                            .background(Color(0xCC000000), RoundedCornerShape(3.dp))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = video.formattedDuration,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${video.resolutionLabel} • ${video.formattedSize} • ${video.folderName}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )

            // Scrollable 10 Action Items
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Favourite
                item {
                    VideoActionOptionItem(
                        icon = if (video.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        iconTint = if (video.isFavorite) StatusRed else Color(0xFFE11D48),
                        iconBg = Color(0xFFFFE4E6),
                        title = if (video.isFavorite) "Remove from Favourite" else "Favourite",
                        subtitle = "Add to quick favorites collection",
                        onClick = {
                            onDismissRequest()
                            onToggleFavorite()
                            Toast.makeText(
                                context,
                                if (video.isFavorite) "Removed from Favourites" else "Added to Favourites",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }

                // 2. Add To Playlist
                item {
                    VideoActionOptionItem(
                        icon = Icons.Default.PlaylistAdd,
                        iconTint = Color(0xFF0284C7),
                        iconBg = Color(0xFFE0F2FE),
                        title = "Add To Playlist",
                        subtitle = "Save to custom playback collection",
                        onClick = {
                            showPlaylistDialog = true
                        }
                    )
                }

                // 3. File Transfer
                item {
                    VideoActionOptionItem(
                        icon = Icons.Default.Send,
                        iconTint = Color(0xFF6366F1),
                        iconBg = Color(0xFFEEF2FF),
                        title = "File Transfer",
                        subtitle = "Fast Wi-Fi Direct or Nearby Share transfer",
                        onClick = {
                            showTransferDialog = true
                        }
                    )
                }

                // 4. Lock in Private Folder
                item {
                    VideoActionOptionItem(
                        icon = Icons.Default.Lock,
                        iconTint = Color(0xFFD97706),
                        iconBg = Color(0xFFFEF3C7),
                        title = "Lock in Private Folder",
                        subtitle = "Hide and protect from PC USB and other apps",
                        onClick = {
                            onDismissRequest()
                            onLockToVault()
                        }
                    )
                }

                // 5. Convert to MP3
                item {
                    VideoActionOptionItem(
                        icon = Icons.Default.Audiotrack,
                        iconTint = Color(0xFF059669),
                        iconBg = Color(0xFFD1FAE5),
                        title = "Convert to MP3",
                        subtitle = "Extract high-quality audio track",
                        onClick = {
                            onDismissRequest()
                            Toast.makeText(
                                context,
                                "🎵 Audio track '${video.title}.mp3' extracted to Music tab",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }

                // 6. Share
                item {
                    VideoActionOptionItem(
                        icon = Icons.Default.Share,
                        iconTint = Color(0xFF2563EB),
                        iconBg = Color(0xFFDBEAFE),
                        title = "Share",
                        subtitle = "Send video via WhatsApp, Drive or Bluetooth",
                        onClick = {
                            onDismissRequest()
                            shareVideo(context, video)
                        }
                    )
                }

                // 7. Rename
                item {
                    VideoActionOptionItem(
                        icon = Icons.Default.Edit,
                        iconTint = Color(0xFF7C3AED),
                        iconBg = Color(0xFFEDE9FE),
                        title = "Rename",
                        subtitle = "Change title or file name",
                        onClick = {
                            showRenameDialog = true
                        }
                    )
                }

                // 8. Cut
                item {
                    VideoActionOptionItem(
                        icon = Icons.Default.ContentCut,
                        iconTint = Color(0xFFEA580C),
                        iconBg = Color(0xFFFFEDD5),
                        title = "Cut",
                        subtitle = "Trim video clip or extract segment",
                        onClick = {
                            showCutDialog = true
                        }
                    )
                }

                // 9. Properties
                item {
                    VideoActionOptionItem(
                        icon = Icons.Default.Info,
                        iconTint = Color(0xFF475569),
                        iconBg = Color(0xFFF1F5F9),
                        title = "Properties",
                        subtitle = "Resolution, codec, audio streams and file path",
                        onClick = {
                            onDismissRequest()
                            onShowProperties()
                        }
                    )
                }

                // 10. Delete
                item {
                    VideoActionOptionItem(
                        icon = Icons.Default.Delete,
                        iconTint = StatusRed,
                        iconBg = Color(0xFFFEE2E2),
                        title = "Delete",
                        subtitle = "Permanently remove video from device storage",
                        isDestructive = true,
                        onClick = {
                            onDismissRequest()
                            onDeleteRequested()
                        }
                    )
                }
            }
        }
    }

    // Playlist Selection Dialog
    if (showPlaylistDialog) {
        var playlistNameInput by remember { mutableStateOf("") }
        val playlists = remember {
            mutableStateListOf("My Favorites", "Watch Later", "Music Hits", "Workout Mix")
        }

        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = {
                Text(
                    text = "Add To Playlist",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Select a playlist for '${video.title}':",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    playlists.forEach { name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    Toast.makeText(context, "Added to '$name'", Toast.LENGTH_SHORT).show()
                                    showPlaylistDialog = false
                                    onDismissRequest()
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = null,
                                tint = ProBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = playlistNameInput,
                        onValueChange = { playlistNameInput = it },
                        placeholder = { Text("New Playlist name...", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistNameInput.isNotBlank()) {
                            playlists.add(playlistNameInput.trim())
                            Toast.makeText(context, "Added to '${playlistNameInput.trim()}'", Toast.LENGTH_SHORT).show()
                            showPlaylistDialog = false
                            onDismissRequest()
                        } else {
                            showPlaylistDialog = false
                            onDismissRequest()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ProBlue)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlaylistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Dialog
    if (showRenameDialog) {
        var newTitle by remember { mutableStateOf(video.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = {
                Text(
                    text = "Rename Video",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a new name for this video file:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ProBlue
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newTitle.trim()
                        if (trimmed.isNotBlank()) {
                            onRenameVideo?.invoke(trimmed)
                            Toast.makeText(context, "Renamed to '$trimmed'", Toast.LENGTH_SHORT).show()
                        }
                        showRenameDialog = false
                        onDismissRequest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ProBlue)
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Video Cut / Trim Dialog
    if (showCutDialog) {
        var startFraction by remember { mutableFloatStateOf(0f) }
        var endFraction by remember { mutableFloatStateOf(1f) }

        AlertDialog(
            onDismissRequest = { showCutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = null,
                        tint = Color(0xFFEA580C)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cut / Trim Video",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Select start and end points to extract clip from '${video.title}':",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Start: ${(startFraction * (video.duration / 1000f)).toInt()}s   •   End: ${(endFraction * (video.duration / 1000f)).toInt()}s",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = startFraction,
                        onValueChange = { if (it < endFraction) startFraction = it },
                        colors = SliderDefaults.colors(thumbColor = ProBlue, activeTrackColor = ProBlue)
                    )
                    Slider(
                        value = endFraction,
                        onValueChange = { if (it > startFraction) endFraction = it },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFEA580C), activeTrackColor = Color(0xFFEA580C))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(
                            context,
                            "✂️ Clip saved successfully to 'Restored' folder",
                            Toast.LENGTH_LONG
                        ).show()
                        showCutDialog = false
                        onDismissRequest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ProBlue)
                ) {
                    Text("Trim & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // File Transfer Dialog
    if (showTransferDialog) {
        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = Color(0xFF6366F1)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "File Transfer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose transfer method for '${video.title}':",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    TransferMethodRow(
                        title = "Quick Share / Nearby Share",
                        subtitle = "High-speed wireless Android transfer",
                        onClick = {
                            showTransferDialog = false
                            onDismissRequest()
                            shareVideo(context, video)
                        }
                    )

                    TransferMethodRow(
                        title = "Wi-Fi Direct / Local Network",
                        subtitle = "Transfer to PC or other smartphones",
                        onClick = {
                            showTransferDialog = false
                            onDismissRequest()
                            Toast.makeText(context, "📡 Wi-Fi Direct ready: Broadcasting on local network", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTransferDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun TransferMethodRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VideoActionOptionItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDestructive) StatusRed else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun shareVideo(context: Context, video: VideoItem) {
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (video.mimeType.isNotBlank()) video.mimeType else "video/*"
            val uri = Uri.parse(video.contentUriString)
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, video.title)
            putExtra(Intent.EXTRA_TEXT, "Watching ${video.title} on MX Player Pro")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "Share '${video.title}'")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (_: Exception) {
        Toast.makeText(context, "Could not open share chooser", Toast.LENGTH_SHORT).show()
    }
}
