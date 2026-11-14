package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ReminderEntity
import com.example.ui.DateCalendarUtils
import com.example.ui.MonthKey
import com.example.ui.theme.JetBrainsClockMono
import java.util.Calendar

@Composable
fun YearCalendarScreen(
    reminders: List<ReminderEntity>,
    selectedDayMillis: Long,
    isTwoPane: Boolean,
    onSelectDay: (Long) -> Unit,
    onAddReminderForDay: (Long) -> Unit,
    onEditReminder: (ReminderEntity) -> Unit,
    onToggleReminder: (ReminderEntity, Boolean) -> Unit,
    onDeleteReminder: (ReminderEntity) -> Unit,
    onTestAlarm: (ReminderEntity) -> Unit
) {
    val selectedCal = remember(selectedDayMillis) {
        Calendar.getInstance().apply { timeInMillis = selectedDayMillis }
    }
    var activeMonth by remember {
        mutableStateOf(MonthKey(selectedCal.get(Calendar.YEAR), selectedCal.get(Calendar.MONTH)))
    }
    var showJumpDialog by remember { mutableStateOf(false) }

    val reminderDaysCount = remember(reminders) {
        reminders.groupBy { DateCalendarUtils.dayStartMillis(it.triggerAtMillis) }
            .mapValues { it.value.size }
    }

    val dayReminders = remember(reminders, selectedDayMillis) {
        reminders.filter {
            DateCalendarUtils.dayStartMillis(it.triggerAtMillis) == selectedDayMillis
        }.sortedBy { it.triggerAtMillis }
    }

    val jumpToToday: () -> Unit = {
        val today = DateCalendarUtils.todayStartMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = today }
        activeMonth = MonthKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        onSelectDay(today)
    }

    if (isTwoPane) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WorldCalendarHeader(
                    activeMonth = activeMonth,
                    onPrevYear = { activeMonth = DateCalendarUtils.shiftYear(activeMonth, -1) },
                    onPrevMonth = { activeMonth = DateCalendarUtils.shiftMonth(activeMonth, -1) },
                    onNextMonth = { activeMonth = DateCalendarUtils.shiftMonth(activeMonth, 1) },
                    onNextYear = { activeMonth = DateCalendarUtils.shiftYear(activeMonth, 1) },
                    onOpenPicker = { showJumpDialog = true },
                    onJumpToday = jumpToToday
                )
                MonthCalendarGrid(
                    monthKey = activeMonth,
                    selectedDayMillis = selectedDayMillis,
                    reminderCounts = reminderDaysCount,
                    onSelectDay = onSelectDay
                )
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight()
            ) {
                DayAgendaList(
                    selectedDayMillis = selectedDayMillis,
                    dayReminders = dayReminders,
                    onAddForDay = { onAddReminderForDay(selectedDayMillis) },
                    onEditReminder = onEditReminder,
                    onToggleReminder = onToggleReminder,
                    onDeleteReminder = onDeleteReminder,
                    onTestAlarm = onTestAlarm
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
                WorldCalendarHeader(
                    activeMonth = activeMonth,
                    onPrevYear = { activeMonth = DateCalendarUtils.shiftYear(activeMonth, -1) },
                    onPrevMonth = { activeMonth = DateCalendarUtils.shiftMonth(activeMonth, -1) },
                    onNextMonth = { activeMonth = DateCalendarUtils.shiftMonth(activeMonth, 1) },
                    onNextYear = { activeMonth = DateCalendarUtils.shiftYear(activeMonth, 1) },
                    onOpenPicker = { showJumpDialog = true },
                    onJumpToday = jumpToToday
                )
            }
            item {
                MonthCalendarGrid(
                    monthKey = activeMonth,
                    selectedDayMillis = selectedDayMillis,
                    reminderCounts = reminderDaysCount,
                    onSelectDay = onSelectDay
                )
            }
            item {
                DayAgendaHeader(
                    selectedDayMillis = selectedDayMillis,
                    count = dayReminders.size,
                    onAddForDay = { onAddReminderForDay(selectedDayMillis) }
                )
            }
            if (dayReminders.isEmpty()) {
                item {
                    EmptyDayCard(onAdd = { onAddReminderForDay(selectedDayMillis) })
                }
            } else {
                items(dayReminders, key = { it.id }) { reminder ->
                    ReminderListCard(
                        reminder = reminder,
                        selected = false,
                        onClick = { onEditReminder(reminder) },
                        onToggle = { onToggleReminder(reminder, it) },
                        onTest = { onTestAlarm(reminder) },
                        onEdit = { onEditReminder(reminder) },
                        onDelete = { onDeleteReminder(reminder) }
                    )
                }
            }
        }
    }

    if (showJumpDialog) {
        WorldYearMonthJumpDialog(
            initialMonth = activeMonth,
            onDismiss = { showJumpDialog = false },
            onConfirm = { chosen ->
                activeMonth = chosen
                showJumpDialog = false
            }
        )
    }
}

