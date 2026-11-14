package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import java.util.Locale
import kotlin.math.roundToInt

enum class ZoomViewerFitMode(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
    STRETCH("Stretch")
}

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
fun FullscreenZoomMediaDialog(
    title: String,
    imageUri: String?,
    videoUri: String?,
    useAlarmAudio: Boolean = false,
    initialMuted: Boolean = false,
    onDismiss: () -> Unit
) {
    val parsedImg = remember(imageUri) { imageUri?.let { runCatching { Uri.parse(it) }.getOrNull() } }
    val parsedVid = remember(videoUri) { videoUri?.let { runCatching { Uri.parse(it) }.getOrNull() } }
    val isVideo = parsedVid != null
    if (parsedImg == null && parsedVid == null) return

    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var boxWidthFraction by remember { mutableFloatStateOf(0.94f) }
    var boxHeightFraction by remember { mutableFloatStateOf(0.68f) }
    var fitMode by remember { mutableStateOf(ZoomViewerFitMode.FIT) }
    var isMuted by remember(parsedVid, initialMuted) { mutableStateOf(initialMuted) }
    var isPlaying by remember(parsedVid) { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        BackHandler(onBack = onDismiss)

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(220)) + scaleIn(
                initialScale = 0.84f,
                animationSpec = tween(260, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.88f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(12.dp)
                    .testTag("fullscreen_zoom_media_dialog")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top Bar: Title + Reset + Video Controls + Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title.ifBlank { if (isVideo) "Video Preview" else "Media Preview" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isVideo) {
                                    "Pinch or drag corner to resize video • ${String.format(Locale.US, "%.1fx", zoomScale)}"
                                } else {
                                    "Pinch, double-tap or drag corner to resize • ${String.format(Locale.US, "%.1fx", zoomScale)}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    zoomScale = 1.0f
                                    panX = 0f
                                    panY = 0f
                                    boxWidthFraction = 0.94f
                                    boxHeightFraction = 0.68f
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("zoom_viewer_reset_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Reset zoom and frame size",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .testTag("zoom_viewer_close_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close full-size media viewer",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Interactive Resizable & Pinch-Zoomable Media Stage
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { },
                        contentAlignment = Alignment.Center
                    ) {
                        val stageWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                        val stageHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                        val latestWidthFrac by rememberUpdatedState(boxWidthFraction)
                        val latestHeightFrac by rememberUpdatedState(boxHeightFraction)

                        val frameWidth = maxWidth * boxWidthFraction.coerceIn(0.45f, 1.0f)
                        val frameHeight = maxHeight * boxHeightFraction.coerceIn(0.35f, 1.0f)

                        Box(
                            modifier = Modifier
                                .width(frameWidth)
                                .height(frameHeight)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF0E1014))
                                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            val nextZoom = (zoomScale * zoom).coerceIn(1.0f, 4.0f)
                                            zoomScale = nextZoom
                                            if (nextZoom > 1.02f) {
                                                val maxPanX = (size.width * (nextZoom - 1f)) / 2f
                                                val maxPanY = (size.height * (nextZoom - 1f)) / 2f
                                                panX = (panX + pan.x).coerceIn(-maxPanX, maxPanX)
                                                panY = (panY + pan.y).coerceIn(-maxPanY, maxPanY)
                                            } else {
                                                panX = 0f
                                                panY = 0f
                                            }
                                        }
                                    }
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                if (zoomScale > 1.25f) {
                                                    zoomScale = 1.0f
                                                    panX = 0f
                                                    panY = 0f
                                                } else {
                                                    zoomScale = 2.2f
                                                }
                                            }
                                        )
                                    }
                            ) {
                                val transformModifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = zoomScale
                                        scaleY = zoomScale
                                        translationX = panX
                                        translationY = panY
                                    }

                                if (parsedVid != null) {
                                    val exoPlayer = remember(parsedVid, useAlarmAudio) {
                                        ExoPlayer.Builder(context).build().apply {
                                            val attrs = AudioAttributes.Builder()
                                                .setUsage(if (useAlarmAudio) C.USAGE_ALARM else C.USAGE_MEDIA)
                                                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                                                .build()
                                            setAudioAttributes(attrs, !useAlarmAudio && !initialMuted)
                                            setMediaItem(MediaItem.fromUri(parsedVid))
                                            repeatMode = Player.REPEAT_MODE_ONE
                                            volume = if (initialMuted) 0f else 1f
                                            prepare()
                                            playWhenReady = true
                                        }
                                    }
                                    LaunchedEffect(exoPlayer, isMuted) {
                                        exoPlayer.volume = if (isMuted) 0f else 1f
                                    }
                                    LaunchedEffect(exoPlayer, isPlaying) {
                                        exoPlayer.playWhenReady = isPlaying
                                    }
                                    DisposableEffect(exoPlayer) {
                                        onDispose { exoPlayer.release() }
                                    }

                                    val targetResizeMode = when (fitMode) {
                                        ZoomViewerFitMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        ZoomViewerFitMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        ZoomViewerFitMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                    }

                                    AndroidView(
                                        factory = { ctx ->
                                            PlayerView(ctx).apply {
                                                player = exoPlayer
                                                useController = false
                                                resizeMode = targetResizeMode
                                            }
                                        },
                                        update = { playerView ->
                                            if (playerView.resizeMode != targetResizeMode) {
                                                playerView.resizeMode = targetResizeMode
                                            }
                                        },
                                        modifier = transformModifier
                                    )
                                } else if (parsedImg != null) {
                                    val targetContentScale = when (fitMode) {
                                        ZoomViewerFitMode.FIT -> ContentScale.Fit
                                        ZoomViewerFitMode.FILL -> ContentScale.Crop
                                        ZoomViewerFitMode.STRETCH -> ContentScale.FillBounds
                                    }
                                    AsyncImage(
                                        model = parsedImg,
                                        imageLoader = AnimatedImageLoader.get(context),
                                        contentDescription = "Zoomable attached image or animated media",
                                        contentScale = targetContentScale,
                                        modifier = transformModifier
                                    )
                                }
                            }

                            // Bottom-right corner drag handle to resize the video/image frame itself
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(48.dp)
                                    .pointerInput(stageWidthPx, stageHeightPx) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val nextW = (latestWidthFrac + (dragAmount.x * 1.5f) / stageWidthPx)
                                                .coerceIn(0.45f, 1.0f)
                                            val nextH = (latestHeightFrac + (dragAmount.y * 1.5f) / stageHeightPx)
                                                .coerceIn(0.35f, 1.0f)
                                            boxWidthFraction = nextW
                                            boxHeightFraction = nextH
                                        }
                                    }
                                    .testTag("zoom_viewer_resize_handle")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    shadowElevation = 6.dp,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.OpenInFull,
                                            contentDescription = "Drag to resize media frame",
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Control Dock: Fit/Fill/Stretch + Zoom Presets + Slider + Video Play/Mute
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1A1D24).copy(alpha = 0.94f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.75.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ZoomViewerFitMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = fitMode == mode,
                                        onClick = { fitMode = mode },
                                        label = { Text(mode.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                            labelColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("zoom_fit_${mode.name.lowercase()}")
                                    )
                                }

                                listOf(1.0f to "1x", 1.8f to "1.8x", 2.8f to "2.8x").forEach { (target, label) ->
                                    FilterChip(
                                        selected = kotlin.math.abs(zoomScale - target) < 0.15f,
                                        onClick = {
                                            zoomScale = target
                                            if (target <= 1.02f) {
                                                panX = 0f
                                                panY = 0f
                                            }
                                        },
                                        label = { Text(label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                                            labelColor = Color.White
                                        )
                                    )
                                }

                                if (isVideo) {
                                    FilterChip(
                                        selected = !isPlaying,
                                        onClick = { isPlaying = !isPlaying },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = { Text(if (isPlaying) "Pause" else "Play") },
                                        colors = FilterChipDefaults.filterChipColors(labelColor = Color.White)
                                    )
                                    FilterChip(
                                        selected = !isMuted,
                                        onClick = { isMuted = !isMuted },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = { Text(if (isMuted) "Muted" else "Sound") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                            labelColor = Color.White
                                        )
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AspectRatio,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Zoom ${String.format(Locale.US, "%.1fx", zoomScale)} • Frame ${(boxWidthFraction * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.width(126.dp)
                                )
                                Slider(
                                    value = zoomScale,
                                    onValueChange = { next ->
                                        zoomScale = next
                                        if (next <= 1.02f) {
                                            panX = 0f
                                            panY = 0f
                                        }
                                    },
                                    valueRange = 1.0f..4.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("zoom_viewer_slider")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
