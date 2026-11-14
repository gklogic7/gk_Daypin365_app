package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MediaType
import com.example.data.model.QuickTemplateEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.TemplateTargetKind
import com.example.data.repository.DailyQuote
import com.example.data.repository.MotivationProvider
import com.example.data.repository.QuoteCategory
import com.example.ui.DateCalendarUtils
import com.example.ui.components.ChecklistInteractiveSection
import com.example.ui.components.MediaPreviewCard
import com.example.ui.components.QuickTemplateStrip
import com.example.ui.theme.JetBrainsClockMono
import com.example.ui.theme.stickyBorderColor
import com.example.ui.theme.stickySurfaceColor

@Composable
fun TodayAlarmsScreen(
    reminders: List<ReminderEntity>,
    templates: List<QuickTemplateEntity> = emptyList(),
    quoteCategory: QuoteCategory = QuoteCategory.ALL,
    selectedReminderId: Long?,
    isTwoPane: Boolean,
    onSelectReminder: (Long?) -> Unit,
    onAddReminder: () -> Unit,
    onApplyTemplate: (QuickTemplateEntity) -> Unit = {},
    onDeleteTemplate: (Long) -> Unit = {},
    onToggleChecklistItem: (Long, String) -> Unit = { _, _ -> },
    onPinQuoteNote: (DailyQuote) -> Unit = {},
    onEditReminder: (ReminderEntity) -> Unit,
    onToggleReminder: (ReminderEntity, Boolean) -> Unit,
    onDeleteReminder: (ReminderEntity) -> Unit,
    onTestAlarm: (ReminderEntity) -> Unit,
    onOpenStickyNotes: () -> Unit,
    onOpenDeskBoard: () -> Unit
) {
    val activeReminders = reminders.filter { it.isEnabled }.sortedBy { it.triggerAtMillis }
    val nextAlarm = activeReminders.firstOrNull { it.triggerAtMillis > System.currentTimeMillis() }
        ?: activeReminders.firstOrNull()
    val alarmTemplates = remember(templates) {
        templates.filter { it.targetKind == TemplateTargetKind.REMINDER }
    }
    var todayQuote by remember(quoteCategory) {
        mutableStateOf(MotivationProvider.randomQuoteEntry(category = quoteCategory))
    }

    val inspectedReminder = reminders.firstOrNull { it.id == selectedReminderId } ?: nextAlarm

    if (isTwoPane) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                item {
                    HeroDeskBanner(
                        nextAlarm = nextAlarm,
                        activeCount = activeReminders.size,
                        onAddReminder = onAddReminder,
                        onOpenStickyNotes = onOpenStickyNotes,
                        onOpenDeskBoard = onOpenDeskBoard
                    )
                }

                if (alarmTemplates.isNotEmpty()) {
                    item {
                        QuickTemplateStrip(
                            title = "Quick Alarm Templates",
                            templates = alarmTemplates,
                            onApplyTemplate = onApplyTemplate,
                            onDeleteCustomTemplate = { tpl -> onDeleteTemplate(tpl.id) }
                        )
                    }
                }

                item {
                    TodayQuotePreviewCard(
                        quote = todayQuote,
                        onShuffle = {
                            todayQuote = MotivationProvider.randomQuoteEntry(
                                category = quoteCategory,
                                exclude = todayQuote.text
                            )
                        },
                        onPinNote = { onPinQuoteNote(todayQuote) }
                    )
                }

                if (reminders.isEmpty()) {
                    item {
                        EmptyRemindersCard(onAddReminder = onAddReminder)
                    }
                } else {
                    items(reminders, key = { it.id }) { reminder ->
                        ReminderListCard(
                            reminder = reminder,
                            selected = reminder.id == inspectedReminder?.id,
                            onClick = { onSelectReminder(reminder.id) },
                            onToggle = { enabled -> onToggleReminder(reminder, enabled) },
                            onToggleChecklistItem = { itemId -> onToggleChecklistItem(reminder.id, itemId) },
                            onTest = { onTestAlarm(reminder) },
                            onEdit = { onEditReminder(reminder) },
                            onDelete = { onDeleteReminder(reminder) }
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight()
            ) {
                ReminderInspectorPane(
                    reminder = inspectedReminder,
                    onToggleChecklistItem = { itemId ->
                        inspectedReminder?.let { onToggleChecklistItem(it.id, itemId) }
                    },
                    onTest = { inspectedReminder?.let(onTestAlarm) },
                    onEdit = { inspectedReminder?.let(onEditReminder) },
                    onAddReminder = onAddReminder
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HeroDeskBanner(
                    nextAlarm = nextAlarm,
                    activeCount = activeReminders.size,
                    onAddReminder = onAddReminder,
                    onOpenStickyNotes = onOpenStickyNotes,
                    onOpenDeskBoard = onOpenDeskBoard
                )
            }

            if (alarmTemplates.isNotEmpty()) {
                item {
                    QuickTemplateStrip(
                        title = "Quick Alarm Templates",
                        templates = alarmTemplates,
                        onApplyTemplate = onApplyTemplate,
                        onDeleteCustomTemplate = { tpl -> onDeleteTemplate(tpl.id) }
                    )
                }
            }

            item {
                TodayQuotePreviewCard(
                    quote = todayQuote,
                    onShuffle = {
                        todayQuote = MotivationProvider.randomQuoteEntry(
                            category = quoteCategory,
                            exclude = todayQuote.text
                        )
                    },
                    onPinNote = { onPinQuoteNote(todayQuote) }
                )
            }

            if (reminders.isEmpty()) {
                item {
                    EmptyRemindersCard(onAddReminder = onAddReminder)
                }
            } else {
                items(reminders, key = { it.id }) { reminder ->
                    ReminderListCard(
                        reminder = reminder,
                        selected = reminder.id == selectedReminderId,
                        onClick = { onEditReminder(reminder) },
                        onToggle = { enabled -> onToggleReminder(reminder, enabled) },
                        onToggleChecklistItem = { itemId -> onToggleChecklistItem(reminder.id, itemId) },
                        onTest = { onTestAlarm(reminder) },
                        onEdit = { onEditReminder(reminder) },
                        onDelete = { onDeleteReminder(reminder) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroDeskBanner(
    nextAlarm: ReminderEntity?,
    activeCount: Int,
    onAddReminder: () -> Unit,
    onOpenStickyNotes: () -> Unit,
    onOpenDeskBoard: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_desk_hero),
                contentDescription = "DayPin 365 desk illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.72f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (nextAlarm != null) {
                        "NEXT • ${DateCalendarUtils.formatCountdown(nextAlarm.triggerAtMillis).uppercase()}"
                    } else {
                        "WORLD CALENDAR PLANNER READY"
                    },
                    fontFamily = JetBrainsClockMono,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFD6B8)
                )
                Text(
                    text = nextAlarm?.title ?: "No active alarms scheduled",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (nextAlarm != null) {
                        "${DateCalendarUtils.formatDateFull(nextAlarm.triggerAtMillis)} at ${DateCalendarUtils.formatTime(nextAlarm.triggerAtMillis)} • $activeCount active"
                    } else {
                        "Add reminders with simultaneous music, image, video, or sticky notes"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAddReminder,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("hero_add_reminder_btn")
            ) {
                Icon(Icons.Filled.AddAlert, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("New Alarm")
            }
            FilledTonalButton(
                onClick = onOpenDeskBoard,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("hero_desk_board_btn")
            ) {
                Icon(Icons.Filled.Alarm, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Desk Clock")
            }
            OutlinedButton(
                onClick = onOpenStickyNotes,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("hero_sticky_notes_btn")
            ) {
                Icon(Icons.Filled.StickyNote2, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Notes")
            }
        }
    }
}

@Composable
fun ReminderListCard(
    reminder: ReminderEntity,
    selected: Boolean,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onToggleChecklistItem: (String) -> Unit = {},
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val borderModifier = if (selected) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
    } else {
        Modifier
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (reminder.isEnabled) 2.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("reminder_card_${reminder.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = DateCalendarUtils.formatTime(reminder.triggerAtMillis),
                            fontFamily = JetBrainsClockMono,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (reminder.isEnabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = reminder.recurrenceSummaryLabel(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${DateCalendarUtils.formatDateFull(reminder.triggerAtMillis)} • ${DateCalendarUtils.formatCountdown(reminder.triggerAtMillis)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = reminder.isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("reminder_toggle_${reminder.id}")
                )
            }

            AnimatedVisibility(visible = reminder.noteText.isNotBlank(), enter = fadeIn()) {
                Surface(
                    color = stickySurfaceColor(reminder.stickyColor.name),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, stickyBorderColor(reminder.stickyColor.name), RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = reminder.noteText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            val checklistItems = remember(reminder.checklistJson) { reminder.checklistItems() }
            if (checklistItems.isNotEmpty()) {
                ChecklistInteractiveSection(
                    items = checklistItems,
                    compact = true,
                    maxVisible = 3,
                    onToggleItem = onToggleChecklistItem
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = onEdit,
                    leadingIcon = {
                        Icon(
                            imageVector = primaryMediaIcon(reminder),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = {
                        Text(
                            text = reminder.mediaSummaryLabel(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onTest,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("test_alarm_${reminder.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Test alarm ring now",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit reminder")
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete reminder")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderInspectorPane(
    reminder: ReminderEntity?,
    onToggleChecklistItem: (String) -> Unit,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onAddReminder: () -> Unit
) {
    if (reminder == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Alarm,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Alarm & Media Preview",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Select any reminder on the left to inspect its music, photo, video, or sticky note attachment.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAddReminder) {
                Text("Create First Reminder")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ALARM INSPECTOR • ${reminder.recurrenceSummaryLabel().uppercase()}",
            fontFamily = JetBrainsClockMono,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = reminder.title,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "${DateCalendarUtils.formatDateFull(reminder.triggerAtMillis)} at ${DateCalendarUtils.formatTime(reminder.triggerAtMillis)}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (reminder.hasAnyMedia) {
            reminder.effectiveMusicUri?.let { uri ->
                MediaPreviewCard(
                    mediaType = MediaType.MUSIC,
                    mediaUri = uri,
                    mediaDisplayName = reminder.effectiveMusicName
                )
            }
            reminder.effectiveImageUri?.let { uri ->
                MediaPreviewCard(
                    mediaType = MediaType.IMAGE,
                    mediaUri = uri,
                    mediaDisplayName = reminder.effectiveImageName
                )
            }
            reminder.effectiveVideoUri?.let { uri ->
                MediaPreviewCard(
                    mediaType = MediaType.VIDEO,
                    mediaUri = uri,
                    mediaDisplayName = reminder.effectiveVideoName
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                    Text("Uses built-in system alarm ringtone", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (reminder.noteText.isNotBlank()) {
            Surface(
                color = stickySurfaceColor(reminder.stickyColor.name),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, stickyBorderColor(reminder.stickyColor.name), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ATTACHED STICKY NOTE", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(reminder.noteText, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        val inspectorChecklist = remember(reminder.checklistJson) { reminder.checklistItems() }
        if (inspectorChecklist.isNotEmpty()) {
            ChecklistInteractiveSection(
                items = inspectorChecklist,
                compact = false,
                onToggleItem = onToggleChecklistItem
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onTest,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Test Alarm Now")
            }
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Edit")
            }
        }
    }
}

@Composable
private fun EmptyRemindersCard(onAddReminder: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Alarm,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Text("Plan across any date or interval", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Tap New Alarm to schedule a reminder with music, image, video, and sticky note.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Button(onClick = onAddReminder) {
                Text("Add First Alarm")
            }
        }
    }
}

private fun primaryMediaIcon(reminder: ReminderEntity) = when {
    !reminder.effectiveVideoUri.isNullOrBlank() -> Icons.Filled.Videocam
    !reminder.effectiveImageUri.isNullOrBlank() -> Icons.Filled.Image
    !reminder.effectiveMusicUri.isNullOrBlank() -> Icons.Filled.MusicNote
    else -> Icons.Filled.NotificationsActive
}

@Composable
private fun TodayQuotePreviewCard(
    quote: DailyQuote,
    onShuffle: () -> Unit,
    onPinNote: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_daily_quote_card")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "DAILY QUOTE • ${quote.category.label.uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onPinNote,
                        modifier = Modifier.testTag("today_quote_pin_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pin quote as sticky note",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Pin Note")
                    }
                    IconButton(
                        onClick = onShuffle,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("today_quote_shuffle_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Next random quote",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            AnimatedContent(targetState = quote.text, label = "today_quote_swap") { quoteText ->
                Text(
                    text = "\"$quoteText\"",
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
