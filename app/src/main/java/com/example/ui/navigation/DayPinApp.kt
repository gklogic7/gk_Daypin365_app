package com.example.ui.navigation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.AppPreferences
import com.example.data.local.DeskLaunchMode
import com.example.data.model.QuickTemplateEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.StickyNoteEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskProofEntity
import com.example.permissions.PermissionHelper
import com.example.permissions.PermissionStatusSnapshot
import com.example.ui.DayPinViewModel
import com.example.ui.components.PermissionOnboardingDialog
import com.example.ui.components.ReminderEditorDialog
import com.example.ui.components.StickyNoteEditorDialog
import com.example.ui.components.TaskEditorDialog
import com.example.ui.screens.DeskStandbyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StickyNotesScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.TodayAlarmsScreen
import com.example.ui.screens.YearCalendarScreen

enum class DayPinTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    TODAY(R.string.nav_today, Icons.Filled.Alarm, Icons.Outlined.Alarm, "nav_today"),
    CALENDAR(R.string.nav_calendar, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_calendar"),
    TASKS(R.string.nav_tasks, Icons.Filled.TaskAlt, Icons.Outlined.TaskAlt, "nav_tasks"),
    NOTES(R.string.nav_notes, Icons.Filled.StickyNote2, Icons.Outlined.StickyNote2, "nav_notes"),
    SETTINGS(R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
}

@Composable
fun DayPinApp(
    viewModel: DayPinViewModel,
    openDeskFromIntent: Boolean = false,
    onConsumeDeskIntent: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val stickyNotes by viewModel.stickyNotes.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val taskProofs by viewModel.taskProofs.collectAsStateWithLifecycle()
    val quickTemplates by viewModel.quickTemplates.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val permissionSnapshot by viewModel.permissionSnapshot.collectAsStateWithLifecycle()
    val selectedDayMillis by viewModel.selectedDayMillis.collectAsStateWithLifecycle()
    val selectedReminderId by viewModel.selectedReminderId.collectAsStateWithLifecycle()
    val selectedTaskId by viewModel.selectedTaskId.collectAsStateWithLifecycle()
    val backupFeedback by viewModel.backupFeedback.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(DayPinTab.TODAY) }
    var showDeskBoard by rememberSaveable { mutableStateOf(false) }
    var evaluatedInitialDeskAutoStart by rememberSaveable { mutableStateOf(false) }

    var showReminderEditor by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ReminderEntity?>(null) }
    var reminderTemplateForEditor by remember { mutableStateOf<QuickTemplateEntity?>(null) }
    var editorDefaultDay by remember { mutableStateOf(selectedDayMillis) }

    var showTaskEditor by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var taskTemplateForEditor by remember { mutableStateOf<QuickTemplateEntity?>(null) }

    var showNoteEditor by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<StickyNoteEntity?>(null) }
    var noteTemplateForEditor by remember { mutableStateOf<QuickTemplateEntity?>(null) }

    // Open Desk Standby Board from charging intent or auto-start preferences
    LaunchedEffect(openDeskFromIntent) {
        if (openDeskFromIntent) {
            showDeskBoard = true
            onConsumeDeskIntent()
        }
    }

    LaunchedEffect(preferences.deskLaunchMode) {
        if (!evaluatedInitialDeskAutoStart) {
            if (preferences.deskLaunchMode == DeskLaunchMode.AUTO_ON_APP_OPEN) {
                showDeskBoard = true
                evaluatedInitialDeskAutoStart = true
            } else if (preferences.deskLaunchMode == DeskLaunchMode.AUTO_ON_CHARGING) {
                val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
                if (isCharging) {
                    showDeskBoard = true
                }
                evaluatedInitialDeskAutoStart = true
            }
        }
    }

    // Event-driven charging listener while app is active (zero polling)
    DisposableEffect(context, preferences.deskLaunchMode) {
        val powerReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_POWER_CONNECTED &&
                    preferences.deskLaunchMode == DeskLaunchMode.AUTO_ON_CHARGING
                ) {
                    showDeskBoard = true
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            powerReceiver,
            IntentFilter(Intent.ACTION_POWER_CONNECTED),
            ContextCompat.RECEIVER_EXPORTED
        )
        onDispose {
            runCatching { context.unregisterReceiver(powerReceiver) }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showDeskBoard) {
        DeskStandbyScreen(
            preferences = preferences,
            reminders = reminders,
            tasks = tasks,
            stickyNotes = stickyNotes,
            onExitDeskMode = { showDeskBoard = false },
            onDeskLaunchModeChange = { viewModel.setDeskLaunchMode(it) },
            onKeepScreenOnChange = { viewModel.setDeskKeepScreenOn(it) },
            onDimModeChange = { viewModel.setDeskDimMode(it) },
            onTestAlarm = { viewModel.testAlarmNow(it) },
            onTestTaskAlarm = { viewModel.testTaskAlarmNow(it) },
            onPersistProofImage = { uri -> viewModel.persistPickedMedia(uri, "desk_proof") },
            onSubmitTaskProof = { taskId, status, uri, name, note ->
                viewModel.submitTaskProof(taskId, status, uri, name, note)
            },
            onDeferTaskLater = { taskId, mins -> viewModel.deferTaskLater(taskId, mins) },
            onTogglePinNote = { viewModel.toggleStickyPin(it) }
        )
        return
    }

    BackHandler(enabled = currentTab != DayPinTab.TODAY) {
        currentTab = DayPinTab.TODAY
    }

    fun handlePrimaryFabClick() {
        when (currentTab) {
            DayPinTab.NOTES -> {
                editingNote = null
                noteTemplateForEditor = null
                showNoteEditor = true
            }
            DayPinTab.TASKS -> {
                editingTask = null
                taskTemplateForEditor = null
                editorDefaultDay = selectedDayMillis
                showTaskEditor = true
            }
            else -> {
                editingReminder = null
                reminderTemplateForEditor = null
                editorDefaultDay = selectedDayMillis
                showReminderEditor = true
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp
        val isTwoPane = maxWidth >= 820.dp

        if (useNavigationRail) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxHeight(),
                    header = {
                        Spacer(Modifier.height(8.dp))
                        FloatingActionButton(
                            onClick = ::handlePrimaryFabClick,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("rail_primary_fab")
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = when (currentTab) {
                                    DayPinTab.NOTES -> "New sticky note"
                                    DayPinTab.TASKS -> "New task"
                                    else -> "New reminder"
                                }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        IconButton(
                            onClick = { showDeskBoard = true },
                            modifier = Modifier.testTag("rail_desk_board_btn")
                        ) {
                            Icon(
                                Icons.Filled.Bedtime,
                                contentDescription = "Open Desk Clock & Standby Board",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                ) {
                    DayPinTab.entries.forEach { tab ->
                        val selected = tab == currentTab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = stringResource(tab.labelRes)
                                )
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    ScreenDestinationContent(
                        currentTab = currentTab,
                        isTwoPane = isTwoPane,
                        viewModel = viewModel,
                        reminders = reminders,
                        stickyNotes = stickyNotes,
                        tasks = tasks,
                        taskProofs = taskProofs,
                        quickTemplates = quickTemplates,
                        preferences = preferences,
                        permissionSnapshot = permissionSnapshot,
                        selectedDayMillis = selectedDayMillis,
                        selectedReminderId = selectedReminderId,
                        selectedTaskId = selectedTaskId,
                        backupFeedback = backupFeedback,
                        onNavigateTab = { currentTab = it },
                        onOpenDeskBoard = { showDeskBoard = true },
                        onOpenReminderEditor = { rem, day, tpl ->
                            editingReminder = rem
                            reminderTemplateForEditor = tpl
                            editorDefaultDay = day
                            showReminderEditor = true
                        },
                        onOpenTaskEditor = { task, day, tpl ->
                            editingTask = task
                            taskTemplateForEditor = tpl
                            editorDefaultDay = day
                            showTaskEditor = true
                        },
                        onOpenNoteEditor = { note, tpl ->
                            editingNote = note
                            noteTemplateForEditor = tpl
                            showNoteEditor = true
                        },
                        onRequestOverlayPermission = {
                            context.startActivity(PermissionHelper.buildOverlaySettingsIntent(context))
                        }
                    )
                }
            }
        } else {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                floatingActionButton = {
                    if (currentTab != DayPinTab.SETTINGS) {
                        FloatingActionButton(
                            onClick = ::handlePrimaryFabClick,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("compact_primary_fab")
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = when (currentTab) {
                                    DayPinTab.NOTES -> "New sticky note"
                                    DayPinTab.TASKS -> "New task"
                                    else -> "New reminder"
                                }
                            )
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        DayPinTab.entries.forEach { tab ->
                            val selected = tab == currentTab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ScreenDestinationContent(
                        currentTab = currentTab,
                        isTwoPane = false,
                        viewModel = viewModel,
                        reminders = reminders,
                        stickyNotes = stickyNotes,
                        tasks = tasks,
                        taskProofs = taskProofs,
                        quickTemplates = quickTemplates,
                        preferences = preferences,
                        permissionSnapshot = permissionSnapshot,
                        selectedDayMillis = selectedDayMillis,
                        selectedReminderId = selectedReminderId,
                        selectedTaskId = selectedTaskId,
                        backupFeedback = backupFeedback,
                        onNavigateTab = { currentTab = it },
                        onOpenDeskBoard = { showDeskBoard = true },
                        onOpenReminderEditor = { rem, day, tpl ->
                            editingReminder = rem
                            reminderTemplateForEditor = tpl
                            editorDefaultDay = day
                            showReminderEditor = true
                        },
                        onOpenTaskEditor = { task, day, tpl ->
                            editingTask = task
                            taskTemplateForEditor = tpl
                            editorDefaultDay = day
                            showTaskEditor = true
                        },
                        onOpenNoteEditor = { note, tpl ->
                            editingNote = note
                            noteTemplateForEditor = tpl
                            showNoteEditor = true
                        },
                        onRequestOverlayPermission = {
                            context.startActivity(PermissionHelper.buildOverlaySettingsIntent(context))
                        }
                    )
                }
            }
        }
    }

    if (!preferences.hasCompletedPermissionOnboarding) {
        PermissionOnboardingDialog(
            snapshot = permissionSnapshot,
            onRefreshPermissions = { viewModel.refreshPermissions() },
            onFinish = { viewModel.completePermissionOnboarding() }
        )
    }

    if (showReminderEditor) {
        ReminderEditorDialog(
            initialReminder = editingReminder,
            defaultDayMillis = editorDefaultDay,
            initialTemplate = reminderTemplateForEditor,
            templates = quickTemplates,
            onSaveAsTemplate = { viewModel.saveQuickTemplate(it) },
            onDeleteTemplate = { viewModel.deleteQuickTemplate(it) },
            onPersistMedia = { uri, prefix -> viewModel.persistPickedMedia(uri, prefix) },
            onDismiss = {
                showReminderEditor = false
                reminderTemplateForEditor = null
            },
            onSave = { entity ->
                viewModel.saveReminder(entity)
                showReminderEditor = false
                reminderTemplateForEditor = null
            }
        )
    }

    if (showTaskEditor) {
        TaskEditorDialog(
            initialTask = editingTask,
            defaultDayMillis = editorDefaultDay,
            initialTemplate = taskTemplateForEditor,
            templates = quickTemplates,
            onSaveAsTemplate = { viewModel.saveQuickTemplate(it) },
            onDeleteTemplate = { viewModel.deleteQuickTemplate(it) },
            onPersistMedia = { uri, prefix -> viewModel.persistPickedMedia(uri, prefix) },
            onDismiss = {
                showTaskEditor = false
                taskTemplateForEditor = null
            },
            onSave = { task ->
                viewModel.saveTask(task)
                showTaskEditor = false
                taskTemplateForEditor = null
            }
        )
    }

    if (showNoteEditor) {
        StickyNoteEditorDialog(
            initialNote = editingNote,
            canOverlay = permissionSnapshot.overlayGranted,
            initialTemplate = noteTemplateForEditor,
            templates = quickTemplates,
            onSaveAsTemplate = { viewModel.saveQuickTemplate(it) },
            onDeleteTemplate = { viewModel.deleteQuickTemplate(it) },
            onPersistMedia = { uri, prefix -> viewModel.persistPickedMedia(uri, prefix) },
            onRequestOverlayPermission = {
                context.startActivity(PermissionHelper.buildOverlaySettingsIntent(context))
            },
            onDismiss = {
                showNoteEditor = false
                noteTemplateForEditor = null
            },
            onSave = { note, pin ->
                viewModel.saveStickyNote(note, pin)
                showNoteEditor = false
                noteTemplateForEditor = null
            }
        )
    }
}

@Composable
private fun ScreenDestinationContent(
    currentTab: DayPinTab,
    isTwoPane: Boolean,
    viewModel: DayPinViewModel,
    reminders: List<ReminderEntity>,
    stickyNotes: List<StickyNoteEntity>,
    tasks: List<TaskEntity>,
    taskProofs: List<TaskProofEntity>,
    quickTemplates: List<QuickTemplateEntity>,
    preferences: AppPreferences,
    permissionSnapshot: PermissionStatusSnapshot,
    selectedDayMillis: Long,
    selectedReminderId: Long?,
    selectedTaskId: Long?,
    backupFeedback: String?,
    onNavigateTab: (DayPinTab) -> Unit,
    onOpenDeskBoard: () -> Unit,
    onOpenReminderEditor: (ReminderEntity?, Long, QuickTemplateEntity?) -> Unit,
    onOpenTaskEditor: (TaskEntity?, Long, QuickTemplateEntity?) -> Unit,
    onOpenNoteEditor: (StickyNoteEntity?, QuickTemplateEntity?) -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    when (currentTab) {
        DayPinTab.TODAY -> TodayAlarmsScreen(
            reminders = reminders,
            templates = quickTemplates,
            quoteCategory = preferences.quoteCategory,
            selectedReminderId = selectedReminderId,
            isTwoPane = isTwoPane,
            onSelectReminder = { viewModel.selectReminder(it) },
            onAddReminder = { onOpenReminderEditor(null, selectedDayMillis, null) },
            onApplyTemplate = { tpl -> onOpenReminderEditor(null, selectedDayMillis, tpl) },
            onDeleteTemplate = { viewModel.deleteQuickTemplate(it) },
            onToggleChecklistItem = { remId, itemId ->
                viewModel.toggleReminderChecklistItem(remId, itemId)
            },
            onPinQuoteNote = { quote -> viewModel.pinQuoteAsStickyNote(quote) },
            onEditReminder = { onOpenReminderEditor(it, it.triggerAtMillis, null) },
            onToggleReminder = { rem, enabled -> viewModel.toggleReminder(rem, enabled) },
            onDeleteReminder = { viewModel.deleteReminder(it) },
            onTestAlarm = { viewModel.testAlarmNow(it) },
            onOpenStickyNotes = { onNavigateTab(DayPinTab.NOTES) },
            onOpenDeskBoard = onOpenDeskBoard
        )

        DayPinTab.CALENDAR -> YearCalendarScreen(
            reminders = reminders,
            selectedDayMillis = selectedDayMillis,
            isTwoPane = isTwoPane,
            onSelectDay = { viewModel.selectCalendarDay(it) },
            onAddReminderForDay = { dayMillis -> onOpenReminderEditor(null, dayMillis, null) },
            onEditReminder = { onOpenReminderEditor(it, it.triggerAtMillis, null) },
            onToggleReminder = { rem, enabled -> viewModel.toggleReminder(rem, enabled) },
            onDeleteReminder = { viewModel.deleteReminder(it) },
            onTestAlarm = { viewModel.testAlarmNow(it) }
        )

        DayPinTab.TASKS -> TasksScreen(
            tasks = tasks,
            proofs = taskProofs,
            templates = quickTemplates,
            selectedTaskId = selectedTaskId,
            defaultSnoozeMinutes = preferences.defaultSnoozeMinutes,
            isTwoPane = isTwoPane,
            onSelectTask = { viewModel.selectTask(it) },
            onAddTask = { onOpenTaskEditor(null, selectedDayMillis, null) },
            onApplyTemplate = { tpl -> onOpenTaskEditor(null, selectedDayMillis, tpl) },
            onDeleteTemplate = { viewModel.deleteQuickTemplate(it) },
            onToggleChecklistItem = { taskId, itemId ->
                viewModel.toggleTaskChecklistItem(taskId, itemId)
            },
            onEditTask = { onOpenTaskEditor(it, it.triggerAtMillis, null) },
            onToggleTaskEnabled = { task, enabled -> viewModel.toggleTaskEnabled(task, enabled) },
            onDeleteTask = { viewModel.deleteTask(it) },
            onTestTaskAlarm = { viewModel.testTaskAlarmNow(it) },
            onPersistProofImage = { uri -> viewModel.persistPickedMedia(uri, "task_proof") },
            onSubmitProof = { taskId, status, uri, name, note ->
                viewModel.submitTaskProof(taskId, status, uri, name, note)
            },
            onDeferLater = { taskId, mins -> viewModel.deferTaskLater(taskId, mins) }
        )

        DayPinTab.NOTES -> StickyNotesScreen(
            notes = stickyNotes,
            templates = quickTemplates,
            canOverlay = permissionSnapshot.overlayGranted,
            overlayMode = preferences.overlayDisplayMode,
            isTwoPane = isTwoPane,
            onOverlayModeChange = { viewModel.setOverlayDisplayMode(it) },
            onAddNote = { onOpenNoteEditor(null, null) },
            onApplyTemplate = { tpl -> onOpenNoteEditor(null, tpl) },
            onDeleteTemplate = { viewModel.deleteQuickTemplate(it) },
            onToggleChecklistItem = { noteId, itemId ->
                viewModel.toggleStickyNoteChecklistItem(noteId, itemId)
            },
            onEditNote = { onOpenNoteEditor(it, null) },
            onTogglePinOverlay = { viewModel.toggleStickyPin(it) },
            onUnpinAll = { viewModel.unpinAllStickyNotes() },
            onDeleteNote = { viewModel.deleteStickyNote(it) },
            onRequestOverlayPermission = onRequestOverlayPermission
        )

        DayPinTab.SETTINGS -> SettingsScreen(
            preferences = preferences,
            permissionSnapshot = permissionSnapshot,
            reminderCount = reminders.size,
            taskCount = tasks.size,
            proofCount = taskProofs.size,
            stickyNoteCount = stickyNotes.size,
            backupFeedback = backupFeedback,
            onClearFeedback = { viewModel.clearBackupFeedback() },
            onSaveExportJsonUri = { viewModel.saveBackupJsonToUri(it) },
            onShareJsonRequest = { onReady -> viewModel.createShareBackupUri(onReady) },
            onImportJsonUri = { viewModel.importBackupJsonFromUri(it) },
            onRefreshPermissions = { viewModel.refreshPermissions() },
            onSnoozeChange = { viewModel.setDefaultSnoozeMinutes(it) },
            onVibrationChange = { viewModel.setVibrationEnabled(it) },
            onFadeInChange = { viewModel.setFadeInAudio(it) },
            onThemeModeChange = { viewModel.setThemeMode(it) },
            onOverlayModeChange = { viewModel.setOverlayDisplayMode(it) },
            onOverlayPanelStyleChange = { viewModel.setOverlayPanelStyle(it) },
            onOverlayAnimationStyleChange = { viewModel.setOverlayAnimationStyle(it) },
            onOverlayPanelScaleChange = { viewModel.setOverlayPanelScale(it) },
            onOverlayMediaHeightScaleChange = { viewModel.setOverlayMediaHeightScale(it) },
            onOverlayTextScaleChange = { viewModel.setOverlayTextScale(it) },
            onAlarmActivePanelStyleChange = { viewModel.setAlarmActivePanelStyle(it) },
            onAlarmActivePanelScaleChange = { viewModel.setAlarmActivePanelScale(it) },
            onAlarmActiveMediaHeightScaleChange = { viewModel.setAlarmActiveMediaHeightScale(it) },
            onAlarmActiveTextScaleChange = { viewModel.setAlarmActiveTextScale(it) },
            onAlarmActiveCornerRadiusChange = { viewModel.setAlarmActiveCornerRadius(it) },
            onAlarmActiveOpacityChange = { viewModel.setAlarmActiveOpacity(it) },
            onAlarmActiveShowQuoteChange = { viewModel.setAlarmActiveShowQuote(it) },
            onOverlayNotificationEnabledChange = { viewModel.setOverlayNotificationEnabled(it) },
            onDeskLaunchModeChange = { viewModel.setDeskLaunchMode(it) },
            onDeskKeepScreenOnChange = { viewModel.setDeskKeepScreenOn(it) },
            onDeskDimModeChange = { viewModel.setDeskDimMode(it) },
            onQuoteNotificationsEnabledChange = { viewModel.setQuoteNotificationsEnabled(it) },
            onQuoteScheduleModeChange = { viewModel.setQuoteScheduleMode(it) },
            onQuoteRandomPerDayChange = { viewModel.setQuoteRandomPerDay(it) },
            onQuoteIntervalMinutesChange = { viewModel.setQuoteIntervalMinutes(it) },
            onQuoteCategoryChange = { viewModel.setQuoteCategory(it) },
            onSendQuoteNow = { viewModel.sendQuoteNotificationNow() },
            onOpenDeskBoard = onOpenDeskBoard
        )
    }
}
