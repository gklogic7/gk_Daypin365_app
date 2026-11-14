package com.example.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.AppPreferences
import com.example.data.local.OverlayAnimationStyle
import com.example.data.local.OverlayDisplayMode
import com.example.data.local.OverlayPanelStyle
import com.example.data.local.QuoteScheduleMode
import com.example.data.model.ChecklistCodec
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
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ImportedTaskWithProofs(
    val task: TaskEntity,
    val proofs: List<TaskProofEntity>
)

data class ImportedBackupPayload(
    val reminders: List<ReminderEntity>,
    val stickyNotes: List<StickyNoteEntity>,
    val tasks: List<ImportedTaskWithProofs> = emptyList(),
    val templates: List<QuickTemplateEntity> = emptyList(),
    val snoozeMinutes: Int?,
    val vibrationEnabled: Boolean?,
    val fadeInAudio: Boolean?,
    val themeMode: ThemeMode?,
    val overlayDisplayMode: OverlayDisplayMode? = null,
    val overlayPanelStyle: OverlayPanelStyle? = null,
    val overlayAnimationStyle: OverlayAnimationStyle? = null,
    val overlayNotificationEnabled: Boolean? = null,
    val overlayPanelScale: Float? = null,
    val overlayMediaHeightScale: Float? = null,
    val overlayTextScale: Float? = null,
    val alarmActivePanelStyle: OverlayPanelStyle? = null,
    val alarmActivePanelScale: Float? = null,
    val alarmActiveMediaHeightScale: Float? = null,
    val alarmActiveTextScale: Float? = null,
    val alarmActiveCornerRadius: Int? = null,
    val alarmActiveOpacity: Float? = null,
    val alarmActiveShowQuote: Boolean? = null,
    val deskLaunchMode: com.example.data.local.DeskLaunchMode? = null,
    val deskKeepScreenOn: Boolean? = null,
    val deskDimMode: Boolean? = null,
    val quoteNotificationsEnabled: Boolean? = null,
    val quoteScheduleMode: QuoteScheduleMode? = null,
    val quoteRandomPerDay: Int? = null,
    val quoteIntervalMinutes: Int? = null,
    val quoteCategory: QuoteCategory? = null
)

object BackupJsonManager {
    private const val SCHEMA_VERSION = 3

