package com.example.ui.screens

import android.app.Activity
import android.media.MediaScannerConnection
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.ui.components.BatchTransferMode
import com.example.ui.components.FolderPickerPasteDialog
import com.example.ui.components.MultiSelectionBottomBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VideoItem
import com.example.data.repository.VideoRepository
import com.example.ui.components.VideoCard
import com.example.ui.components.VideoInfoDialog
import com.example.ui.components.VideoOptionsBottomSheet
import com.example.ui.theme.ProBlue
import com.example.ui.theme.StatusRed
import kotlinx.coroutines.launch

private enum class FolderBottomTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    FOLDERS("Folders", Icons.Default.Folder),
    MY_LIST("My List", Icons.Default.Bookmark),
    PRIVATE("Private", Icons.Default.Lock),
    SETTINGS("Settings", Icons.Default.Settings)
}

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
    onNavigateBack: () -> Unit,
    onOpenSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Multi-Selection Engine States
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedVideoIds by remember { mutableStateOf(setOf<Long>()) }
    var batchTransferMode by remember { mutableStateOf<BatchTransferMode?>(null) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showBatchPropertiesDialog by remember { mutableStateOf(false) }

    val exitSelectionMode: () -> Unit = {
        isSelectionMode = false
        selectedVideoIds = emptySet()
    }

    val toggleVideoSelection: (Long) -> Unit = { id ->
        selectedVideoIds = if (selectedVideoIds.contains(id)) {
            val next = selectedVideoIds - id
            if (next.isEmpty()) isSelectionMode = false
            next
        } else {
            selectedVideoIds + id
        }
    }

    val startVideoSelection: (Long) -> Unit = { id ->
        isSelectionMode = true
        selectedVideoIds = selectedVideoIds + id
    }

    BackHandler {
        if (isSelectionMode) {
            exitSelectionMode()
        } else if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        } else {
            onNavigateBack()
        }
    }

    var selectedVideoForInfo by remember { mutableStateOf<VideoItem?>(null) }
    var selectedVideoForOptions by remember { mutableStateOf<VideoItem?>(null) }
    var videoPendingDelete by remember { mutableStateOf<VideoItem?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val displayedVideos = remember(videos, searchQuery) {
        if (searchQuery.isBlank()) {
            videos
        } else {
            videos.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    val selectedVideos = remember(videos, selectedVideoIds) {
        videos.filter { selectedVideoIds.contains(it.id) }
    }

    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "Deleted selected video(s)", Toast.LENGTH_SHORT).show()
        }
    }

    val onShareSelected: () -> Unit = {
        if (selectedVideos.isNotEmpty()) {
            val uris = ArrayList<Uri>()
            selectedVideos.forEach { v ->
                try {
                    uris.add(Uri.parse(v.contentUriString))
                } catch (e: Exception) {
                    // ignore
                }
            }
            if (uris.size == 1) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "video/*"
                    putExtra(Intent.EXTRA_STREAM, uris[0])
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share video"))
            } else if (uris.size > 1) {
                val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "video/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share ${uris.size} videos"))
            }
        }
    }

    val onBatchFavoriteToggle: () -> Unit = {
        coroutineScope.launch {
            selectedVideos.forEach { v ->
                onToggleFavorite(v)
            }
            Toast.makeText(context, "Favorites updated for ${selectedVideos.size} videos", Toast.LENGTH_SHORT).show()
        }
    }

    val onBatchLockToVault: () -> Unit = {
        coroutineScope.launch {
            val isConfigured = repository.vaultManager.isPinConfigured()
            if (!isConfigured) {
                Toast.makeText(context, "Please set a 4-digit Vault PIN first", Toast.LENGTH_LONG).show()
                onOpenPrivateVault()
            } else {
                val count = repository.batchLockToVault(selectedVideos)
                Toast.makeText(context, "Moved $count videos to Private Vault", Toast.LENGTH_SHORT).show()
                exitSelectionMode()
            }
        }
    }

    val onConfirmBatchDelete: () -> Unit = {
        coroutineScope.launch {
            val count = selectedVideos.size
            val sender = repository.createBatchDeleteIntentSender(selectedVideos)
            if (sender != null) {
                deleteRequestLauncher.launch(
                    IntentSenderRequest.Builder(sender).build()
                )
            } else {
                val deleted = repository.deleteMultipleVideos(selectedVideos)
                Toast.makeText(context, "Deleted $deleted videos", Toast.LENGTH_SHORT).show()
            }
            showBatchDeleteConfirm = false
            exitSelectionMode()
        }
    }

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
            if (isSelectionMode) {
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = {
                        Text(
                            text = "${selectedVideoIds.size} Selected",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { exitSelectionMode() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit selection",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        val allSelected = displayedVideos.isNotEmpty() && selectedVideoIds.size >= displayedVideos.size
                        IconButton(
                            onClick = {
                                if (allSelected) {
                                    selectedVideoIds = emptySet()
                                    isSelectionMode = false
                                } else {
                                    selectedVideoIds = displayedVideos.map { it.id }.toSet()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = if (allSelected) "Deselect All" else "Select All",
                                tint = ProBlue
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else if (isSearchActive) {
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search in $folderName...", fontSize = 14.sp) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = ProBlue)
                            },
                            trailingIcon = {
                                IconButton(onClick = {
                                    isSearchActive = false
                                    searchQuery = ""
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close search")
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = ProBlue,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else {
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = {
                        Column {
                            Text(
                                text = folderName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${displayedVideos.size} videos",
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
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
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
            }
        },
        bottomBar = {
            if (isSelectionMode) {
                MultiSelectionBottomBar(
                    selectedCount = selectedVideoIds.size,
                    onShare = { onShareSelected() },
                    onMove = { batchTransferMode = BatchTransferMode.MOVE },
                    onCopy = { batchTransferMode = BatchTransferMode.COPY },
                    onLockToVault = { onBatchLockToVault() },
                    onDelete = { showBatchDeleteConfirm = true },
                    onToggleFavorite = { onBatchFavoriteToggle() },
                    onShowProperties = { showBatchPropertiesDialog = true }
                )
            } else {
                // Down Bar (Bottom Navigation Bar) matching MX Player Pro
                SurfaceBottomBar(
                    activeTab = FolderBottomTab.FOLDERS,
                    onTabClick = { tab ->
                        when (tab) {
                            FolderBottomTab.HOME -> onNavigateBack()
                            FolderBottomTab.FOLDERS -> onNavigateBack()
                            FolderBottomTab.MY_LIST -> onNavigateBack()
                            FolderBottomTab.PRIVATE -> onOpenPrivateVault()
                            FolderBottomTab.SETTINGS -> onOpenSettings?.invoke() ?: onNavigateBack()
                        }
                    }
                )
            }
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
                    items(displayedVideos, key = { "folder_grid_${it.id}" }) { video ->
                        VideoCard(
                            video = video,
                            isGrid = true,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedVideoIds.contains(video.id),
                            onToggleSelect = { toggleVideoSelection(video.id) },
                            onLongClick = { startVideoSelection(video.id) },
                            onClick = {
                                if (isSelectionMode) {
                                    toggleVideoSelection(video.id)
                                } else {
                                    onVideoClick(video)
                                }
                            },
                            onToggleFavorite = { onToggleFavorite(video) },
                            onShowInfo = { selectedVideoForInfo = video },
                            onDeleteClick = { onDeleteRequested(video) },
                            onLockToVault = { onLockToVaultRequested(video) },
                            onOptionsClick = { selectedVideoForOptions = video }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedVideos, key = { "folder_list_${it.id}" }) { video ->
                        VideoCard(
                            video = video,
                            isGrid = false,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedVideoIds.contains(video.id),
                            onToggleSelect = { toggleVideoSelection(video.id) },
                            onLongClick = { startVideoSelection(video.id) },
                            onClick = {
                                if (isSelectionMode) {
                                    toggleVideoSelection(video.id)
                                } else {
                                    onVideoClick(video)
                                }
                            },
                            onToggleFavorite = { onToggleFavorite(video) },
                            onShowInfo = { selectedVideoForInfo = video },
                            onDeleteClick = { onDeleteRequested(video) },
                            onLockToVault = { onLockToVaultRequested(video) },
                            onOptionsClick = { selectedVideoForOptions = video }
                        )
                    }
                }
            }

            // Video Options Bottom Sheet (10 Actions)
            if (selectedVideoForOptions != null) {
                VideoOptionsBottomSheet(
                    video = selectedVideoForOptions!!,
                    onDismissRequest = { selectedVideoForOptions = null },
                    onToggleFavorite = {
                        val v = selectedVideoForOptions ?: return@VideoOptionsBottomSheet
                        onToggleFavorite(v)
                    },
                    onLockToVault = {
                        val v = selectedVideoForOptions ?: return@VideoOptionsBottomSheet
                        onLockToVaultRequested(v)
                    },
                    onShowProperties = {
                        val v = selectedVideoForOptions ?: return@VideoOptionsBottomSheet
                        selectedVideoForInfo = v
                    },
                    onDeleteRequested = {
                        val v = selectedVideoForOptions ?: return@VideoOptionsBottomSheet
                        onDeleteRequested(v)
                    },
                    onRenameVideo = { newTitle ->
                        val v = selectedVideoForOptions ?: return@VideoOptionsBottomSheet
                        coroutineScope.launch { repository.renameVideo(v, newTitle) }
                    }
                )
            }

            // Video Details Dialog
            if (selectedVideoForInfo != null) {
                VideoInfoDialog(
                    video = selectedVideoForInfo!!,
                    onDismiss = { selectedVideoForInfo = null }
                )
            }

            // Delete Confirmation Dialog
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

            // Batch Folder Move / Copy Paste Dialog
            batchTransferMode?.let { mode ->
                FolderPickerPasteDialog(
                    transferMode = mode,
                    selectedCount = selectedVideos.size,
                    availableFolders = repository.getAvailableFolders(videos),
                    onConfirm = { targetFolderName ->
                        coroutineScope.launch {
                            val count = if (mode == BatchTransferMode.MOVE) {
                                repository.batchMoveVideos(selectedVideos, targetFolderName)
                            } else {
                                repository.batchCopyVideos(selectedVideos, targetFolderName)
                            }
                            val action = if (mode == BatchTransferMode.MOVE) "Moved" else "Copied"
                            Toast.makeText(context, "$action $count videos to '$targetFolderName'", Toast.LENGTH_SHORT).show()
                            batchTransferMode = null
                            exitSelectionMode()
                        }
                    },
                    onDismiss = { batchTransferMode = null }
                )
            }

            // Batch Delete Confirmation Dialog
            if (showBatchDeleteConfirm && selectedVideos.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { showBatchDeleteConfirm = false },
                    title = {
                        Text("Delete ${selectedVideos.size} Videos?", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Text("Are you sure you want to permanently delete these ${selectedVideos.size} selected videos from your device storage? This action cannot be undone.")
                    },
                    confirmButton = {
                        Button(
                            onClick = { onConfirmBatchDelete() },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                        ) {
                            Text("Delete", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBatchDeleteConfirm = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Batch Properties Dialog
            if (showBatchPropertiesDialog && selectedVideos.isNotEmpty()) {
                val totalSize = selectedVideos.sumOf { it.size }
                val totalDurationMs = selectedVideos.sumOf { it.duration }
                val formattedSize = remember(totalSize) {
                    val mb = totalSize.toDouble() / (1024 * 1024)
                    if (mb >= 1024) {
                        String.format("%.2f GB", mb / 1024)
                    } else {
                        String.format("%.1f MB", mb)
                    }
                }
                val formattedDuration = remember(totalDurationMs) {
                    val totalSecs = totalDurationMs / 1000
                    val hrs = totalSecs / 3600
                    val mins = (totalSecs % 3600) / 60
                    val secs = totalSecs % 60
                    if (hrs > 0) String.format("%02d:%02d:%02d", hrs, mins, secs)
                    else String.format("%02d:%02d", mins, secs)
                }

                AlertDialog(
                    onDismissRequest = { showBatchPropertiesDialog = false },
                    title = {
                        Text("Batch Selection Properties", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Selected Items: ${selectedVideos.size} videos", fontWeight = FontWeight.Medium)
                            Text("Total File Size: $formattedSize")
                            Text("Total Playtime: $formattedDuration")
                            Text("Folder: $folderName")
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showBatchPropertiesDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = ProBlue)
                        ) {
                            Text("OK", color = Color.White)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SurfaceBottomBar(
    activeTab: FolderBottomTab,
    onTabClick: (FolderBottomTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FolderBottomTab.values().forEach { tab ->
                val isSelected = activeTab == tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabClick(tab) }
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = if (isSelected) ProBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tab.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) ProBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
