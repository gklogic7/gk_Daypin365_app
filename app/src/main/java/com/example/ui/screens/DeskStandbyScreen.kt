package com.example.ui.screens

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.AppPreferences
import com.example.data.local.DeskLaunchMode
import com.example.data.model.ReminderEntity
import com.example.data.model.StickyNoteEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskStatus
import com.example.data.repository.CachedMediaResult
import com.example.ui.DateCalendarUtils
import com.example.ui.components.TaskProofCaptureDialog
import com.example.ui.theme.JetBrainsClockMono
import com.example.ui.theme.stickyBorderColor
import com.example.ui.theme.stickySurfaceColor
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeskStandbyScreen(
    preferences: AppPreferences,
    reminders: List<ReminderEntity>,
    tasks: List<TaskEntity>,
    stickyNotes: List<StickyNoteEntity>,
    onExitDeskMode: () -> Unit,
    onDeskLaunchModeChange: (DeskLaunchMode) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onDimModeChange: (Boolean) -> Unit,
    onTestAlarm: (ReminderEntity) -> Unit,
    onTestTaskAlarm: (TaskEntity) -> Unit,
    onPersistProofImage: suspend (android.net.Uri) -> CachedMediaResult,
    onSubmitTaskProof: (Long, TaskStatus, String, String, String) -> Unit,
    onDeferTaskLater: (Long, Int) -> Unit,
    onTogglePinNote: (StickyNoteEntity) -> Unit
) {
    val context = LocalContext.current
    val isDim = preferences.deskDimMode

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var batteryPct by remember { mutableIntStateOf(-1) }
    var isCharging by remember { mutableStateOf(false) }

    var proofDialogRequest by remember { mutableStateOf<Pair<TaskEntity, TaskStatus>?>(null) }
    var laterPickerTask by remember { mutableStateOf<TaskEntity?>(null) }

    BackHandler(onBack = onExitDeskMode)

    // Battery-efficient clock ticker: 60s updates in Dim Standby, 1s updates in normal Desk Mode
    LaunchedEffect(isDim) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(if (isDim) 30_000L else 1_000L)
        }
    }

    // Scoped FLAG_KEEP_SCREEN_ON only while Desk Standby is actively displayed
    DisposableEffect(preferences.deskKeepScreenOn) {
        val window = (context as? Activity)?.window
        if (preferences.deskKeepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Event-driven battery & charging monitor (zero polling)
    DisposableEffect(context) {
        fun readBattery(intent: Intent?) {
            if (intent == null) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            if (level >= 0) {
                batteryPct = ((level * 100f) / scale).toInt()
            }
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        }

        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                readBattery(intent)
            }
        }
        val sticky = ContextCompat.registerReceiver(
            context,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        readBattery(sticky)

        onDispose {
            runCatching { context.unregisterReceiver(batteryReceiver) }
        }
    }

    val nextReminder = remember(reminders, nowMillis) {
        reminders.filter { it.isEnabled && it.triggerAtMillis > nowMillis }.minByOrNull { it.triggerAtMillis }
    }
    val activeTasks = remember(tasks) {
        tasks.filter { it.status != TaskStatus.DONE }.sortedBy { it.triggerAtMillis }
    }
    val pinnedOrTopNotes = remember(stickyNotes) {
        val pinned = stickyNotes.filter { it.isPinnedOverlay }
        if (pinned.isNotEmpty()) pinned else stickyNotes.take(4)
    }

    val bgColor = if (isDim) Color(0xFF0E1014) else MaterialTheme.colorScheme.background
    val cardColor = if (isDim) Color(0xFF171A21) else MaterialTheme.colorScheme.surface
    val fgColor = if (isDim) Color(0xFFE7D3B8) else MaterialTheme.colorScheme.onBackground
    val mutedFg = if (isDim) Color(0xFF9EA4B0) else MaterialTheme.colorScheme.onSurfaceVariant

    val timeFmt = remember(isDim) {
        SimpleDateFormat(if (isDim) "HH:mm" else "HH:mm:ss", Locale.getDefault())
    }
    val dateFmt = remember { SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()) }

    Surface(
        color = bgColor,
        modifier = Modifier.fillMaxSize()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            val isWide = maxWidth >= 680.dp

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Control Strip: Auto-Start / Manual selector, Dim toggle, Keep Awake, Exit
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DeskLaunchMode.entries.forEach { mode ->
                            FilterChip(
                                selected = preferences.deskLaunchMode == mode,
                                onClick = { onDeskLaunchModeChange(mode) },
                                label = { Text(mode.label) },
                                modifier = Modifier.testTag("desk_mode_${mode.name.lowercase()}")
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = isDim,
                            onClick = { onDimModeChange(!isDim) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isDim) Icons.Filled.Bedtime else Icons.Filled.LightMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(if (isDim) "Dim On" else "Dim") },
                            modifier = Modifier.testTag("desk_dim_toggle")
                        )

                        FilterChip(
                            selected = preferences.deskKeepScreenOn,
                            onClick = { onKeepScreenOnChange(!preferences.deskKeepScreenOn) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Stay Awake") },
                            modifier = Modifier.testTag("desk_keep_awake_toggle")
                        )

                        FilledTonalButton(
                            onClick = onExitDeskMode,
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("desk_exit_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Exit Desk")
                        }
                    }
                }

                if (isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Left Pane: Clock, Battery, Next Alarm
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DeskClockHeroCard(
                                timeText = timeFmt.format(Date(nowMillis)),
                                dateText = dateFmt.format(Date(nowMillis)),
                                batteryPct = batteryPct,
                                isCharging = isCharging,
                                launchMode = preferences.deskLaunchMode,
                                cardColor = cardColor,
                                fgColor = fgColor,
                                mutedFg = mutedFg
                            )
                            NextAlarmStandbyCard(
                                nextReminder = nextReminder,
                                nextTask = activeTasks.firstOrNull { it.isEnabled && it.triggerAtMillis > nowMillis },
                                cardColor = cardColor,
                                fgColor = fgColor,
                                mutedFg = mutedFg,
                                onTestReminder = onTestAlarm,
                                onTestTask = onTestTaskAlarm
                            )
                        }

                        // Right Pane: Active Tasks + Sticky Notes
                        LazyColumn(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "ACTIVE TASKS (${activeTasks.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = mutedFg
                                )
                            }
                            if (activeTasks.isEmpty()) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = cardColor,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "No active tasks pending. Create a Task alarm in the Tasks tab.",
                                            color = mutedFg,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            } else {
                                items(activeTasks, key = { "desk_task_${it.id}" }) { task ->
                                    DeskTaskQuickCard(
                                        task = task,
                                        cardColor = cardColor,
                                        fgColor = fgColor,
                                        mutedFg = mutedFg,
                                        onRequestProof = { st -> proofDialogRequest = task to st },
                                        onRequestLater = { laterPickerTask = task }
                                    )
                                }
                            }

                            item {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "DESK STICKY NOTES (${pinnedOrTopNotes.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = mutedFg
                                )
                            }
                            items(pinnedOrTopNotes, key = { "desk_note_${it.id}" }) { note ->
                                DeskStickyNoteCard(
                                    note = note,
                                    isDim = isDim,
                                    onTogglePin = { onTogglePinNote(note) }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            DeskClockHeroCard(
                                timeText = timeFmt.format(Date(nowMillis)),
                                dateText = dateFmt.format(Date(nowMillis)),
                                batteryPct = batteryPct,
                                isCharging = isCharging,
                                launchMode = preferences.deskLaunchMode,
                                cardColor = cardColor,
                                fgColor = fgColor,
                                mutedFg = mutedFg
                            )
                        }
                        item {
                            NextAlarmStandbyCard(
                                nextReminder = nextReminder,
                                nextTask = activeTasks.firstOrNull { it.isEnabled && it.triggerAtMillis > nowMillis },
                                cardColor = cardColor,
                                fgColor = fgColor,
                                mutedFg = mutedFg,
                                onTestReminder = onTestAlarm,
                                onTestTask = onTestTaskAlarm
                            )
                        }
                        item {
                            Text(
                                text = "ACTIVE TASKS (${activeTasks.size})",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = mutedFg
                            )
                        }
                        items(activeTasks, key = { "desk_task_m_${it.id}" }) { task ->
                            DeskTaskQuickCard(
                                task = task,
                                cardColor = cardColor,
                                fgColor = fgColor,
                                mutedFg = mutedFg,
                                onRequestProof = { st -> proofDialogRequest = task to st },
                                onRequestLater = { laterPickerTask = task }
                            )
                        }
                        if (pinnedOrTopNotes.isNotEmpty()) {
                            item {
                                Text(
                                    text = "STICKY NOTES (${pinnedOrTopNotes.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = mutedFg
                                )
                            }
                            items(pinnedOrTopNotes, key = { "desk_note_m_${it.id}" }) { note ->
                                DeskStickyNoteCard(
                                    note = note,
                                    isDim = isDim,
                                    onTogglePin = { onTogglePinNote(note) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    proofDialogRequest?.let { (task, targetStatus) ->
        TaskProofCaptureDialog(
            taskTitle = task.title,
            targetStatus = targetStatus,
            onPersistProofImage = onPersistProofImage,
            onDismiss = { proofDialogRequest = null },
            onSaveProof = { uri, name, note ->
                onSubmitTaskProof(task.id, targetStatus, uri, name, note)
                proofDialogRequest = null
            }
        )
    }

    laterPickerTask?.let { task ->
        AlertDialog(
            onDismissRequest = { laterPickerTask = null },
            title = { Text("Defer \"${task.title}\"") },
            text = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(preferences.defaultSnoozeMinutes, 15, 30, 60).distinct().forEach { mins ->
                        FilledTonalButton(
                            onClick = {
                                onDeferTaskLater(task.id, mins)
                                laterPickerTask = null
                            }
                        ) {
                            Text(if (mins >= 60) "${mins / 60} hr" else "$mins min")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { laterPickerTask = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DeskClockHeroCard(
    timeText: String,
    dateText: String,
    batteryPct: Int,
    isCharging: Boolean,
    launchMode: DeskLaunchMode,
    cardColor: Color,
    fgColor: Color,
    mutedFg: Color
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = cardColor,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DESK STANDBY • ${launchMode.label.uppercase()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                if (batteryPct >= 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd,
                            contentDescription = "Battery status",
                            tint = if (isCharging) MaterialTheme.colorScheme.primary else mutedFg,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isCharging) "$batteryPct% Charging" else "$batteryPct%",
                            style = MaterialTheme.typography.labelMedium,
                            color = mutedFg
                        )
                    }
                }
            }

            Text(
                text = timeText,
                fontFamily = JetBrainsClockMono,
                fontSize = 54.sp,
                lineHeight = 58.sp,
                fontWeight = FontWeight.Bold,
                color = fgColor,
                modifier = Modifier.testTag("desk_clock_display")
            )

            Text(
                text = dateText,
                style = MaterialTheme.typography.titleMedium,
                color = mutedFg
            )
        }
    }
}

@Composable
private fun NextAlarmStandbyCard(
    nextReminder: ReminderEntity?,
    nextTask: TaskEntity?,
    cardColor: Color,
    fgColor: Color,
    mutedFg: Color,
    onTestReminder: (ReminderEntity) -> Unit,
    onTestTask: (TaskEntity) -> Unit
) {
    val useTask = when {
        nextReminder == null && nextTask != null -> true
        nextReminder != null && nextTask != null -> nextTask.triggerAtMillis < nextReminder.triggerAtMillis
        else -> false
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardColor,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "NEXT SCHEDULED",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = mutedFg
                )
            }

            if (useTask && nextTask != null) {
                Text(
                    text = nextTask.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = fgColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${DateCalendarUtils.formatTime(nextTask.triggerAtMillis)} • ${DateCalendarUtils.formatCountdown(nextTask.triggerAtMillis)} • Task (${nextTask.status.label})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedFg
                )
                OutlinedButton(onClick = { onTestTask(nextTask) }) {
                    Text("Trigger Task Check-In")
                }
            } else if (nextReminder != null) {
                Text(
                    text = nextReminder.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = fgColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${DateCalendarUtils.formatTime(nextReminder.triggerAtMillis)} • ${DateCalendarUtils.formatCountdown(nextReminder.triggerAtMillis)} • ${nextReminder.mediaSummaryLabel()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedFg
                )
                OutlinedButton(onClick = { onTestReminder(nextReminder) }) {
                    Text("Preview Alarm")
                }
            } else {
                Text(
                    text = "All clear for now",
                    style = MaterialTheme.typography.titleMedium,
                    color = fgColor
                )
                Text(
                    text = "No upcoming alarms or tasks scheduled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedFg
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeskTaskQuickCard(
    task: TaskEntity,
    cardColor: Color,
    fgColor: Color,
    mutedFg: Color,
    onRequestProof: (TaskStatus) -> Unit,
    onRequestLater: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardColor,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = fgColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${task.status.label} • ${DateCalendarUtils.formatTime(task.triggerAtMillis)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledTonalButton(
                    onClick = { onRequestProof(TaskStatus.STARTED) },
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Started")
                }
                FilledTonalButton(
                    onClick = { onRequestProof(TaskStatus.WORKING) },
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Working")
                }
                Button(
                    onClick = { onRequestProof(TaskStatus.DONE) },
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Done")
                }
                OutlinedButton(
                    onClick = onRequestLater,
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Later")
                }
            }
        }
    }
}

@Composable
private fun DeskStickyNoteCard(
    note: StickyNoteEntity,
    isDim: Boolean,
    onTogglePin: () -> Unit
) {
    val bg = if (isDim) Color(0xFF1F232C) else stickySurfaceColor(note.color.name)
    val border = if (isDim) Color(0xFF394150) else stickyBorderColor(note.color.name)
    val textColor = if (isDim) Color(0xFFE7D3B8) else MaterialTheme.colorScheme.onSurface

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bg,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, border, RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = note.title.ifBlank { "Sticky Note" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.content.isNotBlank()) {
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.85f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            TextButton(onClick = onTogglePin) {
                Icon(Icons.Filled.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (note.isPinnedOverlay) "Pinned" else "Pin")
            }
        }
    }
}
