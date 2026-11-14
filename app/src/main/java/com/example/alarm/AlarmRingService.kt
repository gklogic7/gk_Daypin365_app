package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.R
import com.example.data.local.PreferencesManager
import com.example.data.model.ChecklistCodec
import com.example.data.model.MediaType
import com.example.data.model.StickyColor
import com.example.data.repository.MotivationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmRingService : Service() {

    companion object {
        const val ACTION_START_ALARM = "com.example.action.START_ALARM"
        const val ACTION_DISMISS_ALARM = "com.example.action.DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.action.SNOOZE_ALARM"
        const val ACTION_MUTE_AUDIO = "com.example.action.MUTE_AUDIO"
        const val ACTION_ALARM_STOPPED_BROADCAST = "com.example.broadcast.ALARM_STOPPED"

        private const val CHANNEL_ID = "daypin_alarm_channel"
        private const val NOTIFICATION_ID = 4101
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var exoPlayer: ExoPlayer? = null
    private var activeRingtone: Ringtone? = null
    private var fadeJob: Job? = null
    private var vibrator: Vibrator? = null

    private var currentItemKind: String = AlarmScheduler.KIND_REMINDER
    private var currentReminderId: Long = -1L
    private var currentTitle: String = "Reminder"
    private var currentNoteText: String = ""
    private var currentMediaType: String = MediaType.NONE.name
    private var currentMediaUri: String? = null
    private var currentMediaName: String? = null
    private var currentMusicUri: String? = null
    private var currentMusicName: String? = null
    private var currentImageUri: String? = null
    private var currentVideoUri: String? = null
    private var currentStickyColor: String = StickyColor.BUTTERCUP.name
    private var currentShowSticky: Boolean = true
    private var currentTaskStatus: String? = null
    private var currentMotivationLine: String = ""
    private var currentChecklistJson: String = "[]"
    private var currentSnoozeMinutes: Int = 10

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS_ALARM -> {
                stopAlarmAndFinish()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE_ALARM -> {
                snoozeAndFinish()
                return START_NOT_STICKY
            }
            ACTION_MUTE_AUDIO -> {
                fadeJob?.cancel()
                exoPlayer?.pause()
                runCatching { activeRingtone?.stop() }
                stopVibration()
                return START_NOT_STICKY
            }
            ACTION_START_ALARM, null -> {
                if (intent != null) {
                    currentItemKind = intent.getStringExtra(AlarmScheduler.EXTRA_ITEM_KIND)
                        ?: AlarmScheduler.KIND_REMINDER
                    currentReminderId = intent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, -1L)
                    currentTitle = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "DayPin Reminder"
                    currentNoteText = intent.getStringExtra(AlarmScheduler.EXTRA_NOTE_TEXT) ?: ""
                    currentMediaType = intent.getStringExtra(AlarmScheduler.EXTRA_MEDIA_TYPE) ?: MediaType.NONE.name
                    currentMediaUri = intent.getStringExtra(AlarmScheduler.EXTRA_MEDIA_URI)
                    currentMediaName = intent.getStringExtra(AlarmScheduler.EXTRA_MEDIA_NAME)
                    currentMusicUri = intent.getStringExtra(AlarmScheduler.EXTRA_MUSIC_URI)
                    currentMusicName = intent.getStringExtra(AlarmScheduler.EXTRA_MUSIC_NAME)
                    currentImageUri = intent.getStringExtra(AlarmScheduler.EXTRA_IMAGE_URI)
                    currentVideoUri = intent.getStringExtra(AlarmScheduler.EXTRA_VIDEO_URI)
                    currentStickyColor = intent.getStringExtra(AlarmScheduler.EXTRA_STICKY_COLOR) ?: StickyColor.BUTTERCUP.name
                    currentShowSticky = intent.getBooleanExtra(AlarmScheduler.EXTRA_SHOW_STICKY, true)
                    currentTaskStatus = intent.getStringExtra(AlarmScheduler.EXTRA_TASK_STATUS)
                    currentChecklistJson = intent.getStringExtra(AlarmScheduler.EXTRA_CHECKLIST_JSON) ?: "[]"
                    currentMotivationLine = intent.getStringExtra(AlarmScheduler.EXTRA_MOTIVATION_LINE)
                        ?.takeIf { it.isNotBlank() }
                        ?: MotivationProvider.randomQuote()
                } else if (currentMotivationLine.isBlank()) {
                    currentMotivationLine = MotivationProvider.randomQuote()
                }
                startForegroundAlarm()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundAlarm() {
        createNotificationChannel()
        val fullScreenIntent = buildRingActivityIntent()
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissPendingIntent = PendingIntent.getService(
            this,
            NOTIFICATION_ID + 1,
            Intent(this, AlarmRingService::class.java).apply { action = ACTION_DISMISS_ALARM },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozePendingIntent = PendingIntent.getService(
            this,
            NOTIFICATION_ID + 2,
            Intent(this, AlarmRingService::class.java).apply { action = ACTION_SNOOZE_ALARM },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultContentText = if (currentItemKind == AlarmScheduler.KIND_TASK) {
            "Task check-in: Done, Working, Started, or Later"
        } else {
            "Scheduled DayPin 365 alarm"
        }
        val quoteLine = currentMotivationLine.ifBlank { MotivationProvider.randomQuote() }
        val baseExpanded = MotivationProvider.composeNotificationBody(
            motivationLine = quoteLine,
            noteText = currentNoteText,
            fallbackBody = defaultContentText
        )
        val checklistSummary = ChecklistCodec.formatForNotification(currentChecklistJson)
        val expandedBody = if (checklistSummary.isNotBlank()) {
            "$baseExpanded\nChecklist:\n$checklistSummary"
        } else {
            baseExpanded
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(currentTitle)
            .setContentText("\"$quoteLine\"")
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedBody))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(0, if (currentItemKind == AlarmScheduler.KIND_TASK) "Later" else getString(R.string.action_snooze), snoozePendingIntent)
            .addAction(0, getString(R.string.action_dismiss), dismissPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        runCatching { startActivity(fullScreenIntent) }

        serviceScope.launch {
            val prefs = PreferencesManager(applicationContext).preferencesFlow.first()
            currentSnoozeMinutes = prefs.defaultSnoozeMinutes
            if (prefs.vibrationEnabled) {
                startVibration()
            }
            val parsedLegacyType = runCatching { MediaType.valueOf(currentMediaType) }
                .getOrDefault(MediaType.NONE)
            val resolvedMusic = currentMusicUri
                ?: currentMediaUri.takeIf { parsedLegacyType == MediaType.MUSIC }
            val hasVideoOnly = !currentVideoUri.isNullOrBlank() && resolvedMusic.isNullOrBlank()

            // Play custom music even when Image/Video are also attached; only yield to Video audio if no Music was chosen.
            if (!hasVideoOnly) {
                startAudioPlayback(
                    musicUri = resolvedMusic,
                    fadeIn = prefs.fadeInAudio
                )
            }
        }
    }

    private fun startAudioPlayback(musicUri: String?, fadeIn: Boolean) {
        releasePlayer()
        val parsedCustomUri: Uri? = musicUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
        val targetUri: Uri? = parsedCustomUri ?: fallbackAlarmUri()

        if (targetUri == null) return

        if (parsedCustomUri == null || isSystemSettingsRingtoneUri(targetUri)) {
            playSystemRingtone(targetUri, fadeIn)
            return
        }

        val player = ExoPlayer.Builder(this).build().apply {
            val attrs = AudioAttributes.Builder()
                .setUsage(C.USAGE_ALARM)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
            // ExoPlayer rejects automatic audio focus for USAGE_ALARM; disable auto-focus handling.
            setAudioAttributes(attrs, false)
            repeatMode = Player.REPEAT_MODE_ONE
            volume = if (fadeIn) 0.15f else 1.0f
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    exoPlayer?.release()
                    exoPlayer = null
                    fallbackAlarmUri()?.let { fallback ->
                        playSystemRingtone(fallback, fadeIn)
                    }
                }
            })
            setMediaItem(MediaItem.fromUri(targetUri))
            prepare()
            play()
        }
        exoPlayer = player

        startFadeInJob(fadeIn)
    }

    private fun isSystemSettingsRingtoneUri(uri: Uri): Boolean {
        return uri.scheme.equals("content", ignoreCase = true) &&
            uri.authority.equals("settings", ignoreCase = true)
    }

    private fun playSystemRingtone(targetUri: Uri, fadeIn: Boolean) {
        val ringtone = runCatching { RingtoneManager.getRingtone(applicationContext, targetUri) }.getOrNull()
            ?: fallbackAlarmUri()?.let { runCatching { RingtoneManager.getRingtone(applicationContext, it) }.getOrNull() }
            ?: return

        runCatching {
            ringtone.audioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = true
                ringtone.volume = if (fadeIn) 0.15f else 1.0f
            }
            ringtone.play()
        }
        activeRingtone = ringtone
        startFadeInJob(fadeIn)
    }

    private fun startFadeInJob(fadeIn: Boolean) {
        if (!fadeIn) return
        fadeJob?.cancel()
        fadeJob = serviceScope.launch {
            val steps = 16
            for (i in 1..steps) {
                delay(500L)
                val nextVol = (0.15f + (0.85f * (i.toFloat() / steps))).coerceAtMost(1.0f)
                exoPlayer?.volume = nextVol
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    runCatching { activeRingtone?.volume = nextVol }
                }
            }
        }
    }

    private fun fallbackAlarmUri(): Uri? {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        val pattern = longArrayOf(0L, 600L, 500L, 600L, 1000L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopVibration() {
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    private fun snoozeAndFinish() {
        val targetId = if (currentReminderId > 0L) currentReminderId else System.currentTimeMillis()
        if (currentItemKind == AlarmScheduler.KIND_TASK && currentReminderId > 0L) {
            serviceScope.launch(Dispatchers.IO) {
                com.example.data.repository.ReminderRepository(applicationContext)
                    .deferTaskLater(currentReminderId, currentSnoozeMinutes)
            }
        } else {
            AlarmScheduler.scheduleSnooze(
                context = applicationContext,
                itemKind = currentItemKind,
                reminderId = targetId,
                snoozeMinutes = currentSnoozeMinutes,
                title = currentTitle,
                noteText = currentNoteText,
                mediaType = currentMediaType,
                mediaUri = currentMediaUri,
                mediaName = currentMediaName,
                musicUri = currentMusicUri,
                musicName = currentMusicName,
                imageUri = currentImageUri,
                videoUri = currentVideoUri,
                stickyColor = currentStickyColor,
                showSticky = currentShowSticky,
                taskStatus = currentTaskStatus,
                checklistJson = currentChecklistJson
            )
        }
        stopAlarmAndFinish()
    }

    private fun stopAlarmAndFinish() {
        sendBroadcast(Intent(ACTION_ALARM_STOPPED_BROADCAST).setPackage(packageName))
        releasePlayer()
        stopVibration()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releasePlayer() {
        fadeJob?.cancel()
        fadeJob = null
        exoPlayer?.release()
        exoPlayer = null
        runCatching { activeRingtone?.stop() }
        activeRingtone = null
    }

    private fun buildRingActivityIntent(): Intent {
        return Intent(this, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmScheduler.EXTRA_ITEM_KIND, currentItemKind)
            putExtra(AlarmScheduler.EXTRA_REMINDER_ID, currentReminderId)
            putExtra(AlarmScheduler.EXTRA_TITLE, currentTitle)
            putExtra(AlarmScheduler.EXTRA_NOTE_TEXT, currentNoteText)
            putExtra(AlarmScheduler.EXTRA_MEDIA_TYPE, currentMediaType)
            putExtra(AlarmScheduler.EXTRA_MEDIA_URI, currentMediaUri)
            putExtra(AlarmScheduler.EXTRA_MEDIA_NAME, currentMediaName)
            putExtra(AlarmScheduler.EXTRA_MUSIC_URI, currentMusicUri)
            putExtra(AlarmScheduler.EXTRA_MUSIC_NAME, currentMusicName)
            putExtra(AlarmScheduler.EXTRA_IMAGE_URI, currentImageUri)
            putExtra(AlarmScheduler.EXTRA_VIDEO_URI, currentVideoUri)
            putExtra(AlarmScheduler.EXTRA_STICKY_COLOR, currentStickyColor)
            putExtra(AlarmScheduler.EXTRA_SHOW_STICKY, currentShowSticky)
            putExtra(AlarmScheduler.EXTRA_MOTIVATION_LINE, currentMotivationLine)
            putExtra(AlarmScheduler.EXTRA_CHECKLIST_JSON, currentChecklistJson)
            if (currentTaskStatus != null) {
                putExtra(AlarmScheduler.EXTRA_TASK_STATUS, currentTaskStatus)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.alarm_channel_desc)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        releasePlayer()
        stopVibration()
        serviceScope.cancel()
        super.onDestroy()
    }
}
