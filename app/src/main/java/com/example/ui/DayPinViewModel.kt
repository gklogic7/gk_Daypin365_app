package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmScheduler
import com.example.data.local.AppPreferences
import com.example.data.local.DeskLaunchMode
import com.example.data.local.OverlayAnimationStyle
import com.example.data.local.OverlayDisplayMode
import com.example.data.local.OverlayPanelStyle
import com.example.data.local.PreferencesManager
import com.example.data.local.QuoteScheduleMode
import com.example.data.model.ChecklistItem
import com.example.data.model.QuickTemplateEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.StickyColor
import com.example.data.model.StickyNoteEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskProofEntity
import com.example.data.model.TaskStatus
import com.example.data.repository.BackupJsonManager
import com.example.data.repository.CachedMediaResult
import com.example.data.repository.DailyQuote
import com.example.data.repository.MediaStorageHelper
import com.example.data.repository.QuoteCategory
import com.example.data.repository.ReminderRepository
import com.example.overlay.StickyNoteOverlayService
import com.example.permissions.PermissionHelper
import com.example.permissions.PermissionStatusSnapshot
import com.example.quotes.QuoteNotificationScheduler
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DayPinViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val repository = ReminderRepository(appContext)
    private val preferencesManager = PreferencesManager(appContext)

    val reminders: StateFlow<List<ReminderEntity>> = repository.remindersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val stickyNotes: StateFlow<List<StickyNoteEntity>> = repository.stickyNotesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = repository.tasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val taskProofs: StateFlow<List<TaskProofEntity>> = repository.taskProofsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val quickTemplates: StateFlow<List<QuickTemplateEntity>> = repository.quickTemplatesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val preferences: StateFlow<AppPreferences> = preferencesManager.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppPreferences())

    init {
        viewModelScope.launch {
            repository.ensureStarterTemplatesSeeded()
        }
    }

    private val _permissionSnapshot = MutableStateFlow(PermissionHelper.snapshot(appContext))
    val permissionSnapshot: StateFlow<PermissionStatusSnapshot> = _permissionSnapshot.asStateFlow()

    private val _selectedDayMillis = MutableStateFlow(DateCalendarUtils.todayStartMillis())
    val selectedDayMillis: StateFlow<Long> = _selectedDayMillis.asStateFlow()

    private val _selectedReminderId = MutableStateFlow<Long?>(null)
    val selectedReminderId: StateFlow<Long?> = _selectedReminderId.asStateFlow()

    private val _selectedTaskId = MutableStateFlow<Long?>(null)
    val selectedTaskId: StateFlow<Long?> = _selectedTaskId.asStateFlow()

    private val _selectedNoteId = MutableStateFlow<Long?>(null)
    val selectedNoteId: StateFlow<Long?> = _selectedNoteId.asStateFlow()

    private val _backupFeedback = MutableStateFlow<String?>(null)
    val backupFeedback: StateFlow<String?> = _backupFeedback.asStateFlow()

    fun clearBackupFeedback() {
        _backupFeedback.value = null
    }

    fun refreshPermissions() {
        _permissionSnapshot.value = PermissionHelper.snapshot(appContext)
    }

    fun selectCalendarDay(dayStartMillis: Long) {
        _selectedDayMillis.value = DateCalendarUtils.dayStartMillis(dayStartMillis)
    }

    fun selectReminder(reminderId: Long?) {
        _selectedReminderId.value = reminderId
    }

    fun selectTask(taskId: Long?) {
        _selectedTaskId.value = taskId
    }

    fun selectNote(noteId: Long?) {
        _selectedNoteId.value = noteId
    }

    fun saveReminder(reminder: ReminderEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveReminder(reminder)
            _selectedReminderId.value = id
            onSaved(id)
        }
    }

    fun toggleReminder(reminder: ReminderEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleReminderEnabled(reminder, enabled)
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            if (_selectedReminderId.value == reminder.id) {
                _selectedReminderId.value = null
            }
            repository.deleteReminder(reminder)
        }
    }

    fun testAlarmNow(reminder: ReminderEntity) {
        AlarmScheduler.triggerImmediatePreview(appContext, reminder)
    }

    // --- Tasks & Proofs ---

    fun saveTask(task: TaskEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveTask(task)
            _selectedTaskId.value = id
            onSaved(id)
        }
    }

    fun toggleTaskEnabled(task: TaskEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleTaskEnabled(task, enabled)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            if (_selectedTaskId.value == task.id) {
                _selectedTaskId.value = null
            }
            repository.deleteTask(task)
        }
    }

    fun testTaskAlarmNow(task: TaskEntity) {
        AlarmScheduler.triggerImmediateTaskPreview(appContext, task)
    }

    fun submitTaskProof(
        taskId: Long,
        targetStatus: TaskStatus,
        imageUri: String,
        imageDisplayName: String,
        note: String
    ) {
        viewModelScope.launch {
            repository.submitTaskProof(
                taskId = taskId,
                targetStatus = targetStatus,
                imageUri = imageUri,
                imageDisplayName = imageDisplayName,
                note = note
            )
            _selectedTaskId.value = taskId
        }
    }

    fun deferTaskLater(taskId: Long, deferMinutes: Int) {
        viewModelScope.launch {
            repository.deferTaskLater(taskId, deferMinutes)
            _selectedTaskId.value = taskId
        }
    }

    fun toggleReminderChecklistItem(reminderId: Long, itemId: String) {
        viewModelScope.launch {
            repository.toggleReminderChecklistItem(reminderId, itemId)
        }
    }

    fun toggleTaskChecklistItem(taskId: Long, itemId: String) {
        viewModelScope.launch {
            repository.toggleTaskChecklistItem(taskId, itemId)
        }
    }

    fun toggleStickyNoteChecklistItem(noteId: Long, itemId: String) {
        viewModelScope.launch {
            repository.toggleStickyNoteChecklistItem(noteId, itemId)
        }
    }

    fun saveQuickTemplate(template: QuickTemplateEntity) {
        viewModelScope.launch {
            repository.saveQuickTemplate(template)
        }
    }

    fun deleteQuickTemplate(templateId: Long) {
        viewModelScope.launch {
            repository.deleteQuickTemplate(templateId)
        }
    }

    suspend fun persistPickedMedia(uri: Uri, prefix: String): CachedMediaResult {
        return MediaStorageHelper.persistPickedMedia(appContext, uri, prefix)
    }

    fun saveBackupJsonToUri(uri: Uri) {
        viewModelScope.launch {
            val prefs = preferencesManager.preferencesFlow.first()
            val json = repository.exportBackupJson(prefs)
            val result = BackupJsonManager.writeJsonToUri(appContext, uri, json)
            _backupFeedback.value = result.fold(
                onSuccess = {
                    "Saved JSON backup (${reminders.value.size} alarms, ${tasks.value.size} tasks, ${stickyNotes.value.size} notes)."
                },
                onFailure = { "Export failed: ${it.localizedMessage ?: "Unknown error"}" }
            )
        }
    }

    fun createShareBackupUri(onReady: (Uri?) -> Unit) {
        viewModelScope.launch {
            val prefs = preferencesManager.preferencesFlow.first()
            val json = repository.exportBackupJson(prefs)
            val result = BackupJsonManager.createTempShareUri(appContext, json)
            result.onFailure {
                _backupFeedback.value = "Share export failed: ${it.localizedMessage ?: "Unknown error"}"
            }
            onReady(result.getOrNull())
        }
    }

    fun importBackupJsonFromUri(uri: Uri) {
        viewModelScope.launch {
            val rawResult = BackupJsonManager.readJsonFromUri(appContext, uri)
            rawResult.fold(
                onSuccess = { jsonText ->
                    val parsed = BackupJsonManager.parseBackupJson(jsonText)
                    parsed.fold(
                        onSuccess = { payload ->
                            val summary = repository.importBackup(payload)
                            payload.snoozeMinutes?.let { preferencesManager.setDefaultSnoozeMinutes(it) }
                            payload.vibrationEnabled?.let { preferencesManager.setVibrationEnabled(it) }
                            payload.fadeInAudio?.let { preferencesManager.setFadeInAudio(it) }
                            payload.themeMode?.let { preferencesManager.setThemeMode(it) }
                            payload.overlayDisplayMode?.let { preferencesManager.setOverlayDisplayMode(it) }
                            payload.overlayPanelStyle?.let { preferencesManager.setOverlayPanelStyle(it) }
                            payload.overlayAnimationStyle?.let { preferencesManager.setOverlayAnimationStyle(it) }
                            payload.overlayNotificationEnabled?.let { preferencesManager.setOverlayNotificationEnabled(it) }
                            payload.overlayPanelScale?.let { preferencesManager.setOverlayPanelScale(it) }
                            payload.overlayMediaHeightScale?.let { preferencesManager.setOverlayMediaHeightScale(it) }
                            payload.overlayTextScale?.let { preferencesManager.setOverlayTextScale(it) }
                            payload.alarmActivePanelStyle?.let { preferencesManager.setAlarmActivePanelStyle(it) }
                            payload.alarmActivePanelScale?.let { preferencesManager.setAlarmActivePanelScale(it) }
                            payload.alarmActiveMediaHeightScale?.let { preferencesManager.setAlarmActiveMediaHeightScale(it) }
                            payload.alarmActiveTextScale?.let { preferencesManager.setAlarmActiveTextScale(it) }
                            payload.alarmActiveCornerRadius?.let { preferencesManager.setAlarmActiveCornerRadius(it) }
                            payload.alarmActiveOpacity?.let { preferencesManager.setAlarmActiveOpacity(it) }
                            payload.alarmActiveShowQuote?.let { preferencesManager.setAlarmActiveShowQuote(it) }
                            payload.deskLaunchMode?.let { preferencesManager.setDeskLaunchMode(it) }
                            payload.deskKeepScreenOn?.let { preferencesManager.setDeskKeepScreenOn(it) }
                            payload.deskDimMode?.let { preferencesManager.setDeskDimMode(it) }
                            payload.quoteNotificationsEnabled?.let { preferencesManager.setQuoteNotificationsEnabled(it) }
                            payload.quoteScheduleMode?.let { preferencesManager.setQuoteScheduleMode(it) }
                            payload.quoteRandomPerDay?.let { preferencesManager.setQuoteRandomPerDay(it) }
                            payload.quoteIntervalMinutes?.let { preferencesManager.setQuoteIntervalMinutes(it) }
                            payload.quoteCategory?.let { preferencesManager.setQuoteCategory(it) }
                            val updatedPrefs = preferencesManager.preferencesFlow.first()
                            QuoteNotificationScheduler.reschedule(appContext, updatedPrefs)
                            _backupFeedback.value =
                                "Imported ${summary.remindersCount} alarm(s), ${summary.tasksCount} task(s), ${summary.notesCount} note(s), and ${summary.templatesCount} template(s)."
                        },
                        onFailure = {
                            _backupFeedback.value = "Invalid JSON backup format."
                        }
                    )
                },
                onFailure = {
                    _backupFeedback.value = "Unable to read selected JSON file."
                }
            )
        }
    }

    fun saveStickyNote(note: StickyNoteEntity, pinImmediately: Boolean = note.isPinnedOverlay) {
        viewModelScope.launch {
            val canOverlay = PermissionHelper.hasOverlayPermission(appContext)
            val effectivePin = pinImmediately && canOverlay
            val id = repository.saveStickyNote(note.copy(isPinnedOverlay = effectivePin))
            _selectedNoteId.value = id
            if (effectivePin) {
                StickyNoteOverlayService.startOrRefresh(appContext, id)
            }
        }
    }

    fun toggleStickyPin(note: StickyNoteEntity) {
        viewModelScope.launch {
            val nextPin = !note.isPinnedOverlay
            if (nextPin && !PermissionHelper.hasOverlayPermission(appContext)) {
                return@launch
            }
            repository.setStickyNotePinned(note.id, nextPin)
            if (nextPin) {
                StickyNoteOverlayService.startOrRefresh(appContext, note.id)
            } else {
                val remaining = repository.getPinnedStickyNotesOnce()
                if (remaining.isEmpty()) {
                    StickyNoteOverlayService.stop(appContext)
                }
            }
        }
    }

    fun unpinAllStickyNotes() {
        viewModelScope.launch {
            repository.unpinAllStickyNotes()
            StickyNoteOverlayService.stop(appContext)
        }
    }

    fun deleteStickyNote(note: StickyNoteEntity) {
        viewModelScope.launch {
            if (_selectedNoteId.value == note.id) {
                _selectedNoteId.value = null
            }
            repository.deleteStickyNote(note.id)
            val remaining = repository.getPinnedStickyNotesOnce()
            if (remaining.isEmpty()) {
                StickyNoteOverlayService.stop(appContext)
            }
        }
    }

    fun completePermissionOnboarding() {
        viewModelScope.launch {
            preferencesManager.markPermissionOnboardingCompleted()
        }
    }

    fun setDefaultSnoozeMinutes(minutes: Int) {
        viewModelScope.launch {
            preferencesManager.setDefaultSnoozeMinutes(minutes)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setVibrationEnabled(enabled)
        }
    }

    fun setFadeInAudio(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setFadeInAudio(enabled)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }

    fun setOverlayDisplayMode(mode: OverlayDisplayMode) {
        viewModelScope.launch {
            preferencesManager.setOverlayDisplayMode(mode)
        }
    }

    fun setOverlayPanelStyle(style: OverlayPanelStyle) {
        viewModelScope.launch {
            preferencesManager.setOverlayPanelStyle(style)
        }
    }

    fun setOverlayAnimationStyle(style: OverlayAnimationStyle) {
        viewModelScope.launch {
            preferencesManager.setOverlayAnimationStyle(style)
        }
    }

    fun setOverlayNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setOverlayNotificationEnabled(enabled)
        }
    }

    fun setOverlayPanelScale(scale: Float) {
        viewModelScope.launch {
            preferencesManager.setOverlayPanelScale(scale)
        }
    }

    fun setOverlayMediaHeightScale(scale: Float) {
        viewModelScope.launch {
            preferencesManager.setOverlayMediaHeightScale(scale)
        }
    }

    fun setOverlayTextScale(scale: Float) {
        viewModelScope.launch {
            preferencesManager.setOverlayTextScale(scale)
        }
    }

    fun setAlarmActivePanelStyle(style: OverlayPanelStyle) {
        viewModelScope.launch {
            preferencesManager.setAlarmActivePanelStyle(style)
        }
    }

    fun setAlarmActivePanelScale(scale: Float) {
        viewModelScope.launch {
            preferencesManager.setAlarmActivePanelScale(scale)
        }
    }

    fun setAlarmActiveMediaHeightScale(scale: Float) {
        viewModelScope.launch {
            preferencesManager.setAlarmActiveMediaHeightScale(scale)
        }
    }

    fun setAlarmActiveTextScale(scale: Float) {
        viewModelScope.launch {
            preferencesManager.setAlarmActiveTextScale(scale)
        }
    }

    fun setAlarmActiveCornerRadius(radiusDp: Int) {
        viewModelScope.launch {
            preferencesManager.setAlarmActiveCornerRadius(radiusDp)
        }
    }

    fun setAlarmActiveOpacity(opacity: Float) {
        viewModelScope.launch {
            preferencesManager.setAlarmActiveOpacity(opacity)
        }
    }

    fun setAlarmActiveShowQuote(show: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAlarmActiveShowQuote(show)
        }
    }

    fun setDeskLaunchMode(mode: DeskLaunchMode) {
        viewModelScope.launch {
            preferencesManager.setDeskLaunchMode(mode)
        }
    }

    fun setDeskKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDeskKeepScreenOn(enabled)
        }
    }

    fun setDeskDimMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDeskDimMode(enabled)
        }
    }

    fun setQuoteNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setQuoteNotificationsEnabled(enabled)
            val updated = preferencesManager.preferencesFlow.first()
            QuoteNotificationScheduler.reschedule(appContext, updated)
        }
    }

    fun setQuoteScheduleMode(mode: QuoteScheduleMode) {
        viewModelScope.launch {
            preferencesManager.setQuoteScheduleMode(mode)
            val updated = preferencesManager.preferencesFlow.first()
            QuoteNotificationScheduler.reschedule(appContext, updated)
        }
    }

    fun setQuoteRandomPerDay(count: Int) {
        viewModelScope.launch {
            preferencesManager.setQuoteRandomPerDay(count)
            val updated = preferencesManager.preferencesFlow.first()
            QuoteNotificationScheduler.reschedule(appContext, updated)
        }
    }

    fun setQuoteIntervalMinutes(minutes: Int) {
        viewModelScope.launch {
            preferencesManager.setQuoteIntervalMinutes(minutes)
            val updated = preferencesManager.preferencesFlow.first()
            QuoteNotificationScheduler.reschedule(appContext, updated)
        }
    }

    fun setQuoteCategory(category: QuoteCategory) {
        viewModelScope.launch {
            preferencesManager.setQuoteCategory(category)
            val updated = preferencesManager.preferencesFlow.first()
            QuoteNotificationScheduler.reschedule(appContext, updated)
        }
    }

    fun sendQuoteNotificationNow() {
        QuoteNotificationScheduler.sendQuoteNow(appContext, preferences.value.quoteCategory)
    }

    fun pinQuoteAsStickyNote(quote: DailyQuote) {
        saveStickyNote(
            note = StickyNoteEntity(
                title = "${quote.category.label} Quote",
                content = quote.text,
                color = StickyColor.BUTTERCUP,
                isPinnedOverlay = PermissionHelper.hasOverlayPermission(appContext)
            ),
            pinImmediately = PermissionHelper.hasOverlayPermission(appContext)
        )
    }
}
