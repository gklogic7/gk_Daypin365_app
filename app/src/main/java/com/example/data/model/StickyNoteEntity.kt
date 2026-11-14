package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sticky_notes")
data class StickyNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val content: String,
    val color: StickyColor = StickyColor.BUTTERCUP,
    val isPinnedOverlay: Boolean = false,
    val linkedDayStartMillis: Long? = null,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val checklistJson: String = "[]",
    val musicUri: String? = null,
    val musicDisplayName: String? = null,
    val imageUri: String? = null,
    val imageDisplayName: String? = null,
    val videoUri: String? = null,
    val videoDisplayName: String? = null
) {
    fun checklistItems(): List<ChecklistItem> = ChecklistCodec.decode(checklistJson)

    fun checklistProgress(): Pair<Int, Int> = ChecklistCodec.progress(checklistJson)

    fun hasAnyMedia(): Boolean =
        !imageUri.isNullOrBlank() || !videoUri.isNullOrBlank() || !musicUri.isNullOrBlank()
}