    fun suggestedFilename(): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return "daypin365-backup-$stamp.json"
    }

    fun serializeToJson(
        reminders: List<ReminderEntity>,
        stickyNotes: List<StickyNoteEntity>,
        preferences: AppPreferences,
        tasks: List<TaskEntity> = emptyList(),
        taskProofs: List<TaskProofEntity> = emptyList(),
        templates: List<QuickTemplateEntity> = emptyList()
    ): String {
        val proofsByTask = taskProofs.groupBy { it.taskId }

        val root = JSONObject().apply {
            put("app", "DayPin 365")
            put("schemaVersion", SCHEMA_VERSION)
            put("exportedAtMillis", System.currentTimeMillis())

            put("preferences", JSONObject().apply {
                put("defaultSnoozeMinutes", preferences.defaultSnoozeMinutes)
                put("vibrationEnabled", preferences.vibrationEnabled)
                put("fadeInAudio", preferences.fadeInAudio)
                put("themeMode", preferences.themeMode.name)
                put("overlayDisplayMode", preferences.overlayDisplayMode.name)
                put("overlayPanelStyle", preferences.overlayPanelStyle.name)
                put("overlayAnimationStyle", preferences.overlayAnimationStyle.name)
                put("overlayNotificationEnabled", preferences.overlayNotificationEnabled)
                put("overlayPanelScale", preferences.overlayPanelScale.toDouble())
                put("overlayMediaHeightScale", preferences.overlayMediaHeightScale.toDouble())
                put("overlayTextScale", preferences.overlayTextScale.toDouble())
                put("alarmActivePanelStyle", preferences.alarmActivePanelStyle.name)
                put("alarmActivePanelScale", preferences.alarmActivePanelScale.toDouble())
                put("alarmActiveMediaHeightScale", preferences.alarmActiveMediaHeightScale.toDouble())
                put("alarmActiveTextScale", preferences.alarmActiveTextScale.toDouble())
                put("alarmActiveCornerRadius", preferences.alarmActiveCornerRadius)
                put("alarmActiveOpacity", preferences.alarmActiveOpacity.toDouble())
                put("alarmActiveShowQuote", preferences.alarmActiveShowQuote)
                put("deskLaunchMode", preferences.deskLaunchMode.name)
                put("deskKeepScreenOn", preferences.deskKeepScreenOn)
                put("deskDimMode", preferences.deskDimMode)
                put("quoteNotificationsEnabled", preferences.quoteNotificationsEnabled)
                put("quoteScheduleMode", preferences.quoteScheduleMode.name)
                put("quoteRandomPerDay", preferences.quoteRandomPerDay)
                put("quoteIntervalMinutes", preferences.quoteIntervalMinutes)
                put("quoteCategory", preferences.quoteCategory.name)
            })

            val remindersArray = JSONArray()
            reminders.forEach { r ->
                remindersArray.put(JSONObject().apply {
                    put("title", r.title)
                    put("noteText", r.noteText)
                    put("triggerAtMillis", r.triggerAtMillis)
                    put("recurrenceType", r.recurrenceType.name)
                    put("customIntervalDays", r.customIntervalDays)
                    put("useCustomInterval", r.useCustomInterval)
                    put("customIntervalUnit", r.customIntervalUnit.name)
                    put("customIntervalValue", r.customIntervalValue)
                    put("isEnabled", r.isEnabled)
                    put("mediaType", r.mediaType.name)
                    put("musicUri", r.effectiveMusicUri ?: JSONObject.NULL)
                    put("musicDisplayName", r.effectiveMusicName ?: JSONObject.NULL)
                    put("imageUri", r.effectiveImageUri ?: JSONObject.NULL)
                    put("imageDisplayName", r.effectiveImageName ?: JSONObject.NULL)
                    put("videoUri", r.effectiveVideoUri ?: JSONObject.NULL)
                    put("videoDisplayName", r.effectiveVideoName ?: JSONObject.NULL)
                    put("stickyColor", r.stickyColor.name)
                    put("showStickyOnAlarm", r.showStickyOnAlarm)
                    put("createdAtMillis", r.createdAtMillis)
                    put("checklistJson", r.checklistJson)
                })
            }
            put("reminders", remindersArray)

            val notesArray = JSONArray()
            stickyNotes.forEach { n ->
                notesArray.put(JSONObject().apply {
                    put("title", n.title)
                    put("content", n.content)
                    put("color", n.color.name)
                    put("isPinnedOverlay", n.isPinnedOverlay)
                    put("linkedDayStartMillis", n.linkedDayStartMillis ?: JSONObject.NULL)
                    put("updatedAtMillis", n.updatedAtMillis)
                    put("checklistJson", n.checklistJson)
                    put("musicUri", n.musicUri ?: JSONObject.NULL)
                    put("musicDisplayName", n.musicDisplayName ?: JSONObject.NULL)
                    put("imageUri", n.imageUri ?: JSONObject.NULL)
                    put("imageDisplayName", n.imageDisplayName ?: JSONObject.NULL)
                    put("videoUri", n.videoUri ?: JSONObject.NULL)
                    put("videoDisplayName", n.videoDisplayName ?: JSONObject.NULL)
                })
            }
            put("stickyNotes", notesArray)

            val tasksArray = JSONArray()
            tasks.forEach { t ->
                val taskProofArray = JSONArray()
                proofsByTask[t.id].orEmpty().forEach { p ->
                    taskProofArray.put(JSONObject().apply {
                        put("statusAtProof", p.statusAtProof.name)
                        put("imageUri", p.imageUri)
                        put("imageDisplayName", p.imageDisplayName)
                        put("note", p.note)
                        put("capturedAtMillis", p.capturedAtMillis)
                    })
                }
                tasksArray.put(JSONObject().apply {
                    put("title", t.title)
                    put("description", t.description)
                    put("triggerAtMillis", t.triggerAtMillis)
                    put("recurrenceType", t.recurrenceType.name)
                    put("useCustomInterval", t.useCustomInterval)
                    put("customIntervalUnit", t.customIntervalUnit.name)
                    put("customIntervalValue", t.customIntervalValue)
                    put("isEnabled", t.isEnabled)
                    put("status", t.status.name)
                    put("musicUri", t.musicUri ?: JSONObject.NULL)
                    put("musicDisplayName", t.musicDisplayName ?: JSONObject.NULL)
                    put("imageUri", t.imageUri ?: JSONObject.NULL)
                    put("imageDisplayName", t.imageDisplayName ?: JSONObject.NULL)
                    put("videoUri", t.videoUri ?: JSONObject.NULL)
                    put("videoDisplayName", t.videoDisplayName ?: JSONObject.NULL)
                    put("stickyColor", t.stickyColor.name)
                    put("createdAtMillis", t.createdAtMillis)
                    put("updatedAtMillis", t.updatedAtMillis)
                    put("checklistJson", t.checklistJson)
                    put("proofs", taskProofArray)
                })
            }
            put("tasks", tasksArray)

            val templatesArray = JSONArray()
            templates.filter { !it.isBuiltIn }.forEach { tpl ->
                templatesArray.put(JSONObject().apply {
                    put("name", tpl.name)
                    put("targetKind", tpl.targetKind.name)
                    put("defaultTitle", tpl.defaultTitle)
                    put("defaultBody", tpl.defaultBody)
                    put("stickyColor", tpl.stickyColor.name)
                    put("recurrenceType", tpl.recurrenceType.name)
                    put("useCustomInterval", tpl.useCustomInterval)
                    put("customIntervalUnit", tpl.customIntervalUnit.name)
                    put("customIntervalValue", tpl.customIntervalValue)
                    put("checklistJson", tpl.checklistJson)
                    put("createdAtMillis", tpl.createdAtMillis)
                })
            }
            put("templates", templatesArray)
        }
        return root.toString(2)
    }

    fun parseBackupJson(jsonText: String): Result<ImportedBackupPayload> = runCatching {
        val root = JSONObject(jsonText)
        val remindersArr = root.optJSONArray("reminders") ?: JSONArray()
        val notesArr = root.optJSONArray("stickyNotes") ?: JSONArray()
        val tasksArr = root.optJSONArray("tasks") ?: JSONArray()
        val templatesArr = root.optJSONArray("templates") ?: JSONArray()

        val reminders = buildList {
            for (i in 0 until remindersArr.length()) {
                val obj = remindersArr.optJSONObject(i) ?: continue
                val title = obj.optString("title", "").trim()
                if (title.isBlank()) continue

                val triggerAt = obj.optLong("triggerAtMillis", System.currentTimeMillis() + 3600_000L)
                val recurrence = runCatching {
                    RecurrenceType.valueOf(obj.optString("recurrenceType", RecurrenceType.ONCE.name))
                }.getOrDefault(RecurrenceType.ONCE)
                val intervalUnit = runCatching {
                    IntervalUnit.valueOf(obj.optString("customIntervalUnit", IntervalUnit.DAYS.name))
                }.getOrDefault(IntervalUnit.DAYS)
                val mediaType = runCatching {
                    MediaType.valueOf(obj.optString("mediaType", MediaType.NONE.name))
                }.getOrDefault(MediaType.NONE)
                val stickyColor = runCatching {
                    StickyColor.valueOf(obj.optString("stickyColor", StickyColor.BUTTERCUP.name))
                }.getOrDefault(StickyColor.BUTTERCUP)
                val checklistJson = normalizeChecklistJson(obj.optNullableString("checklistJson"))

                add(
                    ReminderEntity(
                        title = title,
                        noteText = obj.optString("noteText", ""),
                        triggerAtMillis = triggerAt,
                        recurrenceType = recurrence,
                        customIntervalDays = obj.optInt("customIntervalDays", 1).coerceAtLeast(1),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        mediaType = mediaType,
                        musicUri = obj.optNullableString("musicUri"),
                        musicDisplayName = obj.optNullableString("musicDisplayName"),
                        imageUri = obj.optNullableString("imageUri"),
                        imageDisplayName = obj.optNullableString("imageDisplayName"),
                        videoUri = obj.optNullableString("videoUri"),
                        videoDisplayName = obj.optNullableString("videoDisplayName"),
                        useCustomInterval = obj.optBoolean("useCustomInterval", false),
                        customIntervalUnit = intervalUnit,
                        customIntervalValue = obj.optInt("customIntervalValue", 1).coerceAtLeast(1),
                        stickyColor = stickyColor,
                        showStickyOnAlarm = obj.optBoolean("showStickyOnAlarm", true),
                        createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
                        checklistJson = checklistJson
                    )
                )
            }
        }

        val notes = buildList {
            for (i in 0 until notesArr.length()) {
                val obj = notesArr.optJSONObject(i) ?: continue
                val title = obj.optString("title", "").trim()
                val content = obj.optString("content", "").trim()
                val checklistJson = normalizeChecklistJson(obj.optNullableString("checklistJson"))
                if (title.isBlank() && content.isBlank() && ChecklistCodec.decode(checklistJson).isEmpty()) continue
                val color = runCatching {
                    StickyColor.valueOf(obj.optString("color", StickyColor.BUTTERCUP.name))
                }.getOrDefault(StickyColor.BUTTERCUP)
                val linkedDay = if (obj.isNull("linkedDayStartMillis")) null else obj.optLong("linkedDayStartMillis")
                add(
                    StickyNoteEntity(
                        title = title.ifBlank { "Imported Note" },
                        content = content,
                        color = color,
                        isPinnedOverlay = false,
                        linkedDayStartMillis = linkedDay,
                        updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis()),
                        checklistJson = checklistJson,
                        musicUri = obj.optNullableString("musicUri"),
                        musicDisplayName = obj.optNullableString("musicDisplayName"),
                        imageUri = obj.optNullableString("imageUri"),
                        imageDisplayName = obj.optNullableString("imageDisplayName"),
                        videoUri = obj.optNullableString("videoUri"),
                        videoDisplayName = obj.optNullableString("videoDisplayName")
                    )
                )
            }
        }

        val importedTasks = buildList {
            for (i in 0 until tasksArr.length()) {
                val obj = tasksArr.optJSONObject(i) ?: continue
                val title = obj.optString("title", "").trim()
                if (title.isBlank()) continue

                val recurrence = runCatching {
                    RecurrenceType.valueOf(obj.optString("recurrenceType", RecurrenceType.ONCE.name))
                }.getOrDefault(RecurrenceType.ONCE)
                val intervalUnit = runCatching {
                    IntervalUnit.valueOf(obj.optString("customIntervalUnit", IntervalUnit.DAYS.name))
                }.getOrDefault(IntervalUnit.DAYS)
                val status = runCatching {
                    TaskStatus.valueOf(obj.optString("status", TaskStatus.PENDING.name))
                }.getOrDefault(TaskStatus.PENDING)
                val stickyColor = runCatching {
                    StickyColor.valueOf(obj.optString("stickyColor", StickyColor.SKY.name))
                }.getOrDefault(StickyColor.SKY)
                val checklistJson = normalizeChecklistJson(obj.optNullableString("checklistJson"))

                val task = TaskEntity(
                    title = title,
                    description = obj.optString("description", ""),
                    triggerAtMillis = obj.optLong("triggerAtMillis", System.currentTimeMillis() + 3600_000L),
                    recurrenceType = recurrence,
                    useCustomInterval = obj.optBoolean("useCustomInterval", false),
                    customIntervalUnit = intervalUnit,
                    customIntervalValue = obj.optInt("customIntervalValue", 1).coerceAtLeast(1),
                    isEnabled = obj.optBoolean("isEnabled", true),
                    status = status,
                    musicUri = obj.optNullableString("musicUri"),
                    musicDisplayName = obj.optNullableString("musicDisplayName"),
                    imageUri = obj.optNullableString("imageUri"),
                    imageDisplayName = obj.optNullableString("imageDisplayName"),
                    videoUri = obj.optNullableString("videoUri"),
                    videoDisplayName = obj.optNullableString("videoDisplayName"),
                    stickyColor = stickyColor,
                    createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
                    updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis()),
                    checklistJson = checklistJson
                )

                val proofsArr = obj.optJSONArray("proofs") ?: JSONArray()
                val proofs = buildList {
                    for (j in 0 until proofsArr.length()) {
                        val pObj = proofsArr.optJSONObject(j) ?: continue
                        val img = pObj.optNullableString("imageUri") ?: continue
                        val proofStatus = runCatching {
                            TaskStatus.valueOf(pObj.optString("statusAtProof", TaskStatus.DONE.name))
                        }.getOrDefault(TaskStatus.DONE)
                        add(
                            TaskProofEntity(
                                taskId = 0L,
                                statusAtProof = proofStatus,
                                imageUri = img,
                                imageDisplayName = pObj.optString("imageDisplayName", "proof.jpg"),
                                note = pObj.optString("note", ""),
                                capturedAtMillis = pObj.optLong("capturedAtMillis", System.currentTimeMillis())
                            )
                        )
                    }
                }
                add(ImportedTaskWithProofs(task = task, proofs = proofs))
            }
        }

        val importedTemplates = buildList {
            for (i in 0 until templatesArr.length()) {
                val obj = templatesArr.optJSONObject(i) ?: continue
                val name = obj.optString("name", "").trim()
                val defTitle = obj.optString("defaultTitle", "").trim()
                if (name.isBlank() || defTitle.isBlank()) continue
                val targetKind = runCatching {
                    TemplateTargetKind.valueOf(obj.optString("targetKind", TemplateTargetKind.REMINDER.name))
                }.getOrDefault(TemplateTargetKind.REMINDER)
                val stickyColor = runCatching {
                    StickyColor.valueOf(obj.optString("stickyColor", StickyColor.BUTTERCUP.name))
                }.getOrDefault(StickyColor.BUTTERCUP)
                val recurrence = runCatching {
                    RecurrenceType.valueOf(obj.optString("recurrenceType", RecurrenceType.ONCE.name))
                }.getOrDefault(RecurrenceType.ONCE)
                val intervalUnit = runCatching {
                    IntervalUnit.valueOf(obj.optString("customIntervalUnit", IntervalUnit.DAYS.name))
                }.getOrDefault(IntervalUnit.DAYS)
                add(
                    QuickTemplateEntity(
                        name = name,
                        targetKind = targetKind,
                        defaultTitle = defTitle,
                        defaultBody = obj.optString("defaultBody", ""),
                        stickyColor = stickyColor,
                        recurrenceType = recurrence,
                        useCustomInterval = obj.optBoolean("useCustomInterval", false),
                        customIntervalUnit = intervalUnit,
                        customIntervalValue = obj.optInt("customIntervalValue", 1).coerceAtLeast(1),
                        checklistJson = normalizeChecklistJson(obj.optNullableString("checklistJson")),
                        isBuiltIn = false,
                        createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }
        }

        val prefsObj = root.optJSONObject("preferences")
        ImportedBackupPayload(
            reminders = reminders,
            stickyNotes = notes,
            tasks = importedTasks,
            templates = importedTemplates,
            snoozeMinutes = prefsObj?.takeIf { it.has("defaultSnoozeMinutes") }?.optInt("defaultSnoozeMinutes"),
            vibrationEnabled = prefsObj?.takeIf { it.has("vibrationEnabled") }?.optBoolean("vibrationEnabled"),
            fadeInAudio = prefsObj?.takeIf { it.has("fadeInAudio") }?.optBoolean("fadeInAudio"),
            themeMode = prefsObj?.optNullableString("themeMode")?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrNull()
            },
            overlayDisplayMode = prefsObj?.optNullableString("overlayDisplayMode")?.let {
                runCatching { OverlayDisplayMode.valueOf(it) }.getOrNull()
            },
            overlayPanelStyle = prefsObj?.optNullableString("overlayPanelStyle")?.let {
                runCatching { OverlayPanelStyle.valueOf(it) }.getOrNull()
            },
            overlayAnimationStyle = prefsObj?.optNullableString("overlayAnimationStyle")?.let {
                runCatching { OverlayAnimationStyle.valueOf(it) }.getOrNull()
            },
            overlayNotificationEnabled = prefsObj?.takeIf { it.has("overlayNotificationEnabled") }?.optBoolean("overlayNotificationEnabled"),
            overlayPanelScale = prefsObj?.takeIf { it.has("overlayPanelScale") }?.optDouble("overlayPanelScale")?.toFloat()?.coerceIn(0.50f, 2.20f),
            overlayMediaHeightScale = prefsObj?.takeIf { it.has("overlayMediaHeightScale") }?.optDouble("overlayMediaHeightScale")?.toFloat()?.coerceIn(0.50f, 2.40f),
            overlayTextScale = prefsObj?.takeIf { it.has("overlayTextScale") }?.optDouble("overlayTextScale")?.toFloat()?.coerceIn(0.80f, 1.45f),
            alarmActivePanelStyle = prefsObj?.optNullableString("alarmActivePanelStyle")?.let {
                runCatching { OverlayPanelStyle.valueOf(it) }.getOrNull()
            },
            alarmActivePanelScale = prefsObj?.takeIf { it.has("alarmActivePanelScale") }?.optDouble("alarmActivePanelScale")?.toFloat()?.coerceIn(0.50f, 2.20f),
            alarmActiveMediaHeightScale = prefsObj?.takeIf { it.has("alarmActiveMediaHeightScale") }?.optDouble("alarmActiveMediaHeightScale")?.toFloat()?.coerceIn(0.50f, 2.40f),
            alarmActiveTextScale = prefsObj?.takeIf { it.has("alarmActiveTextScale") }?.optDouble("alarmActiveTextScale")?.toFloat()?.coerceIn(0.80f, 1.45f),
            alarmActiveCornerRadius = prefsObj?.takeIf { it.has("alarmActiveCornerRadius") }?.optInt("alarmActiveCornerRadius")?.coerceIn(12, 32),
            alarmActiveOpacity = prefsObj?.takeIf { it.has("alarmActiveOpacity") }?.optDouble("alarmActiveOpacity")?.toFloat()?.coerceIn(0.70f, 1.00f),
            alarmActiveShowQuote = prefsObj?.takeIf { it.has("alarmActiveShowQuote") }?.optBoolean("alarmActiveShowQuote"),
            deskLaunchMode = prefsObj?.optNullableString("deskLaunchMode")?.let {
                runCatching { com.example.data.local.DeskLaunchMode.valueOf(it) }.getOrNull()
            },
            deskKeepScreenOn = prefsObj?.takeIf { it.has("deskKeepScreenOn") }?.optBoolean("deskKeepScreenOn"),
            deskDimMode = prefsObj?.takeIf { it.has("deskDimMode") }?.optBoolean("deskDimMode"),
            quoteNotificationsEnabled = prefsObj?.takeIf { it.has("quoteNotificationsEnabled") }?.optBoolean("quoteNotificationsEnabled"),
            quoteScheduleMode = prefsObj?.optNullableString("quoteScheduleMode")?.let {
                runCatching { QuoteScheduleMode.valueOf(it) }.getOrNull()
            },
            quoteRandomPerDay = prefsObj?.takeIf { it.has("quoteRandomPerDay") }?.optInt("quoteRandomPerDay"),
            quoteIntervalMinutes = prefsObj?.takeIf { it.has("quoteIntervalMinutes") }?.optInt("quoteIntervalMinutes"),
            quoteCategory = prefsObj?.optNullableString("quoteCategory")?.let {
                runCatching { QuoteCategory.valueOf(it) }.getOrNull()
            }
        )
    }

    private fun normalizeChecklistJson(raw: String?): String {
        if (raw.isNullOrBlank()) return "[]"
        return ChecklistCodec.encode(ChecklistCodec.decode(raw))
    }

    suspend fun writeJsonToUri(context: Context, uri: Uri, jsonText: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(jsonText.toByteArray(Charsets.UTF_8))
                    stream.flush()
                } ?: error("Could not open destination file")
            }
        }

    suspend fun readJsonFromUri(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                } ?: error("Could not open selected JSON file")
            }
        }

    suspend fun createTempShareUri(context: Context, jsonText: String): Result<Uri> =
        withContext(Dispatchers.IO) {
            runCatching {
                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val file = File(exportDir, suggestedFilename())
                file.writeText(jsonText, Charsets.UTF_8)
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }
        }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key, "").takeIf { it.isNotBlank() }
    }
}
