package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.IntervalUnit
import com.example.data.model.MediaType
import com.example.data.model.RecurrenceType
import com.example.data.model.StickyColor
import com.example.data.model.TaskStatus
import com.example.data.model.TemplateTargetKind

class Converters {
    @TypeConverter
    fun fromRecurrenceType(value: RecurrenceType): String = value.name

    @TypeConverter
    fun toRecurrenceType(value: String): RecurrenceType =
        runCatching { RecurrenceType.valueOf(value) }.getOrDefault(RecurrenceType.ONCE)

    @TypeConverter
    fun fromIntervalUnit(value: IntervalUnit): String = value.name

    @TypeConverter
    fun toIntervalUnit(value: String): IntervalUnit =
        runCatching { IntervalUnit.valueOf(value) }.getOrDefault(IntervalUnit.DAYS)

    @TypeConverter
    fun fromMediaType(value: MediaType): String = value.name

    @TypeConverter
    fun toMediaType(value: String): MediaType =
        runCatching { MediaType.valueOf(value) }.getOrDefault(MediaType.NONE)

    @TypeConverter
    fun fromStickyColor(value: StickyColor): String = value.name

    @TypeConverter
    fun toStickyColor(value: String): StickyColor =
        runCatching { StickyColor.valueOf(value) }.getOrDefault(StickyColor.BUTTERCUP)

    @TypeConverter
    fun fromTaskStatus(value: TaskStatus): String = value.name

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus =
        runCatching { TaskStatus.valueOf(value) }.getOrDefault(TaskStatus.PENDING)

    @TypeConverter
    fun fromTemplateTargetKind(value: TemplateTargetKind): String = value.name

    @TypeConverter
    fun toTemplateTargetKind(value: String): TemplateTargetKind =
        runCatching { TemplateTargetKind.valueOf(value) }.getOrDefault(TemplateTargetKind.REMINDER)
}
