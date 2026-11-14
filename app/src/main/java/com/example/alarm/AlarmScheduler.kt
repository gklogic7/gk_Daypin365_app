package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.data.model.MediaType
import com.example.data.model.ReminderEntity
import com.example.data.model.TaskEntity
import com.example.permissions.PermissionHelper

object AlarmScheduler {
    const val KIND_REMINDER = "REMINDER"
    const val KIND_TASK = "TASK"

    const val EXTRA_ITEM_KIND = "extra_item_kind"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_NOTE_TEXT = "extra_note_text"
    const val EXTRA_MEDIA_TYPE = "extra_media_type"
    const val EXTRA_MEDIA_URI = "extra_media_uri"
    const val EXTRA_MEDIA_NAME = "extra_media_name"
    const val EXTRA_MUSIC_URI = "extra_music_uri"
    const val EXTRA_MUSIC_NAME = "extra_music_name"
    const val EXTRA_IMAGE_URI = "extra_image_uri"
    const val EXTRA_VIDEO_URI = "extra_video_uri"
    const val EXTRA_STICKY_COLOR = "extra_sticky_color"
    const val EXTRA_SHOW_STICKY = "extra_show_sticky"
    const val EXTRA_TASK_STATUS = "extra_task_status"
    const val EXTRA_MOTIVATION_LINE = "extra_motivation_line"
    const val EXTRA_CHECKLIST_JSON = "extra_checklist_json"
    const val EXTRA_IS_TEST = "extra_is_test"

    fun scheduleReminder(context: Context, reminder: ReminderEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAt = reminder.triggerAtMillis
        if (triggerAt <= System.currentTimeMillis()) return

        val pendingIntent = buildAlarmPendingIntent(
            context = context,
            requestCode = reminderRequestCode(reminder.id),
            reminder = reminder
        )
        scheduleExactOrWindow(context, alarmManager, triggerAt, reminderRequestCode(reminder.id), pendingIntent)
    }

    fun scheduleTask(context: Context, task: TaskEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAt = task.triggerAtMillis
        if (triggerAt <= System.currentTimeMillis()) return

        val pendingIntent = buildTaskPendingIntent(
            context = context,
            requestCode = taskRequestCode(task.id),
            task = task
        )
        scheduleExactOrWindow(context, alarmManager, triggerAt, taskRequestCode(task.id), pendingIntent)
    }

