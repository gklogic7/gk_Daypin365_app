package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RecurrenceType(val label: String) {
    ONCE("Once"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    CUSTOM_DAYS("Custom"),
    YEARLY("Yearly")
}

enum class IntervalUnit(val label: String, val shortLabel: String) {
    MINUTES("Minutes", "m"),
    HOURS("Hours", "h"),
    DAYS("Days", "d")
}

enum class MediaType(val label: String) {
    NONE("Tone Only"),
    MUSIC("Play Music"),
    IMAGE("Show Image"),
    VIDEO("Play Video")
}

enum class StickyColor(val label: String) {
    BUTTERCUP("Buttercup"),
    SAGE("Sage"),
    SKY("Sky"),
    BLUSH("Blush")
}

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val noteText: String = "",
    val triggerAtMillis: Long,
    val recurrenceType: RecurrenceType = RecurrenceType.ONCE,
    val customIntervalDays: Int = 1,
    val isEnabled: Boolean = true,
    val mediaType: MediaType = MediaType.NONE,
    val mediaUri: String? = null,
    val mediaDisplayName: String? = null,
    val stickyColor: StickyColor = StickyColor.BUTTERCUP,
    val showStickyOnAlarm: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastTriggeredMillis: Long? = null,
    // v2 multi-media & flexible interval fields
    val musicUri: String? = null,
    val musicDisplayName: String? = null,
    val imageUri: String? = null,
    val imageDisplayName: String? = null,
    val videoUri: String? = null,
    val videoDisplayName: String? = null,
    val useCustomInterval: Boolean = false,
    val customIntervalUnit: IntervalUnit = IntervalUnit.DAYS,
    val customIntervalValue: Int = 1,
    val checklistJson: String = "[]"
) {
    fun checklistItems(): List<ChecklistItem> = ChecklistCodec.decode(checklistJson)

    fun checklistProgress(): Pair<Int, Int> = ChecklistCodec.progress(checklistJson)

    val effectiveMusicUri: String?
        get() = musicUri ?: if (mediaType == MediaType.MUSIC) mediaUri else null

    val effectiveMusicName: String?
        get() = musicDisplayName ?: if (mediaType == MediaType.MUSIC) mediaDisplayName else null

    val effectiveImageUri: String?
        get() = imageUri ?: if (mediaType == MediaType.IMAGE) mediaUri else null

    val effectiveImageName: String?
        get() = imageDisplayName ?: if (mediaType == MediaType.IMAGE) mediaDisplayName else null

    val effectiveVideoUri: String?
        get() = videoUri ?: if (mediaType == MediaType.VIDEO) mediaUri else null

    val effectiveVideoName: String?
        get() = videoDisplayName ?: if (mediaType == MediaType.VIDEO) mediaDisplayName else null

    val hasAnyMedia: Boolean
        get() = !effectiveMusicUri.isNullOrBlank() ||
            !effectiveImageUri.isNullOrBlank() ||
            !effectiveVideoUri.isNullOrBlank()

    fun mediaSummaryLabel(): String {
        val parts = buildList {
            if (!effectiveMusicUri.isNullOrBlank()) add("Music")
            if (!effectiveImageUri.isNullOrBlank()) add("Image")
            if (!effectiveVideoUri.isNullOrBlank()) add("Video")
        }
        return if (parts.isEmpty()) "Tone Only" else parts.joinToString(" + ")
    }

    fun recurrenceSummaryLabel(): String {
        return if (useCustomInterval) {
            "Every $customIntervalValue ${customIntervalUnit.label}"
        } else if (recurrenceType == RecurrenceType.CUSTOM_DAYS) {
            "Every $customIntervalDays Days"
        } else {
            recurrenceType.label
        }
    }
}
