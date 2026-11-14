package com.example.quotes

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.AppPreferences
import com.example.data.local.QuoteScheduleMode
import com.example.data.repository.QuoteCategory
import java.util.Calendar
import kotlin.random.Random

object QuoteNotificationScheduler {
    const val ACTION_DELIVER_QUOTE = "com.example.action.DELIVER_DAILY_QUOTE"
    const val ACTION_NEXT_QUOTE_NOW = "com.example.action.NEXT_QUOTE_NOW"
    const val EXTRA_QUOTE_CATEGORY = "extra_quote_category"
    private const val REQUEST_CODE_QUOTE_TICK = 910_001

    fun computeNextQuoteTriggerMillis(
        prefs: AppPreferences,
        nowMillis: Long = System.currentTimeMillis(),
        random: Random = Random.Default
    ): Long {
        if (prefs.quoteScheduleMode == QuoteScheduleMode.CUSTOM_INTERVAL) {
            val mins = prefs.quoteIntervalMinutes.coerceIn(1, 720)
            return nowMillis + mins * 60_000L
        }

        // RANDOM_PER_DAY: spread across active daytime window (08:00 - 21:00 = 780 minutes)
        val perDay = prefs.quoteRandomPerDay.coerceIn(1, 20)
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val dayStart = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val windowTotalMs = 13L * 60L * 60_000L // 08:00 to 21:00
        val slotMs = (windowTotalMs / perDay).coerceAtLeast(60_000L)

        for (slotIdx in 0 until perDay) {
            val slotStart = dayStart + slotIdx * slotMs
            val slotEnd = (slotStart + slotMs).coerceAtMost(dayStart + windowTotalMs)
            if (slotEnd > nowMillis + 60_000L) {
                val lower = maxOf(slotStart, nowMillis + 60_000L)
                val span = (slotEnd - lower).coerceAtLeast(1L)
                return lower + random.nextLong(span)
            }
        }

        // Tomorrow's first randomized slot starting at 08:00
        cal.timeInMillis = dayStart
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowStart = cal.timeInMillis
        val firstSpan = (slotMs - 60_000L).coerceAtLeast(1L)
        return tomorrowStart + 60_000L + random.nextLong(firstSpan)
    }

    fun reschedule(context: Context, prefs: AppPreferences) {
        if (!prefs.quoteNotificationsEnabled) {
            cancel(context)
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = computeNextQuoteTriggerMillis(prefs)
        val pending = buildTickPendingIntent(context)
        alarmManager.cancel(pending)

        val canUseExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canUseExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pending
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pending
            )
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildTickPendingIntent(context))
    }

    fun sendQuoteNow(context: Context, category: QuoteCategory) {
        val intent = Intent(context, QuoteNotificationReceiver::class.java).apply {
            action = ACTION_NEXT_QUOTE_NOW
            putExtra(EXTRA_QUOTE_CATEGORY, category.name)
        }
        context.sendBroadcast(intent)
    }

    private fun buildTickPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, QuoteNotificationReceiver::class.java).apply {
            action = ACTION_DELIVER_QUOTE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_QUOTE_TICK,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
