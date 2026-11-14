package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val isDone: Boolean = false
)

object ChecklistCodec {
    fun encode(items: List<ChecklistItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            if (item.label.isNotBlank()) {
                arr.put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("label", item.label.trim())
                        put("isDone", item.isDone)
                    }
                )
            }
        }
        return arr.toString()
    }

    fun decode(raw: String?): List<ChecklistItem> {
        if (raw.isNullOrBlank() || raw == "[]") return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val label = obj.optString("label", "").trim()
                    if (label.isBlank()) continue
                    val id = obj.optString("id", "").ifBlank { "step_$i" }
                    add(
                        ChecklistItem(
                            id = id,
                            label = label,
                            isDone = obj.optBoolean("isDone", false)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun toggle(raw: String?, itemId: String): String {
        val updated = decode(raw).map { item ->
            if (item.id == itemId) item.copy(isDone = !item.isDone) else item
        }
        return encode(updated)
    }

    fun progress(raw: String?): Pair<Int, Int> {
        val list = decode(raw)
        return list.count { it.isDone } to list.size
    }

    fun formatForNotification(raw: String?): String {
        val items = decode(raw)
        if (items.isEmpty()) return ""
        return items.take(4).joinToString("\n") { step ->
            val mark = if (step.isDone) "[x]" else "[ ]"
            "$mark ${step.label}"
        }.let { body ->
            if (items.size > 4) "$body\n+${items.size - 4} more step(s)" else body
        }
    }
}

enum class TemplateTargetKind(val label: String) {
    REMINDER("Alarm"),
    TASK("Task"),
    NOTE("Note")
}

@Entity(tableName = "quick_templates")
data class QuickTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val targetKind: TemplateTargetKind,
    val defaultTitle: String,
    val defaultBody: String = "",
    val stickyColor: StickyColor = StickyColor.BUTTERCUP,
    val recurrenceType: RecurrenceType = RecurrenceType.ONCE,
    val useCustomInterval: Boolean = false,
    val customIntervalUnit: IntervalUnit = IntervalUnit.DAYS,
    val customIntervalValue: Int = 1,
    val checklistJson: String = "[]",
    val isBuiltIn: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun checklistItems(): List<ChecklistItem> = ChecklistCodec.decode(checklistJson)
}

object DefaultTemplates {
    fun starterTemplates(): List<QuickTemplateEntity> = listOf(
        QuickTemplateEntity(
            name = "Morning Launch",
            targetKind = TemplateTargetKind.REMINDER,
            defaultTitle = "Morning Launch Routine",
            defaultBody = "Hydrate, review top 3 priorities, and check DayPin 365 schedule.",
            stickyColor = StickyColor.BUTTERCUP,
            recurrenceType = RecurrenceType.DAILY,
            checklistJson = ChecklistCodec.encode(
                listOf(
                    ChecklistItem(id = "ml_1", label = "Drink 500ml water"),
                    ChecklistItem(id = "ml_2", label = "Pick today's top 3 goals"),
                    ChecklistItem(id = "ml_3", label = "Check calendar reminders")
                )
            ),
            isBuiltIn = true
        ),
        QuickTemplateEntity(
            name = "Evening Wind-Down",
            targetKind = TemplateTargetKind.REMINDER,
            defaultTitle = "Evening Desk Wind-Down",
            defaultBody = "Wrap up open tabs, plug in tablet charger, and review tomorrow.",
            stickyColor = StickyColor.SAGE,
            recurrenceType = RecurrenceType.DAILY,
            checklistJson = ChecklistCodec.encode(
                listOf(
                    ChecklistItem(id = "ew_1", label = "Clear workspace desk"),
                    ChecklistItem(id = "ew_2", label = "Plug tablet charger for Desk Mode"),
                    ChecklistItem(id = "ew_3", label = "Set tomorrow's first alarm")
                )
            ),
            isBuiltIn = true
        ),
        QuickTemplateEntity(
            name = "Deep Work Sprint",
            targetKind = TemplateTargetKind.TASK,
            defaultTitle = "Deep Work Focus Block",
            defaultBody = "Work uninterrupted and capture a quick progress photo when done.",
            stickyColor = StickyColor.SKY,
            recurrenceType = RecurrenceType.CUSTOM_DAYS,
            useCustomInterval = true,
            customIntervalUnit = IntervalUnit.HOURS,
            customIntervalValue = 2,
            checklistJson = ChecklistCodec.encode(
                listOf(
                    ChecklistItem(id = "dw_1", label = "Silence extra notifications"),
                    ChecklistItem(id = "dw_2", label = "Complete primary deliverable"),
                    ChecklistItem(id = "dw_3", label = "Capture Task Done proof photo")
                )
            ),
            isBuiltIn = true
        ),
        QuickTemplateEntity(
            name = "Weekly Desk Audit",
            targetKind = TemplateTargetKind.TASK,
            defaultTitle = "Weekly Desk & Backup Audit",
            defaultBody = "Organize workspace and export weekly JSON backup.",
            stickyColor = StickyColor.BLUSH,
            recurrenceType = RecurrenceType.WEEKLY,
            checklistJson = ChecklistCodec.encode(
                listOf(
                    ChecklistItem(id = "wa_1", label = "Organize desk & cables"),
                    ChecklistItem(id = "wa_2", label = "Export DayPin 365 JSON backup"),
                    ChecklistItem(id = "wa_3", label = "Snap proof photo of clean desk")
                )
            ),
            isBuiltIn = true
        ),
        QuickTemplateEntity(
            name = "Daily Essentials",
            targetKind = TemplateTargetKind.NOTE,
            defaultTitle = "Daily Essentials Checklist",
            defaultBody = "Quick pinned checklist for today.",
            stickyColor = StickyColor.BUTTERCUP,
            checklistJson = ChecklistCodec.encode(
                listOf(
                    ChecklistItem(id = "de_1", label = "Keys, wallet & charger"),
                    ChecklistItem(id = "de_2", label = "Refill water bottle"),
                    ChecklistItem(id = "de_3", label = "Review pinned tasks")
                )
            ),
            isBuiltIn = true
        )
    )
}
