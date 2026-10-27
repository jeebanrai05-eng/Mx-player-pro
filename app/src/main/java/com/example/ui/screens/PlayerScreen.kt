package com.example.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.VideoItem
import com.example.data.repository.VideoRepository
import com.example.ui.components.GestureFeedbackOverlay
import com.example.ui.components.GestureOverlayType
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.SleepTimerMode
import com.example.ui.components.SpeedSelectionDialog
import com.example.ui.components.TrackItemInfo
import com.example.ui.components.TrackSelectionDialog
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.StatusRed
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class VideoAspectRatio(val label: String, val resizeMode: Int) {
    FIT("Fit Screen", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    ZOOM("Crop & Fill", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    STRETCH("Stretch", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    FIXED_16_9("16:9", AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH),
    FIXED_4_3("4:3", AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT)
}

enum class ScreenOrientationMode(val label: String) {
    AUTO("Sensor / Auto"),
    LANDSCAPE("Landscape"),
    PORTRAIT("Portrait")
}

@Composable
fun PlayerScreen(
    videoList: List<VideoItem>,
    initialVideoIndex: Int,
    repository: VideoRepository,
    doubleTapSeekSeconds: Int = 10,
    autoResume: Boolean = true,
    backgroundPlaybackDefault: Boolean = false,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    var currentVideoIndex by remember { mutableIntStateOf(initialVideoIndex.coerceIn(0, (videoList.size - 1).coerceAtLeast(0))) }
    val currentVideo = videoList.getOrNull(currentVideoIndex)

    if (currentVideo == null) {
        onNavigateBack()
        return
    }

    // Player state
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(currentVideo.duration) }
    var bufferedPositionMs by remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }

    // Controls visibility & lock
    var areControlsVisible by remember { mutableStateOf(true) }
    var isScreenLocked by remember { mutableStateOf(false) }

    // Dialog states
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showTrackDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    // Modes
    var currentAspectRatio by remember { mutableStateOf(VideoAspectRatio.FIT) }
    var orientationMode by remember { mutableStateOf(ScreenOrientationMode.LANDSCAPE) }
    var isBackgroundAudioEnabled by remember { mutableStateOf(backgroundPlaybackDefault) }
    var sleepTimerMode by remember { mutableStateOf(SleepTimerMode.OFF) }
    var sleepTimerJob by remember { mutableStateOf<Job?>(null) }
    var isFavorite by remember { mutableStateOf(currentVideo.isFavorite) }

    // System bars immersion helper
    fun setFullscreen(enable: Boolean) {
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (enable) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Auto-rotate on video click & cleanup on exit
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        setFullscreen(true)
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            setFullscreen(false)
        }
    }

    // Gestures overlay state
    var gestureOverlay by remember { mutableStateOf<GestureOverlayType>(GestureOverlayType.None) }
    var gestureHideJob by remember { mutableStateOf<Job?>(null) }
    var controlsHideJob by remember { mutableStateOf<Job?>(null) }

    // Audio & Brightness
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var currentBrightnessPercent by remember {
        val windowBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        mutableIntStateOf(if (windowBrightness >= 0f) (windowBrightness * 100).toInt() else 50)
    }

    // Tracks
    var subtitleTracks by remember { mutableStateOf<List<TrackItemInfo>>(emptyList()) }
    var audioTracks by remember { mutableStateOf<List<TrackItemInfo>>(emptyList()) }

    // Subtitle picker launcher
    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if not persistable
            }
            // Add subtitle to current media item
        }
    }

    // Auto hide controls
    fun triggerControlsAutoHide() {
        controlsHideJob?.cancel()
        controlsHideJob = coroutineScope.launch {
            delay(4000)
            if (isPlaying && !isScreenLocked) {
                areControlsVisible = false
            }
        }
    }

    fun showGestureFeedback(feedback: GestureOverlayType) {
        gestureOverlay = feedback
        gestureHideJob?.cancel()
        gestureHideJob = coroutineScope.launch {
            delay(1200)
            gestureOverlay = GestureOverlayType.None
        }
    }

    // Playback error message state
    var playbackErrorMessage by remember { mutableStateOf<String?>(null) }

    // Create & remember ExoPlayer instance with custom User-Agent, redirects, and timeouts
    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                playWhenReady = true
            }
    }

    // Orientation handling
    LaunchedEffect(orientationMode) {
        activity?.let { act ->
            when (orientationMode) {
                ScreenOrientationMode.AUTO -> act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                ScreenOrientationMode.LANDSCAPE -> act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                ScreenOrientationMode.PORTRAIT -> act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    // Media item setup for current video
    LaunchedEffect(currentVideoIndex) {
        playbackErrorMessage = null
        val vid = videoList[currentVideoIndex]
        isFavorite = vid.isFavorite
        val uri = Uri.parse(vid.contentUriString)
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        if (autoResume && vid.playbackPosition > 1000L && vid.duration > 5000L && vid.playbackPosition < vid.duration - 3000L) {
            exoPlayer.seekTo(vid.playbackPosition)
        } else {
            exoPlayer.seekTo(0L)
        }
        exoPlayer.play()
    }

    // Track listener & state polling
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) triggerControlsAutoHide()
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                val cause = error.cause?.localizedMessage ?: error.localizedMessage ?: "Source unreachable"
                playbackErrorMessage = "Error playing video: $cause"
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    playbackErrorMessage = null
                    totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
                } else if (state == Player.STATE_ENDED) {
                    if (sleepTimerMode == SleepTimerMode.END_OF_VIDEO) {
                        exoPlayer.pause()
                    } else if (currentVideoIndex < videoList.size - 1) {
                        currentVideoIndex++
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                // Populate Subtitles & Audio tracks
                val subs = mutableListOf<TrackItemInfo>()
                val audios = mutableListOf<TrackItemInfo>()

                tracks.groups.forEachIndexed { groupIndex, group ->
                    val type = group.type
                    for (trackIndex in 0 until group.length) {
                        val format = group.getTrackFormat(trackIndex)
                        val isSelected = group.isTrackSelected(trackIndex)
                        val name = format.label ?: format.language ?: "Track ${trackIndex + 1}"
                        val info = TrackItemInfo(
                            id = "$groupIndex-$trackIndex",
                            name = name,
                            language = format.language,
                            isSelected = isSelected
                        )
                        if (type == C.TRACK_TYPE_TEXT) {
                            subs.add(info)
                        } else if (type == C.TRACK_TYPE_AUDIO) {
                            audios.add(info)
                        }
                    }
                }
                subtitleTracks = subs
                audioTracks = audios
            }
        }

        exoPlayer.addListener(listener)
        setFullscreen(true)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            // Save position before dispose
            val pos = exoPlayer.currentPosition
            coroutineScope.launch {
                repository.savePlaybackPosition(currentVideo, pos)
            }
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            setFullscreen(false)
        }
    }

    // Polling progress & periodic persistence
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(0L)
            if (exoPlayer.duration > 0) {
                totalDurationMs = exoPlayer.duration
            }
            delay(500)
        }
    }

    // Auto periodic save position every 5s
    LaunchedEffect(currentPositionMs) {
        if (currentPositionMs > 1000L && currentPositionMs % 5000L < 600L) {
            repository.savePlaybackPosition(currentVideo, currentPositionMs)
        }
    }

    // Sleep Timer trigger
    LaunchedEffect(sleepTimerMode) {
        sleepTimerJob?.cancel()
        if (sleepTimerMode != SleepTimerMode.OFF && sleepTimerMode != SleepTimerMode.END_OF_VIDEO) {
            sleepTimerJob = coroutineScope.launch {
                delay(sleepTimerMode.minutes * 60 * 1000L)
                exoPlayer.pause()
                sleepTimerMode = SleepTimerMode.OFF
            }
        }
    }

    // Handle back button
    BackHandler {
        if (isScreenLocked) {
            showGestureFeedback(GestureOverlayType.None)
            // Inform user to tap unlock button
        } else {
            val pos = exoPlayer.currentPosition
            coroutineScope.launch {
                repository.savePlaybackPosition(currentVideo, pos)
                onNavigateBack()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        // Player View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.resizeMode = currentAspectRatio.resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Smart Gestures Detector Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isScreenLocked) {
                    if (isScreenLocked) {
                        detectTapGestures(
                            onTap = {
                                // Locked state feedback
                                areControlsVisible = true
                                triggerControlsAutoHide()
                            }
                        )
                    } else {
                        detectTapGestures(
                            onTap = {
                                areControlsVisible = !areControlsVisible
                                if (areControlsVisible) triggerControlsAutoHide()
                            },
                            onDoubleTap = { offset ->
                                val screenWidth = size.width
                                val isForward = offset.x > screenWidth / 2f
                                val seekDelta = if (isForward) doubleTapSeekSeconds * 1000L else -doubleTapSeekSeconds * 1000L
                                val newPos = (exoPlayer.currentPosition + seekDelta).coerceIn(0L, totalDurationMs)
                                exoPlayer.seekTo(newPos)
                                showGestureFeedback(
                                    GestureOverlayType.DoubleTapSeek(
                                        isForward = isForward,
                                        seekSeconds = doubleTapSeekSeconds
                                    )
                                )
                            }
                        )
                    }
                }
                .pointerInput(isScreenLocked) {
                    if (!isScreenLocked) {
                        var initialTouchX = 0f
                        var isLeftHalf = false
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                initialTouchX = offset.x
                                isLeftHalf = offset.x < size.width / 2f
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val delta = -dragAmount / size.height.toFloat()
                                if (isLeftHalf) {
                                    // Brightness swipe (Left)
                                    val current = currentBrightnessPercent
                                    val newPercent = (current + (delta * 120).toInt()).coerceIn(0, 100)
                                    currentBrightnessPercent = newPercent
                                    activity?.let { act ->
                                        val lp = act.window.attributes
                                        lp.screenBrightness = (newPercent / 100f).coerceIn(0.01f, 1f)
                                        act.window.attributes = lp
                                    }
                                    showGestureFeedback(GestureOverlayType.Brightness(newPercent))
                                } else {
                                    // Volume swipe (Right)
                                    val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val step = if (delta > 0) 1 else -1
                                    val newVol = (currentVol + step).coerceIn(0, maxVolume)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                    val pct = (newVol * 100) / maxVolume
                                    showGestureFeedback(GestureOverlayType.Volume(pct))
                                }
                            }
                        )
                    }
                }
                .pointerInput(isScreenLocked) {
                    if (!isScreenLocked) {
                        var startPosMs = 0L
                        var totalScrubDeltaSec = 0
                        detectHorizontalDragGestures(
                            onDragStart = {
                                startPosMs = exoPlayer.currentPosition
                                totalScrubDeltaSec = 0
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val secDelta = (dragAmount / 15f).toInt()
                                totalScrubDeltaSec += secDelta
                                val target = (startPosMs + (totalScrubDeltaSec * 1000L)).coerceIn(0L, totalDurationMs)
                                showGestureFeedback(
                                    GestureOverlayType.Scrubbing(
                                        targetPositionMs = target,
                                        deltaSeconds = totalScrubDeltaSec,
                                        totalDurationMs = totalDurationMs
                                    )
                                )
                            },
                            onDragEnd = {
                                val target = (startPosMs + (totalScrubDeltaSec * 1000L)).coerceIn(0L, totalDurationMs)
                                exoPlayer.seekTo(target)
                            }
                        )
                    }
                }
        )

        // Loading spinner
        if (isBuffering && playbackErrorMessage == null) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(54.dp)
                    .align(Alignment.Center)
            )
        }

        // Playback Error Card
        if (playbackErrorMessage != null) {
            Card(
                modifier = Modifier
                    .padding(32.dp)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = StatusRed,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Unable to Play Video",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = playbackErrorMessage ?: "",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                playbackErrorMessage = null
                                exoPlayer.prepare()
                                exoPlayer.play()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Retry", color = AmoledBlack, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f))
                        ) {
                            Text("Go Back", color = Color.White)
                        }
                    }
                }
            }
        }

        // Gesture Overlay (Volume / Brightness / Scrubbing / DoubleTap)
        GestureFeedbackOverlay(
            overlayType = gestureOverlay,
            modifier = Modifier.align(Alignment.Center)
        )

        // Screen Lock / Unlock floating button (Always accessible when controls are tapped)
        if (isScreenLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = {
                        isScreenLocked = false
                        areControlsVisible = true
                        triggerControlsAutoHide()
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC000000))
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Screen Locked. Tap to unlock",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Animated Player Controls (Top bar, center controls, bottom seek bar & actions)
        AnimatedVisibility(
            visible = areControlsVisible && !isScreenLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xCC000000),
                                Color.Transparent,
                                Color.Transparent,
                                Color(0xEE000000)
                            )
                        )
                    )
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val pos = exoPlayer.currentPosition
                            coroutineScope.launch {
                                repository.savePlaybackPosition(currentVideo, pos)
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentVideo.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = "${currentVideo.folderName} • ${currentVideo.resolutionLabel}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    // Favorite toggle
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                repository.toggleFavorite(currentVideo)
                                isFavorite = !isFavorite
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) StatusRed else Color.White
                        )
                    }

                    // Background Audio toggle
                    IconButton(
                        onClick = {
                            isBackgroundAudioEnabled = !isBackgroundAudioEnabled
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = "Background Audio",
                            tint = if (isBackgroundAudioEnabled) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }

                    // PiP Button
                    IconButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                                val aspectRatio = Rational(
                                    currentVideo.width.takeIf { it > 0 } ?: 16,
                                    currentVideo.height.takeIf { it > 0 } ?: 9
                                )
                                val pipParams = PictureInPictureParams.Builder()
                                    .setAspectRatio(aspectRatio)
                                    .build()
                                activity.enterPictureInPictureMode(pipParams)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Picture in Picture",
                            tint = Color.White
                        )
                    }

                    // Screen Lock button
                    IconButton(
                        onClick = {
                            isScreenLocked = true
                            areControlsVisible = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Lock Screen",
                            tint = Color.White
                        )
                    }
                }

                // Center Play / Rewind / Forward Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Video
                    IconButton(
                        onClick = {
                            if (currentVideoIndex > 0) currentVideoIndex--
                        },
                        enabled = currentVideoIndex > 0,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Video",
                            tint = if (currentVideoIndex > 0) Color.White else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Rewind 10s
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - doubleTapSeekSeconds * 1000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                            showGestureFeedback(GestureOverlayType.DoubleTapSeek(false, doubleTapSeekSeconds))
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Play / Pause FAB
                    IconButton(
                        onClick = {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            triggerControlsAutoHide()
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(16.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF38BDF8), Color(0xFF1E60FF))
                                )
                            )
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Fast Forward 10s
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition + doubleTapSeekSeconds * 1000L).coerceAtMost(totalDurationMs)
                            exoPlayer.seekTo(newPos)
                            showGestureFeedback(GestureOverlayType.DoubleTapSeek(true, doubleTapSeekSeconds))
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Fast Forward",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Next Video
                    IconButton(
                        onClick = {
                            if (currentVideoIndex < videoList.size - 1) currentVideoIndex++
                        },
                        enabled = currentVideoIndex < videoList.size - 1,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Video",
                            tint = if (currentVideoIndex < videoList.size - 1) Color.White else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom Panel: Seek Bar, Time Indicators, Subtitles, Speed, Aspect, Sleep Timer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    // Time text and duration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatMs(currentPositionMs),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatMs(totalDurationMs),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Seek Bar Slider
                    Slider(
                        value = if (totalDurationMs > 0) currentPositionMs.toFloat() / totalDurationMs.toFloat() else 0f,
                        onValueChange = { fraction ->
                            triggerControlsAutoHide()
                            val seekTarget = (fraction * totalDurationMs).toLong()
                            exoPlayer.seekTo(seekTarget)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Secondary Action Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Aspect Ratio button
                        IconButton(
                            onClick = {
                                val nextIndex = (currentAspectRatio.ordinal + 1) % VideoAspectRatio.values().size
                                currentAspectRatio = VideoAspectRatio.values()[nextIndex]
                            }
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = "Aspect Ratio",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = currentAspectRatio.label.take(6),
                                    fontSize = 9.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // Tracks (Subtitles & Audio)
                        IconButton(onClick = { showTrackDialog = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = "Subtitles & Audio",
                                    tint = if (subtitleTracks.any { it.isSelected }) MaterialTheme.colorScheme.primary else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "Tracks",
                                    fontSize = 9.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // Playback Speed
                        IconButton(onClick = { showSpeedDialog = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Speed",
                                    tint = if (playbackSpeed != 1.0f) MaterialTheme.colorScheme.primary else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "${playbackSpeed}x",
                                    fontSize = 9.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // Sleep Timer
                        IconButton(onClick = { showSleepTimerDialog = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Sleep Timer",
                                    tint = if (sleepTimerMode != SleepTimerMode.OFF) MaterialTheme.colorScheme.primary else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = if (sleepTimerMode == SleepTimerMode.OFF) "Timer" else "${sleepTimerMode.minutes}m",
                                    fontSize = 9.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // Screen Rotation
                        IconButton(
                            onClick = {
                                orientationMode = when (orientationMode) {
                                    ScreenOrientationMode.AUTO -> ScreenOrientationMode.LANDSCAPE
                                    ScreenOrientationMode.LANDSCAPE -> ScreenOrientationMode.PORTRAIT
                                    ScreenOrientationMode.PORTRAIT -> ScreenOrientationMode.AUTO
                                }
                            }
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.ScreenRotation,
                                    contentDescription = "Rotation",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = orientationMode.name.take(4),
                                    fontSize = 9.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dialogs
        if (showSpeedDialog) {
            SpeedSelectionDialog(
                currentSpeed = playbackSpeed,
                onSpeedSelected = { speed ->
                    playbackSpeed = speed
                    exoPlayer.playbackParameters = PlaybackParameters(speed)
                },
                onDismiss = { showSpeedDialog = false }
            )
        }

        if (showTrackDialog) {
            TrackSelectionDialog(
                subtitleTracks = subtitleTracks,
                audioTracks = audioTracks,
                onSelectSubtitle = { trackIndex ->
                    if (trackIndex == -1) {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .build()
                    } else {
                        // Enable selected subtitle track
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            .build()
                    }
                },
                onSelectAudio = { trackIndex ->
                    // Set audio track
                },
                onLoadExternalSubtitle = {
                    subtitlePickerLauncher.launch(arrayOf("*/*", "text/*", "application/x-subrip"))
                },
                onDismiss = { showTrackDialog = false }
            )
        }

        if (showSleepTimerDialog) {
            SleepTimerDialog(
                currentMode = sleepTimerMode,
                onSelectMode = { mode ->
                    sleepTimerMode = mode
                },
                onDismiss = { showSleepTimerDialog = false }
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
