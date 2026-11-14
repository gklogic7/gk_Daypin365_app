package com.example.quotes

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.PreferencesManager
import com.example.data.repository.DailyQuote
import com.example.data.repository.MotivationProvider
import com.example.data.repository.QuoteCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class QuoteNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "daypin_daily_quotes_channel"
        private const val NOTIFICATION_ID = 7301
        private const val PREF_FILE = "daypin_quote_history"
        private const val KEY_LAST_QUOTE = "last_quote_text"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = PreferencesManager(appContext).preferencesFlow.first()
                val requestedCategory = intent.getStringExtra(QuoteNotificationScheduler.EXTRA_QUOTE_CATEGORY)
                    ?.let { runCatching { QuoteCategory.valueOf(it) }.getOrNull() }
                    ?: prefs.quoteCategory

                val historyPrefs = appContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                val lastQuote = historyPrefs.getString(KEY_LAST_QUOTE, null)
                val selected = MotivationProvider.randomQuoteEntry(
                    category = requestedCategory,
                    exclude = lastQuote
                )
                historyPrefs.edit().putString(KEY_LAST_QUOTE, selected.text).apply()

                showQuoteNotification(appContext, selected)

                if (action == QuoteNotificationScheduler.ACTION_DELIVER_QUOTE && prefs.quoteNotificationsEnabled) {
                    QuoteNotificationScheduler.reschedule(appContext, prefs)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showQuoteNotification(context: Context, quote: DailyQuote) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.quote_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.quote_channel_desc)
            }
            nm.createNotificationChannel(channel)
        }

        val openAppIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextQuoteIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID + 1,
            Intent(context, QuoteNotificationReceiver::class.java).apply {
                action = QuoteNotificationScheduler.ACTION_NEXT_QUOTE_NOW
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("${quote.category.label} • Daily Quote")
            .setContentText("\"${quote.text}\"")
            .setStyle(NotificationCompat.BigTextStyle().bigText("\"${quote.text}\""))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .addAction(0, context.getString(R.string.action_next_quote), nextQuoteIntent)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }
}
