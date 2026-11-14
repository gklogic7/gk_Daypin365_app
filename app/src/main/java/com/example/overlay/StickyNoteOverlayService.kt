package com.example.overlay

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppPreferences
import com.example.data.local.OverlayAnimationStyle
import com.example.data.local.OverlayDisplayMode
import com.example.data.local.OverlayPanelStyle
import com.example.data.local.PreferencesManager
import com.example.data.model.StickyNoteEntity
import com.example.data.repository.MotivationProvider
import com.example.data.repository.ReminderRepository
import com.example.permissions.PermissionHelper
import com.example.ui.components.AppleAudioPlayerCard
import com.example.ui.components.ChecklistInteractiveSection
import com.example.ui.components.MotivationQuoteBanner
import com.example.ui.components.OVERLAY_EXIT_DURATION_MS
import com.example.ui.components.OverlayPanelShell
import com.example.ui.components.OverlayVisualMediaTile
import com.example.ui.components.overlayEnterTransition
import com.example.ui.components.overlayExitTransition
import com.example.ui.theme.DayPinTheme
import com.example.ui.theme.stickyBorderColor
import com.example.ui.theme.stickySurfaceColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StickyNoteOverlayService : Service() {

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.example.action.SHOW_OVERLAY"
        const val ACTION_CLOSE_OVERLAY = "com.example.action.CLOSE_OVERLAY"
        const val EXTRA_FOCUS_NOTE_ID = "extra_focus_note_id"

        private const val CHANNEL_ID_ALERT = "daypin_overlay_alert_channel"
        private const val CHANNEL_ID_SILENT = "daypin_overlay_silent_channel"
        private const val NOTIFICATION_ID = 4202

        fun startOrRefresh(context: Context, focusNoteId: Long = -1L) {
            if (!PermissionHelper.hasOverlayPermission(context)) return
            val intent = Intent(context, StickyNoteOverlayService::class.java).apply {
                action = ACTION_SHOW_OVERLAY
                putExtra(EXTRA_FOCUS_NOTE_ID, focusNoteId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StickyNoteOverlayService::class.java))
        }
    }

    private class OverlayComposeOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateController = SavedStateRegistryController.create(this)

        init {
            savedStateController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateController.savedStateRegistry

        fun destroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: ReminderRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var windowManager: WindowManager

    private var overlayView: ComposeView? = null
    private var composeOwner: OverlayComposeOwner? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var savedFloatingX = 48
    private var savedFloatingY = 140
    @Volatile
    private var isCornerResizing = false

    private val pinnedNotesFlow = MutableStateFlow<List<StickyNoteEntity>>(emptyList())
    private val focusNoteIdFlow = MutableStateFlow(-1L)
    private val preferencesFlow = MutableStateFlow(AppPreferences())
    private var lastNotificationEnabled: Boolean? = null
    private val chargingReceiver = com.example.alarm.ChargingDeskReceiver()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repository = ReminderRepository(applicationContext)
        preferencesManager = PreferencesManager(applicationContext)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        androidx.core.content.ContextCompat.registerReceiver(
            this,
            chargingReceiver,
            android.content.IntentFilter(Intent.ACTION_POWER_CONNECTED),
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED
        )

        serviceScope.launch {
            repository.pinnedNotesFlow.collect { notes ->
                pinnedNotesFlow.value = notes
                if (notes.isEmpty() && overlayView != null) {
                    removeOverlayAndStop()
                }
            }
        }

        serviceScope.launch {
            preferencesManager.preferencesFlow.collect { prefs ->
                val prevNotif = lastNotificationEnabled
                preferencesFlow.value = prefs
                applyWindowLayoutForMode(prefs.overlayDisplayMode)
                if (prevNotif != null && prevNotif != prefs.overlayNotificationEnabled && overlayView != null) {
                    startOverlayForegroundNotification(prefs.overlayNotificationEnabled)
                }
                lastNotificationEnabled = prefs.overlayNotificationEnabled
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CLOSE_OVERLAY) {
            serviceScope.launch {
                repository.unpinAllStickyNotes()
                removeOverlayAndStop()
            }
            return START_NOT_STICKY
        }

        if (!PermissionHelper.hasOverlayPermission(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val focusId = intent?.getLongExtra(EXTRA_FOCUS_NOTE_ID, -1L) ?: -1L
        if (focusId > 0L) {
            focusNoteIdFlow.value = focusId
        }

        // Start foreground immediately with current in-memory preference, then sync from DataStore
        startOverlayForegroundNotification(preferencesFlow.value.overlayNotificationEnabled)
        serviceScope.launch {
            val freshPrefs = preferencesManager.preferencesFlow.first()
            preferencesFlow.value = freshPrefs
            lastNotificationEnabled = freshPrefs.overlayNotificationEnabled
            startOverlayForegroundNotification(freshPrefs.overlayNotificationEnabled)
        }
        ensureOverlayAttached()
        return START_STICKY
    }

    private fun startOverlayForegroundNotification(alertEnabled: Boolean) {
        val channelId = if (alertEnabled) CHANNEL_ID_ALERT else CHANNEL_ID_SILENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERT,
                getString(R.string.overlay_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.overlay_channel_desc)
            }
            val silentChannel = NotificationChannel(
                CHANNEL_ID_SILENT,
                getString(R.string.overlay_silent_channel_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = getString(R.string.overlay_silent_channel_desc)
                setShowBadge(false)
            }
            nm.createNotificationChannel(alertChannel)
            nm.createNotificationChannel(silentChannel)
        }

        val openAppIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val closeIntent = PendingIntent.getService(
            this,
            NOTIFICATION_ID + 1,
            Intent(this, StickyNoteOverlayService::class.java).apply {
                action = ACTION_CLOSE_OVERLAY
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val quote = MotivationProvider.randomQuote()
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(if (alertEnabled) "Overlay Panel Active" else "Sticky Overlay (Silent)")
            .setContentText("\"$quote\"")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("\"$quote\"\nTap to open DayPin 365 or close floating note")
            )
            .setPriority(if (alertEnabled) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_MIN)
            .setSilent(!alertEnabled)
            .setOnlyAlertOnce(!alertEnabled)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(0, "Close Note", closeIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun applyWindowLayoutForMode(mode: OverlayDisplayMode) {
        val params = layoutParams ?: return
        val view = overlayView ?: return
        if (mode == OverlayDisplayMode.FULL) {
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.MATCH_PARENT
            params.x = 0
            params.y = 0
        } else {
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            params.x = savedFloatingX
            params.y = savedFloatingY
        }
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun ensureOverlayAttached() {
        if (overlayView != null) return

        val owner = OverlayComposeOwner()
        composeOwner = owner

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val initialMode = preferencesFlow.value.overlayDisplayMode
        val params = WindowManager.LayoutParams(
            if (initialMode == OverlayDisplayMode.FULL) WindowManager.LayoutParams.MATCH_PARENT else WindowManager.LayoutParams.WRAP_CONTENT,
            if (initialMode == OverlayDisplayMode.FULL) WindowManager.LayoutParams.MATCH_PARENT else WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (initialMode == OverlayDisplayMode.FULL) 0 else savedFloatingX
            y = if (initialMode == OverlayDisplayMode.FULL) 0 else savedFloatingY
        }
        layoutParams = params

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                val notes by pinnedNotesFlow.collectAsState()
                val focusId by focusNoteIdFlow.collectAsState()
                val prefs by preferencesFlow.collectAsState()

                DayPinTheme(themeMode = prefs.themeMode) {
                    FloatingStickyNoteCard(
                        notes = notes,
                        focusNoteId = focusId,
                        overlayMode = prefs.overlayDisplayMode,
                        panelStyle = prefs.overlayPanelStyle,
                        animationStyle = prefs.overlayAnimationStyle,
                        overlayNotificationEnabled = prefs.overlayNotificationEnabled,
                        panelScale = prefs.overlayPanelScale,
                        mediaHeightScale = prefs.overlayMediaHeightScale,
                        textScale = prefs.overlayTextScale,
                        onToggleOverlayMode = {
                            val nextMode = if (prefs.overlayDisplayMode == OverlayDisplayMode.FULL) {
                                OverlayDisplayMode.TRANSPARENT
                            } else {
                                OverlayDisplayMode.FULL
                            }
                            serviceScope.launch {
                                preferencesManager.setOverlayDisplayMode(nextMode)
                            }
                        },
                        onCyclePanelStyle = {
                            val styles = OverlayPanelStyle.entries
                            val next = styles[(styles.indexOf(prefs.overlayPanelStyle) + 1) % styles.size]
                            serviceScope.launch {
                                preferencesManager.setOverlayPanelStyle(next)
                            }
                        },
                        onCycleAnimationStyle = {
                            val anims = OverlayAnimationStyle.entries
                            val next = anims[(anims.indexOf(prefs.overlayAnimationStyle) + 1) % anims.size]
                            serviceScope.launch {
                                preferencesManager.setOverlayAnimationStyle(next)
                            }
                        },
                        onCycleSizePreset = {
                            val nextScale = when {
                                prefs.overlayPanelScale < 0.78f -> 0.86f
                                prefs.overlayPanelScale < 0.94f -> 1.0f
                                prefs.overlayPanelScale < 1.18f -> 1.30f
                                prefs.overlayPanelScale < 1.48f -> 1.65f
                                else -> 0.68f
                            }
                            serviceScope.launch {
                                preferencesManager.setOverlayPanelScale(nextScale)
                                preferencesManager.setOverlayMediaHeightScale(nextScale)
                                preferencesManager.setOverlayTextScale(nextScale.coerceIn(0.80f, 1.45f))
                            }
                        },
                        onResizeOverlay = { nextWidthScale, nextMediaHeightScale ->
                            serviceScope.launch {
                                preferencesManager.setOverlayPanelScale(nextWidthScale)
                                preferencesManager.setOverlayMediaHeightScale(nextMediaHeightScale)
                            }
                        },
                        onCornerResizeActiveChange = { active ->
                            isCornerResizing = active
                        },
                        onToggleOverlayNotification = {
                            serviceScope.launch {
                                preferencesManager.setOverlayNotificationEnabled(!prefs.overlayNotificationEnabled)
                            }
                        },
                        onUnpinCurrent = { noteId ->
                            serviceScope.launch {
                                repository.setStickyNotePinned(noteId, false)
                            }
                        },
                        onToggleChecklistStep = { noteId, stepId ->
                            serviceScope.launch(Dispatchers.IO) {
                                repository.toggleStickyNoteChecklistItem(noteId, stepId)
                            }
                        },
                        onCloseAll = {
                            serviceScope.launch {
                                repository.unpinAllStickyNotes()
                                removeOverlayAndStop()
                            }
                        }
                    )
                }
            }
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                if (preferencesFlow.value.overlayDisplayMode == OverlayDisplayMode.FULL || isCornerResizing) return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (isCornerResizing) return false
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (kotlin.math.abs(dx) > 6 || kotlin.math.abs(dy) > 6) {
                            params.x = initialX + dx
                            params.y = initialY + dy
                            savedFloatingX = params.x
                            savedFloatingY = params.y
                            runCatching { windowManager.updateViewLayout(view, params) }
                            return true
                        }
                        return false
                    }
                }
                return false
            }
        })

        overlayView = view
        runCatching { windowManager.addView(view, params) }
    }

    private fun removeOverlayAndStop() {
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        composeOwner?.destroy()
        composeOwner = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(chargingReceiver) }
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        composeOwner?.destroy()
        composeOwner = null
        serviceScope.cancel()
        super.onDestroy()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FloatingStickyNoteCard(
    notes: List<StickyNoteEntity>,
    focusNoteId: Long,
    overlayMode: OverlayDisplayMode,
    panelStyle: OverlayPanelStyle,
    animationStyle: OverlayAnimationStyle,
    overlayNotificationEnabled: Boolean,
    panelScale: Float,
    mediaHeightScale: Float,
    textScale: Float,
    onToggleOverlayMode: () -> Unit,
    onCyclePanelStyle: () -> Unit,
    onCycleAnimationStyle: () -> Unit,
    onCycleSizePreset: () -> Unit,
    onResizeOverlay: (Float, Float) -> Unit,
    onCornerResizeActiveChange: (Boolean) -> Unit,
    onToggleOverlayNotification: () -> Unit,
    onUnpinCurrent: (Long) -> Unit,
    onToggleChecklistStep: (Long, String) -> Unit,
    onCloseAll: () -> Unit
) {
    if (notes.isEmpty()) return

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var selectedIndex by remember(notes, focusNoteId) {
        val foundIdx = notes.indexOfFirst { it.id == focusNoteId }.takeIf { it >= 0 } ?: 0
        mutableIntStateOf(foundIdx)
    }
    var collapsed by remember { mutableStateOf(false) }
    var showQuickTools by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    var motivationQuote by remember { mutableStateOf(MotivationProvider.randomQuote()) }

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

    LaunchedEffect(Unit) {
        visible = true
    }

    val safeIndex = selectedIndex.coerceIn(0, notes.lastIndex)
    val note = notes[safeIndex]
    val baseColor = stickySurfaceColor(note.color.name)
    val isFull = overlayMode == OverlayDisplayMode.FULL
    val borderColor = stickyBorderColor(note.color.name)
    val isGradientOverlay = panelStyle == OverlayPanelStyle.FULL_MEDIA_GRADIENT
    val isSplitOverlay = panelStyle == OverlayPanelStyle.SPLIT_MEDIA
    val isPlainMedia = panelStyle == OverlayPanelStyle.PLAIN_MEDIA
    val isMediaEditorial = panelStyle == OverlayPanelStyle.MEDIA_EDITORIAL
    val isCompactShellMedia = isSplitOverlay ||
        isGradientOverlay ||
        isMediaEditorial ||
        panelStyle == OverlayPanelStyle.DYNAMIC_PILL ||
        panelStyle == OverlayPanelStyle.MEDIA_STRIP
    val sizeTag = when {
        livePanelScale < 0.78f -> "Size: XS"
        livePanelScale < 0.94f -> "Size: S"
        livePanelScale < 1.18f -> "Size: M"
        livePanelScale < 1.48f -> "Size: L"
        else -> "Size: XL"
    }

    val latestOnResize by rememberUpdatedState(onResizeOverlay)
    val latestOnCornerActiveChange by rememberUpdatedState(onCornerResizeActiveChange)
    val baseOverlayWidth = when {
        panelStyle.isSmallVariant -> if (isFull) 260.dp else 210.dp
        isFull -> 352.dp
        else -> 264.dp
    }
    val minOverlayWidth = if (panelStyle.isSmallVariant) 148.dp else 176.dp
    val maxOverlayWidth = if (isFull) 760.dp else 560.dp
    val computedTargetWidth = (baseOverlayWidth.value * livePanelScale).dp
        .coerceIn(minOverlayWidth, maxOverlayWidth)

    fun handleAnimatedClose() {
        if (notes.size > 1) {
            onUnpinCurrent(note.id)
        } else if (!closing) {
            closing = true
            visible = false
            scope.launch {
                delay(OVERLAY_EXIT_DURATION_MS)
                onCloseAll()
            }
        }
    }

    val panelNode: @Composable () -> Unit = {
        AnimatedVisibility(
            visible = visible,
            enter = overlayEnterTransition(animationStyle),
            exit = overlayExitTransition(animationStyle)
        ) {
            Box(modifier = Modifier.wrapContentSize()) {
                OverlayPanelShell(
                    panelStyle = panelStyle,
                    baseSurfaceColor = baseColor,
                    accentBorderColor = borderColor,
                    isFullMode = isFull,
                    minWidth = minOverlayWidth,
                    maxWidth = maxOverlayWidth,
                    targetWidth = computedTargetWidth,
                    maxMediaHeightDp = if (isFull) 540.dp else 400.dp,
                    panelScale = livePanelScale,
                    mediaHeightScale = liveMediaHeightScale,
                    textScale = textScale,
                    stretchMediaToFill = isPlainMedia,
                    spotlightHeaderLabel = note.title.ifBlank { panelStyle.label }.uppercase(),
                    mediaImageUri = note.imageUri,
                    mediaVideoUri = note.videoUri,
                    hasAudioMedia = !note.musicUri.isNullOrBlank(),
                    useAlarmAudio = false,
                topBarContent = if (isMediaEditorial) {
                    {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (notes.size > 1) {
                                OverlayMiniPill(
                                    icon = Icons.AutoMirrored.Filled.NavigateNext,
                                    label = "Next (${safeIndex + 1}/${notes.size})",
                                    isFull = isFull,
                                    onClick = {
                                        selectedIndex = (safeIndex + 1) % notes.size
                                        motivationQuote = MotivationProvider.randomQuote(exclude = motivationQuote)
                                    }
                                )
                            } else {
                                OverlayMiniPill(
                                    icon = Icons.Filled.PushPin,
                                    label = "Pinned",
                                    isFull = isFull,
                                    onClick = {}
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = onCyclePanelStyle,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DashboardCustomize,
                                    contentDescription = "Switch panel style",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { showQuickTools = !showQuickTools },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreHoriz,
                                    contentDescription = "More overlay options",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = ::handleAnimatedClose,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close overlay",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else {
                    null
                }
            ) {
                if (isPlainMedia) {
                    // Minimal unobstructed media panel with sleek bottom buttons only
                    if (!note.musicUri.isNullOrBlank()) {
                        val parsedAudio = remember(note.musicUri) {
                            runCatching { Uri.parse(note.musicUri) }.getOrNull()
                        }
                        if (parsedAudio != null) {
                            AppleAudioPlayerCard(
                                uri = parsedAudio,
                                title = note.musicDisplayName ?: "Audio Track",
                                compact = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (notes.size > 1) {
                                OverlayMiniPill(
                                    icon = Icons.AutoMirrored.Filled.NavigateNext,
                                    label = "Next (${safeIndex + 1}/${notes.size})",
                                    isFull = isFull,
                                    onClick = {
                                        selectedIndex = (safeIndex + 1) % notes.size
                                    }
                                )
                            } else {
                                Text(
                                    text = note.title.ifBlank { "Media Note" },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = onCyclePanelStyle,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DashboardCustomize,
                                    contentDescription = "Switch panel style",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { showQuickTools = !showQuickTools },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreHoriz,
                                    contentDescription = "More overlay options",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = ::handleAnimatedClose,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close panel",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = showQuickTools) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            OverlayMiniPill(
                                icon = if (isFull) Icons.Filled.Layers else Icons.Filled.Fullscreen,
                                label = if (isFull) "Full" else "Small",
                                isFull = isFull,
                                onClick = onToggleOverlayMode
                            )
                            OverlayMiniPill(
                                icon = Icons.Filled.FormatSize,
                                label = sizeTag,
                                isFull = isFull,
                                onClick = onCycleSizePreset
                            )
                            OverlayMiniPill(
                                icon = Icons.Filled.Animation,
                                label = animationStyle.shortLabel,
                                isFull = isFull,
                                onClick = onCycleAnimationStyle
                            )
                            OverlayMiniPill(
                                icon = if (overlayNotificationEnabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                                label = if (overlayNotificationEnabled) "Notif" else "Silent",
                                isFull = isFull,
                                onClick = onToggleOverlayNotification
                            )
                        }
                    }
                } else if (isMediaEditorial) {
                    // Right-side clean Header + Paragraph (no clock/time) below top button bar
                    Text(
                        text = note.title.ifBlank { "Media Note" },
                        style = (if (isFull) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall)
                            .copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (note.content.isNotBlank()) {
                        Text(
                            text = note.content,
                            style = if (isFull) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (isFull) 8 else 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    AnimatedVisibility(visible = showQuickTools) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            OverlayMiniPill(
                                icon = if (isFull) Icons.Filled.Layers else Icons.Filled.Fullscreen,
                                label = if (isFull) "Full" else "Small",
                                isFull = isFull,
                                onClick = onToggleOverlayMode
                            )
                            OverlayMiniPill(
                                icon = Icons.Filled.FormatSize,
                                label = sizeTag,
                                isFull = isFull,
                                onClick = onCycleSizePreset
                            )
                            OverlayMiniPill(
                                icon = Icons.Filled.Animation,
                                label = animationStyle.shortLabel,
                                isFull = isFull,
                                onClick = onCycleAnimationStyle
                            )
                            OverlayMiniPill(
                                icon = if (overlayNotificationEnabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                                label = if (overlayNotificationEnabled) "Notif" else "Silent",
                                isFull = isFull,
                                onClick = onToggleOverlayNotification
                            )
                        }
                    }

                    if (!note.musicUri.isNullOrBlank()) {
                        val parsedAudio = remember(note.musicUri) {
                            runCatching { Uri.parse(note.musicUri) }.getOrNull()
                        }
                        if (parsedAudio != null) {
                            AppleAudioPlayerCard(
                                uri = parsedAudio,
                                title = note.musicDisplayName ?: "Audio Track",
                                compact = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    val editorialChecklist = remember(note.checklistJson) { note.checklistItems() }
                    if (editorialChecklist.isNotEmpty()) {
                        ChecklistInteractiveSection(
                            items = editorialChecklist,
                            compact = true,
                            maxVisible = if (isFull) editorialChecklist.size else 3,
                            onToggleItem = { stepId -> onToggleChecklistStep(note.id, stepId) }
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = "Pinned note",
                                tint = if (isGradientOverlay) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = note.title.ifBlank { "Sticky Note" },
                                style = (if (isFull) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge)
                                    .copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (notes.size > 1) {
                                IconButton(
                                    onClick = {
                                        selectedIndex = (safeIndex + 1) % notes.size
                                        motivationQuote = MotivationProvider.randomQuote(exclude = motivationQuote)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                        contentDescription = "Next pinned note",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = onCyclePanelStyle,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Style,
                                    contentDescription = "Switch overlay panel style",
                                    tint = if (isGradientOverlay) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { showQuickTools = !showQuickTools },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreHoriz,
                                    contentDescription = "Toggle overlay controls",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            if (!isFull && !panelStyle.isSmallVariant) {
                                IconButton(
                                    onClick = { collapsed = !collapsed },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (collapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                                        contentDescription = if (collapsed) "Expand sticky note" else "Collapse sticky note",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = ::handleAnimatedClose,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close sticky note",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Collapsed by default behind minimal ••• button
                    AnimatedVisibility(visible = showQuickTools) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            OverlayMiniPill(
                                icon = if (isFull) Icons.Filled.Layers else Icons.Filled.Fullscreen,
                                label = if (isFull) "Full" else "Small",
                                isFull = isFull,
                                onDarkOverlay = isGradientOverlay,
                                onClick = onToggleOverlayMode
                            )
                            OverlayMiniPill(
                                icon = Icons.Filled.DashboardCustomize,
                                label = panelStyle.shortTag,
                                isFull = isFull,
                                onDarkOverlay = isGradientOverlay,
                                onClick = onCyclePanelStyle
                            )
                            OverlayMiniPill(
                                icon = Icons.Filled.FormatSize,
                                label = sizeTag,
                                isFull = isFull,
                                onDarkOverlay = isGradientOverlay,
                                onClick = onCycleSizePreset
                            )
                            OverlayMiniPill(
                                icon = Icons.Filled.Animation,
                                label = animationStyle.shortLabel,
                                isFull = isFull,
                                onDarkOverlay = isGradientOverlay,
                                onClick = onCycleAnimationStyle
                            )
                            OverlayMiniPill(
                                icon = if (overlayNotificationEnabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                                label = if (overlayNotificationEnabled) "Notif" else "Silent",
                                isFull = isFull,
                                onDarkOverlay = isGradientOverlay,
                                onClick = onToggleOverlayNotification
                            )
                        }
                    }

                    if (isFull || !collapsed) {
                        if (note.content.isNotBlank()) {
                            Text(
                                text = note.content,
                                style = if (isFull) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = when {
                                    isFull && !panelStyle.isSmallVariant -> 10
                                    panelStyle == OverlayPanelStyle.DYNAMIC_PILL || panelStyle == OverlayPanelStyle.MEDIA_STRIP -> 2
                                    panelStyle.isSmallVariant -> 3
                                    isSplitOverlay || isGradientOverlay -> 3
                                    else -> 4
                                },
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!isCompactShellMedia && (!note.imageUri.isNullOrBlank() || !note.videoUri.isNullOrBlank())) {
                            val baseTileHeight = when {
                                panelStyle.isSmallVariant -> 70f
                                isFull -> 136f
                                else -> 84f
                            }
                            OverlayVisualMediaTile(
                                imageUri = note.imageUri,
                                videoUri = note.videoUri,
                                hasAudioMedia = !note.musicUri.isNullOrBlank(),
                                accentColor = borderColor,
                                useAlarmAudio = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((baseTileHeight * liveMediaHeightScale).dp.coerceIn(48.dp, 420.dp))
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }

                        if (!note.musicUri.isNullOrBlank() && panelStyle != OverlayPanelStyle.DYNAMIC_PILL) {
                            val parsedAudio = remember(note.musicUri) {
                                runCatching { Uri.parse(note.musicUri) }.getOrNull()
                            }
                            if (parsedAudio != null) {
                                AppleAudioPlayerCard(
                                    uri = parsedAudio,
                                    title = note.musicDisplayName ?: "Audio Track",
                                    compact = true,
                                    onDarkOverlay = isGradientOverlay || panelStyle == OverlayPanelStyle.DYNAMIC_PILL,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        val checklistItems = remember(note.checklistJson) { note.checklistItems() }
                        if (checklistItems.isNotEmpty() && panelStyle != OverlayPanelStyle.DYNAMIC_PILL && panelStyle != OverlayPanelStyle.MEDIA_STRIP) {
                            ChecklistInteractiveSection(
                                items = checklistItems,
                                compact = true,
                                maxVisible = if (isFull) checklistItems.size else if (panelStyle.isSmallVariant) 2 else 3,
                                onToggleItem = { stepId -> onToggleChecklistStep(note.id, stepId) }
                            )
                        }

                        if (!isCompactShellMedia && !panelStyle.isSmallVariant && isFull) {
                            MotivationQuoteBanner(
                                quote = motivationQuote,
                                compact = true,
                                onShuffle = {
                                    motivationQuote = MotivationProvider.randomQuote(exclude = motivationQuote)
                                }
                            )
                        }
                    }
                }
            }

            // Bottom-right 2D corner drag-to-resize grip across ALL 14 overlay styles
            val dragRefWidthPx = with(density) { 200.dp.toPx().coerceAtLeast(1f) }
            val dragRefHeightPx = with(density) { 150.dp.toPx().coerceAtLeast(1f) }
            val gripColor = if (isPlainMedia) Color(0xFFF08A4B) else MaterialTheme.colorScheme.primary
            val gripContentColor = if (isPlainMedia) Color(0xFF1A1614) else MaterialTheme.colorScheme.onPrimary

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 6.dp, y = 6.dp)
                    .size(44.dp)
                    .pointerInput(dragRefWidthPx, dragRefHeightPx) {
                        detectDragGestures(
                            onDragStart = {
                                isDraggingResize = true
                                latestOnCornerActiveChange(true)
                            },
                            onDragEnd = {
                                isDraggingResize = false
                                latestOnCornerActiveChange(false)
                                latestOnResize(livePanelScale, liveMediaHeightScale)
                            },
                            onDragCancel = {
                                isDraggingResize = false
                                latestOnCornerActiveChange(false)
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
                    .testTag("overlay_corner_resize_handle")
            ) {
                Surface(
                    shape = CircleShape,
                    color = gripColor,
                    contentColor = gripContentColor,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.OpenInFull,
                            contentDescription = "Drag corner to resize overlay width and height",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
        }
    }

    if (isFull) {
        val alignment = if (panelStyle == OverlayPanelStyle.TICKER_STRIP) {
            Alignment.TopCenter
        } else {
            Alignment.Center
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(20.dp),
            contentAlignment = alignment
        ) {
            panelNode()
        }
    } else {
        panelNode()
    }
}

@Composable
private fun OverlayMiniPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isFull: Boolean,
    onDarkOverlay: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    val bg = when {
        onDarkOverlay -> Color.White.copy(alpha = 0.16f)
        isFull -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.80f)
    }
    val border = if (onDarkOverlay) {
        Color.White.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    }
    val fg = if (onDarkOverlay) Color.White else MaterialTheme.colorScheme.onSurface
    val iconTint = if (onDarkOverlay) Color.White else MaterialTheme.colorScheme.primary

    Surface(
        shape = shape,
        color = bg,
        modifier = Modifier
            .border(0.6.dp, border, shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(12.dp),
                tint = iconTint
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg
            )
        }
    }
}