@Composable
private fun WorldCalendarHeader(
    activeMonth: MonthKey,
    onPrevYear: () -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onNextYear: () -> Unit,
    onOpenPicker: () -> Unit,
    onJumpToday: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "World Calendar",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Browse past, present & future years",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = onJumpToday,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("calendar_today_btn")
            ) {
                Icon(Icons.Filled.Today, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Today")
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPrevYear,
                        modifier = Modifier.size(48.dp).testTag("calendar_prev_year_btn")
                    ) {
                        Icon(Icons.Filled.KeyboardDoubleArrowLeft, contentDescription = "Previous year")
                    }
                    IconButton(
                        onClick = onPrevMonth,
                        modifier = Modifier.size(48.dp).testTag("calendar_prev_month_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                }

                TextButton(
                    onClick = onOpenPicker,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("calendar_year_month_picker_btn")
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = activeMonth.label(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier.size(48.dp).testTag("calendar_next_month_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                    }
                    IconButton(
                        onClick = onNextYear,
                        modifier = Modifier.size(48.dp).testTag("calendar_next_year_btn")
                    ) {
                        Icon(Icons.Filled.KeyboardDoubleArrowRight, contentDescription = "Next year")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorldYearMonthJumpDialog(
    initialMonth: MonthKey,
    onDismiss: () -> Unit,
    onConfirm: (MonthKey) -> Unit
) {
    var yearInput by remember(initialMonth) { mutableStateOf(initialMonth.year.toString()) }
    var selectedMonth by remember(initialMonth) { mutableIntStateOf(initialMonth.monthZeroBased) }
    val parsedYear = yearInput.toIntOrNull()?.coerceIn(1, 9999)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 500.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Jump to Any Year & Month", style = MaterialTheme.typography.headlineMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val y = ((parsedYear ?: initialMonth.year) - 10).coerceAtLeast(1)
                            yearInput = y.toString()
                        },
                        modifier = Modifier.height(48.dp)
                    ) { Text("-10y") }
                    OutlinedButton(
                        onClick = {
                            val y = ((parsedYear ?: initialMonth.year) - 1).coerceAtLeast(1)
                            yearInput = y.toString()
                        },
                        modifier = Modifier.height(48.dp)
                    ) { Text("-1y") }
                    OutlinedTextField(
                        value = yearInput,
                        onValueChange = { raw ->
                            if (raw.length <= 4 && raw.all { it.isDigit() }) {
                                yearInput = raw
                            }
                        },
                        label = { Text("Year") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("world_year_input")
                    )
                    OutlinedButton(
                        onClick = {
                            val y = ((parsedYear ?: initialMonth.year) + 1).coerceAtMost(9999)
                            yearInput = y.toString()
                        },
                        modifier = Modifier.height(48.dp)
                    ) { Text("+1y") }
                    OutlinedButton(
                        onClick = {
                            val y = ((parsedYear ?: initialMonth.year) + 10).coerceAtMost(9999)
                            yearInput = y.toString()
                        },
                        modifier = Modifier.height(48.dp)
                    ) { Text("+10y") }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (0..11).forEach { m ->
                        FilterChip(
                            selected = selectedMonth == m,
                            onClick = { selectedMonth = m },
                            label = { Text(DateCalendarUtils.monthNameShort(m)) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = parsedYear != null,
                        onClick = {
                            onConfirm(MonthKey(parsedYear ?: initialMonth.year, selectedMonth))
                        },
                        modifier = Modifier.testTag("world_year_confirm_btn")
                    ) {
                        Text("Go")
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthCalendarGrid(
    monthKey: MonthKey,
    selectedDayMillis: Long,
    reminderCounts: Map<Long, Int>,
    onSelectDay: (Long) -> Unit
) {
    val todayStart = remember { DateCalendarUtils.todayStartMillis() }
    val daysCount = remember(monthKey) { DateCalendarUtils.daysInMonth(monthKey.year, monthKey.monthZeroBased) }
    val offset = remember(monthKey) { DateCalendarUtils.firstDayOfWeekOffset(monthKey.year, monthKey.monthZeroBased) }

    val cells = remember(monthKey, daysCount, offset) {
        buildList<Int?> {
            repeat(offset) { add(null) }
            for (d in 1..daysCount) add(d)
            while (size % 7 != 0) add(null)
        }
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            cells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    week.forEach { day ->
                        if (day == null) {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val dayMillis = DateCalendarUtils.dayStartMillis(monthKey.year, monthKey.monthZeroBased, day)
                            val isSelected = dayMillis == selectedDayMillis
                            val isToday = dayMillis == todayStart
                            val count = reminderCounts[dayMillis] ?: 0

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceContainerLow
                                        }
                                    )
                                    .then(
                                        if (isToday && !isSelected) {
                                            Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                        } else Modifier
                                    )
                                    .clickable { onSelectDay(dayMillis) }
                                    .testTag("calendar_day_${monthKey.year}_${monthKey.monthZeroBased}_$day"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$day",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    if (count > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.tertiary
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayAgendaList(
    selectedDayMillis: Long,
    dayReminders: List<ReminderEntity>,
    onAddForDay: () -> Unit,
    onEditReminder: (ReminderEntity) -> Unit,
    onToggleReminder: (ReminderEntity, Boolean) -> Unit,
    onDeleteReminder: (ReminderEntity) -> Unit,
    onTestAlarm: (ReminderEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DayAgendaHeader(
                selectedDayMillis = selectedDayMillis,
                count = dayReminders.size,
                onAddForDay = onAddForDay
            )
        }
        if (dayReminders.isEmpty()) {
            item {
                EmptyDayCard(onAdd = onAddForDay)
            }
        } else {
            items(dayReminders, key = { it.id }) { reminder ->
                ReminderListCard(
                    reminder = reminder,
                    selected = false,
                    onClick = { onEditReminder(reminder) },
                    onToggle = { onToggleReminder(reminder, it) },
                    onTest = { onTestAlarm(reminder) },
                    onEdit = { onEditReminder(reminder) },
                    onDelete = { onDeleteReminder(reminder) }
                )
            }
        }
    }
}

@Composable
private fun DayAgendaHeader(
    selectedDayMillis: Long,
    count: Int,
    onAddForDay: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = DateCalendarUtils.relativeDayLabel(selectedDayMillis),
                fontFamily = JetBrainsClockMono,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = DateCalendarUtils.formatDateFull(selectedDayMillis),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = if (count == 0) "No alarms scheduled" else "$count reminder(s) scheduled",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = onAddForDay,
            modifier = Modifier.height(48.dp).testTag("day_add_reminder_btn")
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Schedule")
        }
    }
}

@Composable
private fun EmptyDayCard(onAdd: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarToday,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text("Open date on the world calendar", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Tap Schedule to pin an alarm, music track, image, or video for this date.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Button(onClick = onAdd) {
                Text("Add Reminder Here")
            }
        }
    }
}
