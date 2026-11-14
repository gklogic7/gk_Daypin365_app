package com.example.ui.components

import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.model.MediaType
import com.example.ui.theme.JetBrainsClockMono
import kotlinx.coroutines.delay

@Composable
fun MediaPreviewCard(
    mediaType: MediaType,
    mediaUri: String?,
    mediaDisplayName: String?,
    compact: Boolean = false,
    onDarkOverlay: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (mediaType == MediaType.NONE || mediaUri.isNullOrBlank()) return
    val uri = remember(mediaUri) { runCatching { Uri.parse(mediaUri) }.getOrNull() } ?: return

    when (mediaType) {
        MediaType.IMAGE -> {
            val context = LocalContext.current
            AsyncImage(
                model = uri,
                imageLoader = AnimatedImageLoader.get(context),
                contentDescription = mediaDisplayName ?: "Attached reminder image or GIF/WebP",
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .fillMaxWidth()
                    .height(if (compact) 128.dp else 200.dp)
                    .clip(RoundedCornerShape(if (compact) 14.dp else 18.dp))
            )
        }

        MediaType.VIDEO -> {
            InlineVideoPreview(
                uri = uri,
                modifier = modifier
                    .fillMaxWidth()
                    .height(if (compact) 140.dp else 220.dp)
            )
        }

        MediaType.MUSIC -> {
            AppleAudioPlayerCard(
                uri = uri,
                title = mediaDisplayName ?: "Attached audio track",
                compact = compact,
                onDarkOverlay = onDarkOverlay,
                modifier = modifier.fillMaxWidth()
            )
        }

        MediaType.NONE -> Unit
    }
}

