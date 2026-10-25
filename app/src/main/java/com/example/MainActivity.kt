package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.FolderItem
import com.example.data.model.VideoItem
import com.example.data.repository.VideoRepository
import com.example.ui.components.PinLockScreen
import com.example.ui.screens.FolderDetailScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.PrivateVaultScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.UltraPlayerTheme
import kotlinx.coroutines.launch

sealed class Screen {
    object Library : Screen()
    data class FolderDetail(val folderName: String) : Screen()
    data class Player(val videoList: List<VideoItem>, val startIndex: Int) : Screen()
    object Settings : Screen()
    object PrivateVault : Screen()
}

class MainActivity : ComponentActivity() {

    private lateinit var videoRepository: VideoRepository
    private lateinit var preferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enableHighRefreshRate()

        videoRepository = VideoRepository(this)
        preferencesRepository = UserPreferencesRepository(this)

        // Configure Coil with VideoFrameDecoder for thumbnail extraction
        val imageLoader = coil.ImageLoader.Builder(this)
            .components {
                add(coil.decode.VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
        coil.Coil.setImageLoader(imageLoader)

        setContent {
            val userPreferences by preferencesRepository.userPreferencesFlow.collectAsState(
                initial = UserPreferences()
            )

            val accentColor = when (userPreferences.accentColorName) {
                "Purple" -> AccentPurple
                "Green" -> AccentGreen
                "Orange" -> AccentOrange
                "Blue" -> com.example.ui.theme.ProBlue
                "Pink" -> AccentPink
                else -> com.example.ui.theme.ProBlue
            }

            UltraPlayerTheme(
                darkTheme = userPreferences.isDarkTheme,
                accentColor = accentColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UltraVideoApp(
                        videoRepository = videoRepository,
                        preferencesRepository = preferencesRepository,
                        userPreferences = userPreferences,
                        intent = intent
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun enableHighRefreshRate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = display
                val modes = display?.supportedModes ?: emptyArray()
                val highRefreshMode = modes.maxByOrNull { it.refreshRate }
                if (highRefreshMode != null && highRefreshMode.refreshRate >= 80f) {
                    val params = window.attributes
                    params.preferredDisplayModeId = highRefreshMode.modeId
                    window.attributes = params
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val display = windowManager.defaultDisplay
                val modes = display?.supportedModes ?: emptyArray()
                val highRefreshMode = modes.maxByOrNull { it.refreshRate }
                if (highRefreshMode != null && highRefreshMode.refreshRate >= 80f) {
                    val params = window.attributes
                    params.preferredDisplayModeId = highRefreshMode.modeId
                    window.attributes = params
                }
            }
        } catch (_: Exception) {}
    }
}

@Composable
fun UltraVideoApp(
    videoRepository: VideoRepository,
    preferencesRepository: UserPreferencesRepository,
    userPreferences: UserPreferences,
    intent: Intent?
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // PIN lock authorization state
    var isPinAuthorized by remember {
        mutableStateOf(!userPreferences.isPinLockEnabled)
    }

    // Permission state
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permissionToRequest) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(permissionToRequest)
        }
    }

    // Video & Folder data observation
    val allVideos by videoRepository.observeAllVideosWithRecords(includeSamplesIfEmpty = true)
        .collectAsState(initial = videoRepository.sampleVideos)

    val folderList = remember(allVideos) {
        videoRepository.buildFolderList(allVideos)
    }

    // Navigation screen state
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Library) }

    // Handle incoming video VIEW intent
    LaunchedEffect(intent) {
        intent?.data?.let { uri ->
            if (intent.action == Intent.ACTION_VIEW) {
                val openedVideo = videoRepository.getVideoMetadataFromUri(uri)
                currentScreen = Screen.Player(listOf(openedVideo), 0)
            }
        }
    }

    // PIN Lock Gate
    if (userPreferences.isPinLockEnabled && !isPinAuthorized) {
        PinLockScreen(
            expectedPin = userPreferences.pinLockCode,
            onUnlocked = { isPinAuthorized = true }
        )
        return
    }

    // Main App Navigation
    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
        when (screen) {
            is Screen.Library -> {
                LibraryScreen(
                    videos = allVideos,
                    folders = folderList,
                    userPreferences = userPreferences,
                    preferencesRepository = preferencesRepository,
                    repository = videoRepository,
                    onVideoClick = { list, index ->
                        currentScreen = Screen.Player(list, index)
                    },
                    onFolderClick = { folder ->
                        currentScreen = Screen.FolderDetail(folder.name)
                    },
                    onOpenSettings = {
                        currentScreen = Screen.Settings
                    },
                    onOpenPrivateVault = {
                        currentScreen = Screen.PrivateVault
                    }
                )
            }
            is Screen.FolderDetail -> {
                val folderVideos = allVideos.filter { it.folderName == screen.folderName }
                FolderDetailScreen(
                    folderName = screen.folderName,
                    videos = folderVideos,
                    isGridView = userPreferences.isGridView,
                    onToggleGridView = {
                        coroutineScope.launch {
                            preferencesRepository.updateIsGridView(!userPreferences.isGridView)
                        }
                    },
                    onVideoClick = { video ->
                        val index = folderVideos.indexOfFirst { it.contentUriString == video.contentUriString }
                        currentScreen = Screen.Player(folderVideos, if (index != -1) index else 0)
                    },
                    onToggleFavorite = { video ->
                        coroutineScope.launch {
                            videoRepository.toggleFavorite(video)
                        }
                    },
                    onDeleteVideo = { video ->
                        coroutineScope.launch {
                            videoRepository.deleteVideo(video)
                        }
                    },
                    onLockToVault = { video ->
                        coroutineScope.launch {
                            if (!videoRepository.vaultManager.isPinConfigured()) {
                                currentScreen = Screen.PrivateVault
                            } else {
                                videoRepository.vaultManager.lockVideo(video)
                                videoRepository.notifyVideoDeleted(video.contentUriString)
                            }
                        }
                    },
                    onNavigateBack = {
                        currentScreen = Screen.Library
                    }
                )
            }
            is Screen.Player -> {
                PlayerScreen(
                    videoList = screen.videoList,
                    initialVideoIndex = screen.startIndex,
                    repository = videoRepository,
                    doubleTapSeekSeconds = userPreferences.doubleTapSeekSeconds,
                    autoResume = userPreferences.autoResume,
                    backgroundPlaybackDefault = userPreferences.backgroundPlayback,
                    onNavigateBack = {
                        currentScreen = Screen.Library
                    }
                )
            }
            is Screen.Settings -> {
                SettingsScreen(
                    userPreferences = userPreferences,
                    preferencesRepository = preferencesRepository,
                    onNavigateBack = {
                        currentScreen = Screen.Library
                    }
                )
            }
            is Screen.PrivateVault -> {
                PrivateVaultScreen(
                    repository = videoRepository,
                    onPlayVideo = { list, index ->
                        currentScreen = Screen.Player(list, index)
                    },
                    onNavigateBack = {
                        currentScreen = Screen.Library
                    }
                )
            }
        }
    }
}