    private fun scheduleExactOrWindow(
        context: Context,
        alarmManager: AlarmManager,
        triggerAt: Long,
        showRequestCode: Int,
        pendingIntent: PendingIntent
    ) {
        if (PermissionHelper.hasExactAlarmPermission(context)) {
            val showIntent = PendingIntent.getActivity(
                context,
                showRequestCode,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAt, showIntent),
                pendingIntent
            )
        } else {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                60_000L,
                pendingIntent
            )
        }
    }

    fun scheduleSnooze(
        context: Context,
        itemKind: String = KIND_REMINDER,
        reminderId: Long,
        snoozeMinutes: Int,
        title: String,
        noteText: String,
        mediaType: String,
        mediaUri: String?,
        mediaName: String?,
        musicUri: String?,
        musicName: String?,
        imageUri: String?,
        videoUri: String?,
        stickyColor: String,
        showSticky: Boolean,
        taskStatus: String? = null,
        checklistJson: String = "[]"
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAt = System.currentTimeMillis() + snoozeMinutes * 60_000L
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ITEM_KIND, itemKind)
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TITLE, if (itemKind == KIND_TASK) title else "$title (Snoozed)")
            putExtra(EXTRA_NOTE_TEXT, noteText)
            putExtra(EXTRA_MEDIA_TYPE, mediaType)
            putExtra(EXTRA_MEDIA_URI, mediaUri)
            putExtra(EXTRA_MEDIA_NAME, mediaName)
            putExtra(EXTRA_MUSIC_URI, musicUri)
            putExtra(EXTRA_MUSIC_NAME, musicName)
            putExtra(EXTRA_IMAGE_URI, imageUri)
            putExtra(EXTRA_VIDEO_URI, videoUri)
            putExtra(EXTRA_STICKY_COLOR, stickyColor)
            putExtra(EXTRA_SHOW_STICKY, showSticky)
            putExtra(EXTRA_CHECKLIST_JSON, checklistJson)
            if (taskStatus != null) putExtra(EXTRA_TASK_STATUS, taskStatus)
        }
        val reqCode = if (itemKind == KIND_TASK) taskSnoozeRequestCode(reminderId) else snoozeRequestCode(reminderId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (PermissionHelper.hasExactAlarmPermission(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } else {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 60_000L, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        cancelRequestCodes(context, reminderRequestCode(reminderId), snoozeRequestCode(reminderId))
    }

    fun cancelTask(context: Context, taskId: Long) {
        cancelRequestCodes(context, taskRequestCode(taskId), taskSnoozeRequestCode(taskId))
    }

    private fun cancelRequestCodes(context: Context, mainCode: Int, snoozeCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val mainIntent = PendingIntent.getBroadcast(
            context,
            mainCode,
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (mainIntent != null) {
            alarmManager.cancel(mainIntent)
            mainIntent.cancel()
        }
        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            snoozeCode,
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (snoozeIntent != null) {
            alarmManager.cancel(snoozeIntent)
            snoozeIntent.cancel()
        }
    }

    fun triggerImmediatePreview(context: Context, reminder: ReminderEntity) {
        val serviceIntent = Intent(context, AlarmRingService::class.java).apply {
            action = AlarmRingService.ACTION_START_ALARM
            putExtra(EXTRA_ITEM_KIND, KIND_REMINDER)
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_TITLE, reminder.title)
            putExtra(EXTRA_NOTE_TEXT, reminder.noteText)
            putExtra(EXTRA_MEDIA_TYPE, reminder.mediaType.name)
            putExtra(EXTRA_MEDIA_URI, reminder.mediaUri)
            putExtra(EXTRA_MEDIA_NAME, reminder.mediaDisplayName)
            putExtra(EXTRA_MUSIC_URI, reminder.effectiveMusicUri)
            putExtra(EXTRA_MUSIC_NAME, reminder.effectiveMusicName)
            putExtra(EXTRA_IMAGE_URI, reminder.effectiveImageUri)
            putExtra(EXTRA_VIDEO_URI, reminder.effectiveVideoUri)
            putExtra(EXTRA_STICKY_COLOR, reminder.stickyColor.name)
            putExtra(EXTRA_SHOW_STICKY, reminder.showStickyOnAlarm)
            putExtra(EXTRA_CHECKLIST_JSON, reminder.checklistJson)
            putExtra(EXTRA_IS_TEST, true)
        }
        startForegroundServiceCompat(context, serviceIntent)
    }

    fun triggerImmediateTaskPreview(context: Context, task: TaskEntity) {
        val serviceIntent = Intent(context, AlarmRingService::class.java).apply {
            action = AlarmRingService.ACTION_START_ALARM
            putExtra(EXTRA_ITEM_KIND, KIND_TASK)
            putExtra(EXTRA_REMINDER_ID, task.id)
            putExtra(EXTRA_TITLE, task.title)
            putExtra(EXTRA_NOTE_TEXT, task.description)
            putExtra(EXTRA_MEDIA_TYPE, MediaType.NONE.name)
            putExtra(EXTRA_MUSIC_URI, task.musicUri)
            putExtra(EXTRA_MUSIC_NAME, task.musicDisplayName)
            putExtra(EXTRA_IMAGE_URI, task.imageUri)
            putExtra(EXTRA_VIDEO_URI, task.videoUri)
            putExtra(EXTRA_STICKY_COLOR, task.stickyColor.name)
            putExtra(EXTRA_SHOW_STICKY, task.description.isNotBlank())
            putExtra(EXTRA_TASK_STATUS, task.status.name)
            putExtra(EXTRA_CHECKLIST_JSON, task.checklistJson)
            putExtra(EXTRA_IS_TEST, true)
        }
        startForegroundServiceCompat(context, serviceIntent)
    }

    private fun startForegroundServiceCompat(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun buildAlarmPendingIntent(
        context: Context,
        requestCode: Int,
        reminder: ReminderEntity
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ITEM_KIND, KIND_REMINDER)
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_TITLE, reminder.title)
            putExtra(EXTRA_NOTE_TEXT, reminder.noteText)
            putExtra(EXTRA_MEDIA_TYPE, reminder.mediaType.name)
            putExtra(EXTRA_MEDIA_URI, reminder.mediaUri)
            putExtra(EXTRA_MEDIA_NAME, reminder.mediaDisplayName)
            putExtra(EXTRA_MUSIC_URI, reminder.effectiveMusicUri)
            putExtra(EXTRA_MUSIC_NAME, reminder.effectiveMusicName)
            putExtra(EXTRA_IMAGE_URI, reminder.effectiveImageUri)
            putExtra(EXTRA_VIDEO_URI, reminder.effectiveVideoUri)
            putExtra(EXTRA_STICKY_COLOR, reminder.stickyColor.name)
            putExtra(EXTRA_SHOW_STICKY, reminder.showStickyOnAlarm)
            putExtra(EXTRA_CHECKLIST_JSON, reminder.checklistJson)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildTaskPendingIntent(
        context: Context,
        requestCode: Int,
        task: TaskEntity
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ITEM_KIND, KIND_TASK)
            putExtra(EXTRA_REMINDER_ID, task.id)
            putExtra(EXTRA_TITLE, task.title)
            putExtra(EXTRA_NOTE_TEXT, task.description)
            putExtra(EXTRA_MEDIA_TYPE, MediaType.NONE.name)
            putExtra(EXTRA_MUSIC_URI, task.musicUri)
            putExtra(EXTRA_MUSIC_NAME, task.musicDisplayName)
            putExtra(EXTRA_IMAGE_URI, task.imageUri)
            putExtra(EXTRA_VIDEO_URI, task.videoUri)
            putExtra(EXTRA_STICKY_COLOR, task.stickyColor.name)
            putExtra(EXTRA_SHOW_STICKY, task.description.isNotBlank())
            putExtra(EXTRA_TASK_STATUS, task.status.name)
            putExtra(EXTRA_CHECKLIST_JSON, task.checklistJson)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun reminderRequestCode(id: Long): Int = (id % 1_000_000L).toInt() + 1000
    private fun snoozeRequestCode(id: Long): Int = (id % 1_000_000L).toInt() + 2_000_000
    private fun taskRequestCode(id: Long): Int = (id % 1_000_000L).toInt() + 3_000_000
    private fun taskSnoozeRequestCode(id: Long): Int = (id % 1_000_000L).toInt() + 4_000_000
}
