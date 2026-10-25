package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.data.local.SortOption
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.FolderItem
import com.example.data.model.VideoItem
import com.example.data.repository.VideoRepository
import com.example.ui.components.ExploreOnlineSheet
import com.example.ui.components.FolderCard
import com.example.ui.components.VideoCard
import com.example.ui.components.VideoInfoDialog
import com.example.ui.theme.ProBlue
import com.example.ui.theme.ProBlueDark
import kotlinx.coroutines.launch

enum class TopPillTab(val title: String, val icon: ImageVector) {
    FOLDERS("Folders", Icons.Default.Folder),
    ALL_VIDEOS("All Videos", Icons.Default.PlayCircle),
    MUSIC("Music", Icons.Default.MusicNote),
    APPS("Apps", Icons.Default.Apps)
}

enum class BottomNavTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    FOLDERS("Folders", Icons.Default.Folder),
    MY_LIST("My List", Icons.Default.Bookmark),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun LibraryScreen(
    videos: List<VideoItem>,
    folders: List<FolderItem>,
    userPreferences: UserPreferences,
    preferencesRepository: UserPreferencesRepository,
    repository: VideoRepository,
    onVideoClick: (List<VideoItem>, Int) -> Unit,
    onFolderClick: (FolderItem) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activePillTab by remember { mutableStateOf(TopPillTab.FOLDERS) }
    var activeBottomNav by remember { mutableStateOf(BottomNavTab.FOLDERS) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showExploreOnlineSheet by remember { mutableStateOf(false) }
    var selectedVideoForInfo by remember { mutableStateOf<VideoItem?>(null) }

    // SAF File Picker
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val pickedVideo = repository.getVideoMetadataFromUri(uri)
            onVideoClick(listOf(pickedVideo), 0)
        }
    }

    // Filtered videos
    val filteredVideos = remember(videos, searchQuery, userPreferences.sortBy) {
        val list = if (searchQuery.isBlank()) {
            videos
        } else {
            videos.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.folderName.contains(searchQuery, ignoreCase = true)
            }
        }
        when (userPreferences.sortBy) {
            SortOption.DATE_DESC -> list.sortedByDescending { it.dateAdded }
            SortOption.DATE_ASC -> list.sortedBy { it.dateAdded }
            SortOption.NAME_ASC -> list.sortedBy { it.title.lowercase() }
            SortOption.NAME_DESC -> list.sortedByDescending { it.title.lowercase() }
            SortOption.SIZE_DESC -> list.sortedByDescending { it.size }
            SortOption.DURATION_DESC -> list.sortedByDescending { it.duration }
        }
    }

    // My List (Favorites & Recent)
    val favoriteVideos = remember(videos) { videos.filter { it.isFavorite } }
    val recentVideos = remember(videos) { videos.filter { it.lastPlayedTimestamp > 0 }.sortedByDescending { it.lastPlayedTimestamp } }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 12.dp)
            ) {
                // Top Action Bar matching reference image
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search videos or folders...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                                .height(50.dp)
                                .testTag("library_search_input")
                        )
                    } else {
                        // Branding: Glowing Play Logo + "Ultra Player Pro"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag("app_brand_logo")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF38BDF8), ProBlue)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "Ultra Player ",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Pro",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ProBlue
                            )
                        }

                        // Top Right Action Icons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Grid / List View Toggle (4 square icon from reference)
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        preferencesRepository.updateIsGridView(!userPreferences.isGridView)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (userPreferences.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                    contentDescription = "Toggle Grid/List",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Search button (Magnifying glass)
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Overflow menu (3 dots)
                            Box {
                                IconButton(onClick = { showTopMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Menu",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                DropdownMenu(
                                    expanded = showTopMenu,
                                    onDismissRequest = { showTopMenu = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, tint = ProBlue) },
                                        text = { Text("Explore Online", color = MaterialTheme.colorScheme.onSurface) },
                                        onClick = {
                                            showTopMenu = false
                                            showExploreOnlineSheet = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.Sort, contentDescription = null) },
                                        text = { Text("Sort Videos", color = MaterialTheme.colorScheme.onSurface) },
                                        onClick = {
                                            showTopMenu = false
                                            showSortMenu = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (userPreferences.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                                contentDescription = null,
                                                tint = ProBlue
                                            )
                                        },
                                        text = {
                                            Text(
                                                text = if (userPreferences.isDarkTheme) "Switch to White Theme" else "Switch to Dark Theme",
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            showTopMenu = false
                                            coroutineScope.launch {
                                                preferencesRepository.updateDarkTheme(!userPreferences.isDarkTheme)
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                        text = { Text("Settings", color = MaterialTheme.colorScheme.onSurface) },
                                        onClick = {
                                            showTopMenu = false
                                            onOpenSettings()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Horizontal Category Pills Row (Folders, All Videos, Music, Apps) - Exact match to reference image!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopPillTab.values().forEach { tab ->
                        val isActive = activePillTab == tab

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(22.dp))
                                .clickable {
                                    activePillTab = tab
                                    if (tab == TopPillTab.APPS) {
                                        showExploreOnlineSheet = true
                                    }
                                }
                                .then(
                                    if (isActive) {
                                        Modifier
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF0070F3), Color(0xFF0051CC))
                                                )
                                            )
                                            .shadow(elevation = 6.dp, shape = RoundedCornerShape(22.dp))
                                    } else {
                                        Modifier
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                                shape = RoundedCornerShape(22.dp)
                                            )
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = tab.title,
                                    color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Modern Bottom Navigation Bar matching reference UI
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                BottomNavTab.values().forEach { tab ->
                    val isSelected = activeBottomNav == tab

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            activeBottomNav = tab
                            when (tab) {
                                BottomNavTab.HOME -> activePillTab = TopPillTab.ALL_VIDEOS
                                BottomNavTab.FOLDERS -> activePillTab = TopPillTab.FOLDERS
                                BottomNavTab.MY_LIST -> Unit
                                BottomNavTab.SETTINGS -> onOpenSettings()
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ProBlue,
                            selectedTextColor = ProBlue,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            indicatorColor = ProBlue.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    openDocumentLauncher.launch(arrayOf("video/*"))
                },
                containerColor = ProBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("open_video_file_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = "Open Video File"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Open File",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // If "My List" is selected from bottom nav
                activeBottomNav == BottomNavTab.MY_LIST -> {
                    MyListContent(
                        favoriteVideos = favoriteVideos,
                        recentVideos = recentVideos,
                        isGridView = userPreferences.isGridView,
                        onVideoClick = { list, idx -> onVideoClick(list, idx) },
                        onToggleFavorite = { vid -> coroutineScope.launch { repository.toggleFavorite(vid) } },
                        onShowInfo = { vid -> selectedVideoForInfo = vid }
                    )
                }
                // When Folders pill is active
                activePillTab == TopPillTab.FOLDERS -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(folders, key = { it.name }) { folder ->
                            FolderCard(
                                folder = folder,
                                onClick = { onFolderClick(folder) }
                            )
                        }
                    }
                }
                // When All Videos pill is active
                activePillTab == TopPillTab.ALL_VIDEOS -> {
                    if (userPreferences.isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredVideos.size, key = { filteredVideos[it].id }) { index ->
                                val video = filteredVideos[index]
                                VideoCard(
                                    video = video,
                                    isGrid = true,
                                    onClick = { onVideoClick(filteredVideos, index) },
                                    onToggleFavorite = { coroutineScope.launch { repository.toggleFavorite(video) } },
                                    onShowInfo = { selectedVideoForInfo = video }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredVideos.size, key = { filteredVideos[it].id }) { index ->
                                val video = filteredVideos[index]
                                VideoCard(
                                    video = video,
                                    isGrid = false,
                                    onClick = { onVideoClick(filteredVideos, index) },
                                    onToggleFavorite = { coroutineScope.launch { repository.toggleFavorite(video) } },
                                    onShowInfo = { selectedVideoForInfo = video }
                                )
                            }
                        }
                    }
                }
                // When Music pill is active
                activePillTab == TopPillTab.MUSIC -> {
                    MusicTabContent(
                        videos = filteredVideos,
                        onPlayAudio = { video -> onVideoClick(listOf(video), 0) }
                    )
                }
                // When Apps / Explore is active
                activePillTab == TopPillTab.APPS -> {
                    ExploreOnlineSheet(
                        featuredVideos = filteredVideos,
                        onPlayVideo = { video -> onVideoClick(listOf(video), 0) },
                        onDismiss = { activePillTab = TopPillTab.FOLDERS }
                    )
                }
            }

            // Explore Online Modal Sheet
            if (showExploreOnlineSheet) {
                ExploreOnlineSheet(
                    featuredVideos = filteredVideos,
                    onPlayVideo = { video -> onVideoClick(listOf(video), 0) },
                    onDismiss = { showExploreOnlineSheet = false }
                )
            }

            // Video Info Dialog
            selectedVideoForInfo?.let { video ->
                VideoInfoDialog(
                    video = video,
                    onDismiss = { selectedVideoForInfo = null }
                )
            }

            // Sort Menu Dialog
            if (showSortMenu) {
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    SortOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.title,
                                    color = if (userPreferences.sortBy == option) ProBlue else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (userPreferences.sortBy == option) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                showSortMenu = false
                                coroutineScope.launch {
                                    preferencesRepository.updateSortBy(option)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MyListContent(
    favoriteVideos: List<VideoItem>,
    recentVideos: List<VideoItem>,
    isGridView: Boolean,
    onVideoClick: (List<VideoItem>, Int) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onShowInfo: (VideoItem) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Favorites (${favoriteVideos.size})",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (favoriteVideos.isEmpty()) {
            item {
                Text(
                    text = "No favorite videos yet. Tap heart to add.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(favoriteVideos, key = { it.id }) { video ->
                VideoCard(
                    video = video,
                    isGrid = false,
                    onClick = {
                        val idx = favoriteVideos.indexOf(video)
                        onVideoClick(favoriteVideos, if (idx != -1) idx else 0)
                    },
                    onToggleFavorite = { onToggleFavorite(video) },
                    onShowInfo = { onShowInfo(video) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Recently Played (${recentVideos.size})",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (recentVideos.isEmpty()) {
            item {
                Text(
                    text = "No watch history yet.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(recentVideos, key = { it.id }) { video ->
                VideoCard(
                    video = video,
                    isGrid = false,
                    onClick = {
                        val idx = recentVideos.indexOf(video)
                        onVideoClick(recentVideos, if (idx != -1) idx else 0)
                    },
                    onToggleFavorite = { onToggleFavorite(video) },
                    onShowInfo = { onShowInfo(video) }
                )
            }
        }
    }
}

@Composable
private fun MusicTabContent(
    videos: List<VideoItem>,
    onPlayAudio: (VideoItem) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Audio & Music Streams",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Play media files in high-fidelity background audio mode",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(videos, key = { it.id }) { video ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlayAudio(video) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFA855F7), Color(0xFF7C3AED)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${video.formattedDuration} • Audio Stream",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ProBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = ProBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
