package com.example.ui.screens

import android.app.Activity
import android.media.MediaScannerConnection
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VideoItem
import com.example.data.repository.VideoRepository
import com.example.ui.components.VideoCard
import com.example.ui.components.VideoInfoDialog
import com.example.ui.theme.StatusRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderName: String,
    videos: List<VideoItem>,
    repository: VideoRepository,
    isGridView: Boolean,
    onToggleGridView: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onOpenPrivateVault: () -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedVideoForInfo by remember { mutableStateOf<VideoItem?>(null) }
    var videoPendingDelete by remember { mutableStateOf<VideoItem?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val onDeleteRequested: (VideoItem) -> Unit = { video ->
        videoPendingDelete = video
        showDeleteConfirmDialog = true
    }

    val onLockToVaultRequested: (VideoItem) -> Unit = { video ->
        coroutineScope.launch {
            val isConfigured = repository.vaultManager.isPinConfigured()
            if (!isConfigured) {
                Toast.makeText(context, "Please set a 4-digit Vault PIN first", Toast.LENGTH_LONG).show()
                onOpenPrivateVault()
            } else {
                val result = repository.vaultManager.lockVideo(video)
                if (result.isSuccess) {
                    repository.vaultManager.deleteOriginalMedia(video)
                    repository.notifyVideoDeleted(video.contentUriString)
                    Toast.makeText(context, "Moved '${video.title}' to Private Vault & hidden from device", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to move video to vault", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = folderName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${videos.size} videos",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleGridView) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle Grid / List",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(videos, key = { it.id }) { video ->
                        VideoCard(
                            video = video,
                            isGrid = true,
                            onClick = { onVideoClick(video) },
                            onToggleFavorite = { onToggleFavorite(video) },
                            onShowInfo = { selectedVideoForInfo = video },
                            onDeleteClick = { onDeleteRequested(video) },
                            onLockToVault = { onLockToVaultRequested(video) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(videos, key = { it.id }) { video ->
                        VideoCard(
                            video = video,
                            isGrid = false,
                            onClick = { onVideoClick(video) },
                            onToggleFavorite = { onToggleFavorite(video) },
                            onShowInfo = { selectedVideoForInfo = video },
                            onDeleteClick = { onDeleteRequested(video) },
                            onLockToVault = { onLockToVaultRequested(video) }
                        )
                    }
                }
            }

            if (selectedVideoForInfo != null) {
                VideoInfoDialog(
                    video = selectedVideoForInfo!!,
                    onDismiss = { selectedVideoForInfo = null }
                )
            }

            if (showDeleteConfirmDialog && videoPendingDelete != null) {
                val target = videoPendingDelete!!
                AlertDialog(
                    onDismissRequest = {
                        showDeleteConfirmDialog = false
                        videoPendingDelete = null
                    },
                    title = { Text("Delete Video?") },
                    text = {
                        Text("Are you sure you want to delete '${target.title}'? This action will permanently remove the video from your device storage.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteConfirmDialog = false
                                coroutineScope.launch {
                                    repository.deleteVideo(target)
                                    repository.notifyVideoDeleted(target.contentUriString)
                                    Toast.makeText(context, "Video deleted from device storage", Toast.LENGTH_SHORT).show()
                                    videoPendingDelete = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                        ) {
                            Text("Delete", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDeleteConfirmDialog = false
                                videoPendingDelete = null
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
