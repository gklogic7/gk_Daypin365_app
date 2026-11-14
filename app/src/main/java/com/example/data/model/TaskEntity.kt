package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TaskStatus(val label: String) {
    PENDING("Scheduled"),
    STARTED("Started"),
    WORKING("Working"),
    LATER("Later"),
    DONE("Done")
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String = "",
    val triggerAtMillis: Long,
    val recurrenceType: RecurrenceType = RecurrenceType.ONCE,
    val useCustomInterval: Boolean = false,
    val customIntervalUnit: IntervalUnit = IntervalUnit.DAYS,
    val customIntervalValue: Int = 1,
    val isEnabled: Boolean = true,
    val status: TaskStatus = TaskStatus.PENDING,
    val musicUri: String? = null,
    val musicDisplayName: String? = null,
    val imageUri: String? = null,
    val imageDisplayName: String? = null,
    val videoUri: String? = null,
    val videoDisplayName: String? = null,
    val stickyColor: StickyColor = StickyColor.SKY,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val lastTriggeredMillis: Long? = null,
    val checklistJson: String = "[]"
) {
    fun checklistItems(): List<ChecklistItem> = ChecklistCodec.decode(checklistJson)

    fun checklistProgress(): Pair<Int, Int> = ChecklistCodec.progress(checklistJson)

    val hasAnyMedia: Boolean
        get() = !musicUri.isNullOrBlank() || !imageUri.isNullOrBlank() || !videoUri.isNullOrBlank()

    fun mediaSummaryLabel(): String {
        val parts = buildList {
            if (!musicUri.isNullOrBlank()) add("Music")
            if (!imageUri.isNullOrBlank()) add("Image")
            if (!videoUri.isNullOrBlank()) add("Video")
        }
        return if (parts.isEmpty()) "Tone Only" else parts.joinToString(" + ")
    }

    fun recurrenceSummaryLabel(): String {
        return if (useCustomInterval) {
            "Every $customIntervalValue ${customIntervalUnit.label}"
        } else {
            recurrenceType.label
        }
    }
}

@Entity(
    tableName = "task_proofs",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class TaskProofEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val taskId: Long,
    val statusAtProof: TaskStatus,
    val imageUri: String,
    val imageDisplayName: String = "proof_photo.jpg",
    val note: String = "",
    val capturedAtMillis: Long = System.currentTimeMillis()
)
