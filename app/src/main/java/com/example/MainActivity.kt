package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.app.Activity
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.VideoItem
import com.example.data.repository.VideoRepository
import com.example.ui.components.PinLockScreen
import com.example.ui.components.UpdateDialog
import com.example.ui.components.UpdateInfo
import com.example.ui.components.UpdateManager
import com.example.ui.components.UpdateNotificationHelper
import com.example.ui.screens.FolderDetailScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.PrivateVaultScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.ProBlue
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

        // Initialize update notification channel
        UpdateNotificationHelper.createNotificationChannel(this)

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

            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as? Activity)?.window
                    if (window != null) {
                        val insetsController = WindowCompat.getInsetsController(window, view)
                        insetsController.isAppearanceLightStatusBars = !userPreferences.isDarkTheme
                        insetsController.isAppearanceLightNavigationBars = !userPreferences.isDarkTheme
                    }
                }
            }

            val accentColor = when (userPreferences.accentColorName) {
                "Purple" -> AccentPurple
                "Green" -> AccentGreen
                "Orange" -> AccentOrange
                "Blue" -> ProBlue
                "Pink" -> AccentPink
                else -> ProBlue
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

    // Media permission state
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

    // Update state & dialog
    var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var updateDownloadProgress by remember { mutableStateOf(0f) }

    // Notification permission on Android 13+
    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && availableUpdate?.hasUpdate == true) {
            val update = availableUpdate
            if (update != null) {
                UpdateNotificationHelper.showUpdateNotification(context, update)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(permissionToRequest)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotif = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotif) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Background Update Check & System Notification
    LaunchedEffect(userPreferences.autoCheckUpdates, userPreferences.githubOwner, userPreferences.githubRepo) {
        if (userPreferences.autoCheckUpdates) {
            coroutineScope.launch {
                try {
                    val info = UpdateManager.checkUpdate(
                        repoOwner = userPreferences.githubOwner,
                        repoName = userPreferences.githubRepo
                    )
                    if (info.hasUpdate) {
                        availableUpdate = info
                        showUpdateDialog = true
                        UpdateNotificationHelper.showUpdateNotification(context, info)
                    }
                } catch (_: Exception) {}
            }
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

    // Handle incoming video VIEW intent or update notification click
    LaunchedEffect(intent) {
        intent?.data?.let { uri ->
            if (intent.action == Intent.ACTION_VIEW) {
                val openedVideo = videoRepository.getVideoMetadataFromUri(uri)
                currentScreen = Screen.Player(listOf(openedVideo), 0)
            }
        }
        if (intent?.getBooleanExtra(UpdateNotificationHelper.EXTRA_OPEN_UPDATE_DIALOG, false) == true) {
            coroutineScope.launch {
                val info = UpdateManager.checkUpdate(
                    repoOwner = userPreferences.githubOwner,
                    repoName = userPreferences.githubRepo
                )
                if (info.hasUpdate) {
                    availableUpdate = info
                    showUpdateDialog = true
                } else {
                    currentScreen = Screen.Settings
                }
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
                    repository = videoRepository,
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
                    onOpenPrivateVault = {
                        currentScreen = Screen.PrivateVault
                    },
                    onNavigateBack = {
                        currentScreen = Screen.Library
                    },
                    onOpenSettings = {
                        currentScreen = Screen.Settings
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

    // Modern In-App Update Dialog
    if (showUpdateDialog && availableUpdate != null) {
        val updateInfo = availableUpdate!!
        UpdateDialog(
            updateInfo = updateInfo,
            isDownloading = isDownloadingUpdate,
            downloadProgress = updateDownloadProgress,
            onDownloadAndInstall = {
                isDownloadingUpdate = true
                updateDownloadProgress = 0f
                coroutineScope.launch {
                    UpdateManager.downloadAndInstall(
                        context = context,
                        downloadUrl = updateInfo.downloadUrl,
                        sha256Url = updateInfo.sha256Url,
                        expectedSha256 = updateInfo.sha256Checksum,
                        onProgress = { progress ->
                            updateDownloadProgress = progress
                        },
                        onError = { err ->
                            isDownloadingUpdate = false
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    )
                    isDownloadingUpdate = false
                }
            },
            onDismiss = {
                showUpdateDialog = false
            }
        )
    }
}
