package com.example.data.repository

import android.content.Context
import com.example.alarm.AlarmScheduler
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferences
import com.example.data.model.ChecklistCodec
import com.example.data.model.DefaultTemplates
import com.example.data.model.IntervalUnit
import com.example.data.model.QuickTemplateEntity
import com.example.data.model.RecurrenceType
import com.example.data.model.ReminderEntity
import com.example.data.model.StickyNoteEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskProofEntity
import com.example.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

data class ImportSummary(
    val remindersCount: Int,
    val notesCount: Int,
    val tasksCount: Int,
    val templatesCount: Int = 0
)

class ReminderRepository(
    private val context: Context,
    database: AppDatabase = AppDatabase.getInstance(context)
) {
    private val reminderDao = database.reminderDao()
    private val stickyNoteDao = database.stickyNoteDao()
    private val taskDao = database.taskDao()
    private val quickTemplateDao = database.quickTemplateDao()

    val remindersFlow: Flow<List<ReminderEntity>> = reminderDao.observeAllReminders()
    val stickyNotesFlow: Flow<List<StickyNoteEntity>> = stickyNoteDao.observeAllNotes()
    val pinnedNotesFlow: Flow<List<StickyNoteEntity>> = stickyNoteDao.observePinnedNotes()
    val tasksFlow: Flow<List<TaskEntity>> = taskDao.observeAllTasks()
    val taskProofsFlow: Flow<List<TaskProofEntity>> = taskDao.observeAllProofs()
    val quickTemplatesFlow: Flow<List<QuickTemplateEntity>> = quickTemplateDao.observeAllTemplates()

    suspend fun ensureStarterTemplatesSeeded() {
        if (quickTemplateDao.countTemplates() == 0) {
            quickTemplateDao.insertAll(DefaultTemplates.starterTemplates())
        }
    }

    suspend fun saveQuickTemplate(template: QuickTemplateEntity): Long {
        return quickTemplateDao.insertTemplate(
            template.copy(
                name = template.name.trim(),
                defaultTitle = template.defaultTitle.trim(),
                defaultBody = template.defaultBody.trim(),
                isBuiltIn = false,
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteQuickTemplate(templateId: Long) {
        quickTemplateDao.deleteTemplateById(templateId)
    }

    suspend fun toggleReminderChecklistItem(reminderId: Long, itemId: String) {
        val existing = reminderDao.getReminderById(reminderId) ?: return
        val nextJson = ChecklistCodec.toggle(existing.checklistJson, itemId)
        reminderDao.updateReminder(existing.copy(checklistJson = nextJson))
    }

    suspend fun toggleTaskChecklistItem(taskId: Long, itemId: String) {
        val existing = taskDao.getTaskById(taskId) ?: return
        val nextJson = ChecklistCodec.toggle(existing.checklistJson, itemId)
        taskDao.updateTask(
            existing.copy(
                checklistJson = nextJson,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleStickyNoteChecklistItem(noteId: Long, itemId: String) {
        val existing = stickyNoteDao.getNoteById(noteId) ?: return
        val nextJson = ChecklistCodec.toggle(existing.checklistJson, itemId)
        stickyNoteDao.updateNote(
            existing.copy(
                checklistJson = nextJson,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun exportBackupJson(preferences: AppPreferences): String {
        val allReminders = reminderDao.getAllRemindersOnce()
        val allNotes = stickyNoteDao.getAllNotesOnce()
        val allTasks = taskDao.getAllTasksOnce()
        val allProofs = taskDao.getAllProofsOnce()
        val allTemplates = quickTemplateDao.getAllTemplatesOnce()
        return BackupJsonManager.serializeToJson(
            reminders = allReminders,
            stickyNotes = allNotes,
            preferences = preferences,
            tasks = allTasks,
            taskProofs = allProofs,
            templates = allTemplates
        )
    }

    suspend fun importBackup(payload: ImportedBackupPayload): ImportSummary {
        var importedReminders = 0
        var importedNotes = 0
        var importedTasks = 0
        var importedTemplates = 0
        val now = System.currentTimeMillis()
        for (reminder in payload.reminders) {
            val savedId = reminderDao.insertReminder(reminder.copy(id = 0L))
            val saved = reminder.copy(id = savedId)
            if (saved.isEnabled && saved.triggerAtMillis > now) {
                AlarmScheduler.scheduleReminder(context, saved)
            }
            importedReminders++
        }
        for (note in payload.stickyNotes) {
            stickyNoteDao.insertNote(note.copy(id = 0L))
            importedNotes++
        }
        for (item in payload.tasks) {
            val savedTaskId = taskDao.insertTask(item.task.copy(id = 0L))
            val savedTask = item.task.copy(id = savedTaskId)
            for (proof in item.proofs) {
                taskDao.insertProof(proof.copy(id = 0L, taskId = savedTaskId))
            }
            if (savedTask.isEnabled && savedTask.status != TaskStatus.DONE && savedTask.triggerAtMillis > now) {
                AlarmScheduler.scheduleTask(context, savedTask)
            }
            importedTasks++
        }
        val existingTemplateKeys = quickTemplateDao.getAllTemplatesOnce()
            .map { "${it.targetKind.name}:${it.name.lowercase()}" }
            .toHashSet()
        for (tpl in payload.templates) {
            val key = "${tpl.targetKind.name}:${tpl.name.lowercase()}"
            if (key !in existingTemplateKeys) {
                quickTemplateDao.insertTemplate(tpl.copy(id = 0L, isBuiltIn = false))
                existingTemplateKeys.add(key)
                importedTemplates++
            }
        }
        return ImportSummary(importedReminders, importedNotes, importedTasks, importedTemplates)
    }

    suspend fun saveReminder(reminder: ReminderEntity): Long {
        val normalized = if (reminder.id == 0L) {
            reminder.copy(createdAtMillis = System.currentTimeMillis())
        } else {
            val existing = reminderDao.getReminderById(reminder.id)
            if (existing != null) {
                if (existing.effectiveMusicUri != null && existing.effectiveMusicUri != reminder.effectiveMusicUri) {
                    MediaStorageHelper.deleteLocalMediaIfManaged(context, existing.effectiveMusicUri)
                }
                if (existing.effectiveImageUri != null && existing.effectiveImageUri != reminder.effectiveImageUri) {
                    MediaStorageHelper.deleteLocalMediaIfManaged(context, existing.effectiveImageUri)
                }
                if (existing.effectiveVideoUri != null && existing.effectiveVideoUri != reminder.effectiveVideoUri) {
                    MediaStorageHelper.deleteLocalMediaIfManaged(context, existing.effectiveVideoUri)
                }
            }
            reminder
        }

        val id = if (normalized.id == 0L) {
            reminderDao.insertReminder(normalized)
        } else {
            reminderDao.updateReminder(normalized)
            normalized.id
        }

        val saved = normalized.copy(id = id)
        if (saved.isEnabled && saved.triggerAtMillis > System.currentTimeMillis()) {
            AlarmScheduler.scheduleReminder(context, saved)
        } else {
            AlarmScheduler.cancelReminder(context, saved.id)
        }
        return id
    }

    suspend fun toggleReminderEnabled(reminder: ReminderEntity, enabled: Boolean) {
        val now = System.currentTimeMillis()
        var nextTrigger = reminder.triggerAtMillis
        if (enabled && nextTrigger <= now) {
            nextTrigger = computeNextFutureTrigger(reminder, now)
        }
        val updated = reminder.copy(isEnabled = enabled, triggerAtMillis = nextTrigger)
        reminderDao.updateReminder(updated)
        if (enabled) {
            AlarmScheduler.scheduleReminder(context, updated)
        } else {
            AlarmScheduler.cancelReminder(context, updated.id)
        }
    }

    suspend fun deleteReminder(reminder: ReminderEntity) {
        AlarmScheduler.cancelReminder(context, reminder.id)
        MediaStorageHelper.deleteLocalMediaIfManaged(context, reminder.mediaUri)
        MediaStorageHelper.deleteLocalMediaIfManaged(context, reminder.musicUri)
        MediaStorageHelper.deleteLocalMediaIfManaged(context, reminder.imageUri)
        MediaStorageHelper.deleteLocalMediaIfManaged(context, reminder.videoUri)
        reminderDao.deleteReminderById(reminder.id)
    }

    suspend fun getReminderById(id: Long): ReminderEntity? = reminderDao.getReminderById(id)

    suspend fun handleAlarmFiredAndRescheduleIfRecurring(reminderId: Long) {
        val reminder = reminderDao.getReminderById(reminderId) ?: return
        val now = System.currentTimeMillis()
        if (!reminder.useCustomInterval && reminder.recurrenceType == RecurrenceType.ONCE) {
            reminderDao.updateReminder(
                reminder.copy(isEnabled = false, lastTriggeredMillis = now)
            )
        } else {
            val nextTrigger = computeNextFutureTrigger(reminder, now)
            val updated = reminder.copy(
                triggerAtMillis = nextTrigger,
                isEnabled = true,
                lastTriggeredMillis = now
            )
            reminderDao.updateReminder(updated)
            AlarmScheduler.scheduleReminder(context, updated)
        }
    }

    // --- Task Feature ---

    suspend fun saveTask(task: TaskEntity): Long {
        val now = System.currentTimeMillis()
        val normalized = if (task.id == 0L) {
            task.copy(createdAtMillis = now, updatedAtMillis = now)
        } else {
            val existing = taskDao.getTaskById(task.id)
            if (existing != null) {
                if (existing.musicUri != null && existing.musicUri != task.musicUri) {
                    MediaStorageHelper.deleteLocalMediaIfManaged(context, existing.musicUri)
                }
                if (existing.imageUri != null && existing.imageUri != task.imageUri) {
                    MediaStorageHelper.deleteLocalMediaIfManaged(context, existing.imageUri)
                }
                if (existing.videoUri != null && existing.videoUri != task.videoUri) {
                    MediaStorageHelper.deleteLocalMediaIfManaged(context, existing.videoUri)
                }
            }
            task.copy(updatedAtMillis = now)
        }

        val id = if (normalized.id == 0L) {
            taskDao.insertTask(normalized)
        } else {
            taskDao.updateTask(normalized)
            normalized.id
        }

        val saved = normalized.copy(id = id)
        if (saved.isEnabled && saved.status != TaskStatus.DONE && saved.triggerAtMillis > now) {
            AlarmScheduler.scheduleTask(context, saved)
        } else {
            AlarmScheduler.cancelTask(context, saved.id)
        }
        return id
    }

    suspend fun toggleTaskEnabled(task: TaskEntity, enabled: Boolean) {
        val now = System.currentTimeMillis()
        var nextTrigger = task.triggerAtMillis
        if (enabled && nextTrigger <= now) {
            nextTrigger = computeNextFutureTaskTrigger(task, now)
        }
        val nextStatus = if (enabled && task.status == TaskStatus.DONE) TaskStatus.PENDING else task.status
        val updated = task.copy(
            isEnabled = enabled,
            triggerAtMillis = nextTrigger,
            status = nextStatus,
            updatedAtMillis = now
        )
        taskDao.updateTask(updated)
        if (enabled) {
            AlarmScheduler.scheduleTask(context, updated)
        } else {
            AlarmScheduler.cancelTask(context, updated.id)
        }
    }

    suspend fun deleteTask(task: TaskEntity) {
        AlarmScheduler.cancelTask(context, task.id)
        MediaStorageHelper.deleteLocalMediaIfManaged(context, task.musicUri)
        MediaStorageHelper.deleteLocalMediaIfManaged(context, task.imageUri)
        MediaStorageHelper.deleteLocalMediaIfManaged(context, task.videoUri)
        taskDao.getProofsForTaskOnce(task.id).forEach { proof ->
            MediaStorageHelper.deleteLocalMediaIfManaged(context, proof.imageUri)
        }
        taskDao.deleteTaskById(task.id)
    }

    suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getTaskById(id)

    suspend fun submitTaskProof(
        taskId: Long,
        targetStatus: TaskStatus,
        imageUri: String,
        imageDisplayName: String,
        note: String = ""
    ) {
        val task = taskDao.getTaskById(taskId) ?: return
        val now = System.currentTimeMillis()
        taskDao.insertProof(
            TaskProofEntity(
                taskId = taskId,
                statusAtProof = targetStatus,
                imageUri = imageUri,
                imageDisplayName = imageDisplayName,
                note = note.trim(),
                capturedAtMillis = now
            )
        )
        val isRecurring = task.useCustomInterval || task.recurrenceType != RecurrenceType.ONCE
        val shouldStayScheduled = targetStatus != TaskStatus.DONE || isRecurring
        val nextTrigger = if (targetStatus == TaskStatus.DONE && isRecurring) {
            computeNextFutureTaskTrigger(task, now)
        } else {
            task.triggerAtMillis
        }
        val updatedTask = task.copy(
            status = targetStatus,
            isEnabled = if (targetStatus == TaskStatus.DONE && !isRecurring) false else task.isEnabled,
            triggerAtMillis = nextTrigger,
            updatedAtMillis = now
        )
        taskDao.updateTask(updatedTask)
        if (!updatedTask.isEnabled || (targetStatus == TaskStatus.DONE && !isRecurring)) {
            AlarmScheduler.cancelTask(context, taskId)
        } else if (shouldStayScheduled && updatedTask.triggerAtMillis > now) {
            AlarmScheduler.scheduleTask(context, updatedTask)
        }
    }

    suspend fun deferTaskLater(taskId: Long, deferMinutes: Int) {
        val task = taskDao.getTaskById(taskId) ?: return
        val now = System.currentTimeMillis()
        val deferredTrigger = now + deferMinutes.coerceIn(1, 24 * 60) * 60_000L
        val updated = task.copy(
            status = TaskStatus.LATER,
            isEnabled = true,
            triggerAtMillis = deferredTrigger,
            updatedAtMillis = now
        )
        taskDao.updateTask(updated)
        AlarmScheduler.scheduleTask(context, updated)
    }

    suspend fun handleTaskAlarmFiredAndRescheduleIfRecurring(taskId: Long) {
        val task = taskDao.getTaskById(taskId) ?: return
        val now = System.currentTimeMillis()
        if (!task.useCustomInterval && task.recurrenceType == RecurrenceType.ONCE) {
            taskDao.updateTask(
                task.copy(isEnabled = false, lastTriggeredMillis = now, updatedAtMillis = now)
            )
        } else {
            val nextTrigger = computeNextFutureTaskTrigger(task, now)
            val updated = task.copy(
                triggerAtMillis = nextTrigger,
                isEnabled = true,
                lastTriggeredMillis = now,
                updatedAtMillis = now
            )
            taskDao.updateTask(updated)
            AlarmScheduler.scheduleTask(context, updated)
        }
    }

    suspend fun rescheduleAllEnabledAlarms() {
        val now = System.currentTimeMillis()
        val enabled = reminderDao.getAllEnabledReminders()
        for (reminder in enabled) {
            val isRecurring = reminder.useCustomInterval || reminder.recurrenceType != RecurrenceType.ONCE
            val triggerAt = if (reminder.triggerAtMillis <= now && isRecurring) {
                val next = computeNextFutureTrigger(reminder, now)
                reminderDao.updateReminder(reminder.copy(triggerAtMillis = next))
                next
            } else {
                reminder.triggerAtMillis
            }
            if (triggerAt > now) {
                AlarmScheduler.scheduleReminder(context, reminder.copy(triggerAtMillis = triggerAt))
            }
        }

        val enabledTasks = taskDao.getAllEnabledTasks()
        for (task in enabledTasks) {
            if (task.status == TaskStatus.DONE && !task.useCustomInterval && task.recurrenceType == RecurrenceType.ONCE) {
                continue
            }
            val isRecurring = task.useCustomInterval || task.recurrenceType != RecurrenceType.ONCE
            val triggerAt = if (task.triggerAtMillis <= now && isRecurring) {
                val next = computeNextFutureTaskTrigger(task, now)
                taskDao.updateTask(task.copy(triggerAtMillis = next))
                next
            } else {
                task.triggerAtMillis
            }
            if (triggerAt > now) {
                AlarmScheduler.scheduleTask(context, task.copy(triggerAtMillis = triggerAt))
            }
        }
    }

    suspend fun saveStickyNote(note: StickyNoteEntity): Long {
        val updated = note.copy(updatedAtMillis = System.currentTimeMillis())
        return if (updated.id == 0L) {
            stickyNoteDao.insertNote(updated)
        } else {
            stickyNoteDao.updateNote(updated)
            updated.id
        }
    }

    suspend fun setStickyNotePinned(noteId: Long, pinned: Boolean) {
        stickyNoteDao.setNotePinned(noteId, pinned)
    }

    suspend fun unpinAllStickyNotes() {
        stickyNoteDao.unpinAllNotes()
    }

    suspend fun getPinnedStickyNotesOnce(): List<StickyNoteEntity> =
        stickyNoteDao.getPinnedNotesOnce()

    suspend fun deleteStickyNote(noteId: Long) {
        stickyNoteDao.deleteNoteById(noteId)
    }

    companion object {
        fun computeNextFutureTrigger(reminder: ReminderEntity, afterMillis: Long): Long {
            return computeNextTriggerMillis(
                triggerAtMillis = reminder.triggerAtMillis,
                recurrenceType = reminder.recurrenceType,
                useCustomInterval = reminder.useCustomInterval,
                customIntervalUnit = reminder.customIntervalUnit,
                customIntervalValue = reminder.customIntervalValue,
                customIntervalDays = reminder.customIntervalDays,
                afterMillis = afterMillis
            )
        }

        fun computeNextFutureTaskTrigger(task: TaskEntity, afterMillis: Long): Long {
            return computeNextTriggerMillis(
                triggerAtMillis = task.triggerAtMillis,
                recurrenceType = task.recurrenceType,
                useCustomInterval = task.useCustomInterval,
                customIntervalUnit = task.customIntervalUnit,
                customIntervalValue = task.customIntervalValue,
                customIntervalDays = task.customIntervalValue,
                afterMillis = afterMillis
            )
        }

        private fun computeNextTriggerMillis(
            triggerAtMillis: Long,
            recurrenceType: RecurrenceType,
            useCustomInterval: Boolean,
            customIntervalUnit: IntervalUnit,
            customIntervalValue: Int,
            customIntervalDays: Int,
            afterMillis: Long
        ): Long {
            val cal = Calendar.getInstance().apply { timeInMillis = triggerAtMillis }

            if (useCustomInterval) {
                val step = customIntervalValue.coerceIn(1, 999)
                var guard = 0
                while (cal.timeInMillis <= afterMillis && guard < 10_000) {
                    when (customIntervalUnit) {
                        IntervalUnit.MINUTES -> cal.add(Calendar.MINUTE, step)
                        IntervalUnit.HOURS -> cal.add(Calendar.HOUR_OF_DAY, step)
                        IntervalUnit.DAYS -> cal.add(Calendar.DAY_OF_YEAR, step)
                    }
                    guard++
                }
                if (cal.timeInMillis <= afterMillis) {
                    cal.timeInMillis = afterMillis
                    when (customIntervalUnit) {
                        IntervalUnit.MINUTES -> cal.add(Calendar.MINUTE, step)
                        IntervalUnit.HOURS -> cal.add(Calendar.HOUR_OF_DAY, step)
                        IntervalUnit.DAYS -> cal.add(Calendar.DAY_OF_YEAR, step)
                    }
                }
                return cal.timeInMillis
            }

            if (recurrenceType == RecurrenceType.ONCE) {
                while (cal.timeInMillis <= afterMillis) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                return cal.timeInMillis
            }

            val stepDays = customIntervalDays.coerceIn(1, 365)
            var guard = 0
            while (cal.timeInMillis <= afterMillis && guard < 1500) {
                when (recurrenceType) {
                    RecurrenceType.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                    RecurrenceType.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                    RecurrenceType.CUSTOM_DAYS -> cal.add(Calendar.DAY_OF_YEAR, stepDays)
                    RecurrenceType.YEARLY -> cal.add(Calendar.YEAR, 1)
                    RecurrenceType.ONCE -> cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                guard++
            }
            return cal.timeInMillis
        }
    }
}
