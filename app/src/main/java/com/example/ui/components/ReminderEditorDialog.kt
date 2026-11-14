package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChecklistCodec
import com.example.data.model.IntervalUnit
import com.example.data.model.MediaType
import com.example.data.model.QuickTemplateEntity
import com.example.data.model.RecurrenceType
import com.example.data.model.ReminderEntity
import com.example.data.model.StickyColor
import com.example.data.model.TemplateTargetKind
import com.example.data.repository.CachedMediaResult
import com.example.ui.DateCalendarUtils
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderEditorDialog(
    initialReminder: ReminderEntity?,
    defaultDayMillis: Long,
    initialTemplate: QuickTemplateEntity? = null,
    templates: List<QuickTemplateEntity> = emptyList(),
    onSaveAsTemplate: (QuickTemplateEntity) -> Unit = {},
    onDeleteTemplate: (Long) -> Unit = {},
    onPersistMedia: suspend (Uri, String) -> CachedMediaResult,
    onDismiss: () -> Unit,
    onSave: (ReminderEntity) -> Unit
) {
    val scope = rememberCoroutineScope()
    val initCal = remember(initialReminder, defaultDayMillis) {
        Calendar.getInstance().apply {
            if (initialReminder != null) {
                timeInMillis = initialReminder.triggerAtMillis
            } else {
                timeInMillis = defaultDayMillis
                val now = Calendar.getInstance()
                val nextHour = (now.get(Calendar.HOUR_OF_DAY) + 1) % 24
                set(Calendar.HOUR_OF_DAY, nextHour)
                set(Calendar.MINUTE, 0)
            }
        }
    }

    var title by remember(initialReminder, initialTemplate) {
        mutableStateOf(initialReminder?.title ?: initialTemplate?.defaultTitle ?: "")
    }
    var noteText by remember(initialReminder, initialTemplate) {
        mutableStateOf(initialReminder?.noteText ?: initialTemplate?.defaultBody ?: "")
    }
    var checklistItems by remember(initialReminder, initialTemplate) {
        mutableStateOf(initialReminder?.checklistItems() ?: initialTemplate?.checklistItems() ?: emptyList())
    }
    var templateSavedNotice by remember { mutableStateOf(false) }

    var selectedDayMillis by remember(initialReminder, defaultDayMillis) {
        mutableLongStateOf(DateCalendarUtils.dayStartMillis(initCal.timeInMillis))
    }
    var hourOfDay by remember { mutableIntStateOf(initCal.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(initCal.get(Calendar.MINUTE)) }

    var recurrence by remember(initialReminder, initialTemplate) {
        mutableStateOf(initialReminder?.recurrenceType ?: initialTemplate?.recurrenceType ?: RecurrenceType.ONCE)
    }
    var useCustomInterval by remember(initialReminder, initialTemplate) {
        mutableStateOf(
            initialReminder?.useCustomInterval
                ?: initialTemplate?.useCustomInterval
                ?: (initialReminder?.recurrenceType == RecurrenceType.CUSTOM_DAYS)
        )
    }
    var customIntervalUnit by remember(initialReminder, initialTemplate) {
        mutableStateOf(initialReminder?.customIntervalUnit ?: initialTemplate?.customIntervalUnit ?: IntervalUnit.DAYS)
    }
    var customIntervalValue by remember(initialReminder, initialTemplate) {
        mutableIntStateOf(
            (initialReminder?.customIntervalValue
                ?: initialReminder?.customIntervalDays
                ?: initialTemplate?.customIntervalValue
                ?: 15).coerceIn(1, 999)
        )
    }

    // Simultaneous multi-media slots
    var musicUri by remember(initialReminder) { mutableStateOf(initialReminder?.effectiveMusicUri) }
    var musicName by remember(initialReminder) { mutableStateOf(initialReminder?.effectiveMusicName) }
    var imageUri by remember(initialReminder) { mutableStateOf(initialReminder?.effectiveImageUri) }
    var imageName by remember(initialReminder) { mutableStateOf(initialReminder?.effectiveImageName) }
    var videoUri by remember(initialReminder) { mutableStateOf(initialReminder?.effectiveVideoUri) }
    var videoName by remember(initialReminder) { mutableStateOf(initialReminder?.effectiveVideoName) }

    var stickyColor by remember(initialReminder, initialTemplate) {
        mutableStateOf(initialReminder?.stickyColor ?: initialTemplate?.stickyColor ?: StickyColor.BUTTERCUP)
    }
    var showStickyOnAlarm by remember(initialReminder) {
        mutableStateOf(initialReminder?.showStickyOnAlarm ?: true)
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    fun applyTemplate(tpl: QuickTemplateEntity) {
        title = tpl.defaultTitle
        noteText = tpl.defaultBody
        recurrence = tpl.recurrenceType
        useCustomInterval = tpl.useCustomInterval
        customIntervalUnit = tpl.customIntervalUnit
        customIntervalValue = tpl.customIntervalValue.coerceIn(1, 999)
        stickyColor = tpl.stickyColor
        checklistItems = tpl.checklistItems()
        templateSavedNotice = false
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val cached = onPersistMedia(uri, "img")
                imageUri = cached.uriString
                imageName = cached.displayName
            }
        }
    }

    val gifWebpPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val cached = onPersistMedia(uri, "anim")
                imageUri = cached.uriString
                imageName = cached.displayName
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val cached = onPersistMedia(uri, "vid")
                videoUri = cached.uriString
                videoName = cached.displayName
            }
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val cached = onPersistMedia(uri, "aud")
                musicUri = cached.uriString
                musicName = cached.displayName
            }
        }
    }

    fun buildSavedEntity(): ReminderEntity {
        val combined = DateCalendarUtils.combineDateAndTime(selectedDayMillis, hourOfDay, minute)
        val primaryMediaType = when {
            !videoUri.isNullOrBlank() -> MediaType.VIDEO
            !imageUri.isNullOrBlank() -> MediaType.IMAGE
            !musicUri.isNullOrBlank() -> MediaType.MUSIC
            else -> MediaType.NONE
        }
        val primaryUri = videoUri ?: imageUri ?: musicUri
        val primaryName = videoName ?: imageName ?: musicName
        val effectiveRecurrence = if (useCustomInterval) RecurrenceType.CUSTOM_DAYS else recurrence

        return (initialReminder ?: ReminderEntity(
            title = "",
            triggerAtMillis = combined
        )).copy(
            title = title.trim(),
            noteText = noteText.trim(),
            triggerAtMillis = combined,
            recurrenceType = effectiveRecurrence,
            customIntervalDays = if (customIntervalUnit == IntervalUnit.DAYS) customIntervalValue else 1,
            useCustomInterval = useCustomInterval,
            customIntervalUnit = customIntervalUnit,
            customIntervalValue = customIntervalValue.coerceIn(1, 999),
            isEnabled = true,
            mediaType = primaryMediaType,
            mediaUri = primaryUri,
            mediaDisplayName = primaryName,
            musicUri = musicUri,
            musicDisplayName = musicName,
            imageUri = imageUri,
            imageDisplayName = imageName,
            videoUri = videoUri,
            videoDisplayName = videoName,
            stickyColor = stickyColor,
            showStickyOnAlarm = showStickyOnAlarm,
            checklistJson = ChecklistCodec.encode(checklistItems)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 680.dp)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Header with Cancel & Save Alarm buttons pinned in the top-right corner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (initialReminder == null) "New Reminder" else "Edit Reminder",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("reminder_cancel_top_button")
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            enabled = title.isNotBlank(),
                            onClick = { onSave(buildSavedEntity()) },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("reminder_save_top_button")
                        ) {
                            Text("Save Alarm")
                        }
                    }
                }
                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val alarmTemplates = remember(templates) {
                        templates.filter { it.targetKind == TemplateTargetKind.REMINDER }
                    }
                    QuickTemplateStrip(
                        title = "Quick Alarm Templates",
                        templates = alarmTemplates,
                        onApplyTemplate = ::applyTemplate,
                        onDeleteCustomTemplate = { tpl -> onDeleteTemplate(tpl.id) }
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Reminder title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reminder_title_input")
                    )

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Sticky note message (shown when alarm fires)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reminder_note_input")
                    )

                    ChecklistEditorSection(
                        items = checklistItems,
                        onItemsChange = { checklistItems = it }
                    )

                    // Date & Time Row (Unrestricted World Calendar Date)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("reminder_date_button")
                        ) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(DateCalendarUtils.formatDateShort(selectedDayMillis))
                        }
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("reminder_time_button")
                        ) {
                            Icon(Icons.Filled.AccessTime, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            val combined = DateCalendarUtils.combineDateAndTime(selectedDayMillis, hourOfDay, minute)
                            Text(DateCalendarUtils.formatTime(combined))
                        }
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val today = DateCalendarUtils.todayStartMillis()
                        val presets = listOf("Today" to 0, "Tomorrow" to 1, "+7d" to 7, "+30d" to 30, "+365d" to 365)
                        presets.forEach { (label, offsetDays) ->
                            val target = Calendar.getInstance().apply {
                                timeInMillis = today
                                add(Calendar.DAY_OF_YEAR, offsetDays)
                            }.timeInMillis
                            FilterChip(
                                selected = selectedDayMillis == DateCalendarUtils.dayStartMillis(target),
                                onClick = { selectedDayMillis = DateCalendarUtils.dayStartMillis(target) },
                                label = { Text(label) }
                            )
                        }
                    }

                    // Repeat & Toggle Interval Option (Minutes / Hours / Days)
                    Text("Repeat & Interval", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            RecurrenceType.ONCE,
                            RecurrenceType.DAILY,
                            RecurrenceType.WEEKLY,
                            RecurrenceType.YEARLY
                        ).forEach { type ->
                            FilterChip(
                                selected = !useCustomInterval && recurrence == type,
                                onClick = {
                                    useCustomInterval = false
                                    recurrence = type
                                },
                                label = { Text(type.label) }
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Toggle Custom Interval", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = if (useCustomInterval) {
                                            "Repeats every $customIntervalValue ${customIntervalUnit.label.lowercase()}"
                                        } else {
                                            "Enable to repeat every N minutes, hours, or days"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = useCustomInterval,
                                    onCheckedChange = { useCustomInterval = it },
                                    modifier = Modifier.testTag("toggle_custom_interval_switch")
                                )
                            }

                            if (useCustomInterval) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IntervalUnit.entries.forEach { unit ->
                                        FilterChip(
                                            selected = customIntervalUnit == unit,
                                            onClick = { customIntervalUnit = unit },
                                            label = { Text(unit.label) },
                                            modifier = Modifier.testTag("interval_unit_${unit.name.lowercase()}")
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    FilledTonalIconButton(
                                        onClick = { customIntervalValue = (customIntervalValue - 1).coerceAtLeast(1) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Filled.Remove, contentDescription = "Decrease interval")
                                    }
                                    Text(
                                        text = "Every $customIntervalValue ${customIntervalUnit.label}",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilledTonalIconButton(
                                        onClick = { customIntervalValue = (customIntervalValue + 1).coerceAtMost(999) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Increase interval")
                                    }
                                }

                                val quickValues = when (customIntervalUnit) {
                                    IntervalUnit.MINUTES -> listOf(5, 10, 15, 30, 45)
                                    IntervalUnit.HOURS -> listOf(1, 2, 4, 6, 12)
                                    IntervalUnit.DAYS -> listOf(2, 3, 5, 14, 30)
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    quickValues.forEach { v ->
                                        FilterChip(
                                            selected = customIntervalValue == v,
                                            onClick = { customIntervalValue = v },
                                            label = { Text("$v${customIntervalUnit.shortLabel}") }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Multi-Select Alarm Media Actions (Simultaneous Music + Image + Video)
                    Text(
                        text = "Alarm Media Action (Select one or multiple)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val hasMusic = !musicUri.isNullOrBlank()
                        val hasImage = !imageUri.isNullOrBlank()
                        val hasVideo = !videoUri.isNullOrBlank()

                        FilterChip(
                            selected = hasMusic,
                            onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (hasMusic) Icons.Filled.Check else Icons.Filled.MusicNote,
                                    contentDescription = null
                                )
                            },
                            label = { Text(if (hasMusic) "Music Added" else "Play Music") },
                            trailingIcon = if (hasMusic) {
                                {
                                    IconButton(
                                        onClick = {
                                            musicUri = null
                                            musicName = null
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear music")
                                    }
                                }
                            } else null,
                            modifier = Modifier.testTag("pick_music_chip")
                        )

                        val isAnimatedImage = imageUri?.lowercase()?.let {
                            it.endsWith(".gif") || it.endsWith(".webp")
                        } == true

                        FilterChip(
                            selected = hasImage && !isAnimatedImage,
                            onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (hasImage && !isAnimatedImage) Icons.Filled.Check else Icons.Filled.Image,
                                    contentDescription = null
                                )
                            },
                            label = { Text(if (hasImage && !isAnimatedImage) "Image Added" else "Show Image") },
                            trailingIcon = if (hasImage && !isAnimatedImage) {
                                {
                                    IconButton(
                                        onClick = {
                                            imageUri = null
                                            imageName = null
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear image")
                                    }
                                }
                            } else null,
                            modifier = Modifier.testTag("pick_image_chip")
                        )

                        FilterChip(
                            selected = hasImage && isAnimatedImage,
                            onClick = {
                                gifWebpPickerLauncher.launch(arrayOf("image/gif", "image/webp"))
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (hasImage && isAnimatedImage) Icons.Filled.Check else Icons.Filled.Image,
                                    contentDescription = null
                                )
                            },
                            label = {
                                Text(
                                    if (hasImage && isAnimatedImage) {
                                        if (imageUri?.lowercase()?.endsWith(".webp") == true) "WebP Added" else "GIF Added"
                                    } else {
                                        "GIF / WebP"
                                    }
                                )
                            },
                            trailingIcon = if (hasImage && isAnimatedImage) {
                                {
                                    IconButton(
                                        onClick = {
                                            imageUri = null
                                            imageName = null
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear GIF or WebP")
                                    }
                                }
                            } else null,
                            modifier = Modifier.testTag("pick_gif_webp_chip")
                        )

                        FilterChip(
                            selected = hasVideo,
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (hasVideo) Icons.Filled.Check else Icons.Filled.Videocam,
                                    contentDescription = null
                                )
                            },
                            label = { Text(if (hasVideo) "Video Added" else "Play Video") },
                            trailingIcon = if (hasVideo) {
                                {
                                    IconButton(
                                        onClick = {
                                            videoUri = null
                                            videoName = null
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear video")
                                    }
                                }
                            } else null,
                            modifier = Modifier.testTag("pick_video_chip")
                        )
                    }

                    if (!musicUri.isNullOrBlank()) {
                        MediaPreviewCard(
                            mediaType = MediaType.MUSIC,
                            mediaUri = musicUri,
                            mediaDisplayName = musicName
                        )
                    }
                    if (!imageUri.isNullOrBlank()) {
                        MediaPreviewCard(
                            mediaType = MediaType.IMAGE,
                            mediaUri = imageUri,
                            mediaDisplayName = imageName
                        )
                    }
                    if (!videoUri.isNullOrBlank()) {
                        MediaPreviewCard(
                            mediaType = MediaType.VIDEO,
                            mediaUri = videoUri,
                            mediaDisplayName = videoName
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show sticky note on alarm", style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = showStickyOnAlarm,
                            onCheckedChange = { showStickyOnAlarm = it }
                        )
                    }
                    StickyColorSelectorRow(
                        selectedColor = stickyColor,
                        onSelect = { stickyColor = it }
                    )

                    OutlinedButton(
                        enabled = title.isNotBlank(),
                        onClick = {
                            onSaveAsTemplate(
                                QuickTemplateEntity(
                                    name = title.trim().take(24),
                                    targetKind = TemplateTargetKind.REMINDER,
                                    defaultTitle = title.trim(),
                                    defaultBody = noteText.trim(),
                                    stickyColor = stickyColor,
                                    recurrenceType = if (useCustomInterval) RecurrenceType.CUSTOM_DAYS else recurrence,
                                    useCustomInterval = useCustomInterval,
                                    customIntervalUnit = customIntervalUnit,
                                    customIntervalValue = customIntervalValue.coerceIn(1, 999),
                                    checklistJson = ChecklistCodec.encode(checklistItems),
                                    isBuiltIn = false
                                )
                            )
                            templateSavedNotice = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("reminder_save_template_btn")
                    ) {
                        Text(if (templateSavedNotice) "Saved as Reusable Alarm Template" else "Save as Quick Template")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDayMillis,
            yearRange = 1900..2200
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utc ->
                        val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = utc
                        }
                        selectedDayMillis = DateCalendarUtils.dayStartMillis(
                            utcCal.get(Calendar.YEAR),
                            utcCal.get(Calendar.MONTH),
                            utcCal.get(Calendar.DAY_OF_MONTH)
                        )
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = hourOfDay,
            initialMinute = minute
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(state = timeState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            hourOfDay = timeState.hour
                            minute = timeState.minute
                            showTimePicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}
