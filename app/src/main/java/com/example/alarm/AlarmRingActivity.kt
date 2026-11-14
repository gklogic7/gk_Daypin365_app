package com.example.alarm

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.local.AppPreferences
import com.example.data.local.OverlayAnimationStyle
import com.example.data.local.OverlayDisplayMode
import com.example.data.local.OverlayPanelStyle
import com.example.data.local.PreferencesManager
import com.example.data.model.ChecklistCodec
import com.example.data.model.MediaType
import com.example.data.model.StickyColor
import com.example.data.model.StickyNoteEntity
import com.example.data.model.TaskStatus
import com.example.data.repository.MediaStorageHelper
import com.example.data.repository.MotivationProvider
import com.example.data.repository.ReminderRepository
import com.example.overlay.StickyNoteOverlayService
import com.example.permissions.PermissionHelper
import com.example.ui.components.AnimatedImageLoader
import com.example.ui.components.AppleAudioPlayerCard
import com.example.ui.components.ChecklistInteractiveSection
import com.example.ui.components.FullscreenZoomMediaDialog
import com.example.ui.components.MotivationQuoteBanner
import com.example.ui.components.OVERLAY_EXIT_DURATION_MS
import com.example.ui.components.OverlayPanelShell
import com.example.ui.components.TaskProofCaptureDialog
import com.example.ui.components.overlayEnterTransition
import com.example.ui.components.overlayExitTransition
import com.example.ui.theme.DayPinTheme
import com.example.ui.theme.JetBrainsClockMono
import com.example.ui.theme.stickyBorderColor
import com.example.ui.theme.stickySurfaceColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlarmRingActivity : ComponentActivity() {

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockScreenBehavior()

        ContextCompat.registerReceiver(
            this,
            stopReceiver,
            IntentFilter(AlarmRingService.ACTION_ALARM_STOPPED_BROADCAST),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val itemKind = intent.getStringExtra(AlarmScheduler.EXTRA_ITEM_KIND) ?: AlarmScheduler.KIND_REMINDER
        val itemId = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, -1L)
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "DayPin Reminder"
        val noteText = intent.getStringExtra(AlarmScheduler.EXTRA_NOTE_TEXT) ?: ""
        val initialChecklistJson = intent.getStringExtra(AlarmScheduler.EXTRA_CHECKLIST_JSON) ?: "[]"
        val initialMotivation = intent.getStringExtra(AlarmScheduler.EXTRA_MOTIVATION_LINE)
            ?.takeIf { it.isNotBlank() }
            ?: MotivationProvider.randomQuote()

        val legacyType = runCatching {
            MediaType.valueOf(intent.getStringExtra(AlarmScheduler.EXTRA_MEDIA_TYPE) ?: MediaType.NONE.name)
        }.getOrDefault(MediaType.NONE)
        val legacyUri = intent.getStringExtra(AlarmScheduler.EXTRA_MEDIA_URI)
        val legacyName = intent.getStringExtra(AlarmScheduler.EXTRA_MEDIA_NAME)

        val musicUri = intent.getStringExtra(AlarmScheduler.EXTRA_MUSIC_URI)
            ?: legacyUri.takeIf { legacyType == MediaType.MUSIC }
        val musicName = intent.getStringExtra(AlarmScheduler.EXTRA_MUSIC_NAME)
            ?: legacyName.takeIf { legacyType == MediaType.MUSIC }
        val imageUri = intent.getStringExtra(AlarmScheduler.EXTRA_IMAGE_URI)
            ?: legacyUri.takeIf { legacyType == MediaType.IMAGE }
        val videoUri = intent.getStringExtra(AlarmScheduler.EXTRA_VIDEO_URI)
            ?: legacyUri.takeIf { legacyType == MediaType.VIDEO }

        val stickyColor = intent.getStringExtra(AlarmScheduler.EXTRA_STICKY_COLOR) ?: StickyColor.BUTTERCUP.name
        val showSticky = intent.getBooleanExtra(AlarmScheduler.EXTRA_SHOW_STICKY, true)
        val initialTaskStatus = intent.getStringExtra(AlarmScheduler.EXTRA_TASK_STATUS)?.let {
            runCatching { TaskStatus.valueOf(it) }.getOrNull()
        } ?: TaskStatus.PENDING

        if (!videoUri.isNullOrBlank() && musicUri.isNullOrBlank()) {
            muteServiceAudio()
        }

        val prefsManager = PreferencesManager(applicationContext)
        val repository = ReminderRepository(applicationContext)

        setContent {
            val prefs by prefsManager.preferencesFlow.collectAsStateWithLifecycle(initialValue = AppPreferences())
            val scope = rememberCoroutineScope()
            var liveChecklistJson by remember(initialChecklistJson) { mutableStateOf(initialChecklistJson) }

            DayPinTheme(themeMode = prefs.themeMode) {
                AlarmRingScreen(
                    isTaskMode = itemKind == AlarmScheduler.KIND_TASK,
                    taskId = itemId,
                    initialTaskStatus = initialTaskStatus,
                    overlayMode = prefs.overlayDisplayMode,
                    panelStyle = prefs.alarmActivePanelStyle,
                    animationStyle = prefs.overlayAnimationStyle,
                    panelScale = prefs.alarmActivePanelScale,
                    mediaHeightScale = prefs.alarmActiveMediaHeightScale,
                    textScale = prefs.alarmActiveTextScale,
                    cornerRadiusDp = prefs.alarmActiveCornerRadius,
                    surfaceOpacity = prefs.alarmActiveOpacity,
                    showMotivationQuote = prefs.alarmActiveShowQuote,
                    defaultSnoozeMinutes = prefs.defaultSnoozeMinutes,
                    title = title,
                    noteText = noteText,
                    checklistJson = liveChecklistJson,
                    initialMotivation = initialMotivation,
                    musicUri = musicUri,
                    musicName = musicName,
                    imageUri = imageUri,
                    videoUri = videoUri,
                    stickyColor = stickyColor,
                    showSticky = showSticky,
                    onToggleChecklistStep = { stepId ->
                        liveChecklistJson = ChecklistCodec.toggle(liveChecklistJson, stepId)
                        if (itemId > 0L) {
                            scope.launch(Dispatchers.IO) {
                                if (itemKind == AlarmScheduler.KIND_TASK) {
                                    repository.toggleTaskChecklistItem(itemId, stepId)
                                } else {
                                    repository.toggleReminderChecklistItem(itemId, stepId)
                                }
                            }
                        }
                    },
                    onToggleOverlayMode = {
                        val next = if (prefs.overlayDisplayMode == OverlayDisplayMode.FULL) {
                            OverlayDisplayMode.TRANSPARENT
                        } else {
                            OverlayDisplayMode.FULL
                        }
                        scope.launch { prefsManager.setOverlayDisplayMode(next) }
                    },
                    onCyclePanelStyle = {
                        val styles = OverlayPanelStyle.entries
                        val next = styles[(styles.indexOf(prefs.alarmActivePanelStyle) + 1) % styles.size]
                        scope.launch { prefsManager.setAlarmActivePanelStyle(next) }
                    },
                    onSelectPanelStyle = { style ->
                        scope.launch { prefsManager.setAlarmActivePanelStyle(style) }
                    },
                    onCycleAnimationStyle = {
                        val anims = OverlayAnimationStyle.entries
                        val next = anims[(anims.indexOf(prefs.overlayAnimationStyle) + 1) % anims.size]
                        scope.launch { prefsManager.setOverlayAnimationStyle(next) }
                    },
                    onCycleSizePreset = {
                        val nextScale = when {
                            prefs.alarmActivePanelScale < 0.78f -> 0.86f
                            prefs.alarmActivePanelScale < 0.94f -> 1.0f
                            prefs.alarmActivePanelScale < 1.18f -> 1.30f
                            prefs.alarmActivePanelScale < 1.48f -> 1.65f
                            else -> 0.68f
                        }
                        scope.launch {
                            prefsManager.setAlarmActivePanelScale(nextScale)
                            prefsManager.setAlarmActiveMediaHeightScale(nextScale)
                            prefsManager.setAlarmActiveTextScale(nextScale.coerceIn(0.80f, 1.45f))
                        }
                    },
                    onResizeAlarmActive = { nextWidthScale, nextMediaHeightScale ->
                        scope.launch {
                            prefsManager.setAlarmActivePanelScale(nextWidthScale)
                            prefsManager.setAlarmActiveMediaHeightScale(nextMediaHeightScale)
                        }
                    },
                    onMuteAlarmAudio = { muteServiceAudio() },
                    onSnooze = {
                        startService(
                            Intent(this, AlarmRingService::class.java).apply {
                                action = AlarmRingService.ACTION_SNOOZE_ALARM
                            }
                        )
                        finish()
                    },
                    onDismiss = {
                        dismissServiceAndFinish()
                    },
                    onPinStickyNote = {
                        if (noteText.isNotBlank() || title.isNotBlank() || ChecklistCodec.decode(liveChecklistJson).isNotEmpty() || !imageUri.isNullOrBlank() || !videoUri.isNullOrBlank() || !musicUri.isNullOrBlank()) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val noteId = repository.saveStickyNote(
                                    StickyNoteEntity(
                                        title = title,
                                        content = noteText.ifBlank { title },
                                        color = runCatching { StickyColor.valueOf(stickyColor) }
                                            .getOrDefault(StickyColor.BUTTERCUP),
                                        isPinnedOverlay = PermissionHelper.hasOverlayPermission(applicationContext),
                                        checklistJson = liveChecklistJson,
                                        musicUri = musicUri,
                                        musicDisplayName = musicName ?: musicUri?.let { Uri.parse(it).lastPathSegment },
                                        imageUri = imageUri,
                                        imageDisplayName = imageUri?.let { Uri.parse(it).lastPathSegment },
                                        videoUri = videoUri,
                                        videoDisplayName = videoUri?.let { Uri.parse(it).lastPathSegment }
                                    )
                                )
                                if (PermissionHelper.hasOverlayPermission(applicationContext)) {
                                    StickyNoteOverlayService.startOrRefresh(applicationContext, noteId)
                                }
                            }
                        }
                        dismissServiceAndFinish()
                    },
                    onSubmitTaskProof = { targetStatus, proofImgUri, proofImgName, proofNote ->
                        scope.launch {
                            if (itemId > 0L) {
                                repository.submitTaskProof(
                                    taskId = itemId,
                                    targetStatus = targetStatus,
                                    imageUri = proofImgUri,
                                    imageDisplayName = proofImgName,
                                    note = proofNote
                                )
                            }
                            dismissServiceAndFinish()
                        }
                    },
                    onDeferTaskLater = { deferMinutes ->
                        scope.launch {
                            if (itemId > 0L) {
                                repository.deferTaskLater(itemId, deferMinutes)
                            }
                            dismissServiceAndFinish()
                        }
                    }
                )
            }
        }
    }

    private fun muteServiceAudio() {
        startService(
            Intent(this, AlarmRingService::class.java).apply {
                action = AlarmRingService.ACTION_MUTE_AUDIO
            }
        )
    }

    private fun dismissServiceAndFinish() {
        startService(
            Intent(this, AlarmRingService::class.java).apply {
                action = AlarmRingService.ACTION_DISMISS_ALARM
            }
        )
        finish()
    }

    private fun configureLockScreenBehavior() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(stopReceiver) }
        super.onDestroy()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlarmRingScreen(
    isTaskMode: Boolean,
    taskId: Long,
    initialTaskStatus: TaskStatus,
    overlayMode: OverlayDisplayMode,
    panelStyle: OverlayPanelStyle,
    animationStyle: OverlayAnimationStyle,
    panelScale: Float,
    mediaHeightScale: Float,
    textScale: Float,
    cornerRadiusDp: Int,
    surfaceOpacity: Float,
    showMotivationQuote: Boolean,
    defaultSnoozeMinutes: Int,
    title: String,
    noteText: String,
    checklistJson: String,
    initialMotivation: String,
    musicUri: String?,
    musicName: String?,
    imageUri: String?,
    videoUri: String?,
    stickyColor: String,
    showSticky: Boolean,
    onToggleChecklistStep: (String) -> Unit,
    onToggleOverlayMode: () -> Unit,
    onCyclePanelStyle: () -> Unit,
    onSelectPanelStyle: (OverlayPanelStyle) -> Unit,
    onCycleAnimationStyle: () -> Unit,
    onCycleSizePreset: () -> Unit,
    onResizeAlarmActive: (Float, Float) -> Unit,
    onMuteAlarmAudio: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    onPinStickyNote: () -> Unit,
    onSubmitTaskProof: (TaskStatus, String, String, String) -> Unit,
    onDeferTaskLater: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingProofTarget by remember { mutableStateOf<TaskStatus?>(null) }
    var showLaterOptions by remember { mutableStateOf(false) }
    var showLayoutPicker by remember { mutableStateOf(false) }
    var showQuickTools by remember { mutableStateOf(false) }
    var showZoomViewer by remember { mutableStateOf(false) }
    var motivationQuote by remember(initialMotivation) { mutableStateOf(initialMotivation) }
    var panelVisible by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        panelVisible = true
    }

    fun runAnimatedExit(afterExit: () -> Unit) {
        if (isExiting) return
        isExiting = true
        panelVisible = false
        scope.launch {
            delay(OVERLAY_EXIT_DURATION_MS)
            afterExit()
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "alarm_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val hasImage = !imageUri.isNullOrBlank()
    val hasVideo = !videoUri.isNullOrBlank()
    val hasMusic = !musicUri.isNullOrBlank()
    val isTransparentMode = overlayMode == OverlayDisplayMode.TRANSPARENT

    val rootBgModifier = if (isTransparentMode) {
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f))
    } else {
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            )
    }

    Box(modifier = rootBgModifier) {
        if (hasImage) {
            AsyncImage(
                model = Uri.parse(imageUri),
                imageLoader = AnimatedImageLoader.get(context),
                contentDescription = "Reminder image or animated GIF/WebP attachment",
                contentScale = ContentScale.Crop,
                alpha = if (isTransparentMode) 0.55f else 1f,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = if (isTransparentMode) 0.35f else 0.55f),
                                Color.Black.copy(alpha = if (isTransparentMode) 0.25f else 0.35f),
                                Color.Black.copy(alpha = if (isTransparentMode) 0.45f else 0.75f)
                            )
                        )
                    )
            )
        }

        val panelAlignment = if (panelStyle == OverlayPanelStyle.TICKER_STRIP) {
            Alignment.TopCenter
        } else {
            Alignment.Center
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = panelAlignment
        ) {
            val density = LocalDensity.current
            val isLandscape = maxWidth > maxHeight
            val isTablet = maxWidth >= 600.dp || maxHeight >= 600.dp
            val noteBaseColor = stickySurfaceColor(stickyColor)
            val noteBorder = stickyBorderColor(stickyColor)
            val panelBaseSurface = when (panelStyle) {
                OverlayPanelStyle.CLASSIC_CARD, OverlayPanelStyle.CORNER_TAB ->
                    MaterialTheme.colorScheme.surface.copy(alpha = if (isTransparentMode) 0.90f else 0.98f)
                else -> MaterialTheme.colorScheme.surface
            }

            val isSplitOrGradientPanel =
                panelStyle == OverlayPanelStyle.SPLIT_MEDIA ||
                    panelStyle == OverlayPanelStyle.FULL_MEDIA_GRADIENT ||
                    panelStyle == OverlayPanelStyle.PLAIN_MEDIA ||
                    panelStyle == OverlayPanelStyle.MEDIA_EDITORIAL ||
                    panelStyle == OverlayPanelStyle.DYNAMIC_PILL ||
                    panelStyle == OverlayPanelStyle.MEDIA_STRIP
            val isGradientOverlay = panelStyle == OverlayPanelStyle.FULL_MEDIA_GRADIENT
            val isPlainMedia = panelStyle == OverlayPanelStyle.PLAIN_MEDIA
            val isMediaEditorial = panelStyle == OverlayPanelStyle.MEDIA_EDITORIAL

            var livePanelScale by remember { mutableFloatStateOf(panelScale) }
            var liveMediaHeightScale by remember { mutableFloatStateOf(mediaHeightScale) }
            var isDraggingResize by remember { mutableStateOf(false) }

            LaunchedEffect(panelScale) {
                if (!isDraggingResize) {
                    livePanelScale = panelScale
                }
            }
            LaunchedEffect(mediaHeightScale) {
                if (!isDraggingResize) {
                    liveMediaHeightScale = mediaHeightScale
                }
            }

            val sizeTag = when {
                livePanelScale < 0.78f -> "Size: XS"
                livePanelScale < 0.94f -> "Size: S"
                livePanelScale < 1.18f -> "Size: M"
                livePanelScale < 1.48f -> "Size: L"
                else -> "Size: XL"
            }

            val latestOnResize by rememberUpdatedState(onResizeAlarmActive)

            val baseCardWidth = when {
                panelStyle.isSmallVariant -> if (isTransparentMode) 244.dp else 296.dp
                isTransparentMode -> 304.dp
                panelStyle == OverlayPanelStyle.SPLIT_MEDIA ||
                    panelStyle == OverlayPanelStyle.MEDIA_EDITORIAL ->
                    if (isTablet) 440.dp else 368.dp
                isPlainMedia -> when {
                    isLandscape && isTablet -> 396.dp
                    isLandscape -> 352.dp
                    isTablet -> 384.dp
                    else -> 344.dp
                }
                else -> when {
                    isLandscape && isTablet -> 404.dp
                    isLandscape -> 360.dp
                    isTablet -> 392.dp
                    else -> 348.dp
                }
            }
            val minAllowedCardWidth = if (panelStyle.isSmallVariant) 156.dp else 196.dp
            val maxAllowedCardWidth = (maxWidth * 0.92f).coerceAtLeast(minAllowedCardWidth)
            val computedTargetWidth = (baseCardWidth.value * livePanelScale).dp
                .coerceIn(minAllowedCardWidth, maxAllowedCardWidth)

            val maxCardViewportHeight = (maxHeight * 0.92f).coerceAtLeast(240.dp)
            val maxMediaBoxHeight = (maxHeight * 0.78f).coerceIn(110.dp, 680.dp)

            AnimatedVisibility(
                visible = panelVisible,
                enter = overlayEnterTransition(animationStyle),
                exit = overlayExitTransition(animationStyle)
            ) {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .heightIn(max = maxCardViewportHeight)
                ) {
                    OverlayPanelShell(
                        panelStyle = panelStyle,
                        baseSurfaceColor = panelBaseSurface,
                        accentBorderColor = if (isTransparentMode) MaterialTheme.colorScheme.primary else noteBorder,
                        isFullMode = !isTransparentMode,
                        minWidth = minAllowedCardWidth,
                        maxWidth = maxAllowedCardWidth,
                        targetWidth = computedTargetWidth,
                        maxMediaHeightDp = maxMediaBoxHeight,
                        panelScale = livePanelScale,
                        mediaHeightScale = liveMediaHeightScale,
                        textScale = textScale,
                        spotlightHeaderLabel = if (isTaskMode) {
                            "TASK ALARM • ${initialTaskStatus.label.uppercase()}"
                        } else {
                            "ALARM ACTIVE"
                        },
                        mediaImageUri = imageUri,
                        mediaVideoUri = videoUri,
                        hasAudioMedia = hasMusic,
                        useAlarmAudio = true,
                        stretchMediaToFill = isPlainMedia,
                        onMediaClick = if (hasImage || hasVideo) {
                            { showZoomViewer = true }
                        } else {
                            null
                        },
                        cornerRadiusOverrideDp = cornerRadiusDp,
                        surfaceAlphaOverride = surfaceOpacity,
                        topBarContent = if (isMediaEditorial) {
                            {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isTaskMode) {
                                        FilledTonalButton(
                                            onClick = {
                                                onMuteAlarmAudio()
                                                pendingProofTarget = TaskStatus.DONE
                                            },
                                            modifier = Modifier
                                                .height(40.dp)
                                                .testTag("task_alarm_done_btn")
                                        ) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Done", style = MaterialTheme.typography.labelMedium)
                                        }
                                    } else {
                                        FilledTonalButton(
                                            onClick = { runAnimatedExit(onSnooze) },
                                            modifier = Modifier
                                                .height(40.dp)
                                                .testTag("alarm_snooze_button")
                                        ) {
                                            Icon(Icons.Filled.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Snooze", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }

                                    Button(
                                        onClick = { runAnimatedExit(onDismiss) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .height(40.dp)
                                            .testTag("alarm_dismiss_button")
                                    ) {
                                        Icon(Icons.Filled.AlarmOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Dismiss", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    IconButton(
                                        onClick = onCyclePanelStyle,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("ring_panel_next_chip")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Style,
                                            contentDescription = "Switch panel style",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { showQuickTools = !showQuickTools },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("ring_panel_picker_chip")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreHoriz,
                                            contentDescription = "More overlay options",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            null
                        }
                    ) {
                        if (isPlainMedia) {
                            // Reference Card Sleek Dark Footer (only for Plain Media):
                            // Row 1: Title + Snooze/Done pill + Style + Menu + X Dismiss
                            // Row 2: Monospace/muted caption or quote
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (isTaskMode) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .clickable {
                                                        onMuteAlarmAudio()
                                                        pendingProofTarget = TaskStatus.DONE
                                                    }
                                                    .testTag("task_alarm_done_btn")
                                            ) {
                                                Text(
                                                    text = "Done",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color.White.copy(alpha = 0.12f),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .clickable { runAnimatedExit(onSnooze) }
                                                    .testTag("alarm_snooze_button")
                                            ) {
                                                Text(
                                                    text = "Snooze",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = Color.White.copy(alpha = 0.92f),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = onCyclePanelStyle,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("ring_panel_next_chip")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Style,
                                                contentDescription = "Switch Alarm Active card style",
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { showQuickTools = !showQuickTools },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("ring_panel_picker_chip")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.MoreHoriz,
                                                contentDescription = "More panel options",
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { runAnimatedExit(onDismiss) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("alarm_dismiss_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Dismiss alarm",
                                                tint = Color(0xFFFFB4AB),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                val refCaption = when {
                                    showSticky && noteText.isNotBlank() -> noteText
                                    showMotivationQuote -> "\"$motivationQuote\""
                                    else -> ""
                                }
                                if (refCaption.isNotBlank()) {
                                    Text(
                                        text = refCaption,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = JetBrainsClockMono
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(end = 36.dp)
                                    )
                                }

                                if (hasMusic) {
                                    val parsedMusic = remember(musicUri) {
                                        musicUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
                                    }
                                    if (parsedMusic != null) {
                                        AppleAudioPlayerCard(
                                            uri = parsedMusic,
                                            title = musicName ?: title,
                                            compact = true,
                                            onDarkOverlay = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                val refSteps = remember(checklistJson) { ChecklistCodec.decode(checklistJson) }
                                if (refSteps.isNotEmpty()) {
                                    ChecklistInteractiveSection(
                                        items = refSteps,
                                        compact = true,
                                        onToggleItem = onToggleChecklistStep
                                    )
                                }

                                if (isTaskMode) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = {
                                                onMuteAlarmAudio()
                                                pendingProofTarget = TaskStatus.WORKING
                                            },
                                            modifier = Modifier
                                                .height(40.dp)
                                                .testTag("task_alarm_working_btn")
                                        ) {
                                            Text("Working", style = MaterialTheme.typography.labelSmall)
                                        }
                                        FilledTonalButton(
                                            onClick = {
                                                onMuteAlarmAudio()
                                                pendingProofTarget = TaskStatus.STARTED
                                            },
                                            modifier = Modifier
                                                .height(40.dp)
                                                .testTag("task_alarm_started_btn")
                                        ) {
                                            Text("Started", style = MaterialTheme.typography.labelSmall)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onMuteAlarmAudio()
                                                runAnimatedExit { onDeferTaskLater(defaultSnoozeMinutes) }
                                            },
                                            modifier = Modifier
                                                .height(40.dp)
                                                .testTag("task_alarm_later_btn")
                                        ) {
                                            Text("Later", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = showQuickTools) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (hasImage || hasVideo) {
                                                FilterChip(
                                                    selected = false,
                                                    onClick = { showZoomViewer = true },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Filled.OpenInFull,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    },
                                                    label = { Text("Full Zoom") },
                                                    modifier = Modifier.testTag("ring_open_zoom_chip")
                                                )
                                            }
                                            FilterChip(
                                                selected = false,
                                                onClick = onToggleOverlayMode,
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = if (isTransparentMode) Icons.Filled.Layers else Icons.Filled.Fullscreen,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                },
                                                label = { Text(if (isTransparentMode) "Small" else "Full") }
                                            )
                                            FilterChip(
                                                selected = false,
                                                onClick = onCycleSizePreset,
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Filled.FormatSize,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                },
                                                label = { Text(sizeTag) },
                                                modifier = Modifier.testTag("ring_size_cycle_chip")
                                            )
                                            FilterChip(
                                                selected = false,
                                                onClick = onCycleAnimationStyle,
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Filled.Animation,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                },
                                                label = { Text("Anim: ${animationStyle.shortLabel}") }
                                            )
                                            FilterChip(
                                                selected = showLayoutPicker,
                                                onClick = { showLayoutPicker = !showLayoutPicker },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Filled.DashboardCustomize,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                },
                                                label = { Text("All Styles") }
                                            )
                                        }

                                        if (showLayoutPicker) {
                                            FlowRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                OverlayPanelStyle.entries.forEach { style ->
                                                    FilterChip(
                                                        selected = panelStyle == style,
                                                        onClick = {
                                                            onSelectPanelStyle(style)
                                                            showLayoutPicker = false
                                                        },
                                                        label = { Text(style.shortTag) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (isMediaEditorial) {
                        // Right-side clean Header + Paragraph without clock or time
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = noteText.ifBlank {
                                    if (showMotivationQuote) motivationQuote else "Reminder active"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            AnimatedVisibility(visible = showQuickTools) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = false,
                                        onClick = onToggleOverlayMode,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isTransparentMode) Icons.Filled.Layers else Icons.Filled.Fullscreen,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        },
                                        label = { Text(if (isTransparentMode) "Small" else "Full") }
                                    )
                                    FilterChip(
                                        selected = false,
                                        onClick = onCycleSizePreset,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.FormatSize,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        },
                                        label = { Text(sizeTag) },
                                        modifier = Modifier.testTag("ring_size_cycle_chip")
                                    )
                                    FilterChip(
                                        selected = false,
                                        onClick = onCycleAnimationStyle,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Animation,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        },
                                        label = { Text("Anim: ${animationStyle.shortLabel}") }
                                    )
                                }
                            }

                            if (hasMusic) {
                                val parsedMusic = remember(musicUri) {
                                    musicUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
                                }
                                if (parsedMusic != null) {
                                    AppleAudioPlayerCard(
                                        uri = parsedMusic,
                                        title = musicName ?: "Alarm Track",
                                        subtitleOverride = "Alarm Audio",
                                        compact = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            val editorialSteps = remember(checklistJson) { ChecklistCodec.decode(checklistJson) }
                            if (editorialSteps.isNotEmpty()) {
                                ChecklistInteractiveSection(
                                    items = editorialSteps,
                                    compact = true,
                                    onToggleItem = onToggleChecklistStep
                                )
                            }

                            if (isTaskMode) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            onMuteAlarmAudio()
                                            pendingProofTarget = TaskStatus.WORKING
                                        },
                                        modifier = Modifier
                                            .height(40.dp)
                                            .testTag("task_alarm_working_btn")
                                    ) {
                                        Text("Working", style = MaterialTheme.typography.labelMedium)
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            onMuteAlarmAudio()
                                            pendingProofTarget = TaskStatus.STARTED
                                        },
                                        modifier = Modifier
                                            .height(40.dp)
                                            .testTag("task_alarm_started_btn")
                                    ) {
                                        Text("Started", style = MaterialTheme.typography.labelMedium)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onMuteAlarmAudio()
                                            runAnimatedExit { onDeferTaskLater(defaultSnoozeMinutes) }
                                        },
                                        modifier = Modifier
                                            .height(40.dp)
                                            .testTag("task_alarm_later_btn")
                                    ) {
                                        Text("Later", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Signature panel-style header with 1-tap style switcher (no clock or date)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(
                                            if (isGradientOverlay) Color.White else MaterialTheme.colorScheme.primary
                                        )
                                )
                                Text(
                                    text = if (isTaskMode) {
                                        "TASK • ${initialTaskStatus.label.uppercase()}"
                                    } else {
                                        panelStyle.label.uppercase()
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isGradientOverlay) Color.White else MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = onCyclePanelStyle,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("ring_panel_next_chip")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Style,
                                        contentDescription = "Cycle panel style",
                                        tint = if (isGradientOverlay) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { showQuickTools = !showQuickTools },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("ring_panel_picker_chip")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreHoriz,
                                        contentDescription = "Toggle panel layout and size options",
                                        tint = if (isGradientOverlay) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isGradientOverlay) {
                                        Color.White.copy(alpha = 0.18f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable(onClick = onToggleOverlayMode)
                                        .testTag("alarm_overlay_mode_toggle")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isTransparentMode) Icons.Filled.Layers else Icons.Filled.Fullscreen,
                                            contentDescription = "Switch overlay screen mode",
                                            tint = if (isGradientOverlay) Color.White else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (isTransparentMode) "Small" else "Full",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isGradientOverlay) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Collapsed behind ••• button for ultra-minimal panel overlay look
                        AnimatedVisibility(visible = showQuickTools) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = showLayoutPicker,
                                        onClick = { showLayoutPicker = !showLayoutPicker },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.DashboardCustomize,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        },
                                        label = { Text("Panel: ${panelStyle.shortTag}") }
                                    )
                                    FilterChip(
                                        selected = false,
                                        onClick = onCyclePanelStyle,
                                        label = { Text("Next Panel") },
                                        modifier = Modifier.testTag("ring_panel_next_chip")
                                    )
                                    FilterChip(
                                        selected = false,
                                        onClick = onCycleSizePreset,
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.FormatSize,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        },
                                        label = { Text(sizeTag) },
                                        modifier = Modifier.testTag("ring_size_cycle_chip")
                                    )
                                    FilterChip(
                                        selected = false,
                                        onClick = onCycleAnimationStyle,
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.Animation,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        },
                                        label = { Text("Anim: ${animationStyle.shortLabel}") },
                                        modifier = Modifier.testTag("ring_anim_cycle_chip")
                                    )
                                }

                                if (showLayoutPicker) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OverlayPanelStyle.entries.forEach { style ->
                                            FilterChip(
                                                selected = panelStyle == style,
                                                onClick = {
                                                    onSelectPanelStyle(style)
                                                    showLayoutPicker = false
                                                },
                                                label = { Text(style.shortTag) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = title,
                            style = (if (panelStyle.isSmallVariant) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.titleLarge
                            }).copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (showSticky && noteText.isNotBlank()) {
                            Surface(
                                color = noteBaseColor.copy(alpha = if (isTransparentMode) 0.86f else 0.94f),
                                shape = RoundedCornerShape(14.dp),
                                tonalElevation = 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(0.75.dp, noteBorder.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                            ) {
                                Text(
                                    text = noteText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }

                        if (showMotivationQuote && !panelStyle.isSmallVariant) {
                            MotivationQuoteBanner(
                                quote = motivationQuote,
                                compact = true,
                                onShuffle = {
                                    motivationQuote = MotivationProvider.randomQuote(exclude = motivationQuote)
                                }
                            )
                        }

                        if (hasImage && !isSplitOrGradientPanel) {
                            val inlineImgHeight = minOf(
                                ((if (isTablet && !isLandscape) 156f else 132f) * liveMediaHeightScale).dp,
                                maxMediaBoxHeight
                            )
                            com.example.ui.components.OverlayVisualMediaTile(
                                imageUri = imageUri,
                                videoUri = null,
                                hasAudioMedia = hasMusic,
                                accentColor = MaterialTheme.colorScheme.primary,
                                stretchToFill = false,
                                onMediaClick = { showZoomViewer = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(inlineImgHeight)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }

                        if (hasVideo && !isSplitOrGradientPanel) {
                            val inlineVidHeight = minOf(
                                ((if (isTablet && !isLandscape) 156f else 132f) * liveMediaHeightScale).dp,
                                maxMediaBoxHeight
                            )
                            AlarmVideoPlayerCard(
                                videoUri = Uri.parse(videoUri),
                                startMuted = hasMusic,
                                stretchToFill = false,
                                onExpandClick = { showZoomViewer = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(inlineVidHeight)
                            )
                        }

                        if (hasMusic) {
                            val parsedMusic = remember(musicUri) {
                                musicUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
                            }
                            if (parsedMusic != null) {
                                AppleAudioPlayerCard(
                                    uri = parsedMusic,
                                    title = musicName ?: "Alarm Track",
                                    subtitleOverride = "Alarm Audio",
                                    compact = true,
                                    onDarkOverlay = isGradientOverlay || panelStyle == OverlayPanelStyle.DYNAMIC_PILL,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        val checklistSteps = remember(checklistJson) { ChecklistCodec.decode(checklistJson) }
                        if (checklistSteps.isNotEmpty() && panelStyle != OverlayPanelStyle.DYNAMIC_PILL) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(
                                    alpha = if (isTransparentMode) 0.86f else 0.96f
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ChecklistInteractiveSection(
                                    items = checklistSteps,
                                    compact = true,
                                    onToggleItem = onToggleChecklistStep,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        if (isTaskMode) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onMuteAlarmAudio()
                                        pendingProofTarget = TaskStatus.DONE
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("task_alarm_done_btn")
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Done (Proof)")
                                }

                                FilledTonalButton(
                                    onClick = {
                                        onMuteAlarmAudio()
                                        pendingProofTarget = TaskStatus.WORKING
                                    },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("task_alarm_working_btn")
                                ) {
                                    Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Working")
                                }

                                FilledTonalButton(
                                    onClick = {
                                        onMuteAlarmAudio()
                                        pendingProofTarget = TaskStatus.STARTED
                                    },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("task_alarm_started_btn")
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Started")
                                }

                                OutlinedButton(
                                    onClick = {
                                        onMuteAlarmAudio()
                                        showLaterOptions = !showLaterOptions
                                    },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("task_alarm_later_btn")
                                ) {
                                    Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Later")
                                }
                            }

                            if (showLaterOptions) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Remind me later in:",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(defaultSnoozeMinutes, 15, 30, 60).distinct().forEach { mins ->
                                                FilterChip(
                                                    selected = false,
                                                    onClick = {
                                                        runAnimatedExit { onDeferTaskLater(mins) }
                                                    },
                                                    label = { Text(if (mins == 60) "1 hour" else "$mins min") }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                            ) {
                                FilledTonalButton(
                                    onClick = { runAnimatedExit(onSnooze) },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .weight(1f)
                                        .testTag("alarm_snooze_button")
                                ) {
                                    Icon(Icons.Filled.Snooze, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Snooze")
                                }

                                if ((noteText.isNotBlank() || hasImage || hasVideo || hasMusic) && PermissionHelper.hasOverlayPermission(context)) {
                                    OutlinedButton(
                                        onClick = { runAnimatedExit(onPinStickyNote) },
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("alarm_pin_note_button")
                                    ) {
                                        Icon(Icons.Filled.PushPin, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Pin")
                                    }
                                }

                                Button(
                                    onClick = { runAnimatedExit(onDismiss) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .weight(1f)
                                        .testTag("alarm_dismiss_button")
                                ) {
                                    Icon(Icons.Filled.AlarmOff, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Dismiss")
                                }
                            }
                        }
                        }
                    }
                }

                // Floating bottom-right corner 2D drag-to-resize grip across ALL 14 panel styles
                // (Plain Media uses terracotta-orange #F08A4B; other styles use theme accent)
                val dragRefWidthPx = with(density) { 240.dp.toPx().coerceAtLeast(1f) }
                val dragRefHeightPx = with(density) { 180.dp.toPx().coerceAtLeast(1f) }
                val gripColor = if (isPlainMedia) Color(0xFFF08A4B) else MaterialTheme.colorScheme.primary
                val gripContentColor = if (isPlainMedia) Color(0xFF1A1614) else MaterialTheme.colorScheme.onPrimary

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 8.dp, y = 8.dp)
                        .size(48.dp)
                        .pointerInput(dragRefWidthPx, dragRefHeightPx) {
                            detectDragGestures(
                                onDragStart = {
                                    isDraggingResize = true
                                },
                                onDragEnd = {
                                    isDraggingResize = false
                                    latestOnResize(livePanelScale, liveMediaHeightScale)
                                },
                                onDragCancel = {
                                    isDraggingResize = false
                                    latestOnResize(livePanelScale, liveMediaHeightScale)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    livePanelScale = (livePanelScale + dragAmount.x / dragRefWidthPx)
                                        .coerceIn(0.50f, 2.20f)
                                    liveMediaHeightScale = (liveMediaHeightScale + dragAmount.y / dragRefHeightPx)
                                        .coerceIn(0.50f, 2.40f)
                                }
                            )
                        }
                        .testTag("alarm_active_resize_handle")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = gripColor,
                        contentColor = gripContentColor,
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.OpenInFull,
                                contentDescription = "Drag corner to freely resize Alarm Active card width and height",
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    }

    if (showZoomViewer && (hasImage || hasVideo)) {
        FullscreenZoomMediaDialog(
            title = title,
            imageUri = imageUri,
            videoUri = videoUri,
            useAlarmAudio = true,
            initialMuted = hasVideo && hasMusic,
            onDismiss = { showZoomViewer = false }
        )
    }

    pendingProofTarget?.let { targetStatus ->
        TaskProofCaptureDialog(
            taskTitle = title,
            targetStatus = targetStatus,
            onPersistProofImage = { uri ->
                MediaStorageHelper.persistPickedMedia(context, uri, "task_proof_${taskId}")
            },
            onDismiss = { pendingProofTarget = null },
            onSaveProof = { imgUri, imgName, proofNote ->
                pendingProofTarget = null
                runAnimatedExit {
                    onSubmitTaskProof(targetStatus, imgUri, imgName, proofNote)
                }
            }
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun AlarmVideoPlayerCard(
    videoUri: Uri,
    startMuted: Boolean,
    stretchToFill: Boolean = true,
    onExpandClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isMuted by remember(videoUri, startMuted) { mutableStateOf(startMuted) }

    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            val attrs = AudioAttributes.Builder()
                .setUsage(C.USAGE_ALARM)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()
            setAudioAttributes(attrs, false)
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = if (startMuted) 0f else 1f
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(exoPlayer, isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    val targetResize = if (stretchToFill) {
        AspectRatioFrameLayout.RESIZE_MODE_FILL
    } else {
        AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black)
            .let { base ->
                if (onExpandClick != null) {
                    base.clickable(onClick = onExpandClick)
                } else {
                    base
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = targetResize
                }
            },
            update = { view ->
                if (view.resizeMode != targetResize) {
                    view.resizeMode = targetResize
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (onExpandClick != null) {
            IconButton(
                onClick = onExpandClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
            ) {
                Icon(
                    imageVector = Icons.Filled.OpenInFull,
                    contentDescription = "Open full resizable video",
                    tint = Color.White
                )
            }
        }

        IconButton(
            onClick = { isMuted = !isMuted },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                contentDescription = if (isMuted) "Unmute video" else "Mute video",
                tint = Color.White
            )
        }
    }
}
