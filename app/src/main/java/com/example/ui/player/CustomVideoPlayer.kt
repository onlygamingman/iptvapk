package com.example.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.RedLive
import kotlinx.coroutines.delay

enum class ScreenResizeMode(val title: String, val modeValue: Int) {
    STRETCH("Stretch", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    FIT("Fit to Screen", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    ZOOM("Zoom / Crop", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    ORIGINAL("Original", AspectRatioFrameLayout.RESIZE_MODE_FIT)
}

@OptIn(UnstableApi::class)
@Composable
fun CustomVideoPlayer(
    streamUrl: String,
    title: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Manage Fullscreen & Keep Screen Awake to prevent screen dimming during matching gameplay
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Enable Keep Screen On flag
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Hide system bars (Immersive mode)
        activity?.window?.let { window ->
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            // Disable Keep Screen On flag
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.window?.let { window ->
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Load stream link
    LaunchedEffect(streamUrl) {
        val mediaItem = MediaItem.fromUri(streamUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    // UI Overlay state management
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var resizeMode by remember { mutableStateOf(ScreenResizeMode.STRETCH) }
    var isLocked by remember { mutableStateOf(false) }

    // Transient overlay toast notification for options changes (e.g. "Stretched", "Quality: Auto")
    var activeToastMessage by remember { mutableStateOf<String?>(null) }
    var toastTrigger by remember { mutableStateOf(0) }

    // Auto-hide controls after 3.5 seconds of inactivity (if not locked)
    LaunchedEffect(showControls, isLocked) {
        if (showControls && !isLocked) {
            delay(3500)
            showControls = false
        }
    }

    // Clear active toast after 1.5 seconds
    LaunchedEffect(toastTrigger) {
        if (activeToastMessage != null) {
            delay(1500)
            activeToastMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                showControls = !showControls
            }
    ) {
        // Player View Container
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Hide default black system controllers to use custom Compose UI
                    keepScreenOn = true // Keep screen awake at view level as well
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeMode.modeValue
            },
            modifier = Modifier.fillMaxSize()
        )

        // Lock Status Overlay Indicator (Floating action button to lock/unlock easily)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
            exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
        ) {
            IconButton(
                onClick = {
                    isLocked = !isLocked
                    activeToastMessage = if (isLocked) "Controls Locked" else "Controls Unlocked"
                    toastTrigger++
                },
                modifier = Modifier
                    .size(50.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock controls",
                    tint = if (isLocked) RedLive else NeonGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Custom Overlay UI (Only visible when controls are on AND NOT LOCKED)
        AnimatedVisibility(
            visible = showControls && !isLocked,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -50 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -50 }),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(20.dp)
            ) {
                // Header (Back + Stream Name) - Beautiful Glass Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back to list",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "KhelaGhor Pro Streaming",
                                color = NeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Live Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(RedLive, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LIVE",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Middle Buttons (Play, Pause, Forward, Rewind) - Premium Glass Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val currentPos = exoPlayer.currentPosition
                            exoPlayer.seekTo(maxOf(0, currentPos - 10000))
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(27.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(27.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(36.dp))

                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                            isPlaying = !isPlaying
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(NeonGreen, RoundedCornerShape(36.dp))
                            .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(36.dp))
                    ) {
                        if (isPlaying) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.width(5.dp).height(22.dp).background(Color.Black))
                                Box(modifier = Modifier.width(5.dp).height(22.dp).background(Color.Black))
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(36.dp))

                    IconButton(
                        onClick = {
                            val currentPos = exoPlayer.currentPosition
                            exoPlayer.seekTo(currentPos + 10000)
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(27.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(27.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // BOTTOM RIGHT FLOATING GLASSMORPHIC MENU BAR
                // Contains small modern icons like Cast, Crop/Aspect, quality gear options without large text
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Refresh Stream reload Button
                    IconButton(
                        onClick = {
                            activeToastMessage = "Reloading Match Stream..."
                            toastTrigger++
                            val mediaItem = MediaItem.fromUri(streamUrl)
                            exoPlayer.setMediaItem(mediaItem)
                            exoPlayer.prepare()
                            exoPlayer.play()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload stream",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Video quality Settings Mock Icon Button
                    IconButton(
                        onClick = {
                            activeToastMessage = "Video Quality: Auto Adaptive"
                            toastTrigger++
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Set quality options",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Screen Aspect Ratio cycle button with icon indicator
                    IconButton(
                        onClick = {
                            val allModes = ScreenResizeMode.values()
                            val nextIndex = (allModes.indexOf(resizeMode) + 1) % allModes.size
                            resizeMode = allModes[nextIndex]
                            activeToastMessage = "Aspect Ratio: ${resizeMode.title}"
                            toastTrigger++
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(19.dp)
                        ) {
                            when (resizeMode) {
                                ScreenResizeMode.STRETCH -> {
                                    Box(
                                        modifier = Modifier
                                            .width(18.dp)
                                            .height(11.dp)
                                            .border(1.8.dp, NeonGreen, RoundedCornerShape(2.dp))
                                    )
                                }
                                ScreenResizeMode.FIT -> {
                                    Box(
                                        modifier = Modifier
                                            .width(13.dp)
                                            .height(13.dp)
                                            .border(1.8.dp, NeonGreen, RoundedCornerShape(2.dp))
                                    )
                                }
                                ScreenResizeMode.ZOOM -> {
                                    Box(
                                        modifier = Modifier
                                            .width(18.dp)
                                            .height(13.dp)
                                            .border(1.8.dp, NeonGreen, RoundedCornerShape(2.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(3.5.dp)
                                                .background(NeonGreen, CircleShape)
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .width(15.dp)
                                            .height(12.dp)
                                            .border(1.2.dp, NeonGreen, RoundedCornerShape(1.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Elegant Transient Toast overlay for status feedback (e.g. "Aspect Ratio: Stretch")
        AnimatedVisibility(
            visible = activeToastMessage != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { 30 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -30 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 75.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.82f), RoundedCornerShape(12.dp))
                    .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = activeToastMessage ?: "",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