@Composable
private fun InlineVideoPreview(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasError by remember(uri) { mutableStateOf(false) }
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                hasError = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Unable to preview video",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Apple Music-inspired frosted glass audio player card shared across
 * cards, editors, floating overlay panels, and the Alarm Ring Screen.
 */
@Composable
fun AppleAudioPlayerCard(
    uri: Uri,
    title: String,
    compact: Boolean = false,
    onDarkOverlay: Boolean = false,
    subtitleOverride: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember(uri) { mutableStateOf(false) }
    var positionMs by remember(uri) { mutableLongStateOf(0L) }
    var durationMs by remember(uri) { mutableLongStateOf(0L) }
    var isScrubbing by remember(uri) { mutableStateOf(false) }
    var scrubFraction by remember(uri) { mutableFloatStateOf(0f) }

    val isSystemRingtone = remember(uri) {
        uri.scheme.equals("content", ignoreCase = true) &&
            uri.authority.equals("settings", ignoreCase = true)
    }
    var playbackError by remember(uri) { mutableStateOf(false) }

    val systemRingtone: Ringtone? = remember(uri, isSystemRingtone) {
        if (!isSystemRingtone) return@remember null
        runCatching {
            RingtoneManager.getRingtone(context, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    isLooping = false
                }
            }
        }.getOrNull()
    }

    val exoPlayer: ExoPlayer? = remember(uri, isSystemRingtone) {
        if (isSystemRingtone) return@remember null
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }

    DisposableEffect(exoPlayer, systemRingtone) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val player = exoPlayer ?: return
                if (playbackState == Player.STATE_READY) {
                    val dur = player.duration
                    if (dur > 0L) durationMs = dur
                } else if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false
                    player.seekTo(0L)
                    player.pause()
                    positionMs = 0L
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = true
                isPlaying = false
            }
        }
        exoPlayer?.addListener(listener)
        onDispose {
            exoPlayer?.removeListener(listener)
            exoPlayer?.release()
            runCatching { systemRingtone?.stop() }
        }
    }

    LaunchedEffect(exoPlayer, systemRingtone, isPlaying) {
        if (systemRingtone != null) {
            while (isPlaying) {
                val stillPlaying = runCatching { systemRingtone.isPlaying }.getOrDefault(false)
                if (!stillPlaying) {
                    isPlaying = false
                    break
                }
                delay(250L)
            }
            return@LaunchedEffect
        }
        val player = exoPlayer ?: return@LaunchedEffect
        while (true) {
            val dur = player.duration
            if (dur > 0L) durationMs = dur
            if (!isScrubbing) {
                positionMs = player.currentPosition.coerceAtLeast(0L)
            }
            if (!isPlaying) break
            delay(300L)
        }
    }

    val shape = RoundedCornerShape(if (compact) 16.dp else 20.dp)
    val surfaceBg = if (onDarkOverlay) {
        Color.Black.copy(alpha = 0.48f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
    }
    val hairline = if (onDarkOverlay) {
        Color.White.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
    }
    val primaryTextColor = if (onDarkOverlay) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (onDarkOverlay) {
        Color.White.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val accentColor = if (onDarkOverlay) Color.White else MaterialTheme.colorScheme.primary
    val artSize = if (compact) 36.dp else 44.dp

    val displayProgress = when {
        isScrubbing -> scrubFraction
        durationMs > 0L -> (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        else -> 0f
    }

    Surface(
        color = surfaceBg,
        shape = shape,
        tonalElevation = if (onDarkOverlay) 0.dp else 2.dp,
        modifier = modifier
            .border(0.75.dp, hairline, shape)
            .testTag("apple_audio_player_card")
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compact) 10.dp else 14.dp,
                vertical = if (compact) 8.dp else 10.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
            ) {
                // Apple Music style rounded squircle artwork tile
                Box(
                    modifier = Modifier
                        .size(artSize)
                        .clip(RoundedCornerShape(if (compact) 10.dp else 12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.GraphicEq else Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(if (compact) 18.dp else 22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.ifBlank { "Audio Track" },
                        style = (if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall)
                            .copy(fontWeight = FontWeight.SemiBold),
                        color = primaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            if (!subtitleOverride.isNullOrBlank()) {
                                append(subtitleOverride)
                                append(" • ")
                            }
                            if (playbackError) {
                                append("Audio unavailable")
                            } else if (durationMs > 0L) {
                                append(formatTrackMillis(positionMs))
                                append(" / ")
                                append(formatTrackMillis(durationMs))
                            } else if (isSystemRingtone) {
                                append(if (isPlaying) "Playing ringtone" else "System ringtone")
                            } else {
                                append(if (isPlaying) "Playing" else "Tap play")
                            }
                        },
                        fontFamily = JetBrainsClockMono,
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryTextColor,
                        maxLines = 1
                    )
                }

                // Clean Apple transport controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (!compact && exoPlayer != null) {
                        IconButton(
                            onClick = {
                                val next = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(next)
                                positionMs = next
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Replay10,
                                contentDescription = "Back 10 seconds",
                                tint = secondaryTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    FilledIconButton(
                        onClick = {
                            if (systemRingtone != null) {
                                if (isPlaying) {
                                    runCatching { systemRingtone.stop() }
                                    isPlaying = false
                                } else {
                                    val started = runCatching {
                                        systemRingtone.play()
                                        true
                                    }.getOrDefault(false)
                                    isPlaying = started
                                    if (!started) playbackError = true
                                }
                            } else if (exoPlayer != null) {
                                if (isPlaying) {
                                    exoPlayer.pause()
                                    isPlaying = false
                                } else {
                                    exoPlayer.play()
                                    isPlaying = true
                                }
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = accentColor,
                            contentColor = if (onDarkOverlay) Color.Black else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(if (compact) 38.dp else 44.dp)
                            .testTag("apple_audio_play_pause_btn")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause audio" else "Play audio",
                            modifier = Modifier.size(if (compact) 20.dp else 22.dp)
                        )
                    }

                    if (!compact && exoPlayer != null) {
                        IconButton(
                            onClick = {
                                val cap = if (durationMs > 0L) durationMs else Long.MAX_VALUE
                                val next = (exoPlayer.currentPosition + 10_000L).coerceAtMost(cap)
                                exoPlayer.seekTo(next)
                                positionMs = next
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Forward10,
                                contentDescription = "Forward 10 seconds",
                                tint = secondaryTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (exoPlayer != null) {
                // Sleek scrubber bar
                Slider(
                    value = displayProgress,
                    onValueChange = { next ->
                        isScrubbing = true
                        scrubFraction = next
                    },
                    onValueChangeFinished = {
                        if (durationMs > 0L) {
                            val targetMs = (scrubFraction * durationMs).toLong().coerceIn(0L, durationMs)
                            exoPlayer.seekTo(targetMs)
                            positionMs = targetMs
                        }
                        isScrubbing = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = if (onDarkOverlay) {
                            Color.White.copy(alpha = 0.24f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (compact) 22.dp else 28.dp)
                )
            }
        }
    }
}

private fun formatTrackMillis(ms: Long): String {
    val totalSecs = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSecs / 60L
    val secs = totalSecs % 60L
    return String.format("%02d:%02d", mins, secs)
}
