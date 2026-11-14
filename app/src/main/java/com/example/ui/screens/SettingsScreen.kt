package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppPreferences
import com.example.data.local.DeskLaunchMode
import com.example.data.local.OverlayAnimationStyle
import com.example.data.local.OverlayDisplayMode
import com.example.data.local.OverlayPanelStyle
import com.example.data.local.QuoteScheduleMode
import com.example.data.repository.BackupJsonManager
import com.example.data.repository.MotivationProvider
import com.example.data.repository.QuoteCategory
import com.example.permissions.PermissionHelper
import com.example.permissions.PermissionStatusSnapshot
import com.example.ui.components.OverlayPanelShell
import com.example.ui.components.PermissionActionRow
import com.example.ui.theme.JetBrainsClockMono
import com.example.ui.theme.ThemeMode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    permissionSnapshot: PermissionStatusSnapshot,
    reminderCount: Int,
    taskCount: Int,
    proofCount: Int,
    stickyNoteCount: Int,
    backupFeedback: String?,
    onClearFeedback: () -> Unit,
    onSaveExportJsonUri: (Uri) -> Unit,
    onShareJsonRequest: ((Uri?) -> Unit) -> Unit,
    onImportJsonUri: (Uri) -> Unit,
    onRefreshPermissions: () -> Unit,
    onSnoozeChange: (Int) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onFadeInChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOverlayModeChange: (OverlayDisplayMode) -> Unit,
    onOverlayPanelStyleChange: (OverlayPanelStyle) -> Unit,
    onOverlayAnimationStyleChange: (OverlayAnimationStyle) -> Unit,
    onOverlayPanelScaleChange: (Float) -> Unit = {},
    onOverlayMediaHeightScaleChange: (Float) -> Unit = {},
    onOverlayTextScaleChange: (Float) -> Unit = {},
    onAlarmActivePanelStyleChange: (OverlayPanelStyle) -> Unit = {},
    onAlarmActivePanelScaleChange: (Float) -> Unit = {},
    onAlarmActiveMediaHeightScaleChange: (Float) -> Unit = {},
    onAlarmActiveTextScaleChange: (Float) -> Unit = {},
    onAlarmActiveCornerRadiusChange: (Int) -> Unit = {},
    onAlarmActiveOpacityChange: (Float) -> Unit = {},
    onAlarmActiveShowQuoteChange: (Boolean) -> Unit = {},
    onOverlayNotificationEnabledChange: (Boolean) -> Unit = {},
    onDeskLaunchModeChange: (DeskLaunchMode) -> Unit,
    onDeskKeepScreenOnChange: (Boolean) -> Unit,
    onDeskDimModeChange: (Boolean) -> Unit,
    onQuoteNotificationsEnabledChange: (Boolean) -> Unit = {},
    onQuoteScheduleModeChange: (QuoteScheduleMode) -> Unit = {},
    onQuoteRandomPerDayChange: (Int) -> Unit = {},
    onQuoteIntervalMinutesChange: (Int) -> Unit = {},
    onQuoteCategoryChange: (QuoteCategory) -> Unit = {},
    onSendQuoteNow: () -> Unit = {},
    onOpenDeskBoard: () -> Unit
) {
    val context = LocalContext.current

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onRefreshPermissions()
    }

    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            onSaveExportJsonUri(uri)
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportJsonUri(uri)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Save / Export / Import JSON Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text("Save & Export as JSON", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "$reminderCount reminder(s), $taskCount task(s) ($proofCount proof(s)), $stickyNoteCount note(s)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { createJsonLauncher.launch(BackupJsonManager.suggestedFilename()) },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("save_export_json_btn")
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Save / Export JSON")
                        }

                        FilledTonalButton(
                            onClick = {
                                onShareJsonRequest { shareUri ->
                                    if (shareUri != null) {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_STREAM, shareUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Export DayPin 365 JSON"))
                                    }
                                }
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("share_json_btn")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Share JSON")
                        }

                        OutlinedButton(
                            onClick = {
                                importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("import_json_btn")
                        ) {
                            Icon(Icons.Filled.UploadFile, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Import JSON")
                        }
                    }

                    if (!backupFeedback.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = backupFeedback,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onClearFeedback) {
                                    Text("OK")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 10 Curated Themes Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text("10 Curated Themes", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "Select any light or dark palette—applies across tabs, Desk Mode, ring screens, and overlays",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = preferences.themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                leadingIcon = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(mode.previewPrimary)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(mode.previewSurface)
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                },
                                label = { Text(mode.displayName) },
                                modifier = Modifier.testTag("settings_theme_${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // 5 Overlay Panel Variations & 7 Tween Animations Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Overlay Panels & Tween Animations", style = MaterialTheme.typography.titleLarge)

                    Text("Overlay Screen Mode", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OverlayDisplayMode.entries.forEach { mode ->
                            FilterChip(
                                selected = preferences.overlayDisplayMode == mode,
                                onClick = { onOverlayModeChange(mode) },
                                label = { Text(mode.label) },
                                modifier = Modifier.testTag(
                                    if (mode == OverlayDisplayMode.FULL) {
                                        "settings_overlay_full"
                                    } else {
                                        "settings_overlay_transparent"
                                    }
                                )
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DashboardCustomize,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("14 Overlay Panel Variations", style = MaterialTheme.typography.labelLarge)
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OverlayPanelStyle.entries.forEach { style ->
                            FilterChip(
                                selected = preferences.overlayPanelStyle == style,
                                onClick = { onOverlayPanelStyleChange(style) },
                                label = { Text(style.label) },
                                modifier = Modifier.testTag("settings_panel_${style.name.lowercase()}")
                            )
                        }
                    }
                    Text(
                        text = preferences.overlayPanelStyle.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Animation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Overlay Tween Animation (Enter & Exit)", style = MaterialTheme.typography.labelLarge)
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OverlayAnimationStyle.entries.forEach { anim ->
                            FilterChip(
                                selected = preferences.overlayAnimationStyle == anim,
                                onClick = { onOverlayAnimationStyleChange(anim) },
                                label = { Text(anim.label) },
                                modifier = Modifier.testTag("settings_anim_${anim.name.lowercase()}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Overlay Appearance Notification", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Show alert notification when overlay panel appears (off = quiet silent mode)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.overlayNotificationEnabled,
                            onCheckedChange = onOverlayNotificationEnabledChange,
                            modifier = Modifier.testTag("settings_overlay_notif_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Interactive Resizable Panel, Alarm Active & Text Visualizer
                    var previewAlarmActive by rememberSaveable { mutableStateOf(true) }
                    val activePanelScale = if (previewAlarmActive) {
                        preferences.alarmActivePanelScale
                    } else {
                        preferences.overlayPanelScale
                    }
                    val activeMediaHeightScale = if (previewAlarmActive) {
                        preferences.alarmActiveMediaHeightScale
                    } else {
                        preferences.overlayMediaHeightScale
                    }
                    val activeTextScale = if (previewAlarmActive) {
                        preferences.alarmActiveTextScale
                    } else {
                        preferences.overlayTextScale
                    }
                    val panelMinScale = 0.50f
                    val panelMaxScale = 2.20f
                    val textMinScale = 0.80f
                    val textMaxScale = 1.45f

                    val onActivePanelScaleChange: (Float) -> Unit = if (previewAlarmActive) {
                        onAlarmActivePanelScaleChange
                    } else {
                        onOverlayPanelScaleChange
                    }
                    val onActiveMediaHeightScaleChange: (Float) -> Unit = if (previewAlarmActive) {
                        onAlarmActiveMediaHeightScaleChange
                    } else {
                        onOverlayMediaHeightScaleChange
                    }
                    val onActiveTextScaleChange: (Float) -> Unit = if (previewAlarmActive) {
                        onAlarmActiveTextScaleChange
                    } else {
                        onOverlayTextScaleChange
                    }

                    val isModified = if (previewAlarmActive) {
                        preferences.alarmActivePanelStyle != OverlayPanelStyle.PLAIN_MEDIA ||
                            abs(preferences.alarmActivePanelScale - 1.0f) > 0.02f ||
                            abs(preferences.alarmActiveMediaHeightScale - 1.0f) > 0.02f ||
                            abs(preferences.alarmActiveTextScale - 1.0f) > 0.02f ||
                            preferences.alarmActiveCornerRadius != 22 ||
                            abs(preferences.alarmActiveOpacity - 0.94f) > 0.02f ||
                            !preferences.alarmActiveShowQuote
                    } else {
                        abs(preferences.overlayPanelScale - 1.0f) > 0.02f ||
                            abs(preferences.overlayMediaHeightScale - 1.0f) > 0.02f ||
                            abs(preferences.overlayTextScale - 1.0f) > 0.02f
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AspectRatio,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Panel, Alarm Active & Text Visualizer", style = MaterialTheme.typography.labelLarge)
                        }
                        if (isModified) {
                            TextButton(
                                onClick = {
                                    if (previewAlarmActive) {
                                        onAlarmActivePanelStyleChange(OverlayPanelStyle.PLAIN_MEDIA)
                                        onAlarmActivePanelScaleChange(1.0f)
                                        onAlarmActiveMediaHeightScaleChange(1.0f)
                                        onAlarmActiveTextScaleChange(1.0f)
                                        onAlarmActiveCornerRadiusChange(22)
                                        onAlarmActiveOpacityChange(0.94f)
                                        onAlarmActiveShowQuoteChange(true)
                                    } else {
                                        onOverlayPanelScaleChange(1.0f)
                                        onOverlayMediaHeightScaleChange(1.0f)
                                        onOverlayTextScaleChange(1.0f)
                                    }
                                },
                                modifier = Modifier.testTag("settings_visualizer_reset_btn")
                            ) {
                                Text("Reset")
                            }
                        }
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !previewAlarmActive,
                            onClick = { previewAlarmActive = false },
                            label = { Text("Panel Overlay") },
                            modifier = Modifier.testTag("visualizer_target_panel")
                        )
                        FilterChip(
                            selected = previewAlarmActive,
                            onClick = { previewAlarmActive = true },
                            label = { Text("Alarm Active") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Alarm,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("visualizer_target_alarm")
                        )
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF141821),
                                        Color(0xFF1F2735)
                                    )
                                )
                            )
                            .border(
                                width = 0.75.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(16.dp)
                            .testTag("settings_size_visualizer_stage"),
                        contentAlignment = Alignment.Center
                    ) {
                        val density = LocalDensity.current
                        val isPreviewFull = preferences.overlayDisplayMode == OverlayDisplayMode.FULL
                        val previewStyle = if (previewAlarmActive) {
                            preferences.alarmActivePanelStyle
                        } else {
                            preferences.overlayPanelStyle
                        }
                        val isPlainMediaPreview = previewStyle == OverlayPanelStyle.PLAIN_MEDIA
                        val isEditorialPreview = previewStyle == OverlayPanelStyle.MEDIA_EDITORIAL
                        val isGradientPreview = previewStyle == OverlayPanelStyle.FULL_MEDIA_GRADIENT

                        var livePreviewPanelScale by remember(previewAlarmActive) {
                            mutableFloatStateOf(activePanelScale)
                        }
                        var livePreviewMediaHeightScale by remember(previewAlarmActive) {
                            mutableFloatStateOf(activeMediaHeightScale)
                        }
                        var isDraggingPreview by remember { mutableStateOf(false) }

                        LaunchedEffect(activePanelScale) {
                            if (!isDraggingPreview) {
                                livePreviewPanelScale = activePanelScale
                            }
                        }
                        LaunchedEffect(activeMediaHeightScale) {
                            if (!isDraggingPreview) {
                                livePreviewMediaHeightScale = activeMediaHeightScale
                            }
                        }

                        val latestOnScaleChange by rememberUpdatedState(onActivePanelScaleChange)
                        val latestOnMediaHeightChange by rememberUpdatedState(onActiveMediaHeightScaleChange)
                        val latestMinScale by rememberUpdatedState(panelMinScale)
                        val latestMaxScale by rememberUpdatedState(panelMaxScale)

                        val basePreviewWidth = when {
                            previewStyle.isSmallVariant -> if (isPreviewFull) 232.dp else 196.dp
                            previewAlarmActive -> if (isPreviewFull) 288.dp else 238.dp
                            else -> if (isPreviewFull) 268.dp else 224.dp
                        }
                        val minPreviewWidth = if (previewStyle.isSmallVariant) 144.dp else 168.dp
                        val maxStageWidth = (maxWidth - 8.dp).coerceAtLeast(minPreviewWidth)
                        val computedPreviewWidth = (basePreviewWidth.value * livePreviewPanelScale).dp
                            .coerceIn(minPreviewWidth, maxStageWidth)

                        Box(modifier = Modifier.wrapContentSize()) {
                            OverlayPanelShell(
                                panelStyle = previewStyle,
                                baseSurfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                accentBorderColor = if (isPlainMediaPreview) {
                                    Color(0xFFF08A4B)
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                isFullMode = isPreviewFull,
                                minWidth = minPreviewWidth,
                                maxWidth = maxStageWidth,
                                targetWidth = computedPreviewWidth,
                                maxMediaHeightDp = 380.dp,
                                panelScale = livePreviewPanelScale,
                                mediaHeightScale = livePreviewMediaHeightScale,
                                textScale = activeTextScale,
                                stretchMediaToFill = isPlainMediaPreview,
                                cornerRadiusOverrideDp = if (previewAlarmActive) preferences.alarmActiveCornerRadius else null,
                                surfaceAlphaOverride = if (previewAlarmActive) preferences.alarmActiveOpacity else null,
                                spotlightHeaderLabel = if (previewAlarmActive) {
                                    "ALARM • ${previewStyle.label.uppercase()}"
                                } else {
                                    "OVERLAY • ${previewStyle.label.uppercase()}"
                                },
                                hasAudioMedia = true,
                                topBarContent = if (isEditorialPreview) {
                                    {
                                        Text(
                                            text = if (previewAlarmActive) "Snooze" else "Next",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(
                                                imageVector = Icons.Filled.Style,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Icon(
                                                imageVector = Icons.Filled.MoreHoriz,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = null,
                                                tint = if (previewAlarmActive) {
                                                    MaterialTheme.colorScheme.error
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                } else {
                                    null
                                }
                            ) {
                                if (previewStyle == OverlayPanelStyle.PLAIN_MEDIA) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (previewAlarmActive) "Morning Standup" else "Focus Session",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (previewAlarmActive) {
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = Color.White.copy(alpha = 0.14f)
                                                ) {
                                                    Text(
                                                        text = "Snooze",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = Icons.Filled.Style,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Icon(
                                                imageVector = Icons.Filled.MoreHoriz,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = null,
                                                tint = if (previewAlarmActive) {
                                                    Color(0xFFFFB4AB)
                                                } else {
                                                    Color.White.copy(alpha = 0.85f)
                                                },
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    if (previewAlarmActive && preferences.alarmActiveShowQuote) {
                                        Text(
                                            text = "\"Small steps every day build momentum.\"",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsClockMono),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(end = 28.dp)
                                        )
                                    }
                                } else if (isEditorialPreview) {
                                    Text(
                                        text = if (previewAlarmActive) "Morning Standup" else "Morning Focus",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (previewAlarmActive) {
                                            if (preferences.alarmActiveShowQuote) {
                                                "No clock clutter — steady focus beats intensity."
                                            } else {
                                                "Clock-free panel layout with top Snooze, Style & Dismiss controls."
                                            }
                                        } else {
                                            "Top buttons over left media with clean right header and paragraph (no clock)."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else if (previewAlarmActive) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isGradientPreview) Color.White else MaterialTheme.colorScheme.error
                                                    )
                                            )
                                            Text(
                                                text = previewStyle.label.uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isGradientPreview) Color.White else MaterialTheme.colorScheme.error,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Filled.MoreHoriz,
                                            contentDescription = null,
                                            tint = if (isGradientPreview) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "Morning Standup",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (preferences.alarmActiveShowQuote) {
                                        Text(
                                            text = "Consistency compounds quietly every day.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isGradientPreview) {
                                                Color.White.copy(alpha = 0.22f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHighest
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "Snooze",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isGradientPreview) Color.White else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "Dismiss",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                maxLines = 1,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = preferences.overlayPanelStyle.label.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isGradientPreview) Color.White else MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.MoreHoriz,
                                            contentDescription = null,
                                            tint = if (isGradientPreview) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "Morning Focus Sprint",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Free 2D corner drag resize across all 14 styles.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = if (preferences.overlayPanelStyle.isSmallVariant) 1 else 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Interactive 2D Corner Drag-to-Resize Handle (48dp touch target) for ALL 14 styles
                            val dragPreviewWidthPx = with(density) { 200.dp.toPx().coerceAtLeast(1f) }
                            val dragPreviewHeightPx = with(density) { 150.dp.toPx().coerceAtLeast(1f) }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 6.dp, y = 6.dp)
                                    .size(48.dp)
                                    .pointerInput(previewAlarmActive, dragPreviewWidthPx, dragPreviewHeightPx) {
                                        detectDragGestures(
                                            onDragStart = {
                                                isDraggingPreview = true
                                            },
                                            onDragEnd = {
                                                isDraggingPreview = false
                                                latestOnScaleChange(livePreviewPanelScale)
                                                latestOnMediaHeightChange(livePreviewMediaHeightScale)
                                            },
                                            onDragCancel = {
                                                isDraggingPreview = false
                                                latestOnScaleChange(livePreviewPanelScale)
                                                latestOnMediaHeightChange(livePreviewMediaHeightScale)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                livePreviewPanelScale = (livePreviewPanelScale + dragAmount.x / dragPreviewWidthPx)
                                                    .coerceIn(latestMinScale, latestMaxScale)
                                                livePreviewMediaHeightScale = (livePreviewMediaHeightScale + dragAmount.y / dragPreviewHeightPx)
                                                    .coerceIn(0.50f, 2.40f)
                                            }
                                        )
                                    }
                                    .testTag("visualizer_resize_handle")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isPlainMediaPreview) Color(0xFFF08A4B) else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isPlainMediaPreview) Color(0xFF1A1614) else MaterialTheme.colorScheme.onPrimary,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.OpenInFull,
                                            contentDescription = "Drag corner to freely resize preview panel width and height",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = "Drag the corner handle in 2D (width & height) or adjust controls below to resize ${if (previewAlarmActive) "Alarm Active" else "Panel Overlay"} in real time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AspectRatio,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (previewAlarmActive) "Alarm Active Width Scale" else "Panel Overlay Width Scale",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "%.2fx", activePanelScale),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "XS" to 0.68f,
                                "S" to 0.86f,
                                "M" to 1.00f,
                                "L" to 1.30f,
                                "XL" to 1.65f
                            ).forEach { (label, scaleValue) ->
                                FilterChip(
                                    selected = abs(activePanelScale - scaleValue) < 0.05f,
                                    onClick = { onActivePanelScaleChange(scaleValue) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("settings_panel_size_${label.lowercase()}")
                                )
                            }
                        }
                        Slider(
                            value = activePanelScale.coerceIn(panelMinScale, panelMaxScale),
                            onValueChange = onActivePanelScaleChange,
                            valueRange = panelMinScale..panelMaxScale,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_panel_size_slider")
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AspectRatio,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (previewAlarmActive) {
                                        "Alarm Active Height & Media Scale"
                                    } else {
                                        "Panel Overlay Height & Media Scale"
                                    },
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "%.2fx", activeMediaHeightScale),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "XS" to 0.65f,
                                "Compact" to 0.82f,
                                "Default" to 1.00f,
                                "Tall" to 1.40f,
                                "XL" to 1.85f
                            ).forEach { (label, heightScale) ->
                                FilterChip(
                                    selected = abs(activeMediaHeightScale - heightScale) < 0.05f,
                                    onClick = { onActiveMediaHeightScaleChange(heightScale) },
                                    label = { Text(label) }
                                )
                            }
                        }
                        Slider(
                            value = activeMediaHeightScale.coerceIn(0.50f, 2.40f),
                            onValueChange = onActiveMediaHeightScaleChange,
                            valueRange = 0.50f..2.40f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_alarm_media_height_slider")
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FormatSize,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (previewAlarmActive) "Alarm Active Text Size" else "Panel Text Size",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "%.2fx", activeTextScale),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "S" to 0.88f,
                                "M" to 1.00f,
                                "L" to 1.20f,
                                "XL" to 1.38f
                            ).forEach { (label, scaleValue) ->
                                FilterChip(
                                    selected = abs(activeTextScale - scaleValue) < 0.04f,
                                    onClick = { onActiveTextScaleChange(scaleValue) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("settings_text_size_${label.lowercase()}")
                                )
                            }
                        }
                        Slider(
                            value = activeTextScale.coerceIn(textMinScale, textMaxScale),
                            onValueChange = onActiveTextScaleChange,
                            valueRange = textMinScale..textMaxScale,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_text_size_slider")
                        )
                    }

                    if (previewAlarmActive) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                        Text(
                            text = "Alarm Active Customizer",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Style,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Alarm Active Card Look", style = MaterialTheme.typography.labelLarge)
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OverlayPanelStyle.entries.forEach { style ->
                                    FilterChip(
                                        selected = preferences.alarmActivePanelStyle == style,
                                        onClick = { onAlarmActivePanelStyleChange(style) },
                                        label = {
                                            Text(
                                                if (style == OverlayPanelStyle.PLAIN_MEDIA) {
                                                    "Ref Media Card"
                                                } else {
                                                    style.shortTag
                                                }
                                            )
                                        },
                                        modifier = Modifier.testTag("settings_alarm_style_${style.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.RoundedCorner,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("Corner Roundness", style = MaterialTheme.typography.labelLarge)
                                }
                                Text(
                                    text = "${preferences.alarmActiveCornerRadius} dp",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = preferences.alarmActiveCornerRadius.toFloat().coerceIn(12f, 32f),
                                onValueChange = { onAlarmActiveCornerRadiusChange(it.roundToInt()) },
                                valueRange = 12f..32f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_alarm_corner_slider")
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Opacity,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("Surface Opacity", style = MaterialTheme.typography.labelLarge)
                                }
                                Text(
                                    text = "${(preferences.alarmActiveOpacity * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = preferences.alarmActiveOpacity.coerceIn(0.70f, 1.00f),
                                onValueChange = onAlarmActiveOpacityChange,
                                valueRange = 0.70f..1.00f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_alarm_opacity_slider")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Show Motivation Quote", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Display an uplifting quote banner inside the clock-free Alarm Active card",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = preferences.alarmActiveShowQuote,
                                onCheckedChange = onAlarmActiveShowQuoteChange,
                                modifier = Modifier.testTag("settings_alarm_quote_switch")
                            )
                        }
                    }
                }
            }
        }

        // Separate 100+ Motivation & Life Quotes Daily Notification Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "100+ Daily Quotes (No Alarm)",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "Separate gentle notifications (${MotivationProvider.quotes.size} quotes) — never triggers loud alarm ringtones",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = preferences.quoteNotificationsEnabled,
                            onCheckedChange = onQuoteNotificationsEnabledChange,
                            modifier = Modifier.testTag("settings_quotes_enabled_switch")
                        )
                    }

                    Text("Quote Category", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuoteCategory.entries.forEach { cat ->
                            val count = MotivationProvider.countForCategory(cat)
                            FilterChip(
                                selected = preferences.quoteCategory == cat,
                                onClick = { onQuoteCategoryChange(cat) },
                                label = { Text("${cat.label} ($count)") },
                                modifier = Modifier.testTag("settings_quote_cat_${cat.name.lowercase()}")
                            )
                        }
                    }

                    Text("Schedule Timing", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuoteScheduleMode.entries.forEach { mode ->
                            FilterChip(
                                selected = preferences.quoteScheduleMode == mode,
                                onClick = { onQuoteScheduleModeChange(mode) },
                                label = { Text(mode.label) },
                                modifier = Modifier.testTag("settings_quote_mode_${mode.name.lowercase()}")
                            )
                        }
                    }
                    Text(
                        text = preferences.quoteScheduleMode.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (preferences.quoteScheduleMode == QuoteScheduleMode.RANDOM_PER_DAY) {
                        Text("Quotes Per Day (Random Times)", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 3, 5, 10).forEach { count ->
                                FilterChip(
                                    selected = preferences.quoteRandomPerDay == count,
                                    onClick = { onQuoteRandomPerDayChange(count) },
                                    label = { Text("$count / day") },
                                    modifier = Modifier.testTag("settings_quote_per_day_$count")
                                )
                            }
                        }
                    } else {
                        Text("Custom Interval (Minutes / Hours)", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                10 to "10 min",
                                15 to "15 min",
                                30 to "30 min",
                                60 to "1 hr",
                                120 to "2 hr",
                                240 to "4 hr"
                            ).forEach { (mins, label) ->
                                FilterChip(
                                    selected = preferences.quoteIntervalMinutes == mins,
                                    onClick = { onQuoteIntervalMinutesChange(mins) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("settings_quote_interval_${mins}m")
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    onQuoteIntervalMinutesChange((preferences.quoteIntervalMinutes - 5).coerceAtLeast(1))
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease quote interval")
                            }
                            Text(
                                text = "Every ${preferences.quoteIntervalMinutes} min",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            FilledTonalIconButton(
                                onClick = {
                                    onQuoteIntervalMinutesChange((preferences.quoteIntervalMinutes + 5).coerceAtMost(720))
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase quote interval")
                            }
                        }
                    }

                    FilledTonalButton(
                        onClick = onSendQuoteNow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_send_quote_now_btn")
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Send Quote Notification Now")
                    }
                }
            }
        }

        // Permissions Card
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Permissions & Battery Settings", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "DayPin 365 uses zero background polling and wakes only when an exact alarm is scheduled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PermissionActionRow(
                    icon = Icons.Filled.NotificationsActive,
                    title = "Alarm Notifications",
                    subtitle = "Required for Android 13+ alarm alerts.",
                    granted = permissionSnapshot.notificationsGranted,
                    testTag = "settings_perm_notifications",
                    onGrantClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            context.startActivity(PermissionHelper.buildNotificationSettingsIntent(context))
                        }
                    }
                )

                PermissionActionRow(
                    icon = Icons.Filled.Layers,
                    title = "Floating Sticky Note Overlay",
                    subtitle = "Display pinned sticky notes over other apps.",
                    granted = permissionSnapshot.overlayGranted,
                    testTag = "settings_perm_overlay",
                    onGrantClick = {
                        context.startActivity(PermissionHelper.buildOverlaySettingsIntent(context))
                    }
                )

                PermissionActionRow(
                    icon = Icons.Filled.Alarm,
                    title = "Exact Alarms",
                    subtitle = "Ensures scheduled alarms fire at the exact minute.",
                    granted = permissionSnapshot.exactAlarmGranted,
                    testTag = "settings_perm_exact_alarm",
                    onGrantClick = {
                        context.startActivity(PermissionHelper.buildExactAlarmSettingsIntent(context))
                    }
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    PermissionActionRow(
                        icon = Icons.Filled.Fullscreen,
                        title = "Full-Screen Alarm Intent",
                        subtitle = "Shows image, music, and video alarms over lock screen.",
                        granted = permissionSnapshot.fullScreenIntentGranted,
                        testTag = "settings_perm_fullscreen",
                        onGrantClick = {
                            context.startActivity(PermissionHelper.buildFullScreenIntentSettingsIntent(context))
                        }
                    )
                }
            }
        }

        // Alarm Behavior Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Alarm Behavior", style = MaterialTheme.typography.titleLarge)

                    Text("Default Snooze Duration", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15, 20, 30).forEach { mins ->
                            FilterChip(
                                selected = preferences.defaultSnoozeMinutes == mins,
                                onClick = { onSnoozeChange(mins) },
                                label = { Text("$mins min") }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gentle Audio Fade-In", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Gradually increases music or alarm volume over 8 seconds",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.fadeInAudio,
                            onCheckedChange = onFadeInChange
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Alarm Vibration", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Vibrate gently alongside alarm media",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.vibrationEnabled,
                            onCheckedChange = onVibrationChange
                        )
                    }
                }
            }
        }

        // Tablet Desk Clock & Standby Board Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Desk Clock & Standby Board", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Lightweight, zero-polling standby clock with active tasks and sticky notes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text("Start Mode (Manual / Auto-Start)", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeskLaunchMode.entries.forEach { mode ->
                            FilterChip(
                                selected = preferences.deskLaunchMode == mode,
                                onClick = { onDeskLaunchModeChange(mode) },
                                label = { Text(mode.label) },
                                modifier = Modifier.testTag("settings_desk_${mode.name.lowercase()}")
                            )
                        }
                    }
                    Text(
                        text = preferences.deskLaunchMode.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dim Night Standby", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Low-luminance dark slate clock with slower refresh to save battery",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.deskDimMode,
                            onCheckedChange = onDeskDimModeChange
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Keep Screen On in Desk Mode", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Only keeps display awake while Desk Standby Board is visible",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.deskKeepScreenOn,
                            onCheckedChange = onDeskKeepScreenOnChange
                        )
                    }

                    Button(
                        onClick = onOpenDeskBoard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_open_desk_btn")
                    ) {
                        Icon(Icons.Filled.Alarm, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open Desk Clock & Standby Board")
                    }
                }
            }
        }
    }
}
