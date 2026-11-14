package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferences
import com.example.data.local.OverlayAnimationStyle
import com.example.data.local.OverlayDisplayMode
import com.example.data.local.OverlayPanelStyle
import com.example.data.local.QuoteScheduleMode
import com.example.data.model.ChecklistCodec
import com.example.data.model.ChecklistItem
import com.example.data.model.DefaultTemplates
import com.example.data.model.IntervalUnit
import com.example.data.model.MediaType
import com.example.data.model.QuickTemplateEntity
import com.example.data.model.RecurrenceType
import com.example.data.model.ReminderEntity
import com.example.data.model.StickyColor
import com.example.data.model.StickyNoteEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskProofEntity
import com.example.data.model.TaskStatus
import com.example.data.model.TemplateTargetKind
import com.example.data.repository.BackupJsonManager
import com.example.data.repository.MotivationProvider
import com.example.data.repository.QuoteCategory
import com.example.data.repository.ReminderRepository
import com.example.quotes.QuoteNotificationScheduler
import com.example.ui.DateCalendarUtils
import com.example.ui.MonthKey
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.resolveColorScheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `app name matches DayPin 365`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("DayPin 365", appName)
    }

    @Test
    fun `365 day horizon covers full year of months`() {
        val months = DateCalendarUtils.next365DaysMonths()
        assertTrue(months.size in 12..13)
        val today = DateCalendarUtils.todayStartMillis()
        assertEquals(1, DateCalendarUtils.dayNumberIn365Horizon(today))
        val day365 = today + TimeUnit.DAYS.toMillis(364)
        assertEquals(365, DateCalendarUtils.dayNumberIn365Horizon(day365))
    }

    @Test
    fun `world calendar navigates infinitely to past and future years`() {
        val start = MonthKey(year = 2026, monthZeroBased = Calendar.JANUARY)
        val prevDec = DateCalendarUtils.shiftMonth(start, -1)
        assertEquals(2025, prevDec.year)
        assertEquals(Calendar.DECEMBER, prevDec.monthZeroBased)

        val past1995 = DateCalendarUtils.shiftYear(start, -31)
        assertEquals(1995, past1995.year)
        val future2120 = DateCalendarUtils.shiftYear(start, 94)
        assertEquals(2120, future2120.year)

        assertEquals(29, DateCalendarUtils.daysInMonth(2024, Calendar.FEBRUARY))
        assertEquals(28, DateCalendarUtils.daysInMonth(2100, Calendar.FEBRUARY))

        val pastDay = DateCalendarUtils.dayStartMillis(1995, Calendar.JUNE, 15)
        assertTrue(DateCalendarUtils.relativeDayLabel(pastDay).endsWith("DAYS AGO"))

        val farFuture = DateCalendarUtils.todayStartMillis() + TimeUnit.DAYS.toMillis(500)
        assertTrue(DateCalendarUtils.relativeDayLabel(farFuture).contains("IN FUTURE"))
    }

    @Test
    fun `custom recurrence advances accurately for days hours and minutes`() {
        val now = System.currentTimeMillis()
        val pastTrigger = now - TimeUnit.DAYS.toMillis(2)
        val dayReminder = ReminderEntity(
            title = "Water plants",
            triggerAtMillis = pastTrigger,
            recurrenceType = RecurrenceType.CUSTOM_DAYS,
            customIntervalDays = 5
        )
        val nextDay = ReminderRepository.computeNextFutureTrigger(dayReminder, now)
        assertTrue(nextDay > now)
        assertEquals(TimeUnit.DAYS.toMillis(5), nextDay - pastTrigger)

        val pastMinutes = now - TimeUnit.MINUTES.toMillis(25)
        val minuteReminder = ReminderEntity(
            title = "Stretch break",
            triggerAtMillis = pastMinutes,
            useCustomInterval = true,
            customIntervalUnit = IntervalUnit.MINUTES,
            customIntervalValue = 45
        )
        val nextMinute = ReminderRepository.computeNextFutureTrigger(minuteReminder, now)
        assertTrue(nextMinute > now)
        assertEquals(TimeUnit.MINUTES.toMillis(45), nextMinute - pastMinutes)

        val pastHours = now - TimeUnit.HOURS.toMillis(2)
        val hourTask = TaskEntity(
            title = "Inspect lab equipment",
            triggerAtMillis = pastHours,
            useCustomInterval = true,
            customIntervalUnit = IntervalUnit.HOURS,
            customIntervalValue = 3
        )
        val nextHour = ReminderRepository.computeNextFutureTaskTrigger(hourTask, now)
        assertTrue(nextHour > now)
        assertEquals(TimeUnit.HOURS.toMillis(3), nextHour - pastHours)
    }

    @Test
    fun `room stores multi-media reminder task proofs and sticky note`() = runTest {
        val reminderId = db.reminderDao().insertReminder(
            ReminderEntity(
                title = "Morning Brief",
                noteText = "Review quarterly tablet slides",
                triggerAtMillis = System.currentTimeMillis() + 3600_000L,
                mediaType = MediaType.MUSIC,
                musicUri = "file:///music/jazz.mp3",
                musicDisplayName = "jazz.mp3",
                imageUri = "file:///images/agenda.png",
                imageDisplayName = "agenda.png",
                videoUri = "file:///videos/intro.mp4",
                videoDisplayName = "intro.mp4",
                useCustomInterval = true,
                customIntervalUnit = IntervalUnit.HOURS,
                customIntervalValue = 6,
                stickyColor = StickyColor.SAGE
            )
        )
        val savedReminder = db.reminderDao().getReminderById(reminderId)
        assertNotNull(savedReminder)
        assertEquals("Music + Image + Video", savedReminder?.mediaSummaryLabel())

        val taskId = db.taskDao().insertTask(
            TaskEntity(
                title = "Warehouse Audit",
                description = "Capture photo at start, working, and completion",
                triggerAtMillis = System.currentTimeMillis() + 7200_000L,
                status = TaskStatus.STARTED
            )
        )
        db.taskDao().insertProof(
            TaskProofEntity(
                taskId = taskId,
                statusAtProof = TaskStatus.STARTED,
                imageUri = "file:///proofs/start.jpg",
                imageDisplayName = "start.jpg",
                note = "Started aisle 3"
            )
        )
        db.taskDao().insertProof(
            TaskProofEntity(
                taskId = taskId,
                statusAtProof = TaskStatus.WORKING,
                imageUri = "file:///proofs/working.jpg",
                imageDisplayName = "working.jpg",
                note = "Halfway through shelves"
            )
        )
        val proofs = db.taskDao().getProofsForTaskOnce(taskId)
        assertEquals(2, proofs.size)

        val noteId = db.stickyNoteDao().insertNote(
            StickyNoteEntity(
                title = "Desk Pin",
                content = "Keep tablet charged for meeting",
                color = StickyColor.BUTTERCUP,
                isPinnedOverlay = true
            )
        )
        val pinned = db.stickyNoteDao().observePinnedNotes().first()
        assertEquals(1, pinned.size)
        assertEquals(noteId, pinned.first().id)
    }

    @Test
    fun `json backup round-trip preserves tasks proofs overlay mode reminders and prefs`() {
        val reminder = ReminderEntity(
            id = 42L,
            title = "Studio Alarm",
            noteText = "Play soundtrack with slide",
            triggerAtMillis = 1_800_000_000_000L,
            recurrenceType = RecurrenceType.DAILY,
            useCustomInterval = true,
            customIntervalUnit = IntervalUnit.MINUTES,
            customIntervalValue = 30,
            isEnabled = true,
            mediaType = MediaType.MUSIC,
            musicUri = "file:///music/lofi.mp3",
            musicDisplayName = "lofi.mp3",
            imageUri = "file:///images/cover.jpg",
            imageDisplayName = "cover.jpg",
            stickyColor = StickyColor.SKY,
            showStickyOnAlarm = true
        )
        val task = TaskEntity(
            id = 7L,
            title = "Clean Desk",
            description = "Send proof when started and done",
            triggerAtMillis = 1_800_000_500_000L,
            status = TaskStatus.WORKING,
            useCustomInterval = true,
            customIntervalUnit = IntervalUnit.HOURS,
            customIntervalValue = 4
        )
        val proof = TaskProofEntity(
            id = 1L,
            taskId = 7L,
            statusAtProof = TaskStatus.WORKING,
            imageUri = "file:///proofs/desk_wip.jpg",
            imageDisplayName = "desk_wip.jpg",
            note = "Cleared cables"
        )
        val note = StickyNoteEntity(
            id = 9L,
            title = "Pinned Checklist",
            content = "Export weekly backup",
            color = StickyColor.BLUSH,
            isPinnedOverlay = true
        )
        val prefs = AppPreferences(
            defaultSnoozeMinutes = 15,
            vibrationEnabled = false,
            fadeInAudio = true,
            themeMode = ThemeMode.MIDNIGHT_INDIGO,
            overlayDisplayMode = OverlayDisplayMode.FULL,
            overlayPanelStyle = OverlayPanelStyle.ACTION_SPOTLIGHT,
            overlayAnimationStyle = OverlayAnimationStyle.ZOOM_BOUNCE,
            deskLaunchMode = com.example.data.local.DeskLaunchMode.AUTO_ON_CHARGING,
            deskKeepScreenOn = false,
            deskDimMode = true
        )

        val json = BackupJsonManager.serializeToJson(
            reminders = listOf(reminder),
            stickyNotes = listOf(note),
            preferences = prefs,
            tasks = listOf(task),
            taskProofs = listOf(proof)
        )
        val parsed = BackupJsonManager.parseBackupJson(json).getOrThrow()

        assertEquals(1, parsed.reminders.size)
        assertEquals(1, parsed.tasks.size)
        assertEquals("Clean Desk", parsed.tasks.first().task.title)
        assertEquals(TaskStatus.WORKING, parsed.tasks.first().task.status)
        assertEquals(1, parsed.tasks.first().proofs.size)
        assertEquals("file:///proofs/desk_wip.jpg", parsed.tasks.first().proofs.first().imageUri)
        assertEquals(OverlayDisplayMode.FULL, parsed.overlayDisplayMode)
        assertEquals(OverlayPanelStyle.ACTION_SPOTLIGHT, parsed.overlayPanelStyle)
        assertEquals(OverlayAnimationStyle.ZOOM_BOUNCE, parsed.overlayAnimationStyle)
        assertEquals(com.example.data.local.DeskLaunchMode.AUTO_ON_CHARGING, parsed.deskLaunchMode)
        assertEquals(false, parsed.deskKeepScreenOn)
        assertEquals(true, parsed.deskDimMode)
        assertEquals(15, parsed.snoozeMinutes)
        assertEquals(ThemeMode.MIDNIGHT_INDIGO, parsed.themeMode)
    }

    @Test
    fun `motivation provider themes overlay panels and tween animations match specifications`() {
        val q1 = MotivationProvider.randomQuote()
        assertTrue(q1.isNotBlank())
        val q2 = MotivationProvider.randomQuote(exclude = q1)
        assertTrue(q2.isNotBlank() && q2 != q1)

        val body = MotivationProvider.composeNotificationBody(
            motivationLine = "Stay steady",
            noteText = "Bring laptop charger",
            fallbackBody = "Fallback"
        )
        assertTrue(body.contains("Stay steady"))
        assertTrue(body.contains("Notes: Bring laptop charger"))

        assertEquals(10, ThemeMode.curatedThemes.size)
        assertEquals(14, OverlayPanelStyle.entries.size)
        assertEquals(7, OverlayAnimationStyle.entries.size)

        ThemeMode.curatedThemes.forEach { mode ->
            val scheme = resolveColorScheme(mode, systemDark = false)
            assertNotNull(scheme.primary)
        }
    }

    @Test
    fun `checklist codec encodes decodes toggles and formats notification text`() {
        val raw = ChecklistCodec.encode(
            listOf(
                ChecklistItem(id = "s1", label = "Pack badge", isDone = false),
                ChecklistItem(id = "s2", label = "Charge tablet", isDone = true),
                ChecklistItem(id = "s3", label = "   ", isDone = false)
            )
        )
        val decoded = ChecklistCodec.decode(raw)
        assertEquals(2, decoded.size)
        assertEquals(1 to 2, ChecklistCodec.progress(raw))

        val toggled = ChecklistCodec.toggle(raw, "s1")
        assertEquals(2 to 2, ChecklistCodec.progress(toggled))

        val notifText = ChecklistCodec.formatForNotification(toggled)
        assertTrue(notifText.contains("[x] Pack badge"))
        assertTrue(notifText.contains("[x] Charge tablet"))
    }

    @Test
    fun `room v4 stores starter and custom quick templates alongside entity checklists`() = runTest {
        db.quickTemplateDao().insertAll(DefaultTemplates.starterTemplates())
        val seededCount = db.quickTemplateDao().countTemplates()
        assertEquals(5, seededCount)

        val customTplId = db.quickTemplateDao().insertTemplate(
            QuickTemplateEntity(
                name = "Gym Prep",
                targetKind = TemplateTargetKind.REMINDER,
                defaultTitle = "Evening Workout",
                defaultBody = "Don't skip warm-up",
                stickyColor = StickyColor.SAGE,
                recurrenceType = RecurrenceType.DAILY,
                checklistJson = ChecklistCodec.encode(
                    listOf(ChecklistItem(id = "g1", label = "Fill water bottle"))
                ),
                isBuiltIn = false
            )
        )
        val allTemplates = db.quickTemplateDao().getAllTemplatesOnce()
        assertEquals(6, allTemplates.size)
        assertTrue(allTemplates.any { it.id == customTplId && it.name == "Gym Prep" && !it.isBuiltIn })
    }

    @Test
    fun `json backup v3 preserves checklists and custom templates cleanly`() {
        val checklistJson = ChecklistCodec.encode(
            listOf(
                ChecklistItem(id = "a1", label = "Step one", isDone = true),
                ChecklistItem(id = "a2", label = "Step two", isDone = false)
            )
        )
        val reminder = ReminderEntity(
            title = "Checklist Alarm",
            noteText = "Follow steps",
            triggerAtMillis = 1_800_000_000_000L,
            checklistJson = checklistJson
        )
        val task = TaskEntity(
            title = "Checklist Task",
            description = "Verify items",
            triggerAtMillis = 1_800_000_500_000L,
            checklistJson = checklistJson
        )
        val note = StickyNoteEntity(
            title = "Checklist Note",
            content = "Desk groceries",
            checklistJson = checklistJson
        )
        val builtIn = DefaultTemplates.starterTemplates().first()
        val customTpl = QuickTemplateEntity(
            name = "Client Call",
            targetKind = TemplateTargetKind.TASK,
            defaultTitle = "Prep Client Deck",
            defaultBody = "Review quarterly figures",
            stickyColor = StickyColor.BLUSH,
            checklistJson = checklistJson,
            isBuiltIn = false
        )

        val json = BackupJsonManager.serializeToJson(
            reminders = listOf(reminder),
            stickyNotes = listOf(note),
            preferences = AppPreferences(),
            tasks = listOf(task),
            templates = listOf(builtIn, customTpl)
        )
        val parsed = BackupJsonManager.parseBackupJson(json).getOrThrow()

        assertEquals(2, parsed.reminders.first().checklistItems().size)
        assertEquals(1 to 2, parsed.reminders.first().checklistProgress())
        assertEquals(2, parsed.tasks.first().task.checklistItems().size)
        assertEquals(2, parsed.stickyNotes.first().checklistItems().size)
        assertEquals(1, parsed.templates.size)
        assertEquals("Client Call", parsed.templates.first().name)
        assertEquals(TemplateTargetKind.TASK, parsed.templates.first().targetKind)
        assertEquals(2, parsed.templates.first().checklistItems().size)
    }

    @Test
    fun `motivation provider has 100 plus categorized quotes and filters cleanly`() {
        assertTrue(MotivationProvider.quotes.size >= 100)
        assertEquals(112, MotivationProvider.countForCategory(QuoteCategory.ALL))
        assertEquals(28, MotivationProvider.countForCategory(QuoteCategory.MOTIVATION))
        assertEquals(28, MotivationProvider.countForCategory(QuoteCategory.LIFE))
        assertEquals(28, MotivationProvider.countForCategory(QuoteCategory.CALM))
        assertEquals(28, MotivationProvider.countForCategory(QuoteCategory.FOCUS))

        val calmPick = MotivationProvider.randomQuoteEntry(QuoteCategory.CALM)
        assertEquals(QuoteCategory.CALM, calmPick.category)
        val nextCalm = MotivationProvider.randomQuoteEntry(QuoteCategory.CALM, exclude = calmPick.text)
        assertEquals(QuoteCategory.CALM, nextCalm.category)
        assertTrue(nextCalm.text != calmPick.text)
    }

    @Test
    fun `quote scheduler handles custom 10 minute interval and daytime random slots`() {
        val morningNow = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 10, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val intervalPrefs = AppPreferences(
            quoteNotificationsEnabled = true,
            quoteScheduleMode = QuoteScheduleMode.CUSTOM_INTERVAL,
            quoteIntervalMinutes = 10
        )
        val nextInterval = QuoteNotificationScheduler.computeNextQuoteTriggerMillis(
            prefs = intervalPrefs,
            nowMillis = morningNow
        )
        assertEquals(morningNow + TimeUnit.MINUTES.toMillis(10), nextInterval)

        val randomPrefs = AppPreferences(
            quoteNotificationsEnabled = true,
            quoteScheduleMode = QuoteScheduleMode.RANDOM_PER_DAY,
            quoteRandomPerDay = 5
        )
        val nextRandomMorning = QuoteNotificationScheduler.computeNextQuoteTriggerMillis(
            prefs = randomPrefs,
            nowMillis = morningNow
        )
        assertTrue(nextRandomMorning > morningNow + 59_000L)

        val lateNightNow = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 10, 22, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val nextRandomTomorrow = QuoteNotificationScheduler.computeNextQuoteTriggerMillis(
            prefs = randomPrefs,
            nowMillis = lateNightNow
        )
        val calTomorrow = Calendar.getInstance().apply { timeInMillis = nextRandomTomorrow }
        assertEquals(11, calTomorrow.get(Calendar.DAY_OF_MONTH))
        assertTrue(calTomorrow.get(Calendar.HOUR_OF_DAY) >= 8)
    }

    @Test
    fun `json backup round-trip preserves quote notification settings and animated gif webp attachments`() {
        val gifReminder = ReminderEntity(
            title = "Stretch Break",
            triggerAtMillis = 1_800_000_000_000L,
            imageUri = "file:///media/stretch.gif",
            imageDisplayName = "stretch.gif"
        )
        val webpTask = TaskEntity(
            title = "UI Polish Check",
            triggerAtMillis = 1_800_000_600_000L,
            imageUri = "file:///media/loading.webp",
            imageDisplayName = "loading.webp"
        )
        val prefs = AppPreferences(
            quoteNotificationsEnabled = true,
            quoteScheduleMode = QuoteScheduleMode.CUSTOM_INTERVAL,
            quoteRandomPerDay = 5,
            quoteIntervalMinutes = 10,
            quoteCategory = QuoteCategory.FOCUS
        )

        val json = BackupJsonManager.serializeToJson(
            reminders = listOf(gifReminder),
            stickyNotes = emptyList(),
            preferences = prefs,
            tasks = listOf(webpTask)
        )
        val parsed = BackupJsonManager.parseBackupJson(json).getOrThrow()

        assertEquals(true, parsed.quoteNotificationsEnabled)
        assertEquals(QuoteScheduleMode.CUSTOM_INTERVAL, parsed.quoteScheduleMode)
        assertEquals(5, parsed.quoteRandomPerDay)
        assertEquals(10, parsed.quoteIntervalMinutes)
        assertEquals(QuoteCategory.FOCUS, parsed.quoteCategory)
        assertEquals("file:///media/stretch.gif", parsed.reminders.first().imageUri)
        assertEquals("file:///media/loading.webp", parsed.tasks.first().task.imageUri)
    }

    @Test
    fun `overlay panel variations include media editorial and 5 small compact overlays`() = runTest {
        assertTrue(OverlayPanelStyle.entries.contains(OverlayPanelStyle.SPLIT_MEDIA))
        assertTrue(OverlayPanelStyle.entries.contains(OverlayPanelStyle.FULL_MEDIA_GRADIENT))
        assertTrue(OverlayPanelStyle.entries.contains(OverlayPanelStyle.PLAIN_MEDIA))
        assertTrue(OverlayPanelStyle.entries.contains(OverlayPanelStyle.MEDIA_EDITORIAL))

        val smallStyles = OverlayPanelStyle.entries.filter { it.isSmallVariant }
        assertEquals(5, smallStyles.size)
        assertTrue(smallStyles.containsAll(
            listOf(
                OverlayPanelStyle.DYNAMIC_PILL,
                OverlayPanelStyle.FROSTED_MINI,
                OverlayPanelStyle.TAPE_NOTE,
                OverlayPanelStyle.MEDIA_STRIP,
                OverlayPanelStyle.POCKET_TILE
            )
        ))

        val mediaNote = StickyNoteEntity(
            title = "Workout Chill Mix",
            content = "Warm up 10 mins before sprint",
            color = StickyColor.SKY,
            isPinnedOverlay = true,
            imageUri = "file:///media/sprint.gif",
            imageDisplayName = "sprint.gif",
            musicUri = "file:///media/track.mp3",
            musicDisplayName = "track.mp3"
        )
        assertTrue(mediaNote.hasAnyMedia())
        val savedId = db.stickyNoteDao().insertNote(mediaNote)
        val loaded = db.stickyNoteDao().getNoteById(savedId)
        assertEquals("file:///media/sprint.gif", loaded?.imageUri)
        assertEquals("file:///media/track.mp3", loaded?.musicUri)

        val prefs = AppPreferences(
            overlayPanelStyle = OverlayPanelStyle.MEDIA_EDITORIAL,
            overlayNotificationEnabled = false,
            overlayPanelScale = 1.65f,
            overlayMediaHeightScale = 1.90f,
            overlayTextScale = 1.20f,
            alarmActivePanelStyle = OverlayPanelStyle.PLAIN_MEDIA,
            alarmActivePanelScale = 1.85f,
            alarmActiveMediaHeightScale = 2.15f,
            alarmActiveTextScale = 1.15f,
            alarmActiveCornerRadius = 28,
            alarmActiveOpacity = 0.85f,
            alarmActiveShowQuote = false
        )
        val json = BackupJsonManager.serializeToJson(
            reminders = emptyList(),
            stickyNotes = listOf(mediaNote),
            preferences = prefs
        )
        val parsed = BackupJsonManager.parseBackupJson(json).getOrThrow()
        assertEquals(OverlayPanelStyle.MEDIA_EDITORIAL, parsed.overlayPanelStyle)
        assertEquals(false, parsed.overlayNotificationEnabled)
        assertNotNull(parsed.overlayPanelScale)
        assertNotNull(parsed.overlayMediaHeightScale)
        assertNotNull(parsed.overlayTextScale)
        assertEquals(1.65f, parsed.overlayPanelScale!!, 0.001f)
        assertEquals(1.90f, parsed.overlayMediaHeightScale!!, 0.001f)
        assertEquals(1.20f, parsed.overlayTextScale!!, 0.001f)
        assertEquals(OverlayPanelStyle.PLAIN_MEDIA, parsed.alarmActivePanelStyle)
        assertNotNull(parsed.alarmActivePanelScale)
        assertNotNull(parsed.alarmActiveMediaHeightScale)
        assertNotNull(parsed.alarmActiveTextScale)
        assertEquals(1.85f, parsed.alarmActivePanelScale!!, 0.001f)
        assertEquals(2.15f, parsed.alarmActiveMediaHeightScale!!, 0.001f)
        assertEquals(1.15f, parsed.alarmActiveTextScale!!, 0.001f)
        assertEquals(28, parsed.alarmActiveCornerRadius)
        assertNotNull(parsed.alarmActiveOpacity)
        assertEquals(0.85f, parsed.alarmActiveOpacity!!, 0.001f)
        assertEquals(false, parsed.alarmActiveShowQuote)
        assertEquals("file:///media/sprint.gif", parsed.stickyNotes.first().imageUri)
        assertEquals("file:///media/track.mp3", parsed.stickyNotes.first().musicUri)
    }
}
