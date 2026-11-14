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
import androidx.compose.material.icons.filled.Save
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
import com.example.data.model.StickyColor
import com.example.data.model.TaskEntity
import com.example.data.model.TaskStatus
import com.example.data.model.TemplateTargetKind
import com.example.data.repository.CachedMediaResult
import com.example.ui.DateCalendarUtils
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorDialog(
    initialTask: TaskEntity?,
    defaultDayMillis: Long,
    initialTemplate: QuickTemplateEntity? = null,
    templates: List<QuickTemplateEntity> = emptyList(),
    onSaveAsTemplate: (QuickTemplateEntity) -> Unit = {},
    onDeleteTemplate: (Long) -> Unit = {},
    onPersistMedia: suspend (Uri, String) -> CachedMediaResult,
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit
) {
    val scope = rememberCoroutineScope()
    val initCal = remember(initialTask, defaultDayMillis) {
        Calendar.getInstance().apply {
            if (initialTask != null) {
                timeInMillis = initialTask.triggerAtMillis
            } else {
                timeInMillis = defaultDayMillis
                val now = Calendar.getInstance()
                val nextHour = (now.get(Calendar.HOUR_OF_DAY) + 1) % 24
                set(Calendar.HOUR_OF_DAY, nextHour)
                set(Calendar.MINUTE, 0)
            }
        }
    }

    var title by remember(initialTask, initialTemplate) {
        mutableStateOf(initialTask?.title ?: initialTemplate?.defaultTitle ?: "")
    }
    var description by remember(initialTask, initialTemplate) {
        mutableStateOf(initialTask?.description ?: initialTemplate?.defaultBody ?: "")
    }
    var checklistItems by remember(initialTask, initialTemplate) {
        mutableStateOf(initialTask?.checklistItems() ?: initialTemplate?.checklistItems() ?: emptyList())
    }
    var templateSavedNotice by remember { mutableStateOf(false) }

    var selectedDayMillis by remember(initialTask, defaultDayMillis) {
        mutableLongStateOf(DateCalendarUtils.dayStartMillis(initCal.timeInMillis))
    }
    var hourOfDay by remember { mutableIntStateOf(initCal.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(initCal.get(Calendar.MINUTE)) }

    var recurrence by remember(initialTask, initialTemplate) {
        mutableStateOf(initialTask?.recurrenceType ?: initialTemplate?.recurrenceType ?: RecurrenceType.ONCE)
    }
    var useCustomInterval by remember(initialTask, initialTemplate) {
        mutableStateOf(initialTask?.useCustomInterval ?: initialTemplate?.useCustomInterval ?: false)
    }
    var customIntervalUnit by remember(initialTask, initialTemplate) {
        mutableStateOf(initialTask?.customIntervalUnit ?: initialTemplate?.customIntervalUnit ?: IntervalUnit.DAYS)
    }
    var customIntervalValue by remember(initialTask, initialTemplate) {
        mutableIntStateOf((initialTask?.customIntervalValue ?: initialTemplate?.customIntervalValue ?: 1).coerceIn(1, 999))
    }

    var musicUri by remember(initialTask) { mutableStateOf(initialTask?.musicUri) }
    var musicName by remember(initialTask) { mutableStateOf(initialTask?.musicDisplayName) }
    var imageUri by remember(initialTask) { mutableStateOf(initialTask?.imageUri) }
    var imageName by remember(initialTask) { mutableStateOf(initialTask?.imageDisplayName) }
    var videoUri by remember(initialTask) { mutableStateOf(initialTask?.videoUri) }
    var videoName by remember(initialTask) { mutableStateOf(initialTask?.videoDisplayName) }
    var stickyColor by remember(initialTask, initialTemplate) {
        mutableStateOf(initialTask?.stickyColor ?: initialTemplate?.stickyColor ?: StickyColor.SKY)
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    fun applyTemplate(tpl: QuickTemplateEntity) {
        title = tpl.defaultTitle
        description = tpl.defaultBody
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
                val cached = onPersistMedia(uri, "task_img")
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
                val cached = onPersistMedia(uri, "task_anim")
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
                val cached = onPersistMedia(uri, "task_vid")
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
                val cached = onPersistMedia(uri, "task_aud")
                musicUri = cached.uriString
                musicName = cached.displayName
            }
        }
    }

    fun buildTask(): TaskEntity {
        val combined = DateCalendarUtils.combineDateAndTime(selectedDayMillis, hourOfDay, minute)
        val effectiveRecurrence = if (useCustomInterval) RecurrenceType.CUSTOM_DAYS else recurrence
        return (initialTask ?: TaskEntity(
            title = "",
            triggerAtMillis = combined
        )).copy(
            title = title.trim(),
            description = description.trim(),
            triggerAtMillis = combined,
            recurrenceType = effectiveRecurrence,
            useCustomInterval = useCustomInterval,
            customIntervalUnit = customIntervalUnit,
            customIntervalValue = customIntervalValue.coerceIn(1, 999),
            isEnabled = true,
            status = initialTask?.status?.takeIf { it != TaskStatus.DONE } ?: TaskStatus.PENDING,
            musicUri = musicUri,
            musicDisplayName = musicName,
            imageUri = imageUri,
            imageDisplayName = imageName,
            videoUri = videoUri,
            videoDisplayName = videoName,
            stickyColor = stickyColor,
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
                .widthIn(max = 640.dp)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Header with Save Task & Cancel in top-right corner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 14.dp, top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialTask == null) "New Task Alarm" else "Edit Task Alarm",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("task_cancel_button")
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            enabled = title.isNotBlank(),
                            onClick = { onSave(buildTask()) },
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("task_save_button")
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save Task")
                        }
                    }
                }

                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Rings like an alarm and asks if the task is Done, Working, Started (each asks for an image proof), or Later.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    val taskTemplates = remember(templates) {
                        templates.filter { it.targetKind == TemplateTargetKind.TASK }
                    }
                    QuickTemplateStrip(
                        title = "Quick Task Templates",
                        templates = taskTemplates,
                        onApplyTemplate = ::applyTemplate,
                        onDeleteCustomTemplate = { tpl -> onDeleteTemplate(tpl.id) }
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task title") },
                        placeholder = { Text("e.g., Clean workbench or Submit daily report") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_title_input")
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Instructions / Notes (optional)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_desc_input")
                    )

                    ChecklistEditorSection(
                        items = checklistItems,
                        onItemsChange = { checklistItems = it }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
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
                        ) {
                            Icon(Icons.Filled.AccessTime, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            val previewCal = DateCalendarUtils.combineDateAndTime(selectedDayMillis, hourOfDay, minute)
                            Text(DateCalendarUtils.formatTime(previewCal))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Toggle Custom Interval", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = if (useCustomInterval) {
                                    "Repeats every $customIntervalValue ${customIntervalUnit.label.lowercase()}"
                                } else {
                                    "Standard presets (Once / Daily / Weekly / Yearly)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useCustomInterval,
                            onCheckedChange = { useCustomInterval = it }
                        )
                    }

                    if (!useCustomInterval) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                RecurrenceType.ONCE,
                                RecurrenceType.DAILY,
                                RecurrenceType.WEEKLY,
                                RecurrenceType.YEARLY
                            ).forEach { type ->
                                FilterChip(
                                    selected = recurrence == type,
                                    onClick = { recurrence = type },
                                    label = { Text(type.label) }
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IntervalUnit.entries.forEach { unit ->
                                        FilterChip(
                                            selected = customIntervalUnit == unit,
                                            onClick = { customIntervalUnit = unit },
                                            label = { Text(unit.label) }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Every $customIntervalValue ${customIntervalUnit.label}", style = MaterialTheme.typography.titleMedium)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FilledTonalIconButton(
                                            onClick = { customIntervalValue = (customIntervalValue - 1).coerceAtLeast(1) },
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                                        }
                                        FilledTonalIconButton(
                                            onClick = { customIntervalValue = (customIntervalValue + 1).coerceAtMost(999) },
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(Icons.Filled.Add, contentDescription = "Increase")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text("Alarm Media (Select multiple)", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !musicUri.isNullOrBlank(),
                            onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                            leadingIcon = { Icon(Icons.Filled.MusicNote, contentDescription = null) },
                            trailingIcon = if (!musicUri.isNullOrBlank()) {
                                {
                                    IconButton(
                                        onClick = { musicUri = null; musicName = null },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Remove music", modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else null,
                            label = { Text(musicName ?: "Play Music") }
                        )

                        val isAnimatedImage = imageUri?.lowercase()?.let {
                            it.endsWith(".gif") || it.endsWith(".webp")
                        } == true

                        FilterChip(
                            selected = !imageUri.isNullOrBlank() && !isAnimatedImage,
                            onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null) },
                            trailingIcon = if (!imageUri.isNullOrBlank() && !isAnimatedImage) {
                                {
                                    IconButton(
                                        onClick = { imageUri = null; imageName = null },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Remove image", modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else null,
                            label = { Text(if (!isAnimatedImage) (imageName ?: "Show Image") else "Show Image") },
                            modifier = Modifier.testTag("task_pick_image_chip")
                        )

                        FilterChip(
                            selected = !imageUri.isNullOrBlank() && isAnimatedImage,
                            onClick = {
                                gifWebpPickerLauncher.launch(arrayOf("image/gif", "image/webp"))
                            },
                            leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null) },
                            trailingIcon = if (!imageUri.isNullOrBlank() && isAnimatedImage) {
                                {
                                    IconButton(
                                        onClick = { imageUri = null; imageName = null },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Remove GIF or WebP", modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else null,
                            label = {
                                Text(
                                    if (!imageUri.isNullOrBlank() && isAnimatedImage) {
                                        imageName ?: "GIF / WebP Added"
                                    } else {
                                        "GIF / WebP"
                                    }
                                )
                            },
                            modifier = Modifier.testTag("task_pick_gif_webp_chip")
                        )

                        FilterChip(
                            selected = !videoUri.isNullOrBlank(),
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            leadingIcon = { Icon(Icons.Filled.Videocam, contentDescription = null) },
                            trailingIcon = if (!videoUri.isNullOrBlank()) {
                                {
                                    IconButton(
                                        onClick = { videoUri = null; videoName = null },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Remove video", modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else null,
                            label = { Text(videoName ?: "Play Video") }
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

                    Text("Task Card Color", style = MaterialTheme.typography.labelLarge)
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
                                    targetKind = TemplateTargetKind.TASK,
                                    defaultTitle = title.trim(),
                                    defaultBody = description.trim(),
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
                            .testTag("task_save_template_btn")
                    ) {
                        Text(if (templateSavedNotice) "Saved as Reusable Task Template" else "Save as Quick Template")
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
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            selectedDayMillis = DateCalendarUtils.dayStartMillis(
                                utcCal.get(Calendar.YEAR),
                                utcCal.get(Calendar.MONTH),
                                utcCal.get(Calendar.DAY_OF_MONTH)
                            )
                        }
                        showDatePicker = false
                    }
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Set Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = hourOfDay,
            initialMinute = minute,
            is24Hour = false
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Select Task Alarm Time", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                hourOfDay = timePickerState.hour
                                minute = timePickerState.minute
                                showTimePicker = false
                            }
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        }
    }
}
